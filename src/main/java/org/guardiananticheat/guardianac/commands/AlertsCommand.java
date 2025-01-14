package org.guardiananticheat.guardianac.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.guardiananticheat.guardianac.GuardianAC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AlertsCommand implements CommandExecutor {
    private Map<UUID, Boolean> alertToggle = new HashMap<>();

    private final GuardianAC plugin;

    public AlertsCommand(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.isOp()) {
                player.sendMessage(color("[&bGuardianAC&f] You do not have permission to toggle alerts."));
                return true;
            }

            UUID playerId = player.getUniqueId();
            boolean currentState = alertToggle.getOrDefault(playerId, true);
            alertToggle.put(playerId, !currentState);

            if (currentState) {
                player.sendMessage(color("[&bGuardianAC&f] Alerts disabled."));
            } else {
                player.sendMessage(color("[&bGuardianAC&f] Alerts enabled."));
            }
            return true;
        }
        return false;
    }

    private String color(String message) {
        return message.replace("&", "§");
    }
}
