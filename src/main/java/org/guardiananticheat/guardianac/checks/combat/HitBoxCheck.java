package org.guardiananticheat.guardianac.checks.combat;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import org.guardiananticheat.guardianac.GuardianAC;
import org.guardiananticheat.guardianac.utils.MovementUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HitBoxCheck implements Listener {

    private GuardianAC plugin = null;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final double MAX_REACH = 3.5;
    private final double MAX_ANGLE = 60.0;
    private final double MAX_HITBOX_EXPANSION = 0.4;
    private final int VL_THRESHOLD = 5;

    public HitBoxCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Player target)) {
            return;
        }

        if (shouldSkipCheck(player)) {
            return;
        }

        PlayerData data = playerDataMap.computeIfAbsent(player.getUniqueId(), k -> new PlayerData());

        checkReach(player, target, data);
        checkAngle(player, target, data);
        checkHitBoxExpansion(player, target, data);
    }

    private boolean shouldSkipCheck(Player player) {
        return player.isOp() ||
                player.hasPermission("guardianac.bypass") ||
                !plugin.getConfig().getBoolean("detections.hitbox", true) ||
                player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR;
    }

    private void checkReach(Player player, Player target, PlayerData data) {
        Location playerLoc = player.getEyeLocation();
        Location targetLoc = target.getLocation();

        double distance = MovementUtils.getDistance3D(playerLoc, targetLoc);
        double maxReach = MAX_REACH;

        if (player.getInventory().getItemInMainHand().getType().toString().contains("SWORD")) {
            maxReach += 0.5;
        }

        if (distance > maxReach) {
            data.incrementReachVL();

            if (data.getReachVL() >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] HitBox Reach detected: %s -> %s | Distance: %.2f | Max: %.2f | Ping: %d",
                        player.getName(),
                        target.getName(),
                        distance,
                        maxReach,
                        player.getPing()
                );

                sendAlert(player, alertMsg);
                data.resetReachVL();
            }
        } else {
            data.decrementReachVL();
        }
    }

    private void checkAngle(Player player, Player target, PlayerData data) {
        double angle = calculateAngle(player, target);

        if (angle > MAX_ANGLE) {
            data.incrementAngleVL();

            if (data.getAngleVL() >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] HitBox Angle detected: %s -> %s | Angle: %.1f° | Max: %.1f°",
                        player.getName(),
                        target.getName(),
                        angle,
                        MAX_ANGLE
                );

                sendAlert(player, alertMsg);
                data.resetAngleVL();
            }
        } else {
            data.decrementAngleVL();
        }
    }

    private void checkHitBoxExpansion(Player player, Player target, PlayerData data) {
        Location playerLoc = player.getEyeLocation();
        Location targetLoc = target.getLocation();
        Vector direction = playerLoc.getDirection();
        Vector toTarget = targetLoc.toVector().subtract(playerLoc.toVector());

        double distance = toTarget.length();
        double hitboxExpansion = distance - toTarget.normalize().dot(direction) * distance;

        if (hitboxExpansion > MAX_HITBOX_EXPANSION) {
            data.incrementHitBoxVL();

            if (data.getHitBoxVL() >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] HitBox Expansion detected: %s -> %s | Expansion: %.2f | Max: %.2f",
                        player.getName(),
                        target.getName(),
                        hitboxExpansion,
                        MAX_HITBOX_EXPANSION
                );

                sendAlert(player, alertMsg);
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

    public static class PlayerData {
        private int reachVL = 0;
        private int angleVL = 0;
        private int hitBoxVL = 0;

        public void incrementReachVL() {
            this.reachVL++;
        }

        public void decrementReachVL() {
            if (this.reachVL > 0) this.reachVL--;
        }

        public void resetReachVL() {
            this.reachVL = 0;
        }

        public int getReachVL() {
            return reachVL;
        }

        public void incrementAngleVL() {
            this.angleVL++;
        }

        public void decrementAngleVL() {
            if (this.angleVL > 0) this.angleVL--;
        }

        public void resetAngleVL() {
            this.angleVL = 0;
        }

        public int getAngleVL() {
            return angleVL;
        }

        public void incrementHitBoxVL() {
            this.hitBoxVL++;
        }

        public void decrementHitBoxVL() {
            if (this.hitBoxVL > 0) this.hitBoxVL--;
        }

        public void resetHitBoxVL() {
            this.hitBoxVL = 0;
        }

        public int getHitBoxVL() {
            return hitBoxVL;
        }
    }
}