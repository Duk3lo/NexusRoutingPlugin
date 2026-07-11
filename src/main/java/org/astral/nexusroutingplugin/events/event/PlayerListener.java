package org.astral.nexusroutingplugin.events.event;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.astral.nexusroutingplugin.NexusRoutingPlugin;
import org.astral.nexusroutingplugin.config.ConfigManager;
import org.astral.nexusroutingplugin.config.PlayerDataManager;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class PlayerListener {
    private final ProxyServer proxy;
    private final ConfigManager config;
    private final PlayerDataManager playerData;
    private final Logger logger;

    public static final MinecraftChannelIdentifier ROUTER_CHANNEL = MinecraftChannelIdentifier.from("lumineriabase:router");
    public static final MinecraftChannelIdentifier SYNC_CHANNEL = MinecraftChannelIdentifier.from("lumineriabase:sync");

    private final Map<UUID, RoutingState> routingMap = new ConcurrentHashMap<>();
    private final Set<UUID> resolvedPlayers = ConcurrentHashMap.newKeySet();

    private static class RoutingState {
        final List<String> discoveryQueue;
        int discoveryIndex;
        String currentServer;

        RoutingState(List<String> queue, int startIndex) {
            this.discoveryQueue = queue;
            this.discoveryIndex = startIndex;
        }
    }

    public PlayerListener(@NonNull ProxyServer proxy, ConfigManager config, PlayerDataManager playerData, Logger logger) {
        this.proxy = proxy;
        this.config = config;
        this.playerData = playerData;
        this.logger = logger;
        proxy.getChannelRegistrar().register(ROUTER_CHANNEL, SYNC_CHANNEL);
    }

    @Subscribe(priority = 32767)
    public void onPlayerChooseInitialServer(@NonNull PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        List<String> queue = config.getDiscoveryQueue();
        if (queue.isEmpty()) {
            logger.warn("[Discovery] No hay servidores configurados en 'discovery' dentro de config.yml.");
            return;
        }

        int startIndex = 0;
        String remembered = playerData.getLastServer(id);
        if (remembered != null) {
            int idx = queue.indexOf(remembered);
            if (idx >= 0) {
                startIndex = idx;
                logger.info("[Memoria] {} tiene registro previo: '{}'. Se probará primero.", player.getUsername(), remembered);
            }
        }

        RoutingState state = new RoutingState(queue, startIndex);
        routingMap.put(id, state);

        placeCandidate(player, state, event);
    }

    private void placeCandidate(Player player, @NonNull RoutingState state, PlayerChooseInitialServerEvent event) {
        while (state.discoveryIndex < state.discoveryQueue.size()) {
            String candidate = state.discoveryQueue.get(state.discoveryIndex);
            Optional<RegisteredServer> server = proxy.getServer(candidate);

            if (server.isPresent()) {
                state.currentServer = candidate;
                event.setInitialServer(server.get());
                logger.info("[Discovery] Servidor inicial para {}: '{}'", player.getUsername(), candidate);
                return;
            }

            logger.warn("[Discovery] '{}' no está registrado en velocity.toml, se salta.", candidate);
            state.discoveryIndex++;
        }

        logger.warn("[Discovery] Se agotó la lista de candidatos para {} sin encontrar ninguno registrado.", player.getUsername());
        routingMap.remove(player.getUniqueId());
    }

    @Subscribe
    public void onDisconnect(@NonNull DisconnectEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        routingMap.remove(id);
        resolvedPlayers.remove(id);
    }

    @Subscribe
    public void onPluginMessage(@NonNull PluginMessageEvent event) {
        if (!event.getIdentifier().equals(ROUTER_CHANNEL)) return;
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    @Subscribe
    public void onKicked(@NonNull KickedFromServerEvent event) {
        Player player = event.getPlayer();
        String serverName = event.getServer().getServerInfo().getName();

        if (event.getServerKickReason().isEmpty()) return;

        String reason = PlainTextComponentSerializer.plainText().serialize(event.getServerKickReason().get()).toLowerCase();
        boolean modMismatch = reason.contains("mod") || reason.contains("forge") || reason.contains("mismatch")
                || reason.contains("registry") || reason.contains("incompatible") || reason.contains("fml");

        if (!modMismatch) return;

        RoutingState state = routingMap.get(player.getUniqueId());

        if (state == null) {
            logger.info("[Interceptado] {} expulsado de '{}' sin estado previo. Iniciando Discovery desde cero.", player.getUsername(), serverName);
            state = new RoutingState(config.getDiscoveryQueue(), 0);
            routingMap.put(player.getUniqueId(), state);
        } else {
            logger.info("[Interceptado] {} expulsado de '{}' por mods. Probando siguiente candidato...", player.getUsername(), serverName);
            state.discoveryIndex++;
        }

        while (state.discoveryIndex < state.discoveryQueue.size()) {
            String nextName = state.discoveryQueue.get(state.discoveryIndex);
            Optional<RegisteredServer> next = proxy.getServer(nextName);

            if (next.isPresent()) {
                state.currentServer = nextName;
                logger.info("[Discovery] Redirigiendo a {} hacia '{}'", player.getUsername(), nextName);
                event.setResult(KickedFromServerEvent.RedirectPlayer.create(next.get()));
                return;
            }
            state.discoveryIndex++;
        }

        logger.warn("[Discovery] Se agotaron los candidatos para {}.", player.getUsername());
        routingMap.remove(player.getUniqueId());
    }

    @Subscribe
    public void onServerConnected(@NonNull ServerConnectedEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        String actualServer = event.getServer().getServerInfo().getName();

        if (resolvedPlayers.contains(id)) return;

        playerData.setLastServer(id, actualServer);
        resolvedPlayers.add(id);
        routingMap.remove(id);

        sendSyncPacket(player, actualServer);
        logger.info("[¡Éxito!] {} quedó en '{}'. Guardado para futuras conexiones.", player.getUsername(), actualServer);
    }

    private void sendSyncPacket(@NonNull Player player, @NonNull String newKey) {
        proxy.getScheduler().buildTask(NexusRoutingPlugin.getInstance(), () -> {
            byte[] stringBytes = newKey.getBytes(StandardCharsets.UTF_8);

            if (player.getProtocolVersion().getProtocol() <= ProtocolVersion.MINECRAFT_1_20.getProtocol()) {
                MinecraftChannelIdentifier FORGE_CHANNEL = MinecraftChannelIdentifier.from("lumineriabase:main");
                byte[] payload = new byte[1 + stringBytes.length];
                payload[0] = 4;
                System.arraycopy(stringBytes, 0, payload, 1, stringBytes.length);
                player.sendPluginMessage(FORGE_CHANNEL, payload);
            } else {
                player.sendPluginMessage(SYNC_CHANNEL, stringBytes);
            }
        }).delay(1500, TimeUnit.MILLISECONDS).schedule();
    }
}