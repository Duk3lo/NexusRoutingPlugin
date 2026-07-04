package org.astral.nexusroutingplugin.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ConfigManager {
    private final Path configFile;
    private final YamlConfigurationLoader loader;
    private CommentedConfigurationNode root;
    private final Logger logger;

    public ConfigManager(@NonNull Path dataDirectory, Logger logger) {
        this.logger = logger;
        this.configFile = dataDirectory.resolve("config.yml");
        this.loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .nodeStyle(NodeStyle.BLOCK)
                .build();
    }

    public void load() {
        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(configFile.getParent());
                root = loader.createNode();

                root.node("first-try", "default-client").set("neoforge")
                        .comment("Si no se detecta el cliente, ¿qué intentamos primero? (neoforge o forge)");

                root.node("routes", "vanilla").setList(String.class, List.of("auth"));
                root.node("routes", "forge").setList(String.class, List.of("arclight_horror"));
                root.node("routes", "neoforge").setList(String.class, List.of("auth_arclight"));
                root.node("routes", "fabric").setList(String.class, List.of("auth"));
                root.node("routes", "horror").setList(String.class, List.of("auth_arclight_horror"));

                loader.save(root);
            } else {
                root = loader.load();
            }
        } catch (Exception e) {
            logger.error("Error cargando config.yml", e);
        }
    }

    public @NonNull String getDefaultClient() { return root.node("first-try", "default-client").getString("neoforge").toLowerCase(); }
    public @NonNull List<String> getRoutesVanilla() { return getListSafely("routes", "vanilla"); }
    public @NonNull List<String> getRoutesForge() { return getListSafely("routes", "forge"); }
    public @NonNull List<String> getRoutesNeoForge() { return getListSafely("routes", "neoforge"); }
    public @NonNull List<String> getRoutesFabric() { return getListSafely("routes", "fabric"); }
    public @NonNull List<String> getRoutesHorror() { return getListSafely("routes", "horror"); }
    public @NonNull List<String> getForceGlobal() { return getListSafely("force-routes", "global"); }

    private @NonNull List<String> getListSafely(String... path) {
        try {
            List<String> list = root.node((Object[]) path).getList(String.class);
            return list != null ? list : List.of();
        } catch (Exception e) { return List.of(); }
    }
}