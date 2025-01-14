package org.guardiananticheat.guardianac.checks.combat;

import org.bukkit.Location;
import org.bukkit.entity.Entity; import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler; import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import org.guardiananticheat.guardianac.GuardianAC;
import org.guardiananticheat.guardianac.utils.AlertsUtil;

public class HitBoxCheck implements Listener {

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (entity instanceof Player) {
            Player attacker = (Player) entity;
            Entity damagedEntity = event.getEntity();
            if (damagedEntity instanceof Player) {
                Player target = (Player) damagedEntity;
                if (attacker.isOp()) {
                    return;
                }
                double angle = calculateAngle(attacker, target);
                if (angle > 120.0D) {
                    AlertsUtil.alert(attacker, "KillAura Detected");
                }
            }
        }

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        if (player.isOp() || !GuardianAC.getPluginConfig().getBoolean("detections.hitbox", true)) {
            return;
        }

        Location playerLocation = player.getLocation();
        Location targetLocation = target.getLocation();

        double distance = playerLocation.distance(targetLocation);
        double maxReach = 4.0;

        if (distance > maxReach) {
            AlertsUtil.alert(player, "Hitbox Cheat Detected (Reach)");
            return;
        }

        double angle = calculateAngle(player, target);
        double maxAngle = 60.0;

        if (angle > maxAngle) {
            AlertsUtil.alert(player, "Hitbox Cheat Detected (Invalid Angle)");
        }
    }

    private double calculateAngle(Player attacker, Player target) {
        Vector attackerDirection = attacker.getLocation().getDirection().normalize();
        Vector toTarget = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
        double angle = Math.toDegrees(attackerDirection.angle(toTarget));
        return angle;
    }
}