package com.olziedev.skillranks.rank;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public record RankSection(String id, String permission, List<Skill> skills) {

    public static List<RankSection> parse(ConfigurationSection section) {
        List<RankSection> rankSections = new ArrayList<>();
        if (section == null) return rankSections;

        for (String key : section.getKeys(false)) {
            String permission = section.getString("permission", "");
            List<Skill> skills = Skill.parseList(section.getConfigurationSection(key + ".skills"));
            rankSections.add(new RankSection(key, permission, skills));
        }
        return rankSections;
    }
}
