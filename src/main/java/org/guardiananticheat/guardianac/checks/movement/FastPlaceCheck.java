package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.Player;
import org.guardiananticheat.guardianac.GuardianAC;
import org.guardiananticheat.guardianac.utils.AlertsUtil;

import java.util.HashMap;
import java.util.UUID;

public class FastPlaceCheck implements Listener {

    private final HashMap<UUID, Long> lastPlaceTime = new HashMap<>();
    private final long minPlaceDelay = 100;

    public FastPlaceCheck(GuardianAC guardianAC) {
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) return;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (lastPlaceTime.containsKey(playerId)) {
            long timeSinceLastPlace = currentTime - lastPlaceTime.get(playerId);
            if (timeSinceLastPlace < minPlaceDelay) {
                AlertsUtil.alert(player, "FastPlace Detected");
            }
        }

        lastPlaceTime.put(playerId, currentTime);
    }
}

