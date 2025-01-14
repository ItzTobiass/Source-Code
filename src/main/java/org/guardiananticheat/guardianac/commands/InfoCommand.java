package org.guardiananticheat.guardianac.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.guardiananticheat.guardianac.GuardianAC;

public class InfoCommand implements CommandExecutor {
    private final GuardianAC plugin;

    public InfoCommand(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(color("&a===== &bGuardian&cAC &fInfo &a====="));
        sender.sendMessage(color("&bPlugin Name&7: &fGuardianAC"));
        sender.sendMessage(color("&bVersion&7: &f1.5"));
        sender.sendMessage(color("&bAuthor&7: &fItzTobiass"));
        sender.sendMessage(color("&bDescription&7: &fAn advanced anti-cheat plugin to keep your server safe."));
        sender.sendMessage(color("&bWebsite&7: &fSoon"));
        sender.sendMessage(color("&a==========================="));
        return true;
    }

    private String color(String message) {
        return message.replace("&", "§");
    }
}
