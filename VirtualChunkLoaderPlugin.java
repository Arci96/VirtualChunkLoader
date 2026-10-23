package dev.virtualchunkloader;

import dev.virtualchunkloader.command.VclCommand;
import dev.virtualchunkloader.config.PluginSettings;
import dev.virtualchunkloader.config.ServerSettings;
import dev.virtualchunkloader.config.ServerSettingsReader;
import dev.virtualchunkloader.core.LoaderManager;
import dev.virtualchunkloader.core.TicketManager;
import dev.virtualchunkloader.listener.WorldListener;
import dev.virtualchunkloader.storage.LoaderStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class VirtualChunkLoaderPlugin extends JavaPlugin {

    private volatile ServerSettings serverSettings = ServerSettings.unknown();
    private LoaderManager manager;
    private PluginSettings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = PluginSettings.from(getConfig(), getLogger());

        LoaderStorage storage = new LoaderStorage(this, new File(getDataFolder(), settings.storageFile()));
        TicketManager tickets = new TicketManager(this);
        manager = new LoaderManager(this, settings, tickets, storage);

        PluginCommand command = getCommand("vcl");
        if (command == null) {
            getLogger().severe("Befehl 'vcl' fehlt in der plugin.yml. Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        VclCommand handler = new VclCommand(this, manager);
        command.setExecutor(handler);
        command.setTabCompleter(handler);
        getServer().getPluginManager().registerEvents(new WorldListener(manager), this);

        refreshServerSettings();
        manager.loadAsync();
        getLogger().info("VirtualChunkLoader " + getPluginMeta().getVersion() + " gestartet (Modus: "
                + settings.simulationMode() + ").");
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
    }

    /** Lädt die Konfiguration neu (Hauptthread). */
    public void reloadAll() {
        reloadConfig();
        settings = PluginSettings.from(getConfig(), getLogger());
        manager.applySettings(settings);
        refreshServerSettings();
    }

    /** Liest server.properties/spigot.yml asynchron neu ein. */
    public void refreshServerSettings() {
        File root = getDataFolder().getAbsoluteFile().getParentFile().getParentFile();
        getServer().getScheduler().runTaskAsynchronously(this,
                () -> serverSettings = ServerSettingsReader.read(root, getLogger()));
    }

    public ServerSettings serverSettings() {
        return serverSettings;
    }

    public PluginSettings settings() {
        return settings;
    }

    public void debug(String message) {
        if (settings != null && settings.debug()) {
            getLogger().info("[debug] " + message);
        }
    }
}
