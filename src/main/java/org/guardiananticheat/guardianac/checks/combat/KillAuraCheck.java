package org.guardiananticheat.guardianac.checks.combat;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import org.guardiananticheat.guardianac.GuardianAC;
import org.guardiananticheat.guardianac.utils.AlertsUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KillAuraCheck implements Listener {
    private final GuardianAC plugin;
    private final Map<UUID, Long> lastAttackTime = new HashMap<>();
    private static final long ATTACK_COOLDOWN = 1000;
    private static final double MAX_DISTANCE = 4.0;
    private static final double MAX_ANGLE = 60.0;

    public KillAuraCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (!(entity instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player target)) return;

        if (attacker.isOp() || !GuardianAC.getPluginConfig().getBoolean("detections.hitbox", true)) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (lastAttackTime.containsKey(attacker.getUniqueId())) {
            long lastAttack = lastAttackTime.get(attacker.getUniqueId());
            if (currentTime - lastAttack < ATTACK_COOLDOWN) {
                return;
            }
        }

        lastAttackTime.put(attacker.getUniqueId(), currentTime);

        Location playerLocation = attacker.getLocation();
        Location targetLocation = target.getLocation();

        double distance = playerLocation.distance(targetLocation);
        if (distance > MAX_DISTANCE) {
            AlertsUtil.alert(attacker, "KillAura Detected: Hitbox Cheat (Reach)");
            return;
        }

        double angle = calculateAngle(attacker, target);
        if (angle > MAX_ANGLE) {
            AlertsUtil.alert(attacker, "KillAura Detected: Hitbox Cheat (Invalid Angle)");
        }
    }

    private double calculateAngle(Player attacker, Player target) {
        Vector attackerDirection = attacker.getLocation().getDirection().normalize();
        Vector toTarget = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
        return Math.toDegrees(attackerDirection.angle(toTarget));
    }
}