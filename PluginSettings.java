package dev.virtualchunkloader.config;

import dev.virtualchunkloader.model.ChunkLoaderData;
import dev.virtualchunkloader.model.SimulationMode;
import org.bukkit.configuration.ConfigurationSection;

import java.util.logging.Logger;

/** Unveränderlicher, validierter Schnappschuss der config.yml. */
public record PluginSettings(
        int maxLoaders,
        int maxChunksPerLoader,
        int maxTotalChunks,
        int defaultRadius,
        boolean restoreOnStartup,
        String storageFile,
        int scheduleCheckSeconds,
        SimulationMode simulationMode,
        String playerNameFormat,
        boolean diagnosticsEnabled,
        int golemMinVillagers,
        int maxVillagersScanned,
        boolean spawnAreaScan,
        boolean debug,
        boolean logTicketChanges) {

    public static PluginSettings from(ConfigurationSection c, Logger log) {
        int maxLoaders = clamp(c.getInt("limits.max-loaders", 5), 1, 100);
        int maxPer = clamp(c.getInt("limits.max-chunks-per-loader", 25), 1, 1024);
        int maxTotal = clamp(c.getInt("limits.max-total-chunks", 100), 1, 10000);
        int defRadius = clamp(c.getInt("limits.default-radius", 2), 0, 15);
        int allowed = ChunkLoaderData.maxRadiusFor(maxPer);
        if (defRadius > allowed) {
            log.warning("limits.default-radius (" + defRadius + ") überschreitet limits.max-chunks-per-loader ("
                    + maxPer + "). Verwende Radius " + allowed + ".");
            defRadius = allowed;
        }

        String file = c.getString("storage.file", "loaders.yml");
        if (file == null || file.isBlank() || file.contains("/") || file.contains("\\") || file.contains("..")) {
            log.warning("Ungültiger Wert für storage.file. Verwende loaders.yml.");
            file = "loaders.yml";
        }

        SimulationMode mode = SimulationMode.parse(c.getString("virtual-player.mode", "TICKET_ONLY"));
        if (mode == null) {
            log.warning("Unbekannter virtual-player.mode (nicht implementiert). Verwende TICKET_ONLY.");
            mode = SimulationMode.TICKET_ONLY;
        }
        String format = c.getString("virtual-player.name-format", "FarmLoader_%name%");
        if (format == null || format.isBlank()) {
            format = "FarmLoader_%name%";
        }

        return new PluginSettings(
                maxLoaders, maxPer, maxTotal, defRadius,
                c.getBoolean("restore.on-startup", true),
                file,
                clamp(c.getInt("schedule.check-interval-seconds", 30), 5, 3600),
                mode, format,
                c.getBoolean("diagnostics.enabled", true),
                clamp(c.getInt("diagnostics.golem-min-villagers", 5), 1, 20),
                clamp(c.getInt("diagnostics.max-villagers-scanned", 200), 10, 2000),
                c.getBoolean("diagnostics.spawn-area-scan", true),
                c.getBoolean("debug", false),
                c.getBoolean("logging.log-ticket-changes", true));
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
