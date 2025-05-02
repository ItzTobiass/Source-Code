package org.guardiananticheat.guardianac.checks.combat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.GameMode;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import org.guardiananticheat.guardianac.GuardianAC;
import org.guardiananticheat.guardianac.utils.MovementUtils;
import org.guardiananticheat.guardianac.utils.PlayerData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillAuraCheck implements Listener {

    private final GuardianAC plugin;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();

    // Konfigurovatelné hodnoty
    private final double MAX_REACH = 3.5;
    private final double MAX_ANGLE = 60.0;
    private final double MAX_HITBOX_EXPANSION = 0.4;
    private final int CPS_THRESHOLD = 15;
    private final int VL_THRESHOLD = 5;
    private static final long TIME_WINDOW = 1000L; // 1 sekunda

    public KillAuraCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player target)) {
            return;
        }

        if (shouldSkipCheck(attacker)) {
            return;
        }

        PlayerData data = playerDataMap.computeIfAbsent(attacker.getUniqueId(), k -> new PlayerData());
        long currentTime = System.currentTimeMillis();

        // Aktualizace počtu útoků
        data.addAttack(currentTime);

        // Hlavní detekční metody
        checkCPS(attacker, data, currentTime);
        checkReach(attacker, target, data);
        checkAngle(attacker, target, data);
        checkHitBoxExpansion(attacker, target, data);
    }

    private boolean shouldSkipCheck(Player player) {
        return player.isOp() ||
                player.hasPermission("guardianac.bypass") ||
                !plugin.getConfig().getBoolean("detections.killaura", true) ||
                player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR;
    }

    private void checkCPS(Player player, PlayerData data, long currentTime) {
        int cps = data.getCPS(currentTime);

        if (cps > CPS_THRESHOLD) {
            data.incrementCPSVL();

            if (data.getCPSVL() >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] KillAura CPS detected: %s | CPS: %d | Max: %d",
                        player.getName(),
                        cps,
                        CPS_THRESHOLD
                );

                sendAlert(player, alertMsg);
                data.resetCPSVL();
            }
        } else {
            data.decrementCPSVL();
        }
    }

    private void checkReach(Player attacker, Player target, PlayerData data) {
        Location attackerLoc = attacker.getEyeLocation();
        Location targetLoc = target.getLocation();

        double distance = MovementUtils.getDistance3D(attackerLoc, targetLoc);
        double maxReach = MAX_REACH;

        if (distance > maxReach) {
            data.incrementReachVL();

            if (data.getReachVL() >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] KillAura Reach detected: %s -> %s | Distance: %.2f | Max: %.2f | Ping: %d",
                        attacker.getName(),
                        target.getName(),
                        distance,
                        maxReach,
                        attacker.getPing()
                );

                sendAlert(attacker, alertMsg);
                data.resetReachVL();
            }
        } else {
            data.decrementReachVL();
        }
    }

    private void checkAngle(Player attacker, Player target, PlayerData data) {
        double angle = calculateAngle(attacker, target);

        if (angle > MAX_ANGLE) {
            data.incrementAngleVL();

            if (data.getAngleVL() >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] KillAura Angle detected: %s -> %s | Angle: %.1f° | Max: %.1f°",
                        attacker.getName(),
                        target.getName(),
                        angle,
                        MAX_ANGLE
                );

                sendAlert(attacker, alertMsg);
                data.resetAngleVL();
            }
        } else {
            data.decrementAngleVL();
        }
    }

    private void checkHitBoxExpansion(Player attacker, Player target, PlayerData data) {
        Location attackerLoc = attacker.getEyeLocation();
        Location targetLoc = target.getLocation();
        Vector direction = attackerLoc.getDirection();
        Vector toTarget = targetLoc.toVector().subtract(attackerLoc.toVector());

        double distance = toTarget.length();
        double hitboxExpansion = distance - toTarget.normalize().dot(direction) * distance;

        if (hitboxExpansion > MAX_HITBOX_EXPANSION) {
            data.incrementHitBoxVL();

            if (data.getHitBoxVL() >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] KillAura HitBox detected: %s -> %s | Expansion: %.2f | Max: %.2f",
                        attacker.getName(),
                        target.getName(),
                        hitboxExpansion,
                        MAX_HITBOX_EXPANSION
                );

                sendAlert(attacker, alertMsg);
                data.resetHitBoxVL();
            }
        } else {
            data.decrementHitBoxVL();
        }
    }

    private double calculateAngle(Player attacker, Player target) {
        Vector attackerDirection = attacker.getEyeLocation().getDirection().normalize();
        Vector toTarget = target.getLocation().toVector().subtract(attacker.getEyeLocation().toVector()).normalize();
        return Math.toDegrees(attackerDirection.angle(toTarget));
    }

    private void sendAlert(Player player, String message) {
        Bukkit.getLogger().warning(message);

        if (plugin.getConfig().getBoolean("alerts.broadcast")) {
            Bukkit.broadcast(message, "guardianac.alerts");
        }
    }

    private static class PlayerData {
        private final Map<Long, Long> attackTimes = new HashMap<>();
        private int reachVL = 0;
        private int angleVL = 0;
        private int hitBoxVL = 0;
        private int cpsVL = 0;
        private long counter = 0;

        public void addAttack(long currentTime) {
            attackTimes.put(counter++, currentTime);
        }

        public int getCPS(long currentTime) {
            attackTimes.values().removeIf(time -> currentTime - time > TIME_WINDOW);
            return attackTimes.size();
        }

        public void incrementReachVL() { this.reachVL++; }
        public void decrementReachVL() { if (this.reachVL > 0) this.reachVL--; }
        public void resetReachVL() { this.reachVL = 0; }
        public int getReachVL() { return reachVL; }

        public void incrementAngleVL() { this.angleVL++; }
        public void decrementAngleVL() { if (this.angleVL > 0) this.angleVL--; }
        public void resetAngleVL() { this.angleVL = 0; }
        public int getAngleVL() { return angleVL; }

        public void incrementHitBoxVL() { this.hitBoxVL++; }
        public void decrementHitBoxVL() { if (this.hitBoxVL > 0) this.hitBoxVL--; }
        public void resetHitBoxVL() { this.hitBoxVL = 0; }
        public int getHitBoxVL() { return hitBoxVL; }

        public void incrementCPSVL() { this.cpsVL++; }
        public void decrementCPSVL() { if (this.cpsVL > 0) this.cpsVL--; }
        public void resetCPSVL() { this.cpsVL = 0; }
        public int getCPSVL() { return cpsVL; }
    }
}