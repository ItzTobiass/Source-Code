package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.guardiananticheat.guardianac.utils.AlertsUtil;
import org.guardiananticheat.guardianac.GuardianAC;

import java.util.HashMap;
import java.util.Map;

public class FlyCheck implements Listener {
    private final GuardianAC plugin;
    private final Map<Player, Long> alertCooldowns = new HashMap<>();

    public FlyCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.isOp() || player.getAllowFlight() || player.isFlying()) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from == null) return;

        double deltaY = to.getY() - from.getY();
        double verticalVelocity = player.getVelocity().getY();

        Material blockBelow = to.clone().subtract(0, 1, 0).getBlock().getType();
        boolean isOnGround = blockBelow != Material.AIR;

        if (!isOnGround && deltaY > 0 && verticalVelocity > 0.42) {
            long currentTime = System.currentTimeMillis();
            long lastAlertTime = alertCooldowns.getOrDefault(player, 0L);

            if (currentTime - lastAlertTime > 5000) {
                AlertsUtil.alert(player, "Fly Hack Detected");
                alertCooldowns.put(player, currentTime);
            }
        }
    }
}