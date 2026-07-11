package org.astral.nexusroutingplugin.config;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataManager {
    private final Path dataFile;
    private final YamlConfigurationLoader loader;
    private final Logger logger;
    private final Map<UUID, String> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(@NonNull Path dataDirectory, Logger logger) {
        this.logger = logger;
        this.dataFile = dataDirectory.resolve("playerdata.yml");
        this.loader = YamlConfigurationLoader.builder().path(dataFile).nodeStyle(NodeStyle.BLOCK).build();
    }

    public void load() {
        try {
            if (!Files.exists(dataFile)) return;
            CommentedConfigurationNode root = loader.load();
            for (Map.Entry<Object, ? extends CommentedConfigurationNode> e : root.node("players").childrenMap().entrySet()) {
                try {
                    UUID id = UUID.fromString(e.getKey().toString());
                    String server = e.getValue().getString();
                    if (server != null && !server.isEmpty()) {
                        cache.put(id, server);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            logger.info("[PlayerData] Cargados {} registros de jugadores.", cache.size());
        } catch (Exception e) {
            logger.error("Error cargando playerdata.yml", e);
        }
    }

    public @Nullable String getLastServer(@NonNull UUID playerId) {
        return cache.get(playerId);
    }

    public void setLastServer(@NonNull UUID playerId, @NonNull String serverName) {
        if (serverName.equals(cache.get(playerId))) return;
        cache.put(playerId, serverName);
        save();
    }

    public void save() {
        try {
            CommentedConfigurationNode root = loader.createNode();
            for (Map.Entry<UUID, String> entry : cache.entrySet()) {
                root.node("players", entry.getKey().toString()).set(entry.getValue());
            }
            loader.save(root);
        } catch (Exception e) {
            logger.error("Error guardando playerdata.yml", e);
        }
    }
}