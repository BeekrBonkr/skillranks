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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkillRanks extends JavaPlugin {

    private static SkillRanks instance;

    private List<RankSection> rankSections;

    @Override
    public void onEnable() {
        instance = this;
        new Configuration(this).load();

        this.rankSections = RankSection.parse(Configuration.getConfig().getConfigurationSection("ranks"));

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

    private void runPeriodicCheck() {
        long ttlMs = Configuration.getConfig().getLong("placeholder-cache-ms", 2000L);
        // copy: an add/remove-command could mutate the online-player list
        for (Player player : new ArrayList<>(Bukkit.getOnlinePlayers())) {
            RankService.updateAll(player, rankSections, ttlMs);
        }
    }

    private static final class PlaceholderCacheEntry {
        final long ts;
        final Integer value; // null = did not resolve to a whole number

        PlaceholderCacheEntry(long ts, Integer value) {
            this.ts = ts;
            this.value = value;
        }
    }

    // uuid -> (placeholder expression -> cached result), so trees tracking
    // different placeholders never see each other's values
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, PlaceholderCacheEntry>> placeholderCache = new ConcurrentHashMap<>();

    /**
     * Resolves the placeholder for the player, caching the result (including
     * failures, so a bad value isn't re-resolved and re-warned every check)
     * for {@code ttlMs}. Returns null if it doesn't resolve to a whole number.
     */
    public Integer getCachedPlaceholderInt(Player player, String placeholder, long ttlMs) {
        long now = System.currentTimeMillis();
        ConcurrentHashMap<String, PlaceholderCacheEntry> perPlayer =
                placeholderCache.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());

        PlaceholderCacheEntry entry = perPlayer.get(placeholder);
        if (entry != null && (now - entry.ts) <= ttlMs) {
            return entry.value;
        }

        String raw = PlaceholderAPI.setPlaceholders(player, placeholder);
        Integer parsed;
        try {
            parsed = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            parsed = null;
            getLogger().warning("Placeholder \"" + placeholder + "\" resolved to \"" + raw + "\" for "
                    + player.getName() + " - it must be a whole number. Skipping their rank check.");
        }
        perPlayer.put(placeholder, new PlaceholderCacheEntry(now, parsed));
        return parsed;
    }

    public void evictPlaceholderCache(UUID uuid) {
        placeholderCache.remove(uuid);
    }

}
