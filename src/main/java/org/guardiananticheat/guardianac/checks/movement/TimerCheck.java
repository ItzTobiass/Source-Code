package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.guardiananticheat.guardianac.GuardianAC;

import java.util.HashMap;
import java.util.UUID;

public class TimerCheck implements Listener {

    private final HashMap<UUID, Long> lastMoveTime = new HashMap<>();
    private final HashMap<UUID, Integer> violations = new HashMap<>();

    private final int MIN_INTERVAL_MS = 45; // min. mezera mezi packetama
    private final int MAX_VIOLATIONS = 6;   // kolik flagů tolerujeme
    private final long RESET_TIME = 5_000;  // po kolika ms se vynuluje počítání

    public TimerCheck(GuardianAC guardianAC) {
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (lastMoveTime.containsKey(uuid)) {
            long last = lastMoveTime.get(uuid);
            long diff = now - last;

            if (diff < MIN_INTERVAL_MS) {
                int currentViolations = violations.getOrDefault(uuid, 0) + 1;
                violations.put(uuid, currentViolations);

                if (currentViolations >= MAX_VIOLATIONS) {
                    alert(player, diff, currentViolations);
                    violations.put(uuid, 0); // resetuj po alertu
                }
            } else {
                // Pokud pohyb není podezřelý, časem violation level klesá
                violations.put(uuid, Math.max(violations.getOrDefault(uuid, 0) - 1, 0));
            }
        }

        lastMoveTime.put(uuid, now);

        // Po RESET_TIME neaktivity smaž hráče z paměti
        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugin("GuardianAC"), () -> {
                    if (System.currentTimeMillis() - lastMoveTime.getOrDefault(uuid, 0L) > RESET_TIME) {
                        lastMoveTime.remove(uuid);
                        violations.remove(uuid);
                    }
                }, 100L // 5 sekund = 100 ticků
        );
    }

    private void alert(Player player, long diff, int vl) {
        String msg = "§c[GuardianAC] " + player.getName() + " failed Timer Check (Interval: " + diff + "ms, VL: " + vl + ")";
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("guardianac.alerts"))
                .forEach(p -> p.sendMessage(msg));
        Bukkit.getLogger().info("[GuardianAC] " + msg);
    }
}