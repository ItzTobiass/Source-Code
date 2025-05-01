package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.guardiananticheat.guardianac.utils.AlertsUtil;
import org.guardiananticheat.guardianac.GuardianAC;

import java.util.HashMap;
import java.util.Map;

public class ScaffoldCheck implements Listener {

    private final GuardianAC plugin;
    private final Map<Player, Integer> blockPlaceCount = new HashMap<>();

    public ScaffoldCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            return;
        }

        if (event.getBlockPlaced().getType() == Material.SCAFFOLDING) {
            blockPlaceCount.put(player, blockPlaceCount.getOrDefault(player, 0) + 1);
            if (blockPlaceCount.get(player) > 5) {
                AlertsUtil.alert(player, "Scaffold Hack Detected - Too many scaffold blocks placed!");
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            return;
        }

        Vector from = event.getFrom().toVector();
        Vector to = event.getTo().toVector();
        double distance = from.distance(to);
        boolean isOnGround = player.isOnGround();

        if (isOnGround) {
            blockPlaceCount.remove(player);
        }
    }
}
