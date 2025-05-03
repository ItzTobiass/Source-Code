package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.potion.*;
import org.guardiananticheat.guardianac.*;
import org.guardiananticheat.guardianac.utils.*;

import java.util.*;

public class NoSlowCheck implements Listener {

    private final GuardianAC plugin;
    private final Map<UUID, NoSlowData> playerData = new HashMap<>();

    // Configuration values
    private final double NORMAL_SPEED = 0.1; // Normal walking speed
    private final double BLOCKING_SPEED = 0.03; // Expected speed while blocking
    private final double EATING_SPEED = 0.02; // Expected speed while eating
    private final double BOW_SPEED = 0.05; // Expected speed while drawing bow
    private final int VL_THRESHOLD = 5; // Violations needed before flagging
    private final long RESET_TIME = 3000; // Time to reset violations (ms)

    public NoSlowCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().equals(event.getTo())) return; // Ignore head rotation

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Initialize player data if not present
        playerData.putIfAbsent(uuid, new NoSlowData());
        NoSlowData data = playerData.get(uuid);

        // Skip checks for exempt players
        if (shouldSkipCheck(player, data)) {
            data.reset();
            return;
        }

        // Update movement data
        data.updateMovement(event);

        // Main detection logic
        checkNoSlow(player, event, data);
    }

    private boolean shouldSkipCheck(Player player, NoSlowData data) {
        return player.isOp() ||
                player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR ||
                player.isFlying() ||
                player.hasPotionEffect(PotionEffectType.SPEED) ||
                player.getLocation().getBlock().isLiquid() ||
                player.isInsideVehicle() ||
                data.getLastVelocityTime() + 1000 > System.currentTimeMillis();
    }

    private void checkNoSlow(Player player, PlayerMoveEvent event, NoSlowData data) {
        double horizontalDistance = MovementUtils.getHorizontalDistance(event.getFrom(), event.getTo());
        double maxAllowed = getMaxAllowedSpeed(player);

        if (horizontalDistance > maxAllowed) {
            data.incrementViolationLevel();

            if (data.getViolationLevel() >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] NoSlow detected: %s | Speed: %.3f | Max: %.3f | Action: %s | Ping: %d",
                        player.getName(),
                        horizontalDistance,
                        maxAllowed,
                        getSlowingAction(player),
                        player.getPing()
                );

                sendAlert(player, alertMsg);
                data.resetViolationLevel();
            }
        } else {
            data.decrementViolationLevel();
        }
    }

    private double getMaxAllowedSpeed(Player player) {
        double speed = NORMAL_SPEED;

        // Apply slowing modifiers
        if (isBlocking(player)) {
            speed = BLOCKING_SPEED;
        } else if (isEating(player)) {
            speed = EATING_SPEED;
        } else if (isDrawingBow(player)) {
            speed = BOW_SPEED;
        }

        // Apply speed potion effect if present
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            int level = player.getPotionEffect(PotionEffectType.SPEED).getAmplifier() + 1;
            speed *= 1.0 + (0.2 * level);
        }

        // Add small buffer to prevent false positives
        speed *= 1.2;

        return speed;
    }

    private boolean isBlocking(Player player) {
        return player.isBlocking() ||
                player.getInventory().getItemInMainHand().getType() == Material.SHIELD ||
                player.getInventory().getItemInOffHand().getType() == Material.SHIELD;
    }

    private boolean isEating(Player player) {
        return player.isHandRaised() &&
                (player.getInventory().getItemInMainHand().getType().isEdible() ||
                        player.getInventory().getItemInOffHand().getType().isEdible());
    }

    private boolean isDrawingBow(Player player) {
        return player.isHandRaised() &&
                (player.getInventory().getItemInMainHand().getType() == Material.BOW ||
                        player.getInventory().getItemInOffHand().getType() == Material.BOW);
    }

    private String getSlowingAction(Player player) {
        if (isBlocking(player)) return "Blocking";
        if (isEating(player)) return "Eating";
        if (isDrawingBow(player)) return "Bow Drawing";
        return "None";
    }

    private void sendAlert(Player player, String message) {
        Bukkit.getLogger().warning(message);
        if (plugin.getConfig().getBoolean("alerts.broadcast")) {
            Bukkit.broadcast(message, "guardianac.alerts");
        }
    }

    private static class NoSlowData {
        private int violationLevel = 0;
        private long lastVelocityTime = 0;

        public void updateMovement(PlayerMoveEvent event) {
            // Can be expanded to track more movement data
        }

        public void incrementViolationLevel() { this.violationLevel++; }
        public void decrementViolationLevel() { if (this.violationLevel > 0) this.violationLevel--; }
        public void resetViolationLevel() { this.violationLevel = 0; }
        public void reset() { this.violationLevel = 0; }
        public int getViolationLevel() { return violationLevel; }
        public long getLastVelocityTime() { return lastVelocityTime; }
    }
}