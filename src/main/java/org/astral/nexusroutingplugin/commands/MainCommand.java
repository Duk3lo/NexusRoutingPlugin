package org.astral.nexusroutingplugin.commands;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.astral.nexusroutingplugin.commands.subcommands.SubCommand;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MainCommand implements SimpleCommand {
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public void register(SubCommand cmd) {
        subCommands.put(cmd.getName().toLowerCase(), cmd);
    }

    @Override
    public void execute(@NonNull Invocation invocation) {
        String[] args = invocation.arguments();
        var source = invocation.source();

        if (args.length == 0 || !subCommands.containsKey(args[0].toLowerCase())) {
            source.sendMessage(Component.text("Comandos: " + String.join(", ", subCommands.keySet()), NamedTextColor.YELLOW));
            return;
        }

        SubCommand sub = subCommands.get(args[0].toLowerCase());

        if (!source.hasPermission(sub.getPermission())) {
            source.sendMessage(Component.text("No tienes permisos.", NamedTextColor.RED));
            return;
        }

        sub.execute(source, args);
    }

    @Override
    public List<String> suggest(@NonNull Invocation invocation) {
        String[] args = invocation.arguments();
        var source = invocation.source();

        if (args.length <= 1) {
            String input = args.length == 1 ? args[0].toLowerCase() : "";
            return subCommands.values().stream()
                    .filter(cmd -> source.hasPermission(cmd.getPermission()))
                    .map(SubCommand::getName)
                    .filter(name -> name.startsWith(input))
                    .collect(Collectors.toList());
        }

        SubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub != null && source.hasPermission(sub.getPermission())) {
            return sub.suggest(source, args);
        }

        return List.of();
    }
}