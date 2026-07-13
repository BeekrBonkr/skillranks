package com.olziedev.skillranks.rank;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public record RankSection(String id, String permission, List<Skill> skills) {

    public static List<RankSection> parse(ConfigurationSection section) {
        List<RankSection> rankSections = new ArrayList<>();
        if (section == null) return rankSections;

        for (String key : section.getKeys(false)) {
            ConfigurationSection rankSection = section.getConfigurationSection(key);
            String permission = rankSection.getString("permission", "");
            List<Skill> skills = Skill.parseList(rankSection.getConfigurationSection("skills"), key);
            rankSections.add(new RankSection(key, permission, skills));
        }
        return rankSections;
    }
}
