package org.astral.nexusroutingplugin.commands.subcommands;

import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.astral.nexusroutingplugin.config.ConfigManager;
import org.jspecify.annotations.NonNull;

import java.util.List;

public final class ReloadCmd implements SubCommand {
    private final ConfigManager configManager;

    public ReloadCmd(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override public @NonNull String getName() { return "reload"; }
    @Override public @NonNull String getPermission() { return "nexusrouting.admin"; }

    @Override
    public void execute(@NonNull CommandSource source, String[] args) {
        source.sendMessage(Component.text("Recargando reglas de enrutamiento...", NamedTextColor.YELLOW));
        configManager.load();
        source.sendMessage(Component.text("¡NexusRouting recargado con éxito!", NamedTextColor.GREEN));
    }

    @Override
    public List<String> suggest(CommandSource source, String[] args) { return List.of(); }
}