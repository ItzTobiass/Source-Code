package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.potion.*;
import org.guardiananticheat.guardianac.*;
import org.guardiananticheat.guardianac.utils.*;

import java.util.*;

public class NoFallCheck implements Listener {

    private final GuardianAC plugin;
    private final Map<UUID, NoFallData> playerData = new HashMap<>();
    private final int VL_THRESHOLD = 5;
    private final float MIN_FALL_DISTANCE = 3.5f;
    private final long GRACE_PERIOD = 2000; // 2 seconds after teleport/velocity

    public NoFallCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        UUID uuid = player.getUniqueId();
        playerData.putIfAbsent(uuid, new NoFallData());
        NoFallData data = playerData.get(uuid);

        if (shouldSkipCheck(player, data)) {
            data.reset();
            return;
        }

        checkNoFall(player, event, data);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        playerData.putIfAbsent(uuid, new NoFallData());
        NoFallData data = playerData.get(uuid);

        // Update last ground position
        if (player.isOnGround()) {
            data.setLastGroundLocation(player.getLocation());
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        playerData.putIfAbsent(uuid, new NoFallData());
        playerData.get(uuid).setLastTeleport(System.currentTimeMillis());
    }

    private boolean shouldSkipCheck(Player player, NoFallData data) {
        return player.isOp() ||
                player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR ||
                player.isFlying() ||
                player.getAllowFlight() ||
                player.hasPotionEffect(PotionEffectType.SLOW_FALLING) ||
                player.isInsideVehicle() ||
                data.getLastTeleport() + GRACE_PERIOD > System.currentTimeMillis() ||
                isInWater(player) ||
                isOnClimbable(player);
    }

    private void checkNoFall(Player player, EntityDamageEvent event, NoFallData data) {
        float fallDistance = player.getFallDistance();
        double damage = event.getDamage();
        Location lastGround = data.getLastGroundLocation();

        // Check if player should have taken fall damage
        if (fallDistance > MIN_FALL_DISTANCE && damage == 0.0) {
            data.incrementViolationLevel();

            if (data.getViolationLevel() >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] NoFall detected: %s | FallDistance: %.1f | ExpectedDamage: %.1f | Y-Diff: %.1f | VL: %d",
                        player.getName(),
                        fallDistance,
                        calculateExpectedDamage(fallDistance),
                        lastGround != null ? lastGround.getY() - player.getLocation().getY() : 0,
                        data.getViolationLevel()
                );

                sendAlert(player, alertMsg);
                data.resetViolationLevel();

                // Optionally cancel the event if you want to enforce damage
                // event.setDamage(calculateExpectedDamage(fallDistance));
            }
        } else {
            data.decrementViolationLevel();
        }
    }

    private double calculateExpectedDamage(float fallDistance) {
        return Math.max(0, fallDistance - 3.0) * 2.0; // Standard Minecraft fall damage calculation
    }

    private boolean isInWater(Player player) {
        return player.getLocation().getBlock().isLiquid() ||
                player.getEyeLocation().getBlock().isLiquid();
    }

    private boolean isOnClimbable(Player player) {
        Material blockType = player.getLocation().getBlock().getType();
        return blockType == Material.LADDER ||
                blockType == Material.VINE ||
                blockType == Material.SCAFFOLDING ||
                blockType.toString().contains("CLIMBABLE");
    }

    private void sendAlert(Player player, String message) {
        Bukkit.getLogger().warning(message);
        if (plugin.getConfig().getBoolean("alerts.broadcast")) {
            Bukkit.broadcast(message, "guardianac.alerts");
        }
    }

    private static class NoFallData {
        private int violationLevel = 0;
        private long lastTeleport = 0;
        private Location lastGroundLocation;

        public void incrementViolationLevel() { this.violationLevel++; }
        public void decrementViolationLevel() { if (this.violationLevel > 0) this.violationLevel--; }
        public void resetViolationLevel() { this.violationLevel = 0; }
        public void reset() {
            this.violationLevel = 0;
            this.lastTeleport = 0;
            this.lastGroundLocation = null;
        }
        public int getViolationLevel() { return violationLevel; }
        public long getLastTeleport() { return lastTeleport; }
        public void setLastTeleport(long time) { this.lastTeleport = time; }
        public Location getLastGroundLocation() { return lastGroundLocation; }
        public void setLastGroundLocation(Location loc) { this.lastGroundLocation = loc; }
    }
}