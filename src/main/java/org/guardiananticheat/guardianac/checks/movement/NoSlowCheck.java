package org.guardiananticheat.guardianac.checks.movement;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.guardiananticheat.guardianac.GuardianAC;

import java.util.HashMap;
import java.util.UUID;

public class NoSlowCheck implements Listener {

    private final HashMap<UUID, Long> slowViolation = new HashMap<>();

    public NoSlowCheck(GuardianAC guardianAC) {
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isBlocking() || player.getInventory().getItemInMainHand().getType() == Material.SHIELD) {
            double delta = event.getFrom().distance(event.getTo());
            if (delta > 0.15) {
                long last = slowViolation.getOrDefault(player.getUniqueId(), 0L);
                if (System.currentTimeMillis() - last > 1500) {
                    Bukkit.getLogger().warning("[GuardianAC] NoSlow: " + player.getName() + " moved too fast while blocking (" + delta + ")");
                    slowViolation.put(player.getUniqueId(), System.currentTimeMillis());
                }
            }
        }
    }
}
