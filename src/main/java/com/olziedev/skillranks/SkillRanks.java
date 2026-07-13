package com.olziedev.skillranks;

import com.olziedev.olziecommand.v1_3_3.OlzieCommand;
import com.olziedev.olziecommand.v1_3_3.framework.action.CommandActionType;
import com.olziedev.skillranks.listener.PlayerListener;
import com.olziedev.skillranks.rank.RankSection;
import com.olziedev.skillranks.rank.RankService;
import com.olziedev.skillranks.utils.Configuration;
import com.olziedev.skillranks.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkillRanks extends JavaPlugin {

    private static SkillRanks instance;

    private List<RankSection> rankSections;
    private String placeholder;

    @Override
    public void onEnable() {
        instance = this;
        new Configuration(this).load();

        this.rankSections = RankSection.parse(Configuration.getConfig().getConfigurationSection("ranks"));
        this.placeholder = Configuration.getConfig().getString("placeholder-to-listen-for");

        new OlzieCommand(this, getClass())
                .getActionRegister()
                .registerAction(CommandActionType.CMD_NO_PERMISSION, cmd -> {
                    Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.no-permission"));
                })
                .registerAction(CommandActionType.CMD_HELP_MENU, cmd -> {
                    for (String msg : Configuration.getConfig().getStringList("lang.help")) {
                        Utils.sendMessage(cmd.getSender(), msg.replace("%cmd%", cmd.getLabel()));
                    }
                }).buildActions()
                .registerCommands(); // automatically register commands

        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        long intervalSeconds = Configuration.getConfig().getLong("auto-check.interval-seconds", 0L);
        if (intervalSeconds > 0) {
            long intervalTicks = intervalSeconds * 20L;
            Bukkit.getScheduler().runTaskTimer(this, this::runPeriodicCheck, intervalTicks, intervalTicks);
        }
    }

    @Override
    public void onDisable() {
        placeholderCache.clear();
        Bukkit.getScheduler().cancelTasks(this);
        instance = null;
    }

    public static SkillRanks getInstance() {
        return instance;
    }

    public List<RankSection> getRankSections() {
        return rankSections;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    private void runPeriodicCheck() {
        long ttlMs = Configuration.getConfig().getLong("placeholder-cache-ms", 2000L);
        for (Player player : Bukkit.getOnlinePlayers()) {
            RankService.updateAll(player, rankSections, placeholder, ttlMs);
        }
    }

    private static final class PlaceholderCacheEntry {
        final long ts;
        final int value;
        PlaceholderCacheEntry(long ts, int value) {
            this.ts = ts;
            this.value = value;
        }
    }

    private final ConcurrentHashMap<UUID, PlaceholderCacheEntry> placeholderCache = new ConcurrentHashMap<>();

    public int getCachedPlaceholderInt(Player player, String placeholder, long ttlMs) throws NumberFormatException {
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        PlaceholderCacheEntry entry = placeholderCache.get(uuid);
        if (entry != null && (now - entry.ts) <= ttlMs) {
            return entry.value;
        }

        String raw = PlaceholderAPI.setPlaceholders(player, placeholder);
        int parsed = Integer.parseInt(raw.trim());
        placeholderCache.put(uuid, new PlaceholderCacheEntry(now, parsed));
        return parsed;
    }

    public void evictPlaceholderCache(UUID uuid) {
        placeholderCache.remove(uuid);
    }

}
