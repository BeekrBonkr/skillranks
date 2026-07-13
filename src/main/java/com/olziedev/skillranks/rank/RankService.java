package com.olziedev.skillranks.rank;

import com.olziedev.skillranks.SkillRanks;
import com.olziedev.skillranks.utils.Configuration;
import org.bukkit.entity.Player;

import java.util.List;

public final class RankService {

    private RankService() {}

    public enum Result { CHANGED, UNCHANGED, NO_MATCH, INVALID_PLACEHOLDER }

    public static Result updateRank(Player player, RankSection rankSection, String placeholder, long ttlMs) {
        Skill currentSkill = rankSection.skills().stream().filter(x -> x.hasRank(player)).findFirst().orElse(null);

        int level;
        try {
            level = SkillRanks.getInstance().getCachedPlaceholderInt(player, placeholder, ttlMs);
        } catch (NumberFormatException e) {
            SkillRanks.getInstance().getLogger().warning("Invalid placeholder value for " + player.getName() + ", it must be a number: " + e.getMessage());
            return Result.INVALID_PLACEHOLDER;
        }

        Skill matched = rankSection.skills().stream().filter(x -> x.meetsRange(level)).findFirst().orElse(null);
        Result result;
        if (matched == null) {
            result = Result.NO_MATCH;
        } else if (matched.hasRank(player)) {
            result = Result.UNCHANGED;
        } else {
            if (currentSkill != null) currentSkill.removeRank(player);
            matched.giveRank(player);
            result = Result.CHANGED;
        }

        if (Configuration.isDebug()) {
            SkillRanks.getInstance().getLogger().info("Checked " + player.getName() + " against rank tree \"" + rankSection.id()
                    + "\": level=" + level + " result=" + result);
        }
        return result;
    }

    public static void updateAll(Player player, List<RankSection> rankSections, String placeholder, long ttlMs) {
        for (RankSection section : rankSections) {
            if (!section.permission().isEmpty() && !player.hasPermission(section.permission())) continue;
            updateRank(player, section, placeholder, ttlMs);
        }
    }
}
