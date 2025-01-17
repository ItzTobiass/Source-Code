package org.guardiananticheat.guardianac.checks.combat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.guardiananticheat.guardianac.utils.AlertsUtil;

import java.util.HashMap;
import java.util.UUID;

public class nohitdelay extends JavaPlugin implements Listener {

    private final HashMap<UUID, Long> lastHitTime = new HashMap<>();

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();

            if (lastHitTime.containsKey(playerId)) {
                long timeSinceLastHit = currentTime - lastHitTime.get(playerId);

                int minimumDelay = 500;
                if (timeSinceLastHit < minimumDelay) {
                    AlertsUtil.alert(player, "NoHitDelay detected");
                }
            }

            lastHitTime.put(playerId, currentTime);
        }
    }
}
