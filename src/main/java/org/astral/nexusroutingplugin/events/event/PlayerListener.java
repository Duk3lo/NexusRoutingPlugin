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
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.astral.nexusroutingplugin.NexusRoutingPlugin;
import org.astral.nexusroutingplugin.config.ConfigManager;
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
    private final Logger logger;

    public static final MinecraftChannelIdentifier ROUTER_CHANNEL = MinecraftChannelIdentifier.from("lumineriabase:router");
    public static final MinecraftChannelIdentifier SYNC_CHANNEL = MinecraftChannelIdentifier.from("lumineriabase:sync");

    private final Map<UUID, RoutingState> routingMap = new ConcurrentHashMap<>();
    private final Set<UUID> resolvedPlayers = ConcurrentHashMap.newKeySet();

    private static class RoutingState {
        String originalKey;
        String currentServer;
        boolean inDiscovery = false;
        List<String> discoveryQueue;
        int discoveryIndex = 0;

        RoutingState(String key) { this.originalKey = key; }
    }

    public PlayerListener(@NonNull ProxyServer proxy, ConfigManager config, Logger logger) {
        this.proxy = proxy;
        this.config = config;
        this.logger = logger;
        proxy.getChannelRegistrar().register(ROUTER_CHANNEL, SYNC_CHANNEL);
    }

    @Subscribe(priority = 32767)
    public void onPlayerChooseInitialServer(@NonNull PlayerChooseInitialServerEvent event) {
        proxy.getServer(config.getDefaultServer()).ifPresent(event::setInitialServer);
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
        if (!(event.getSource() instanceof Player player)) return;

        if (resolvedPlayers.contains(player.getUniqueId())) return;

        String routingKey;
        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
            routingKey = in.readUTF().trim();
        } catch (Exception e) {
            routingKey = new String(event.getData(), StandardCharsets.UTF_8).trim();
        }

        if (routingKey.isEmpty() || routingKey.equalsIgnoreCase("null")) return;

        RoutingState existing = routingMap.get(player.getUniqueId());
        if (existing != null && existing.inDiscovery) {
            existing.originalKey = routingKey;
            return;
        }

        RoutingState state = new RoutingState(routingKey);
        logger.info("[Enrutador] Jugador {} envió la llave: '{}'", player.getUsername(), routingKey);

        Optional<RegisteredServer> targetServer = proxy.getServer(routingKey);

        if (targetServer.isEmpty()) {
            String manualOverride = config.getManualRoute(routingKey);
            if (manualOverride != null && !manualOverride.isEmpty()) {
                targetServer = proxy.getServer(manualOverride);
            }
        }

        if (targetServer.isPresent()) {
            state.currentServer = targetServer.get().getServerInfo().getName();
            routingMap.put(player.getUniqueId(), state);
            player.createConnectionRequest(targetServer.get()).connect().thenAccept(result -> {
                if (!result.isSuccessful()) {
                    startDiscovery(player, state);
                }
            });
        } else {
            startDiscovery(player, state);
        }
    }

    private void startDiscovery(Player player, @NonNull RoutingState state) {
        state.discoveryQueue = config.getDiscoveryQueue();
        state.inDiscovery = true;
        state.discoveryIndex = 0;

        if (state.discoveryQueue.isEmpty()) {
            logger.warn("[Discovery] No hay servidores configurados. Mandando a Vanilla...");
            routingMap.remove(player.getUniqueId());
            return;
        }

        logger.info("[Discovery] Buscando servidor compatible para '{}'", player.getUsername());
        routingMap.put(player.getUniqueId(), state);
        tryNextDiscoveryServer(player, state);
    }

    private void tryNextDiscoveryServer(Player player, @NonNull RoutingState state) {
        if (state.discoveryIndex >= state.discoveryQueue.size()) {
            logger.warn("[Discovery] Agotamos la lista para {}.", player.getUsername());
            routingMap.remove(player.getUniqueId());
            return;
        }

        String nextServer = state.discoveryQueue.get(state.discoveryIndex);
        state.currentServer = nextServer;
        Optional<RegisteredServer> serverOpt = proxy.getServer(nextServer);

        if (serverOpt.isPresent()) {
            logger.info("[Discovery] Probando servidor '{}' para {}", nextServer, player.getUsername());
            player.createConnectionRequest(serverOpt.get()).connect().thenAccept(result -> {
                if (!result.isSuccessful() && result.getReasonComponent().isEmpty()) {
                    state.discoveryIndex++;
                    tryNextDiscoveryServer(player, state);
                }
            });
        } else {
            state.discoveryIndex++;
            tryNextDiscoveryServer(player, state);
        }
    }

    @Subscribe
    public void onKicked(@NonNull KickedFromServerEvent event) {
        Player player = event.getPlayer();
        String serverName = event.getServer().getServerInfo().getName();

        if (event.getServerKickReason().isPresent()) {
            String reason = PlainTextComponentSerializer.plainText().serialize(event.getServerKickReason().get()).toLowerCase();
            if (reason.contains("mod") || reason.contains("forge") || reason.contains("mismatch") || reason.contains("registry") || reason.contains("incompatible") || reason.contains("fml")) {

                RoutingState state = routingMap.get(player.getUniqueId());

                if (state == null) {
                    logger.info("[Interceptado] {} expulsado de '{}' por mods antes de enviar llave. Iniciando Discovery forzado.", player.getUsername(), serverName);
                    state = new RoutingState("UNKNOWN");
                    state.discoveryQueue = config.getDiscoveryQueue();
                    state.inDiscovery = true;
                    state.discoveryIndex = 0;
                    routingMap.put(player.getUniqueId(), state);
                } else {
                    logger.info("[Interceptado] {} expulsado de '{}' por mods. Redirigiendo al siguiente...", player.getUsername(), serverName);
                    state.discoveryIndex++;
                }

                while (state.discoveryIndex < state.discoveryQueue.size()) {
                    String nextServerName = state.discoveryQueue.get(state.discoveryIndex);
                    Optional<RegisteredServer> nextServer = proxy.getServer(nextServerName);

                    if (nextServer.isPresent()) {
                        state.currentServer = nextServerName;
                        logger.info("[Discovery] Probando INMEDIATAMENTE el servidor '{}' para {}", nextServerName, player.getUsername());
                        event.setResult(KickedFromServerEvent.RedirectPlayer.create(nextServer.get()));
                        return;
                    }
                    state.discoveryIndex++;
                }

                routingMap.remove(player.getUniqueId());
            }
        }
    }

    @Subscribe
    public void onServerConnected(@NonNull ServerConnectedEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        String actualServer = event.getServer().getServerInfo().getName();

        if (resolvedPlayers.contains(id)) return;

        RoutingState state = routingMap.get(id);

        if (state != null) {
            if (actualServer.equals(state.currentServer)) {
                if (!actualServer.equals(state.originalKey)) {
                    sendSyncPacket(player, actualServer);
                    logger.info("[¡Éxito!] {} compatible con '{}'. Sincronizando cliente...", player.getUsername(), actualServer);
                } else {
                    logger.info("[¡Éxito!] {} entró directo a '{}'.", player.getUsername(), actualServer);
                }
                routingMap.remove(id);
                resolvedPlayers.add(id);
            }
        } else {
            sendSyncPacket(player, actualServer);
            resolvedPlayers.add(id);
            logger.info("[Fallback] {} conectó a '{}'. Sincronizando cliente...", player.getUsername(), actualServer);
        }
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