package org.astral.nexusroutingplugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.astral.nexusroutingplugin.commands.CommandRegistry;
import org.astral.nexusroutingplugin.config.ConfigManager;
import org.astral.nexusroutingplugin.events.RegisterEvents;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "nexusrouting",
        name = "NexusRouting",
        version = "2.0.0",
        description = "Enrutador inteligente para redes mixtas (Vanilla/Mods)",
        authors = {"Duk3lo", "Astral"}
)
public final class NexusRoutingPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public NexusRoutingPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        ConfigManager configManager = new ConfigManager(dataDirectory, logger);
        configManager.load();

        RegisterEvents.registerAll(this, proxy, configManager, logger);
        CommandRegistry.registerAll(this, proxy, configManager, logger);

        logger.info("NexusRouting v2.0 cargado. Lógica de enrutamiento inteligente activada.");
    }
}