package org.astral.nexusroutingplugin.config;

import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ConfigManager {
    private final Path configFile;
    private final YamlConfigurationLoader loader;
    private CommentedConfigurationNode root;
    private final Logger logger;

    public ConfigManager(@NonNull Path dataDirectory, Logger logger) {
        this.logger = logger;
        this.configFile = dataDirectory.resolve("config.yml");
        this.loader = YamlConfigurationLoader.builder().path(configFile).nodeStyle(NodeStyle.BLOCK).build();
    }

    public void load() {
        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(configFile.getParent());
                root = loader.createNode();

                root.node("default-server").comment("Servidor Vanilla / Lobby por defecto").set("auth");
                root.node("routes").comment("Mapeos manuales por si quieres forzar alguno (opcional)");

                root.node("discovery").comment("Orden de búsqueda si hay conflicto de mods");
                root.node("discovery", "neoforge", "enabled").set(true);
                root.node("discovery", "neoforge", "servers").setList(String.class, List.of("auth_neo1", "auth_neo2"));
                root.node("discovery", "forge", "enabled").set(true);
                root.node("discovery", "forge", "servers").setList(String.class, List.of("auth_forge1", "auth_arclight"));

                loader.save(root);
            } else {
                root = loader.load();
            }
        } catch (Exception e) { logger.error("Error cargando config.yml", e); }
    }

    public @NonNull String getDefaultServer() {
        return root.node("default-server").getString("auth");
    }

    public @Nullable String getManualRoute(String key) {
        String route = root.node("routes", key).getString();
        return (route != null && !route.isEmpty()) ? route : null;
    }

    public @NonNull Map<String, String> getManualRoutes() {
        Map<String, String> routes = new java.util.HashMap<>();
        try {
            root.node("routes").childrenMap().forEach((k, v) -> routes.put(k.toString(), v.getString()));
        } catch (Exception ignored) {}
        return routes;
    }

    public @NonNull @Unmodifiable List<String> getDiscoveryQueue() {
        List<String> queue = new ArrayList<>();
        try {
            Map<Object, ? extends CommentedConfigurationNode> sections = root.node("discovery").childrenMap();
            for (CommentedConfigurationNode section : sections.values()) {
                if (section.node("enabled").getBoolean(false)) {
                    queue.addAll(section.node("servers").getList(String.class, List.of()));
                }
            }
        } catch (Exception ignored) {}
        return queue.stream().distinct().toList();
    }
}