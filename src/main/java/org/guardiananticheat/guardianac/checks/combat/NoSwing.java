package org.guardiananticheat.guardianac.checks.combat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.guardiananticheat.guardianac.utils.AlertsUtil;

import java.util.HashSet;
import java.util.UUID;

public class NoSwing implements Listener {

    private final HashSet<UUID> recentlySwung = new HashSet<>();

    @EventHandler
    public void onPlayerSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
            recentlySwung.add(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        if (player.isOp()) return;

        UUID playerId = player.getUniqueId();
        if (!recentlySwung.contains(playerId)) {
            AlertsUtil.alert(player, "NoSwing Detected");
        } else {
            recentlySwung.remove(playerId);
        }
    }
}

