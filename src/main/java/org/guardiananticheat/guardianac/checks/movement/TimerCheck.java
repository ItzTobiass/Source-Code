package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.guardiananticheat.guardianac.utils.AlertsUtil;
import org.guardiananticheat.guardianac.GuardianAC;

import java.util.HashMap;
import java.util.UUID;

public class TimerCheck implements Listener {
    private final GuardianAC plugin;
    private final HashMap<UUID, Integer> timerViolations = new HashMap<>();
    private final HashMap<UUID, Long> lastMoveTime = new HashMap<>();

    public TimerCheck(GuardianAC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTimerDetection(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOp() || !plugin.getPluginConfig().getBoolean("detections.timer", true)) return;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        if (lastMoveTime.containsKey(playerId)) {
            long lastTime = lastMoveTime.get(playerId);
            long timeDifference = currentTime - lastTime;

            long expectedTime = 50;
            long deviationThreshold = plugin.getPluginConfig().getLong("thresholds.timer_tolerance", 30);

            if (timeDifference < expectedTime - deviationThreshold) {
                timerViolations.put(playerId, timerViolations.getOrDefault(playerId, 0) + 1);

                int maxViolations = plugin.getPluginConfig().getInt("thresholds.timer_violations", 5);
                if (timerViolations.get(playerId) >= maxViolations) {
                    AlertsUtil.alert(player, "Timer Cheat Detected");
                    timerViolations.put(playerId, 0);
                }
            } else {
                timerViolations.put(playerId, 0);
            }
        }

        lastMoveTime.put(playerId, currentTime);
    }
}