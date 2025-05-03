package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.util.*;
import org.guardiananticheat.guardianac.*;
import org.guardiananticheat.guardianac.utils.*;

import java.util.*;

public class ScaffoldCheck implements Listener {

    private final GuardianAC plugin;
    private final Map<UUID, ScaffoldData> playerData = new HashMap<>();
    private final int VL_THRESHOLD = 5;
    private final int BLOCK_THRESHOLD = 8;
    private final long TIME_WINDOW = 1000L; // 1 second

    // Rotation thresholds in degrees
    private final double MAX_ROTATION_DIFF = 90.0;
    private final double MIN_PLACE_ACCURACY = 30.0;

    public ScaffoldCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Initialize player data if not present
        playerData.putIfAbsent(uuid, new ScaffoldData());
        ScaffoldData data = playerData.get(uuid);

        // Skip checks for exempt players
        if (shouldSkipCheck(player)) {
            data.reset();
            return;
        }

        // Skip non-scaffold blocks
        Material placed = event.getBlockPlaced().getType();
        if (!isScaffoldMaterial(placed)) return;

        // Update placement data
        data.recordPlacement(player, event);

        // Run detection checks
        checkPlacementRate(player, data);
        checkPlacementAccuracy(player, data);
        checkRotationPattern(player, data);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ScaffoldData data = playerData.get(player.getUniqueId());

        if (data != null && player.isOnGround()) {
            // Reset some counters when on ground
            data.resetPlacementCount();
        }
    }

    private boolean shouldSkipCheck(Player player) {
        return player.isOp() ||
                player.getGameMode() == GameMode.CREATIVE ||
                player.getGameMode() == GameMode.SPECTATOR ||
                player.isFlying() ||
                player.hasPotionEffect(PotionEffectType.SPEED) ||
                player.getLocation().getBlock().isLiquid();
    }

    private boolean isScaffoldMaterial(Material material) {
        return material == Material.SCAFFOLDING ||
                material.toString().contains("SLAB") ||
                material.toString().contains("STAIRS") ||
                material == Material.HAY_BLOCK;
    }

    private void checkPlacementRate(Player player, ScaffoldData data) {
        int placements = data.getRecentPlacements();

        if (placements > BLOCK_THRESHOLD) {
            data.incrementViolationLevel("rate");

            if (data.getViolationLevel("rate") >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] Scaffold-Rate detected: %s | Blocks: %d/%d | Ping: %d",
                        player.getName(),
                        placements,
                        BLOCK_THRESHOLD,
                        player.getPing()
                );

                AlertsUtil.alert(player, alertMsg);
                data.resetViolationLevel("rate");
            }
        }
    }

    private void checkPlacementAccuracy(Player player, ScaffoldData data) {
        if (data.getPlacementAccuracy() < MIN_PLACE_ACCURACY) {
            data.incrementViolationLevel("accuracy");

            if (data.getViolationLevel("accuracy") >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] Scaffold-Accuracy detected: %s | Accuracy: %.1f° | Min: %.1f°",
                        player.getName(),
                        data.getPlacementAccuracy(),
                        MIN_PLACE_ACCURACY
                );

                AlertsUtil.alert(player, alertMsg);
                data.resetViolationLevel("accuracy");
            }
        }
    }

    private void checkRotationPattern(Player player, ScaffoldData data) {
        if (data.getRotationDifference() > MAX_ROTATION_DIFF) {
            data.incrementViolationLevel("rotation");

            if (data.getViolationLevel("rotation") >= VL_THRESHOLD) {
                String alertMsg = String.format(
                        "[GuardianAC] Scaffold-Rotation detected: %s | Diff: %.1f° | Max: %.1f°",
                        player.getName(),
                        data.getRotationDifference(),
                        MAX_ROTATION_DIFF
                );

                AlertsUtil.alert(player, alertMsg);
                data.resetViolationLevel("rotation");
            }
        }
    }

    private class ScaffoldData {
        private final Map<String, Integer> violationLevels = new HashMap<>();
        private final List<Long> placementTimes = new ArrayList<>();
        private Location lastPlaceLocation;
        private float lastYaw;
        private float lastPitch;
        private double placementAccuracySum;
        private int placementCount;

        public ScaffoldData() {
            violationLevels.put("rate", 0);
            violationLevels.put("accuracy", 0);
            violationLevels.put("rotation", 0);
        }

        public void recordPlacement(Player player, BlockPlaceEvent event) {
            long now = System.currentTimeMillis();

            // Record placement time
            placementTimes.add(now);
            placementTimes.removeIf(time -> now - time > TIME_WINDOW);

            // Calculate rotation difference
            Location loc = player.getLocation();
            if (lastYaw != 0 || lastPitch != 0) {
                double yawDiff = Math.abs(loc.getYaw() - lastYaw);
                double pitchDiff = Math.abs(loc.getPitch() - lastPitch);
                double totalDiff = Math.sqrt(yawDiff*yawDiff + pitchDiff*pitchDiff);
                violationLevels.put("rotation", (int) totalDiff);
            }
            lastYaw = loc.getYaw();
            lastPitch = loc.getPitch();

            // Calculate placement accuracy
            Block placed = event.getBlockPlaced();
            Vector eyePos = player.getEyeLocation().toVector();
            Vector blockPos = placed.getLocation().toVector().add(new Vector(0.5, 0.5, 0.5));
            Vector direction = player.getLocation().getDirection();
            Vector toBlock = blockPos.subtract(eyePos).normalize();

            double angle = Math.toDegrees(direction.angle(toBlock));
            placementAccuracySum += angle;
            placementCount++;

            lastPlaceLocation = placed.getLocation();
        }

        public int getRecentPlacements() {
            return placementTimes.size();
        }

        public double getPlacementAccuracy() {
            return placementCount > 0 ? placementAccuracySum / placementCount : 100.0;
        }

        public double getRotationDifference() {
            return violationLevels.get("rotation");
        }

        public void incrementViolationLevel(String type) {
            violationLevels.put(type, violationLevels.getOrDefault(type, 0) + 1);
        }

        public void resetViolationLevel(String type) {
            violationLevels.put(type, 0);
        }

        public int getViolationLevel(String type) {
            return violationLevels.getOrDefault(type, 0);
        }

        public void resetPlacementCount() {
            placementTimes.clear();
        }

        public void reset() {
            placementTimes.clear();
            violationLevels.replaceAll((k, v) -> 0);
            placementAccuracySum = 0;
            placementCount = 0;
        }
    }
}