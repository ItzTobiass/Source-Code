package org.guardiananticheat.guardianac.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.guardiananticheat.guardianac.GuardianAC;

public class ReloadCommand implements CommandExecutor {
    private final GuardianAC plugin;

    public ReloadCommand(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("guardianac.reload") || sender.isOp()) {
            try {
                plugin.reloadConfig();
                plugin.config = plugin.getConfig();
                sender.sendMessage(color("[&bGuardianAC&f] &aPlugin and configuration reloaded successfully!"));
            } catch (Exception e) {
                sender.sendMessage(color("[&bGuardianAC&f] &cAn error occurred while reloading the plugin."));
                e.printStackTrace();
            }
            return true;
        } else {
            sender.sendMessage(color(GuardianAC.C_YOU_DON_T_HAVE_PERMISSION_TO_USE_THIS_COMMAND));
            return true;
        }
    }

    private String color(String message) {
        return message.replace("&", "§");
    }
}