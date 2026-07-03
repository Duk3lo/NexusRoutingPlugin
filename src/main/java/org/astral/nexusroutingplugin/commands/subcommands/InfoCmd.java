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
        source.sendMessage(Component.text("=== Configuración de Roles NexusRouting ===", NamedTextColor.AQUA));

        source.sendMessage(Component.text("• Servidor Vanilla/Bedrock: ", NamedTextColor.GRAY)
                .append(Component.text(configManager.getVanilla(), NamedTextColor.GREEN)));

        source.sendMessage(Component.text("• Servidor Mods (Principal): ", NamedTextColor.GRAY)
                .append(Component.text(configManager.getMainModded(), NamedTextColor.GREEN)));

        source.sendMessage(Component.text("• Servidor Mods (Horror): ", NamedTextColor.GRAY)
                .append(Component.text(configManager.getHorrorModded(), NamedTextColor.GREEN)));

        source.sendMessage(Component.text("Lógica activa: Redirección automática por rechazo de NeoForge.", NamedTextColor.YELLOW));
    }

    @Override public @NonNull List<String> suggest(CommandSource source, String[] args) { return List.of(); }
}