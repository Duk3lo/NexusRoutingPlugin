package org.astral.nexusroutingplugin.commands;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.proxy.ProxyServer;
import org.astral.nexusroutingplugin.NexusRoutingPlugin;
import org.astral.nexusroutingplugin.commands.subcommands.ReloadCmd;
import org.astral.nexusroutingplugin.commands.subcommands.InfoCmd;
import org.astral.nexusroutingplugin.config.ConfigManager;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

public final class CommandRegistry {

    public static void registerAll(NexusRoutingPlugin plugin, @NonNull ProxyServer proxy, ConfigManager config, @NonNull Logger logger) {
        MainCommand mainCommand = new MainCommand();

        mainCommand.register(new ReloadCmd(config));
        mainCommand.register(new InfoCmd(config));

        CommandMeta meta = proxy.getCommandManager()
                .metaBuilder("nexusrouting")
                .aliases("nr", "router")
                .plugin(plugin)
                .build();

        proxy.getCommandManager().register(meta, mainCommand);
        logger.info("Comandos registrados correctamente.");
    }
}