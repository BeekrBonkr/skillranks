package com.olziedev.skillranks.rank;

import com.olziedev.skillranks.SkillRanks;
import com.olziedev.skillranks.rank.range.RangeParser;
import com.olziedev.skillranks.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record Skill(String id, String name, String message, String range, List<String> addCommand, List<String> removeCommand, NamespacedKey key) {

    public static List<Skill> parseList(ConfigurationSection skills) {
        List<Skill> skillList = new ArrayList<>();
        if (skills == null) return skillList;

        for (String rank : skills.getKeys(false)) {
            ConfigurationSection section = skills.getConfigurationSection(rank);
            if (section == null) continue;

            skillList.add(new Skill(rank,
                    section.getString("name", ""),
                    section.getString("message", ""),
                    section.getString("range", ""),
                    section.getStringList("add-command"),
                    section.getStringList("remove-command"),
                    new NamespacedKey(SkillRanks.getInstance(), "rank-" + rank.toLowerCase())
            ));
        }
        return skillList;
    }

    public void giveRank(Player player) {
        Function<String, String> replacer = s -> s
                .replace("%player%", player.getName())
                .replace("%name%", name);
        for (String command : addCommand) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replacer.apply(command));
        }
        Utils.sendMessage(player, replacer.apply(message));
        player.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);
    }

    public void removeRank(Player player) {
        Function<String, String> replacer = s -> s
                .replace("%player%", player.getName())
                .replace("%name%", name);
        for (String command : removeCommand) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replacer.apply(command));
        }
        player.getPersistentDataContainer().remove(key);
    }

    public boolean hasRank(Player player) {
        return player.getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
    }

    public boolean meetsRange(int level) {
        try {
            RangeParser rangeParser = RangeParser.getRangeParser(this.range);
            return rangeParser.isInRange(level, this.range);
        } catch (Exception e) {
            SkillRanks.getInstance().getLogger().warning("Invalid range for rank " + id + " (" + range + ")");
            return false;
        }
    }
}
