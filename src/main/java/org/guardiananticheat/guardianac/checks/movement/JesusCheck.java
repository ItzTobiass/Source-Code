package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class JesusCheck implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Block blockBelow = player.getLocation().subtract(0, 0.01, 0).getBlock();

        if ((blockBelow.getType() == Material.WATER || blockBelow.getType() == Material.KELP)
                && !player.isSwimming() && !player.isFlying() && player.getVelocity().getY() == 0) {
            Bukkit.getLogger().warning("[GuardianAC] Jesus: " + player.getName() + " might be walking on water.");
        }
    }
}
