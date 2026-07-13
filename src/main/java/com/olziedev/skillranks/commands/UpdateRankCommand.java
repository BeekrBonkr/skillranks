package com.olziedev.skillranks.commands;

import com.olziedev.olziecommand.v1_3_3.framework.CommandExecutor;
import com.olziedev.olziecommand.v1_3_3.framework.ExecutorType;
import com.olziedev.olziecommand.v1_3_3.framework.api.FrameworkCommand;
import com.olziedev.skillranks.rank.RankSection;
import com.olziedev.skillranks.rank.RankService;
import com.olziedev.skillranks.utils.Configuration;
import com.olziedev.skillranks.utils.Utils;
import org.bukkit.entity.Player;
import com.olziedev.skillranks.SkillRanks;

import java.util.List;
import java.util.stream.Collectors;

public class UpdateRankCommand extends FrameworkCommand {

    private final String placeholder;
    private final List<RankSection> rankSections;

    public UpdateRankCommand() {
        super(Configuration.getConfig().getString("command", "updaterank"));
        this.setPermissions("skillranks.updaterank");
        this.setExecutorType(ExecutorType.PLAYER_ONLY);
        this.setRunAsync(false);
        this.placeholder = SkillRanks.getInstance().getPlaceholder();
        this.rankSections = SkillRanks.getInstance().getRankSections();
    }

    @Override
    public void onExecute(CommandExecutor cmd) {
        String[] args = cmd.getArguments();
        if (args.length == 0) {
            this.sendSyntax(cmd);
            return;
        }
        Player player = (Player) cmd.getSender();
        RankSection rank = this.rankSections.stream()
                .filter(x -> x.id().equalsIgnoreCase(args[0]))
                .findFirst()
                .orElse(null);
        if (rank == null || (!rank.permission().isEmpty() && !player.hasPermission(rank.permission()))) {
            Utils.sendMessage(cmd.getSender(), Configuration.getConfig().getString("lang.invalid-rank"));
            return;
        }

        long ttlMs = Configuration.getConfig().getLong("placeholder-cache-ms", 2000L);
        RankService.Result result = RankService.updateRank(player, rank, this.placeholder, ttlMs);
        if (result == RankService.Result.UNCHANGED) {
            Utils.sendMessage(player, Configuration.getConfig().getString("lang.nothing-changed"));
        } else if (result == RankService.Result.NO_MATCH) {
            Utils.sendMessage(player, Configuration.getConfig().getString("lang.no-rank"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandExecutor cmd) {
        return this.rankSections.stream()
                .filter(x -> x.permission().isEmpty() || cmd.getSender().hasPermission(x.permission()))
                .map(RankSection::id)
                .collect(Collectors.toList());
    }
}
