package org.astral.nexusroutingplugin.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.astral.nexusroutingplugin.config.ConfigManager;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import java.util.List;

public final class ReloadCmd implements SubCommand {
    private final ConfigManager config;
    public ReloadCmd(ConfigManager config) { this.config = config; }

    @Override public @NonNull String getName() { return "reload"; }
    @Override public @NonNull String getPermission() { return "nexusrouting.admin"; }

    @Override
    public void execute(@NonNull CommandSource source, String[] args) {
        config.load();
        source.sendMessage(Component.text("¡NexusRouting recargado!", NamedTextColor.GREEN));
    }
    @Contract(pure = true)
    @Override public @NonNull List<String> suggest(CommandSource s, String[] a) { return List.of(); }
}