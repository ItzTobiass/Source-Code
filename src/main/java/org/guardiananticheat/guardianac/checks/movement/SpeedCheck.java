package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.potion.*;
import org.bukkit.util.*;
import org.guardiananticheat.guardianac.*;
import org.guardiananticheat.guardianac.utils.*;

import java.util.*;

public class SpeedCheck implements Listener {

    private final GuardianAC plugin;
    private final Map<UUID, PlayerData> playerData = new HashMap<>();
    private final int VL_THRESHOLD = 7;
    private final double BASE_SPEED = 0.2873;

    // Grace periods in ticks
    private final int DAMAGE_GRACE = 20;
    private final int TELEPORT_GRACE = 10;
    private final int VELOCITY_GRACE = 15;

    public SpeedCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Initialize player data if not present
        playerData.putIfAbsent(uuid, new PlayerData());
        PlayerData data = playerData.get(uuid);

        // Skip checks for players who should be excluded
        if (shouldSkipCheck(player, data)) {
            data.resetViolations();
            return;
        }

        // Update movement data
        data.updateMovement(event);

        // Skip during grace periods
        if (data.hasGracePeriod()) {
            data.decrementGrace();
            return;
        }

        // Main detection logic
        checkSpeed(player, event, data);
    }

    private boolean shouldSkipCheck(Player player, PlayerData data) {
        return player.isOp() ||
                player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR ||
                player.isFlying() ||
                player.isInsideVehicle() ||
                player.hasPotionEffect(PotionEffectType.SPEED) ||
                player.getLocation().getBlock().isLiquid() ||
                player.getLocation().getBlock().getType().toString().contains("SLAB") ||
                player.getLocation().getBlock().getType().toString().contains("STAIRS");
    }

    private void checkSpeed(Player player, PlayerMoveEvent event, PlayerData data) {
        Location from = event.getFrom();
        Location to = event.getTo();

        // Calculate horizontal distance only
        double distance = Math.sqrt(Math.pow(to.getX() - from.getX(), 2) + Math.pow(to.getZ() - from.getZ(), 2));

        // Get allowed speed based on player state
        double maxAllowed = calculateMaxAllowedSpeed(player, data);

        if (distance > maxAllowed) {
            data.incrementViolationLevel();

            if (data.getViolationLevel() >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] Speed detected: %s | Dist: %.3f | Max: %.3f | Ping: %d | Ground: %b | Sprint: %b",
                        player.getName(),
                        distance,
                        maxAllowed,
                        player.getPing(),
                        player.isOnGround(),
                        player.isSprinting()
                );

                AlertsUtil.alert(player, alertMsg);
                data.resetViolationLevel();

                // Apply punishment if configured
                if (plugin.getConfig().getBoolean("punishments.speed.enabled")) {
                    // Add your punishment logic here
                }
            }
        } else {
            // Decrease VL when not detecting violations
            data.decrementViolationLevel();
        }
    }

    private double calculateMaxAllowedSpeed(Player player, PlayerData data) {
        double speed = BASE_SPEED;

        // Apply modifiers based on player state
        if (player.isSprinting()) speed *= 1.3;
        if (!player.isOnGround()) speed *= 1.2;
        if (player.isGliding()) speed *= 1.5;

        // Ice and other special blocks
        Material under = player.getLocation().subtract(0, 0.1, 0).getBlock().getType();
        if (under == Material.ICE || under == Material.PACKED_ICE || under == Material.BLUE_ICE) {
            speed *= 1.6;
        }

        // Slabs and stairs
        if (under.toString().contains("SLAB") || under.toString().contains("STAIRS")) {
            speed *= 1.15;
        }

        // Recent damage gives temporary speed boost
        if (data.getDamageGrace() > 0) {
            speed *= 1.25;
        }

        // Add small random factor to prevent false positives
        speed *= 1 + (Math.random() * 0.05);

        return speed;
    }

    private class PlayerData {
        private int violationLevel = 0;
        private int damageGrace = 0;
        private int teleportGrace = 0;
        private int velocityGrace = 0;
        private Location lastLocation;

        public void updateMovement(PlayerMoveEvent event) {
            this.lastLocation = event.getFrom();
        }

        public boolean hasGracePeriod() {
            return damageGrace > 0 || teleportGrace > 0 || velocityGrace > 0;
        }

        public void decrementGrace() {
            if (damageGrace > 0) damageGrace--;
            if (teleportGrace > 0) teleportGrace--;
            if (velocityGrace > 0) velocityGrace--;
        }

        public void incrementViolationLevel() {
            violationLevel++;
        }

        public void decrementViolationLevel() {
            if (violationLevel > 0) violationLevel--;
        }

        public void resetViolationLevel() {
            violationLevel = 0;
        }

        public void resetViolations() {
            violationLevel = 0;
            damageGrace = 0;
            teleportGrace = 0;
            velocityGrace = 0;
        }

        public int getViolationLevel() {
            return violationLevel;
        }

        public int getDamageGrace() {
            return damageGrace;
        }
    }
}