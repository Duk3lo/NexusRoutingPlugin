package org.astral.nexusroutingplugin.events;

import com.velocitypowered.api.proxy.ProxyServer;
import org.astral.nexusroutingplugin.NexusRoutingPlugin;
import org.astral.nexusroutingplugin.config.ConfigManager;
import org.astral.nexusroutingplugin.events.event.PlayerListener;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

public final class RegisterEvents {

    public static void registerAll(
            @NonNull NexusRoutingPlugin plugin,
            @NonNull ProxyServer proxy,
            @NonNull ConfigManager configManager,
            @NonNull Logger logger
    ) {
        var manager = proxy.getEventManager();
        manager.register(plugin, new PlayerListener(proxy, configManager, logger));
    }
}