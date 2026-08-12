package com.olziedev.skillranks.rank;

import com.olziedev.skillranks.SkillRanks;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public record RankSection(String id, String permission, String placeholder, List<Skill> skills) {

    public static List<RankSection> parse(ConfigurationSection section) {
        List<RankSection> rankSections = new ArrayList<>();
        if (section == null) return rankSections;

        for (String key : section.getKeys(false)) {
            ConfigurationSection rankSection = section.getConfigurationSection(key);
            if (rankSection == null) continue;

            String permission = rankSection.getString("permission", "");
            String placeholder = rankSection.getString("placeholder", "");
            if (placeholder.isEmpty()) {
                SkillRanks.getInstance().getLogger().warning("Rank tree \"" + key
                        + "\" has no \"placeholder\" set - skipping it.");
                continue;
            }

            List<Skill> skills = Skill.parseList(rankSection.getConfigurationSection("skills"), key);
            rankSections.add(new RankSection(key, permission, placeholder, skills));
        }
        return rankSections;
    }
}
