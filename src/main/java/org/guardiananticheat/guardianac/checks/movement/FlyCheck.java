package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.guardiananticheat.guardianac.GuardianAC;
import org.guardiananticheat.guardianac.utils.MovementUtils;
import org.guardiananticheat.guardianac.utils.PlayerData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlyCheck implements Listener {

    private final GuardianAC plugin;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final double HOVER_THRESHOLD = 0.005;
    private final int VL_THRESHOLD = 5;
    private final int AIR_TICKS_THRESHOLD = 15;
    private final int GRACE_PERIOD_TICKS = 10;

    public FlyCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        playerDataMap.putIfAbsent(uuid, new PlayerData());
        PlayerData data = playerDataMap.get(uuid);

        if (shouldSkipCheck(player)) {
            data.reset();
            return;
        }

        data.onMove(event);

        if (data.getGraceTicks() > 0) {
            data.decrementGraceTicks();
            return;
        }

        if (System.currentTimeMillis() - data.getLastDamageTime() < 1000) {
            return;
        }

        if (isHovering(player, data)) {
            sendAlert(player, "Hovering", data);
        }

        if (isFlyingIllegally(player, data)) {
            sendAlert(player, "Flying", data);
        }
    }

    private void sendAlert(Player player, String type, PlayerData data) {
        data.incrementViolationLevel();

        if (data.getViolationLevel() >= VL_THRESHOLD) {
            String alertMessage = String.format(
                    "[GuardianAC] Flight %s detected: %s | VL: %d | AirTicks: %d | Y-Vel: %.3f | Location: %s",
                    type,
                    player.getName(),
                    data.getViolationLevel(),
                    data.getAirTicks(),
                    player.getVelocity().getY(),
                    formatLocation(player.getLocation())
            );

            Bukkit.getLogger().warning(alertMessage);

            if (plugin.getConfig().getBoolean("alerts.broadcast")) {
                Bukkit.broadcast(alertMessage, "guardianac.alerts");
            }

            data.resetViolationLevel();
        }
    }

    private String formatLocation(Location loc) {
        return String.format("X: %.1f, Y: %.1f, Z: %.1f", loc.getX(), loc.getY(), loc.getZ());
    }

    private boolean shouldSkipCheck(Player player) {
        return player.isFlying() ||
                player.getAllowFlight() ||
                player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR ||
                player.isInsideVehicle() ||
                player.hasPotionEffect(PotionEffectType.LEVITATION) ||
                player.getLocation().getBlock().isLiquid();
    }

    private boolean isHovering(Player player, PlayerData data) {
        Vector velocity = player.getVelocity();
        Location from = data.getFromLocation();
        Location to = player.getLocation();

        return Math.abs(velocity.getY()) < HOVER_THRESHOLD &&
                !player.isOnGround() &&
                data.getAirTicks() > AIR_TICKS_THRESHOLD &&
                Math.abs(from.getY() - to.getY()) < 0.001;
    }

    private boolean isFlyingIllegally(Player player, PlayerData data) {
        Vector velocity = player.getVelocity();
        Location to = player.getLocation();

        boolean inAir = isInAir(player, to);

        if (inAir) {
            data.incrementAirTicks();
        } else {
            data.resetAirTicks();
            return false;
        }

        if (velocity.getY() > 0 && !data.isJumping() && data.getAirTicks() > 2 && !isBlockAbove(player)) {
            return true;
        }

        if (velocity.getY() < 0 && Math.abs(velocity.getY()) < 0.08 && data.getAirTicks() > 20) {
            return true;
        }

        return velocity.getY() == 0 &&
                data.getAirTicks() > 10 &&
                MovementUtils.getHorizontalDistance(data.getFromLocation(), to) > 0.2;
    }

    private boolean isInAir(Player player, Location location) {
        if (player.isOnGround()) return false;

        for (int i = 0; i < 3; i++) {
            Block block = location.clone().subtract(0, i, 0).getBlock();
            if (!block.isPassable() && block.getType() != Material.AIR) {
                return false;
            }
        }

        return true;
    }

    private boolean isBlockAbove(Player player) {
        Location loc = player.getLocation();
        for (int i = 0; i < 2; i++) {
            Block block = loc.clone().add(0, i + 1, 0).getBlock();
            if (!block.isPassable() && block.getType() != Material.AIR) {
                return true;
            }
        }
        return false;
    }

    private static class PlayerData {
        private Location fromLocation;
        private int airTicks = 0;
        private int violationLevel = 0;
        private boolean jumping = false;
        private long lastDamageTime = 0;
        private int graceTicks = 0;

        public void onMove(PlayerMoveEvent event) {
            this.fromLocation = event.getFrom();

            if (event.getPlayer().getVelocity().getY() > 0.3 && event.getPlayer().isOnGround()) {
                jumping = true;
            } else if (event.getPlayer().isOnGround()) {
                jumping = false;
            }
        }

        public Location getFromLocation() {
            return fromLocation;
        }

        public int getAirTicks() {
            return airTicks;
        }

        public void incrementAirTicks() {
            this.airTicks++;
        }

        public void resetAirTicks() {
            this.airTicks = 0;
        }

        public int getViolationLevel() {
            return violationLevel;
        }

        public void incrementViolationLevel() {
            this.violationLevel++;
        }

        public void decrementViolationLevel() {
            if (this.violationLevel > 0) {
                this.violationLevel--;
            }
        }

        public void resetViolationLevel() {
            this.violationLevel = 0;
        }

        public boolean isJumping() {
            return jumping;
        }

        public long getLastDamageTime() {
            return lastDamageTime;
        }

        public void setLastDamageTime(long time) {
            this.lastDamageTime = time;
        }

        public int getGraceTicks() {
            return graceTicks;
        }

        public void setGraceTicks(int ticks) {
            this.graceTicks = ticks;
        }

        public void decrementGraceTicks() {
            if (this.graceTicks > 0) {
                this.graceTicks--;
            }
        }

        public void reset() {
            resetAirTicks();
            resetViolationLevel();
            jumping = false;
        }
    }
}