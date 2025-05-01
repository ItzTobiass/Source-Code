package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.guardiananticheat.guardianac.GuardianAC;

public class FlyCheck implements Listener {

    public FlyCheck(GuardianAC guardianAC) {
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.isFlying() || player.getAllowFlight() || player.getGameMode().toString().contains("CREATIVE")) return;

        if (player.getLocation().subtract(0, 0.1, 0).getBlock().getType() == Material.AIR) {
            Vector velocity = player.getVelocity();
            if (velocity.getY() == 0.0 && !player.isOnGround()) {
                Bukkit.getLogger().warning("[GuardianAC] Flight: " + player.getName() + " may be hovering.");
            }
        }
    }
}
