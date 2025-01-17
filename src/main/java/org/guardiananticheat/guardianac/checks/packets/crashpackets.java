package org.guardiananticheat.guardianac.checks.packets;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.guardiananticheat.guardianac.utils.AlertsUtil;

import java.util.HashMap;
import java.util.UUID;

public class crashpackets extends JavaPlugin implements Listener {

    private final HashMap<UUID, Long> packetCount = new HashMap<>();
    private final HashMap<UUID, Long> lastPacketTime = new HashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        long currentTime = System.currentTimeMillis();
        packetCount.put(playerId, packetCount.getOrDefault(playerId, 0L) + 1);

        if (lastPacketTime.containsKey(playerId)) {
            long timeDifference = currentTime - lastPacketTime.get(playerId);

            if (timeDifference < 10) {
                AlertsUtil.alert(player, "Possible crash packet detected (move packets)");
                kickPlayer(player, "Sending invalid packets");
                return;
            }
        }

        lastPacketTime.put(playerId, currentTime);
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();

        if (command.length() > 300) {
            AlertsUtil.alert(player, "Possible crash packet detected (long command)");
            kickPlayer(player, "Sending invalid packets");
            event.setCancelled(true);
        }
    }

    private void kickPlayer(Player player, String reason) {
        player.kickPlayer("[GuardianAC] " + reason);
        getLogger().warning("Player " + player.getName() + " was kicked: " + reason);
    }
}


