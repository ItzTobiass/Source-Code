package org.guardiananticheat.guardianac.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class AlertsUtil {
    public static void alert(Player player, String cheatType) {
        if (player.isOp())
            Bukkit.broadcastMessage(color("[&bGuardianAC&f] " + player.getName() + " suspected of: " + cheatType));
    }

    public static String color(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }
}