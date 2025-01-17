package org.guardiananticheat.guardianac.checks.combat;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import org.guardiananticheat.guardianac.utils.AlertsUtil;

public class criticals implements Listener {

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        if (player.isOp()) return;

        boolean isCritical = player.getFallDistance() > 0.0F &&
                !player.isOnGround() &&
                !player.isInsideVehicle() &&
                !player.hasPotionEffect(PotionEffectType.BLINDNESS) &&
                player.getLocation().getBlock().getType() == Material.AIR;

        if (!isCritical && event.getDamage() > 0) {
            AlertsUtil.alert(player, "Criticals Detected");
        }
    }
}

