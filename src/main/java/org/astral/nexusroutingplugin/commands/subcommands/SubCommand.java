package org.astral.nexusroutingplugin.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import java.util.List;

public interface SubCommand {
    void execute(CommandSource source, String[] args);
    List<String> suggest(CommandSource source, String[] args);
    String getName();
    String getPermission();
}