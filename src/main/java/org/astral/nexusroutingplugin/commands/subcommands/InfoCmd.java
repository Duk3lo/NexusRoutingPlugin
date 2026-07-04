package org.astral.nexusroutingplugin.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.astral.nexusroutingplugin.config.ConfigManager;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class InfoCmd implements SubCommand {
    private final ConfigManager configManager;

    public InfoCmd(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override public @NonNull String getName() { return "info"; }
    @Override public @NonNull String getPermission() { return "nexusrouting.admin"; }

    @Override
    public void execute(@NonNull CommandSource source, String[] args) {
        source.sendMessage(Component.text("=== Rutas NexusRouting ===", NamedTextColor.AQUA));

        source.sendMessage(Component.text("• First-Try (Por defecto): ", NamedTextColor.GOLD)
                .append(Component.text(configManager.getDefaultClient().toUpperCase(), NamedTextColor.YELLOW)));

        source.sendMessage(Component.text("• Vanilla/Bedrock: ", NamedTextColor.GRAY)
                .append(Component.text(String.join(", ", configManager.getRoutesVanilla()), NamedTextColor.GREEN)));

        source.sendMessage(Component.text("• Forge: ", NamedTextColor.GRAY)
                .append(Component.text(String.join(", ", configManager.getRoutesForge()), NamedTextColor.GREEN)));

        source.sendMessage(Component.text("• NeoForge: ", NamedTextColor.GRAY)
                .append(Component.text(String.join(", ", configManager.getRoutesNeoForge()), NamedTextColor.GREEN)));

        source.sendMessage(Component.text("• Fabric: ", NamedTextColor.GRAY)
                .append(Component.text(String.join(", ", configManager.getRoutesFabric()), NamedTextColor.GREEN)));

        source.sendMessage(Component.text("• Horror Modpack: ", NamedTextColor.GRAY)
                .append(Component.text(String.join(", ", configManager.getRoutesHorror()), NamedTextColor.GREEN)));

        List<String> globalForce = configManager.getForceGlobal();
        if (globalForce.isEmpty()) {
            source.sendMessage(Component.text("• Forzado Global: DESACTIVADO", NamedTextColor.YELLOW));
        } else {
            source.sendMessage(Component.text("• Forzado Global: ACTIVO -> " + String.join(", ", globalForce), NamedTextColor.RED));
        }
    }

    @Override public @NonNull List<String> suggest(CommandSource source, String[] args) { return List.of(); }
}