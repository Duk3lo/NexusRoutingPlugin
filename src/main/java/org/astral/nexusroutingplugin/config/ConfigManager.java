package org.astral.nexusroutingplugin.config;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private final Path configFile;
    private final YamlConfigurationLoader loader;
    private CommentedConfigurationNode root;
    private final Logger logger;

    public ConfigManager(@NonNull Path dataDirectory, Logger logger) {
        this.logger = logger;
        this.configFile = dataDirectory.resolve("config.yml");
        this.loader = YamlConfigurationLoader.builder().path(configFile).build();
    }

    public void load() {
        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(configFile.getParent());
                root = loader.createNode();

                root.node("server-vanilla").set("auth");
                root.node("server-modded-main").set("auth_arclight");
                root.node("server-modded-horror").set("auth_arclight_horror");

                loader.save(root);
                logger.info("Configuración generada. Asegúrate de que los nombres coincidan con velocity.toml");
            } else {
                root = loader.load();
            }
        } catch (Exception e) {
            logger.error("Error cargando config.yml", e);
        }
    }

    public @NonNull String getVanilla() { return root.node("server-vanilla").getString("auth"); }
    public @NonNull String getMainModded() { return root.node("server-modded-main").getString("auth_arclight"); }
    public @NonNull String getHorrorModded() { return root.node("server-modded-horror").getString("auth_arclight_horror"); }
}