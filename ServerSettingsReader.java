package dev.virtualchunkloader.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Liest server.properties und spigot.yml (nur lesend). Enthält Datei-I/O: NICHT im Hauptthread aufrufen. */
public final class ServerSettingsReader {

    private ServerSettingsReader() {
    }

    public static ServerSettings read(File serverRoot, Logger log) {
        Integer pause = null;
        File props = new File(serverRoot, "server.properties");
        if (props.isFile()) {
            try (Reader r = Files.newBufferedReader(props.toPath(), StandardCharsets.UTF_8)) {
                Properties p = new Properties();
                p.load(r);
                String v = p.getProperty("pause-when-empty-seconds");
                if (v != null) {
                    pause = Integer.parseInt(v.trim());
                }
            } catch (IOException | NumberFormatException e) {
                log.log(Level.WARNING, "server.properties konnte nicht ausgewertet werden: " + e.getMessage());
            }
        }

        YamlConfiguration spigot = null;
        File sf = new File(serverRoot, "spigot.yml");
        if (sf.isFile()) {
            YamlConfiguration y = new YamlConfiguration();
            try {
                y.load(sf);
                spigot = y;
            } catch (IOException | InvalidConfigurationException e) {
                log.log(Level.WARNING, "spigot.yml konnte nicht ausgewertet werden: " + e.getMessage());
            }
        }
        return new ServerSettings(pause, spigot);
    }
}
