package org.guardiananticheat.guardianac;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.guardiananticheat.guardianac.checks.combat.*;
import org.guardiananticheat.guardianac.checks.movement.*;
import org.guardiananticheat.guardianac.commands.*;

public class GuardianAC extends JavaPlugin {
    public static final String C_YOU_DON_T_HAVE_PERMISSION_TO_USE_THIS_COMMAND = "You don't have permission to use this command.";
    public static FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();

        // Registrace combat checks
        Bukkit.getPluginManager().registerEvents(new HitBoxCheck(this), this);
        Bukkit.getPluginManager().registerEvents(new KillAuraCheck(this), this);
        Bukkit.getPluginManager().registerEvents(new criticals(this), this);
        Bukkit.getPluginManager().registerEvents(new NoSwing(this), this);
        Bukkit.getPluginManager().registerEvents(new nohitdelay(this), this);
        Bukkit.getPluginManager().registerEvents(new FastPlaceCheck(this), this);

        // Registrace movement checks
        Bukkit.getPluginManager().registerEvents(new NoFallCheck(this), this);
        Bukkit.getPluginManager().registerEvents(new FlyCheck(this), this);
        Bukkit.getPluginManager().registerEvents(new SpeedCheck(this), this);
        Bukkit.getPluginManager().registerEvents(new NoSlowCheck(this), this);
        Bukkit.getPluginManager().registerEvents(new TimerCheck(this), this);

        // Registrace příkazů
        getCommand("greload").setExecutor(new ReloadCommand(this));
        getCommand("ghelp").setExecutor(new HelpCommand(this));
        getCommand("ginfo").setExecutor(new InfoCommand(this));
        getCommand("galerts").setExecutor(new AlertsCommand(this));

        getLogger().info("GuardianAC has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("GuardianAC has been disabled!");
    }

    public static FileConfiguration getPluginConfig() {
        return config;
    }
}