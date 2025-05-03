package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.potion.*;
import org.guardiananticheat.guardianac.*;
import org.guardiananticheat.guardianac.utils.*;

import java.lang.reflect.*;
import java.util.*;

public class TimerCheck implements Listener {

    private final GuardianAC plugin;
    private final Map<UUID, TimerData> playerData = new HashMap<>();

    // Configuration values
    private final int MIN_INTERVAL = 45; // Minimum allowed time between packets (ms)
    private final int VL_THRESHOLD = 8;  // Violations needed before flagging
    private final long RESET_TIME = 5000; // Time to reset violations (ms)
    private final double PING_MULTIPLIER = 1.5; // Allow more leniency for high ping

    // Grace periods (ms)
    private final long DAMAGE_GRACE = 1000;
    private final long TELEPORT_GRACE = 500;
    private final long VELOCITY_GRACE = 750;

    public TimerCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().equals(event.getTo())) return; // Ignore head rotation

        UUID uuid = player.getUniqueId();
        playerData.putIfAbsent(uuid, new TimerData());
        TimerData data = playerData.get(uuid);

        if (shouldSkipCheck(player, data)) {
            data.reset();
            return;
        }

        long now = System.currentTimeMillis();
        data.updateMovement(now);

        if (data.hasGracePeriod(now)) return;

        checkTimer(player, data, now);
    }

    private boolean shouldSkipCheck(Player player, TimerData data) {
        return player.isOp() ||
                player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR ||
                player.isFlying() ||
                player.hasPotionEffect(PotionEffectType.SPEED) ||
                player.getLocation().getBlock().isLiquid();
    }

    private void checkTimer(Player player, TimerData data, long currentTime) {
        long lastMove = data.getLastMoveTime();
        long interval = currentTime - lastMove;

        int ping = getPlayerPing(player);
        double allowedInterval = MIN_INTERVAL * (ping > 200 ? PING_MULTIPLIER : 1.0);

        if (interval < allowedInterval) {
            data.incrementViolationLevel();

            if (data.getViolationLevel() >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] Timer detected: %s | Interval: %dms | Min: %dms | VL: %d | Ping: %d",
                        player.getName(),
                        interval,
                        (int)allowedInterval,
                        data.getViolationLevel(),
                        ping
                );

                sendAlert(player, alertMsg);
                data.resetViolationLevel();
            }
        } else {
            data.decrementViolationLevel();
        }
    }

    private int getPlayerPing(Player player) {
        try {
            // Modern versions (1.16+)
            Method getPing = Player.class.getMethod("getPing");
            return (int) getPing.invoke(player);
        } catch (Exception e1) {
            try {
                // Old Spigot versions
                Object spigot = player.getClass().getMethod("spigot").invoke(player);
                Method getPing = spigot.getClass().getMethod("getPing");
                return (int) getPing.invoke(spigot);
            } catch (Exception e2) {
                // Reflection fallback
                try {
                    Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
                    Field pingField = entityPlayer.getClass().getField("ping");
                    return pingField.getInt(entityPlayer);
                } catch (Exception e3) {
                    return 0; // Default if all methods fail
                }
            }
        }
    }

    private void sendAlert(Player player, String message) {
        Bukkit.getLogger().warning(message);
        if (plugin.getConfig().getBoolean("alerts.broadcast")) {
            Bukkit.broadcast(message, "guardianac.alerts");
        }
    }

    private class TimerData {
        private long lastMoveTime;
        private long lastDamageTime;
        private long lastTeleportTime;
        private long lastVelocityTime;
        private int violationLevel;

        public void updateMovement(long currentTime) {
            this.lastMoveTime = currentTime;
        }

        public boolean hasGracePeriod(long currentTime) {
            return currentTime - lastDamageTime < DAMAGE_GRACE ||
                    currentTime - lastTeleportTime < TELEPORT_GRACE ||
                    currentTime - lastVelocityTime < VELOCITY_GRACE;
        }

        public void incrementViolationLevel() { this.violationLevel++; }
        public void decrementViolationLevel() { if (this.violationLevel > 0) this.violationLevel--; }
        public void resetViolationLevel() { this.violationLevel = 0; }
        public void reset() {
            this.violationLevel = 0;
            this.lastDamageTime = 0;
            this.lastTeleportTime = 0;
            this.lastVelocityTime = 0;
        }
        public long getLastMoveTime() { return lastMoveTime; }
        public int getViolationLevel() { return violationLevel; }
    }
}