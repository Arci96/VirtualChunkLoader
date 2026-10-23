package dev.virtualchunkloader.storage;

import dev.virtualchunkloader.model.ChunkLoaderData;
import dev.virtualchunkloader.model.LoaderSchedule;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persistenz der Chunkloader in einer YAML-Datei.
 * Serialisierung passiert im Hauptthread (schnell, im Speicher), das Schreiben asynchron und atomar.
 */
public final class LoaderStorage {

    private final Plugin plugin;
    private final File file;
    private final Logger log;
    private final AtomicLong sequence = new AtomicLong();
    private final Object writeLock = new Object();
    private long lastWritten = 0;

    public LoaderStorage(Plugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
        this.log = plugin.getLogger();
    }

    /** Liest die Datei. Enthält Datei-I/O: nicht im Hauptthread aufrufen (außer beim Herunterfahren). */
    public List<ChunkLoaderData> load() {
        List<ChunkLoaderData> result = new ArrayList<>();
        if (!file.isFile()) {
            return result;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            log.log(Level.SEVERE, "Chunkloader-Datei ist unlesbar: " + e.getMessage());
            backupBroken();
            return result;
        }
        ConfigurationSection root = yaml.getConfigurationSection("loaders");
        if (root == null) {
            return result;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(name);
            if (s == null) {
                continue;
            }
            try {
                String world = s.getString("world");
                if (world == null) {
                    throw new IllegalArgumentException("Eintrag 'world' fehlt");
                }
                LoaderSchedule schedule = LoaderSchedule.always();
                if ("TIME_WINDOW".equalsIgnoreCase(s.getString("schedule.mode", "ALWAYS"))) {
                    schedule = LoaderSchedule.window(
                            LocalTime.parse(s.getString("schedule.start", "")),
                            LocalTime.parse(s.getString("schedule.end", "")));
                }
                result.add(new ChunkLoaderData(name, world,
                        s.getInt("x"), s.getInt("y"), s.getInt("z"),
                        s.getInt("radius", 1),
                        s.getBoolean("enabled", true),
                        s.getString("player-name", "FarmLoader_" + name),
                        schedule));
            } catch (RuntimeException e) {
                log.warning("Chunkloader '" + name + "' wird übersprungen: " + e.getMessage());
            }
        }
        return result;
    }

    /** Erzeugt den YAML-Text (reine Speicheroperation). */
    public static String serialize(Collection<ChunkLoaderData> data) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("version", 1);
        for (ChunkLoaderData d : data) {
            String p = "loaders." + d.name() + ".";
            y.set(p + "world", d.world());
            y.set(p + "x", d.x());
            y.set(p + "y", d.y());
            y.set(p + "z", d.z());
            y.set(p + "radius", d.radius());
            y.set(p + "enabled", d.enabled());
            y.set(p + "player-name", d.playerName());
            y.set(p + "schedule.mode", d.schedule().mode().name());
            y.set(p + "schedule.start", d.schedule().start().format(LoaderSchedule.FORMAT));
            y.set(p + "schedule.end", d.schedule().end().format(LoaderSchedule.FORMAT));
        }
        return y.saveToString();
    }

    /** Schreibt asynchron. Neuere Schreibvorgänge haben Vorrang vor älteren. */
    public void writeAsync(String yaml) {
        long seq = sequence.incrementAndGet();
        Runnable task = () -> write(yaml, seq);
        try {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        } catch (IllegalPluginAccessException e) {
            task.run();
        }
    }

    /** Synchrones Schreiben, nur für onDisable. */
    public void writeNow(String yaml) {
        write(yaml, sequence.incrementAndGet());
    }

    private void write(String yaml, long seq) {
        synchronized (writeLock) {
            if (seq < lastWritten) {
                return;
            }
            lastWritten = seq;
            try {
                File parent = file.getAbsoluteFile().getParentFile();
                if (parent != null) {
                    Files.createDirectories(parent.toPath());
                }
                Path target = file.toPath();
                Path tmp = target.resolveSibling(file.getName() + ".tmp");
                Files.writeString(tmp, yaml, StandardCharsets.UTF_8);
                try {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                log.log(Level.SEVERE, "Chunkloader konnten nicht gespeichert werden: " + e.getMessage());
            }
        }
    }

    private void backupBroken() {
        try {
            Path backup = file.toPath().resolveSibling(file.getName() + ".broken-" + System.currentTimeMillis());
            Files.copy(file.toPath(), backup);
            log.severe("Defekte Datei gesichert unter " + backup.getFileName());
        } catch (IOException e) {
            log.severe("Sicherung der defekten Datei fehlgeschlagen: " + e.getMessage());
        }
    }
}
