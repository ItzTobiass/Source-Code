package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.guardiananticheat.guardianac.utils.AlertsUtil;
import org.guardiananticheat.guardianac.GuardianAC;

public class SpeedCheck implements Listener {
    private final GuardianAC plugin;

    public SpeedCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerSpeedCheck(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            return;
        }

        Vector from = event.getFrom().toVector();
        Vector to = event.getTo().toVector();
        double distance = from.distance(to);

        boolean isSprinting = player.isSprinting();
        boolean isJumping = event.getTo().getY() > event.getFrom().getY();
        boolean isOnGround = player.isOnGround();

        double threshold = 0.7D;

        if (isSprinting && isJumping) {
            threshold = 1.0D;
        } else if (isSprinting) {
            threshold = 0.9D;
        } else if (isJumping) {
            threshold = 0.8D;
        }

        if (isOnGround && distance > threshold) {
            AlertsUtil.alert(player, "Speed Hack Detected");
        } else if (!isOnGround && distance > threshold * 1.5) {
            AlertsUtil.alert(player, "Speed Hack Detected (Jumping)");
        }
    }
}