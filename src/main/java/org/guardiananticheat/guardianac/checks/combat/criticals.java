package org.guardiananticheat.guardianac.checks.combat;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.potion.*;
import org.guardiananticheat.guardianac.*;

import java.util.*;

public class criticals implements Listener {

    private GuardianAC plugin;
    private final Map<UUID, CriticalsData> playerData = new HashMap<>();
    private final int VL_THRESHOLD = 5;
    private final double MIN_FALL_DISTANCE = 0.5;
    private final double MAX_NO_CRIT_DAMAGE = 7.0; // Max normal damage before checking for crits

    public void CriticalsCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    public criticals(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        UUID uuid = player.getUniqueId();
        playerData.putIfAbsent(uuid, new CriticalsData());
        CriticalsData data = playerData.get(uuid);

        if (shouldSkipCheck(player, data)) {
            data.reset();
            return;
        }

        checkCriticals(player, event, data);
    }

    private boolean shouldSkipCheck(Player player, CriticalsData data) {
        return player.isOp() ||
                player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR ||
                player.isInsideVehicle() ||
                player.hasPotionEffect(PotionEffectType.BLINDNESS) ||
                !player.getLocation().getBlock().getType().isAir() ||
                data.getLastVelocityTime() + 1000 > System.currentTimeMillis();
    }

    private void checkCriticals(Player player, EntityDamageByEntityEvent event, CriticalsData data) {
        boolean shouldBeCritical = shouldBeCriticalHit(player);
        boolean isCritical = event.getDamage() > MAX_NO_CRIT_DAMAGE;

        if (isCritical && !shouldBeCritical) {
            data.incrementViolationLevel();

            if (data.getViolationLevel() >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] Criticals detected: %s | Damage: %.1f | FallDistance: %.1f | OnGround: %b | VL: %d",
                        player.getName(),
                        event.getDamage(),
                        player.getFallDistance(),
                        player.isOnGround(),
                        data.getViolationLevel()
                );

                sendAlert(player, alertMsg);
                data.resetViolationLevel();

                // Optionally cancel the critical damage
                // event.setDamage(event.getDamage() - 1.5); // Remove critical bonus
            }
        } else {
            data.decrementViolationLevel();
        }
    }

    private boolean shouldBeCriticalHit(Player player) {
        // Proper critical hit conditions
        return player.getFallDistance() > MIN_FALL_DISTANCE &&
                !player.isOnGround() &&
                !player.isClimbing() &&
                !player.isSwimming() &&
                player.getVelocity().getY() < 0.0 &&
                player.getLocation().getBlock().getType().isAir();
    }

    private boolean isClimbing(Player player) {
        Material blockType = player.getLocation().getBlock().getType();
        return blockType == Material.LADDER ||
                blockType == Material.VINE ||
                blockType == Material.SCAFFOLDING ||
                blockType.toString().contains("CLIMBABLE");
    }

    private void sendAlert(Player player, String message) {
        Bukkit.getLogger().warning(message);
        if (plugin.getConfig().getBoolean("alerts.broadcast")) {
            Bukkit.broadcast(message, "guardianac.alerts");
        }
    }

    private static class CriticalsData {
        private int violationLevel = 0;
        private long lastVelocityTime = 0;

        public void incrementViolationLevel() { this.violationLevel++; }
        public void decrementViolationLevel() { if (this.violationLevel > 0) this.violationLevel--; }
        public void resetViolationLevel() { this.violationLevel = 0; }
        public void reset() { this.violationLevel = 0; }
        public int getViolationLevel() { return violationLevel; }
        public long getLastVelocityTime() { return lastVelocityTime; }
        public void setLastVelocityTime(long time) { this.lastVelocityTime = time; }
    }
}