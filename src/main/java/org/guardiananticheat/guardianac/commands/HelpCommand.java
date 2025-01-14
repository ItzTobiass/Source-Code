package org.guardiananticheat.guardianac.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.guardiananticheat.guardianac.GuardianAC;

public class HelpCommand implements CommandExecutor {
    private final GuardianAC plugin;

    public HelpCommand(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(color("&a===== &bGuardian&cAC &fCommands &a====="));
        sender.sendMessage(color("&b/galerts &f- Toggles alerts for your account."));
        sender.sendMessage(color("&b/greload &f- Reloads the plugin and configuration."));
        sender.sendMessage(color("&b/ghelp &f- Displays this help menu."));
        sender.sendMessage(color("&b/ginfo &f- Displays info of plugin."));
        sender.sendMessage(color("&a==============================="));
        return true;
    }

    private String color(String message) {
        return message.replace("&", "§");
    }
}