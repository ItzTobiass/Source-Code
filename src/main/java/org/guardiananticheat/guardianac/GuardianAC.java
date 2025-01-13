package org.guardiananticheat.guardianac;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class GuardianAC extends JavaPlugin implements Listener {
    public static final String C_YOU_DON_T_HAVE_PERMISSION_TO_USE_THIS_COMMAND = "cYou don't have permission to use this command.";
    public static final String B_GINFO = "&b/ginfo .";
    private final HashMap<UUID, Long> lastAlert = new HashMap<>();

    private final HashMap<UUID, Integer> minedOres = new HashMap<>();

    private final HashMap<UUID, Long> miningStartTime = new HashMap<>();

    private final HashMap<UUID, Boolean> alertToggle = new HashMap<>();

    private final HashMap<UUID, Long> lastMoveTime = new HashMap<>();

    private final HashMap<UUID, Integer> timerViolations = new HashMap<>();

    FileConfiguration config = getConfig();

    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, (Plugin)this);
        getLogger().info("GuardianAC has been enabled!");
    }

    public void onDisable() {
        getLogger().info("GuardianAC has been disabled!");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || player.getAllowFlight() || player.isFlying() || player.getGameMode().equals(org.bukkit.GameMode.CREATIVE)) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null || from == null) {
            return;
        }

        double deltaY = to.getY() - from.getY();
        double verticalVelocity = player.getVelocity().getY();
        
        Material blockBelow = to.clone().subtract(0, 1, 0).getBlock().getType();
        if (blockBelow == Material.AIR && deltaY > 0 && verticalVelocity > 0.42) {
            alert(player, "Fly Hack Detected");
        }
    }


    @EventHandler
    public void onPlayerSpeedCheck(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.isOp()) {
            double distance = event.getFrom().distance(Objects.requireNonNull(event.getTo()));
            double threshold = 0.0D;
            if (player.isSprinting()) {
                threshold = 0.87D;
            } else if (!player.isSprinting()) {
                threshold = 0.67;
            } else if (player.isSneaking()) {
                threshold = 0.27D;
            }
            if (distance > threshold) {
                this.alert(player, "Speed Hack (A)");
            }
            Vector velocity = player.getVelocity();
            if (velocity.length() > 0.57) {
                this.alert(player, "Speed Hack (B)");
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (entity instanceof Player) {
            Player attacker = (Player) entity;
            Entity damagedEntity = event.getEntity();
            if (damagedEntity instanceof Player) {
                Player target = (Player) damagedEntity;
                if (attacker.isOp()) {
                    return;
                }
                double angle = calculateAngle(attacker, target);
                if (angle > 120.0D) {
                    alert(attacker, "KillAura Detected");
                }
            }
        }
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        if (player.isOp() || !getConfig().getBoolean("detections.hitbox", true)) {
            return;
        }

        Location playerLocation = player.getLocation();
        Location targetLocation = target.getLocation();
        
        double distance = playerLocation.distance(targetLocation);
        double maxReach = 4.0;

        if (distance > maxReach) {
            alert(player, "Hitbox Cheat Detected (Reach)");
            return;
        }

        double angle = calculateAngle(player, target);
        double maxAngle = 60.0;

        if (angle > maxAngle) {
            alert(player, "Hitbox Cheat Detected (Invalid Angle)");
        }
    }

    @EventHandler
    public void onPlayerJump(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp())
            return;
        if (player.getVelocity().getY() > 1.0D && player.getFallDistance() > 3.0F)
            alert(player, "LongJump Detected");
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (player.isOp() || player.getAllowFlight()) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            float fallDistance = player.getFallDistance();
            double damage = event.getDamage();


            if (fallDistance > 3.0F && damage == 0.0) {
                alert(player, "NoFall Detected");
            }
        }
    }

    private double calculateAngle(Player attacker, Player target) {
        Vector attackerDirection = attacker.getLocation().getDirection().normalize();
        Vector toTarget = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
        double angle = Math.toDegrees(attackerDirection.angle(toTarget));
        return angle;
    }

    @EventHandler
    public void onRapidAttack(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (entity instanceof Player) {
            Player player = (Player)entity;
            if (player.isOp())
                return;
            long currentTime = System.currentTimeMillis();
            if (this.lastAlert.containsKey(player.getUniqueId()) && currentTime - ((Long)this.lastAlert.get(player.getUniqueId())).longValue() < 100L)
                alert(player, "Rapid Attack Detected");
            this.lastAlert.put(player.getUniqueId(), Long.valueOf(currentTime));
        }
    }

    @EventHandler
    public void onStepDetection(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp())
            return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to != null && to.getY() - from.getY() > 1.0D)
            alert(player, "Step Cheat Detected");
    }

    @EventHandler
    public void onAutoArmor(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp())
            return;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor != null && armor.getType() != Material.AIR &&
                    player.getVelocity().getY() > 0.5D)
                alert(player, "AutoArmor Detected");
        }
    }

    @EventHandler
    public void onNoSlow(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp())
            return;
        if (player.isBlocking() || player.isSneaking()) {
            double speed = event.getFrom().distance(event.getTo());
            double threshold = player.isSprinting() ? 0.25D : 0.2D;
            if (speed > threshold)
                alert(player, "NoSlow Detected");
        }
    }

    @EventHandler
    public void onAirJump(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp())
            return;
        Location loc = player.getLocation();
        if (loc.getBlock().getType() == Material.AIR && player.getFallDistance() == 0.0F && player.getVelocity().getY() > 0.5D)
            alert(player, "AirJump Detected");
    }

    @EventHandler
    public void onElytraFly(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp())
            return;
        if (player.isGliding()) {
            double speed = event.getFrom().distance(event.getTo());
            if (speed > 1.0D)
                alert(player, "ElytraFly Detected");
        }
    }

    @EventHandler
    public void onScaffold(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || !getConfig().getBoolean("detections.scaffold", true)) {
            return;
        }

        Location loc = player.getLocation();
        Material blockBelow = loc.clone().subtract(0, 1, 0).getBlock().getType();
        if (blockBelow == Material.AIR && player.isOnGround()) {
            double velocityY = player.getVelocity().getY();
            if (velocityY > -0.01 && velocityY < 0.01) {
                alert(player, "Scaffold Detected");
            }
        }
    }

    @EventHandler
    public void onTimerDetection(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || !config.getBoolean("detections.timer", true)) return;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (lastMoveTime.containsKey(playerId)) {
            long lastTime = lastMoveTime.get(playerId);
            long timeDifference = currentTime - lastTime;

            long expectedTime = 50;
            long deviationThreshold = config.getLong("thresholds.timer_tolerance", 30);

            if (timeDifference < expectedTime - deviationThreshold) {
                timerViolations.put(playerId, timerViolations.getOrDefault(playerId, 0) + 1);

                int maxViolations = config.getInt("thresholds.timer_violations", 5);
                if (timerViolations.get(playerId) >= maxViolations) {
                    alert(player, "Timer Cheat Detected");
                    timerViolations.put(playerId, 0);
                }
            } else {
                timerViolations.put(playerId, 0);
            }
        }

        lastMoveTime.put(playerId, currentTime);
    }

    private void alert(Player player, String cheatType) {
        if (((Boolean)this.alertToggle.getOrDefault(player.getUniqueId(), Boolean.valueOf(true))).booleanValue()) {
            long currentTime = System.currentTimeMillis();
            if (!this.lastAlert.containsKey(player.getUniqueId()) || currentTime - ((Long)this.lastAlert.get(player.getUniqueId())).longValue() > 5000L) {
                String clientType = detectClientType(player);
                Bukkit.broadcastMessage(color("[&bGuardianAC&f] " + player.getName() + " suspected of: " + cheatType + " (Client: " + clientType + ")"));
                this.lastAlert.put(player.getUniqueId(), Long.valueOf(currentTime));
            }
        }
    }

    private String detectClientType(Player player) {
        if (player.getAddress() != null) {
            String hostname = player.getAddress().getHostName();
            if (hostname.contains("fabric"))
                return "Fabric";
            if (hostname.contains("forge"))
                return "Forge";
            return "Vanilla";
        }
        return "Unknown";
    }

    public final String color(String string) {
        return ChatColor.translateAlternateColorCodes('&', string);

    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("greload")) {
            if (sender.hasPermission("guardianac.reload") || sender.isOp()) {
                try {
                    reloadConfig();
                    config = getConfig();
                    sender.sendMessage(color("[&bGuardianAC&f] &aPlugin and configuration reloaded successfully!"));
                } catch (Exception e) {
                    sender.sendMessage(color("[&bGuardianAC&f] &cAn error occurred while reloading the plugin."));
                    e.printStackTrace();
                }
                return true;
            } else {
                sender.sendMessage(color("&cYou don't have permission to use this command."));
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("ghelp")) {
            sender.sendMessage(color("&a===== &bGuardian&cAC &fCommands &a====="));
            sender.sendMessage(color("&b/galerts &f- Toggles alerts for your account."));
            sender.sendMessage(color("&b/greload &f- Reloads the plugin and configuration."));
            sender.sendMessage(color("&b/ghelp &f- Displays this help menu."));
            sender.sendMessage(color("&b/ginfo &f- Displays info of plugin."));
            sender.sendMessage(color("&a==============================="));
            return true;
        }

        if (command.getName().equalsIgnoreCase("ginfo")) {
            sender.sendMessage(color("&a===== &bGuardian&cAC &fInfo &a====="));
            sender.sendMessage(color("&bPlugin Name&7: &fGuardianAC"));
            sender.sendMessage(color("&bVersion&7: &f1.5"));
            sender.sendMessage(color("&bAuthor&7: &fItzTobiass"));
            sender.sendMessage(color("&bDescription&7: &fAn advanced anti-cheat plugin to keep your server safe."));
            sender.sendMessage(color("&bWebsite&7: &fSoon"));
            sender.sendMessage(color("&a==========================="));
            return true;
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (command.getName().equalsIgnoreCase("galerts")) {
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
        }
        return false;
    }
}