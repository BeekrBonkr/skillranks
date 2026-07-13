package com.olziedev.skillranks.listener;

import com.olziedev.skillranks.SkillRanks;
import com.olziedev.skillranks.rank.RankService;
import com.olziedev.skillranks.utils.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final SkillRanks plugin;

    public PlayerListener(SkillRanks plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!Configuration.getConfig().getBoolean("auto-check.on-join", true)) return;

        Player player = event.getPlayer();
        long delayTicks = Configuration.getConfig().getLong("auto-check.join-delay-ticks", 40L);
        long ttlMs = Configuration.getConfig().getLong("placeholder-cache-ms", 2000L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                RankService.updateAll(player, plugin.getRankSections(), plugin.getPlaceholder(), ttlMs);
            }
        }, delayTicks);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.evictPlaceholderCache(event.getPlayer().getUniqueId());
    }
}
