package org.astral.nexusroutingplugin.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.astral.nexusroutingplugin.config.ConfigManager;
import org.jspecify.annotations.NonNull;
import java.util.List;

public final class InfoCmd implements SubCommand {
    private final ConfigManager config;
    public InfoCmd(ConfigManager config) { this.config = config; }

    @Override public @NonNull String getName() { return "info"; }
    @Override public @NonNull String getPermission() { return "nexusrouting.admin"; }

    @Override
    public void execute(@NonNull CommandSource source, String[] args) {
        source.sendMessage(Component.text("=== NexusRouting Status ===", NamedTextColor.AQUA));
        source.sendMessage(Component.text("Default Vanilla: " + config.getDefaultServer(), NamedTextColor.YELLOW));
        source.sendMessage(Component.text("Discovery Queue: " + String.join(", ", config.getDiscoveryQueue()), NamedTextColor.GOLD));

        source.sendMessage(Component.text("- Rutas Manuales (Overrides):", NamedTextColor.GRAY));
        config.getManualRoutes().forEach((k, v) ->
                source.sendMessage(Component.text("  " + k + " -> " + v, NamedTextColor.WHITE))
        );
    }

    @Override public @NonNull List<String> suggest(CommandSource s, String[] a) { return List.of(); }
}