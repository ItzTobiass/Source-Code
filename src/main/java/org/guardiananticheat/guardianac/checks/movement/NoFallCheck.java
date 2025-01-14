package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.guardiananticheat.guardianac.utils.AlertsUtil;

public class NoFallCheck implements Listener {

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.isOp() || player.getAllowFlight()) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            float fallDistance = player.getFallDistance();
            double damage = event.getDamage();

            if (fallDistance > 3.0F && damage == 0.0) {
                AlertsUtil.alert(player, "NoFall Detected");
            }
        }
    }
}