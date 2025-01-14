package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.guardiananticheat.guardianac.utils.AlertsUtil;
import org.guardiananticheat.guardianac.GuardianAC;

import java.util.HashMap; import java.util.Map;

public class NoSlowCheck implements Listener { private final GuardianAC plugin; private final Map<Player, Boolean> isEatingMap = new HashMap<>();

    public NoSlowCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNoSlow(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;

        double speed = event.getFrom().distance(event.getTo());
        Boolean isEating = isEatingMap.getOrDefault(player, false);
        if (isEating && speed > 0.2) {
            AlertsUtil.alert(player, "NoSlow Detected: Moving Too Fast While Eating");
        }
    }

    @EventHandler
    public void onPlayerEat(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;

        isEatingMap.put(player, true);
        AlertsUtil.alert(player, "NoSlow Detected: Eating");
    }

    @EventHandler
    public void onPlayerStopEat(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            isEatingMap.put(player, false);
        }, 20L);
    }
}