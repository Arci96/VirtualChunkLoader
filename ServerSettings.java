package dev.virtualchunkloader.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/** Gelesene Server-Einstellungen, die für Farmen ohne Spieler relevant sind (null = unbekannt). */
public final class ServerSettings {

    private static final String KEY = "entity-activation-range.tick-inactive-villagers";

    private final Integer pauseWhenEmptySeconds;
    private final YamlConfiguration spigot;

    public ServerSettings(Integer pauseWhenEmptySeconds, YamlConfiguration spigot) {
        this.pauseWhenEmptySeconds = pauseWhenEmptySeconds;
        this.spigot = spigot;
    }

    public static ServerSettings unknown() {
        return new ServerSettings(null, null);
    }

    /** Wert von pause-when-empty-seconds aus server.properties; null = unbekannt. */
    public Integer pauseWhenEmptySeconds() {
        return pauseWhenEmptySeconds;
    }

    /** Wert von tick-inactive-villagers aus spigot.yml (Welt-Override vor default); null = unbekannt. */
    public Boolean tickInactiveVillagers(String worldName) {
        if (spigot == null) {
            return null;
        }
        ConfigurationSection ws = spigot.getConfigurationSection("world-settings");
        if (ws == null) {
            return null;
        }
        ConfigurationSection world = ws.getConfigurationSection(worldName);
        if (world != null && world.isSet(KEY)) {
            return world.getBoolean(KEY);
        }
        ConfigurationSection def = ws.getConfigurationSection("default");
        if (def != null && def.isSet(KEY)) {
            return def.getBoolean(KEY);
        }
        return null;
    }
}
