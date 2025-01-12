package org.guardiananticheat.guardianac;

import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class GuardianAC extends JavaPlugin implements Listener {
    private final HashMap<UUID, Long> lastAlert = new HashMap<>();

    private final HashMap<UUID, Integer> minedOres = new HashMap<>();

    private final HashMap<UUID, Long> miningStartTime = new HashMap<>();

    private final HashMap<UUID, Boolean> alertToggle = new HashMap<>();

    public void onEnable() {
        saveDefaultConfig();

        FileConfiguration config = getConfig();
        Bukkit.getPluginManager().registerEvents(this, (Plugin)this);
        getLogger().info("GuardianAC has been enabled!");
    }

    public void onDisable() {
        getLogger().info("GuardianAC has been disabled!");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp())
            return;
        Location loc = player.getLocation();
        if (!player.isFlying() && !player.getAllowFlight() && loc.getY() > 0.0D) {
            Material blockBelow = loc.clone().subtract(0.0D, 1.0D, 0.0D).getBlock().getType();
            if (blockBelow == Material.AIR && player.getVelocity().getY() > 0.5D)
                alert(player, "Fly Hack Detected");
        }
    }

    @EventHandler
    public void onPlayerSpeedCheck(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp())
            return;
        double distance = event.getFrom().distance(event.getTo());
        double threshold = player.isSprinting() ? 0.9D : 0.7D;
        if (distance > threshold)
            alert(player, "Speed Hack Detected");
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
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player)entity;
            if (player.isOp())
                return;
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL && player.getFallDistance() > 3.0F &&
                    event.getDamage() == 0.0D)
                alert(player, "NoFall Detected");
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (entity instanceof Player) {
            Player player = (Player)entity;
            entity = event.getEntity();
            if (entity instanceof Player) {
                Player target = (Player)entity;
                if (player.isOp())
                    return;
                double angle = calculateAngle(player, target);
                if (angle > 120.0D)
                    alert(player, "KillAura Detected");
            }
        }
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
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.isOp())
            return;
        Material blockType = event.getBlock().getType();
        if (isOre(blockType)) {
            UUID playerId = player.getUniqueId();
            this.minedOres.put(playerId, Integer.valueOf(((Integer)this.minedOres.getOrDefault(playerId, Integer.valueOf(0))).intValue() + 1));
            if (!this.miningStartTime.containsKey(playerId))
                this.miningStartTime.put(playerId, Long.valueOf(System.currentTimeMillis()));
            long elapsedTime = System.currentTimeMillis() - ((Long)this.miningStartTime.get(playerId)).longValue();
            if (elapsedTime > 300000L) {
                int oreCount = ((Integer)this.minedOres.get(playerId)).intValue();
                if (oreCount >= 64) {
                    alert(player, "Suspended for X-Ray");
                    this.minedOres.put(playerId, Integer.valueOf(0));
                    this.miningStartTime.put(playerId, Long.valueOf(System.currentTimeMillis()));
                }
            }
        }
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
    public void onDisabler(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp())
            return;
        if (!player.isOnline() && !player.isInvulnerable())
            alert(player, "Disabler Detected");
    }

    @EventHandler
    public void onNoHunger(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (player.isOp())
            return;
        if (player.getFoodLevel() == 20 && event.getItem().getType().isEdible())
            alert(player, "NoHunger Detected");
    }

    @EventHandler
    public void onScaffold(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp())
            return;
        Location loc = player.getLocation();
        Material blockBelow = loc.clone().subtract(0.0D, 1.0D, 0.0D).getBlock().getType();
        if (blockBelow == Material.AIR && player.isOnGround() && player.getVelocity().getY() == 0.0D)
            alert(player, "Scaffold Detected");
    }

    private boolean isOre(Material material) {
        return (material == Material.DIAMOND_ORE || material == Material.EMERALD_ORE || material == Material.GOLD_ORE || material == Material.IRON_ORE || material == Material.COAL_ORE || material == Material.COPPER_ORE || material == Material.DEEPSLATE_DIAMOND_ORE || material == Material.DEEPSLATE_EMERALD_ORE || material == Material.DEEPSLATE_GOLD_ORE || material == Material.DEEPSLATE_IRON_ORE || material == Material.DEEPSLATE_COAL_ORE || material == Material.DEEPSLATE_COPPER_ORE);
    }

    private void alert(Player player, String cheatType) {
        if (((Boolean)this.alertToggle.getOrDefault(player.getUniqueId(), Boolean.valueOf(true))).booleanValue()) {
            long currentTime = System.currentTimeMillis();
            if (!this.lastAlert.containsKey(player.getUniqueId()) || currentTime - ((Long)this.lastAlert.get(player.getUniqueId())).longValue() > 5000L) {
                String clientType = detectClientType(player);
                Bukkit.broadcastMessage("[&bGuardianAC&f] " + player.getName() + " suspected of: " + cheatType + " (Client: " + clientType + ")");
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

    private double calculateAngle(Player attacker, Player target) {
        Location attackerLoc = attacker.getLocation();
        Location targetLoc = target.getLocation();
        double dx = targetLoc.getX() - attackerLoc.getX();
        double dz = targetLoc.getZ() - attackerLoc.getZ();
        double yaw = attackerLoc.getYaw();
        double angle = Math.toDegrees(Math.atan2(dz, dx)) - yaw;
        if (angle < 0.0D)
            angle += 360.0D;
        return angle;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the sender is a player
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Check if the command is "alerts"
            if (command.getName().equalsIgnoreCase("alerts")) {
                UUID playerId = player.getUniqueId();
                // Get the current state of alerts for the player, defaulting to true (enabled)
                boolean currentState = alertToggle.getOrDefault(playerId, true);
                // Toggle the alert state
                alertToggle.put(playerId, !currentState);

                // Send feedback to the player based on the new state
                if (currentState) {
                    player.sendMessage("[&bGuardianAC&f] Alerts disabled.");
                } else {
                    player.sendMessage("[&bGuardianAC&f] Alerts enabled.");
                }
                return true; // Command executed successfully
            }
        }
        // If the sender is not a player or the command is not recognized, return false
        return false;
    }
}