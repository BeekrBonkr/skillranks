package com.olziedev.skillranks.rank;

import com.olziedev.skillranks.SkillRanks;
import com.olziedev.skillranks.utils.Configuration;
import org.bukkit.entity.Player;

import java.util.List;

public final class RankService {

    private RankService() {}

    public enum Result { CHANGED, UNCHANGED, NO_MATCH, INVALID_PLACEHOLDER }

    /**
     * Checks the player against the tree's placeholder value and promotes/
     * demotes them if needed. With {@code force}, the matched rank's
     * add-commands are re-run even if the player already holds it - a
     * resync for when the underlying permission plugin has drifted.
     */
    public static Result updateRank(Player player, RankSection rankSection, long ttlMs, boolean force) {
        Skill currentSkill = rankSection.skills().stream().filter(x -> x.hasRank(player)).findFirst().orElse(null);

        Integer level = SkillRanks.getInstance().getCachedPlaceholderInt(player, rankSection.placeholder(), ttlMs);
        if (level == null) return Result.INVALID_PLACEHOLDER;

        Skill matched = rankSection.skills().stream().filter(x -> x.meetsRange(level)).findFirst().orElse(null);
        Result result;
        if (matched == null) {
            result = Result.NO_MATCH;
        } else if (!force && matched.hasRank(player)) {
            result = Result.UNCHANGED;
        } else {
            if (currentSkill != null && currentSkill != matched) currentSkill.removeRank(player);
            matched.giveRank(player);
            result = Result.CHANGED;
        }

        if (Configuration.isDebug()) {
            SkillRanks.getInstance().getLogger().info("Checked " + player.getName() + " against rank tree \"" + rankSection.id()
                    + "\": level=" + level + " result=" + result);
        }
        return result;
    }

    public static void updateAll(Player player, List<RankSection> rankSections, long ttlMs) {
        for (RankSection section : rankSections) {
            if (!section.permission().isEmpty() && !player.hasPermission(section.permission())) continue;
            updateRank(player, section, ttlMs, false);
        }
    }
}
