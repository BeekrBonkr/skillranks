package com.olziedev.skillranks;

import com.olziedev.olziecommand.v1_3_3.OlzieCommand;
import com.olziedev.olziecommand.v1_3_3.framework.action.CommandActionType;
import com.olziedev.skillranks.utils.Configuration;
import com.olziedev.skillranks.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class SkillRanks extends JavaPlugin {

    private static SkillRanks instance;

    @Override
    public void onEnable() {
        instance = this;
        new Configuration(this).load();
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
    }

    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        instance = null;
    }

    public static SkillRanks getInstance() {
        return instance;
    }
}
