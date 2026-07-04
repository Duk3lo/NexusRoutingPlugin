package org.astral.nexusroutingplugin.events.event;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.ModInfo;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.astral.nexusroutingplugin.config.ConfigManager;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public final class PlayerListener {

    private final ProxyServer proxy;
    private final ConfigManager config;
    private final Logger logger;

    public PlayerListener(ProxyServer proxy, ConfigManager config, Logger logger) {
        this.proxy = proxy;
        this.config = config;
        this.logger = logger;
    }

    @Subscribe(priority = 32767)
    public void onPlayerChooseInitialServer(@NonNull PlayerChooseInitialServerEvent event) {
        String clientType = detectByModInfo(event);
        if (clientType.equals("unknown")) {
            clientType = detectByBrand(event.getPlayer().getClientBrand());
        }

        if (clientType.equals("unknown") || clientType.equals("vanilla")) {
            clientType = config.getDefaultClient();
        }

        String finalClientType = clientType;
        Optional<RegisteredServer> target = switch (finalClientType) {
            case "neoforge" -> getFirstValidServer(config.getRoutesNeoForge());
            case "forge" -> getFirstValidServer(config.getRoutesForge());
            case "fabric" -> getFirstValidServer(config.getRoutesFabric());
            default -> getFirstValidServer(config.getRoutesVanilla());
        };

        target.ifPresent(server -> {
            logger.info("[Router] Jugador {} detectado como [{}]. Enviando a: {}",
                    event.getPlayer().getUsername(), finalClientType.toUpperCase(), server.getServerInfo().getName());
            event.setInitialServer(server);
        });
    }

    private @NonNull String detectByModInfo(@NonNull PlayerChooseInitialServerEvent event) {
        Optional<ModInfo> modInfo = event.getPlayer().getModInfo();
        if (modInfo.isPresent() && !modInfo.get().getMods().isEmpty()) {
            List<ModInfo.Mod> mods = modInfo.get().getMods();
            if (mods.stream().anyMatch(m -> m.getId().contains("neoforge"))) return "neoforge";
            if (mods.stream().anyMatch(m -> m.getId().contains("fabric"))) return "fabric";
            if (mods.stream().anyMatch(m -> m.getId().contains("forge"))) return "forge";
        }
        return "unknown";
    }

    private @NonNull String detectByBrand(String brand) {
        if (brand == null) return "unknown";
        brand = brand.toLowerCase();
        if (brand.contains("neoforge")) return "neoforge";
        if (brand.contains("forge")) return "forge";
        if (brand.contains("fabric")) return "fabric";
        return "vanilla";
    }

    @Subscribe
    public void onPlayerKicked(@NonNull KickedFromServerEvent event) {
        if (event.getServerKickReason().isEmpty()) return;

        String reason = PlainTextComponentSerializer.plainText()
                .serialize(event.getServerKickReason().get()).toLowerCase();
        String kickedFrom = event.getServer().getServerInfo().getName();

        List<String> neoForgeServers = config.getRoutesNeoForge();
        List<String> forgeServers = config.getRoutesForge();
        List<String> vanillaServers = config.getRoutesVanilla();
        List<String> horrorServers = config.getRoutesHorror();
        String defaultClient = config.getDefaultClient();

        if (neoForgeServers.contains(kickedFrom)) {
            if (reason.contains("neoforge") || reason.contains("not running")) {
                if (defaultClient.equals("neoforge")) {
                    getFirstValidServer(forgeServers).ifPresent(s -> redirect(event, s, "NeoForge falló -> Probando Forge"));
                } else {
                    getFirstValidServer(vanillaServers).ifPresent(s -> redirect(event, s, "NeoForge falló -> Enviando a Vanilla"));
                }
                return;
            }
        }

        if (forgeServers.contains(kickedFrom)) {
            if (reason.contains("mismatch") || reason.contains("rejected") || reason.contains("missing") || reason.contains("registry")) {
                getFirstValidServer(horrorServers).ifPresent(s -> redirect(event, s, "Forge -> Enviando a Modpack Horror"));
                return;
            }

            if (reason.contains("forge") || reason.contains("fml") || reason.contains("not running") || reason.contains("require")) {
                if (defaultClient.equals("forge")) {
                    getFirstValidServer(neoForgeServers).ifPresent(s -> redirect(event, s, "Forge falló -> Probando NeoForge"));
                } else {
                    getFirstValidServer(vanillaServers).ifPresent(s -> redirect(event, s, "Forge falló -> Enviando a Vanilla"));
                }
            }
        }
    }

    private void redirect(@NonNull KickedFromServerEvent event, RegisteredServer server, String type) {
        logger.info("[Router Fallback] Cascada activa: {} para {}", type, event.getPlayer().getUsername());
        event.setResult(KickedFromServerEvent.RedirectPlayer.create(server));
    }

    private @NonNull Optional<RegisteredServer> getFirstValidServer(@NonNull List<String> names) {
        for (String name : names) {
            Optional<RegisteredServer> s = proxy.getServer(name);
            if (s.isPresent()) return s;
        }
        return Optional.empty();
    }
}