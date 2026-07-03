package org.astral.nexusroutingplugin.events.event;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.astral.nexusroutingplugin.config.ConfigManager;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

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

    @Subscribe
    public void onPlayerChooseInitialServer(@NonNull PlayerChooseInitialServerEvent event) {
        proxy.getServer(config.getMainModded()).ifPresent(event::setInitialServer);
    }

    @Subscribe
    public void onPlayerKicked(@NonNull KickedFromServerEvent event) {
        if (event.getServerKickReason().isEmpty()) return;

        String reason = PlainTextComponentSerializer.plainText()
                .serialize(event.getServerKickReason().get()).toLowerCase();

        if (reason.contains("neoforge") || reason.contains("mods missing") || reason.contains("not running")) {
            redirect(event, config.getVanilla(), "Vanilla/Bedrock");
            return;
        }

        if (reason.contains("mismatch") || reason.contains("rejected") || reason.contains("incompatible")) {
            if (event.getServer().getServerInfo().getName().equals(config.getMainModded())) {
                redirect(event, config.getHorrorModded(), "Modpack Horror");
            }
        }
    }

    private void redirect(KickedFromServerEvent event, String serverName, String type) {
        Optional<RegisteredServer> target = proxy.getServer(serverName);
        if (target.isPresent()) {
            logger.info("[Router] Jugador {} detectado como {}. Redirigiendo a {}",
                    event.getPlayer().getUsername(), type, serverName);
            event.setResult(KickedFromServerEvent.RedirectPlayer.create(target.get()));
        }
    }
}