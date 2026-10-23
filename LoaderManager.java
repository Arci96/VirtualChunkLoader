package dev.virtualchunkloader.core;

import dev.virtualchunkloader.VirtualChunkLoaderPlugin;
import dev.virtualchunkloader.config.PluginSettings;
import dev.virtualchunkloader.model.ChunkKey;
import dev.virtualchunkloader.model.ChunkLoaderData;
import dev.virtualchunkloader.model.LoaderSchedule;
import dev.virtualchunkloader.storage.LoaderStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/** Verwaltet alle Chunkloader. Ausschließlich im Hauptthread verwenden. */
public final class LoaderManager {

    public record Result(boolean ok, String message) {
    }

    private final VirtualChunkLoaderPlugin plugin;
    private final TicketManager tickets;
    private final LoaderStorage storage;
    private PluginSettings settings;
    private final Map<String, ChunkLoaderData> loaders = new LinkedHashMap<>();
    private final Map<String, String> errors = new HashMap<>();
    private BukkitTask scheduleTask;
    private boolean loaded = false;

    public LoaderManager(VirtualChunkLoaderPlugin plugin, PluginSettings settings,
                         TicketManager tickets, LoaderStorage storage) {
        this.plugin = plugin;
        this.settings = settings;
        this.tickets = tickets;
        this.storage = storage;
    }

    // ------------------------------------------------------------ Start / Stop

    /** Liest die gespeicherten Loader asynchron und wendet sie danach im Hauptthread an. */
    public void loadAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<ChunkLoaderData> data = storage.load();
            try {
                Bukkit.getScheduler().runTask(plugin, () -> applyLoaded(data));
            } catch (IllegalPluginAccessException ignored) {
                // Plugin wurde währenddessen deaktiviert
            }
        });
    }

    private void applyLoaded(List<ChunkLoaderData> data) {
        for (ChunkLoaderData d : data) {
            loaders.put(d.key(), d);
        }
        loaded = true;
        if (settings.restoreOnStartup()) {
            refreshAll();
        } else if (!data.isEmpty()) {
            plugin.getLogger().info("Auto-Wiederherstellung ist aus: " + data.size()
                    + " Chunkloader geladen, aber inaktiv. Aktivieren mit /vcl enable <name>.");
        }
        updateScheduleTask();
        plugin.getLogger().info(data.size() + " Chunkloader geladen, " + tickets.registry().uniqueCount()
                + " Chunks mit Ticket.");
    }

    /** Beim Herunterfahren: Task stoppen, speichern, ALLE Tickets freigeben. */
    public void shutdown() {
        if (scheduleTask != null) {
            scheduleTask.cancel();
            scheduleTask = null;
        }
        if (loaded) {
            storage.writeNow(LoaderStorage.serialize(loaders.values()));
        }
        tickets.releaseAll();
        loaders.clear();
        errors.clear();
    }

    /** Neue Einstellungen übernehmen und alle Loader neu bewerten. */
    public void applySettings(PluginSettings newSettings) {
        this.settings = newSettings;
        if (scheduleTask != null) {
            scheduleTask.cancel();
            scheduleTask = null;
        }
        if (loaded) {
            refreshAll();
        }
        updateScheduleTask();
    }

    // ------------------------------------------------------------ Befehle

    public Result create(String name, Location loc, Integer radiusOrNull) {
        if (!loaded) {
            return notReady();
        }
        if (!ChunkLoaderData.isValidName(name)) {
            return new Result(false, "Ungültiger Name. Erlaubt sind 1-32 Zeichen: A-Z, a-z, 0-9, _ und -.");
        }
        if (loaders.containsKey(name.toLowerCase(Locale.ROOT))) {
            return new Result(false, "Ein Chunkloader mit diesem Namen existiert bereits.");
        }
        if (loaders.size() >= settings.maxLoaders()) {
            return new Result(false, "Limit erreicht: maximal " + settings.maxLoaders()
                    + " Chunkloader (limits.max-loaders).");
        }
        World world = loc.getWorld();
        if (world == null) {
            return new Result(false, "Die aktuelle Welt konnte nicht bestimmt werden.");
        }
        int radius = radiusOrNull != null ? radiusOrNull : settings.defaultRadius();
        Result radiusCheck = checkRadius(radius);
        if (!radiusCheck.ok()) {
            return radiusCheck;
        }
        String playerName = settings.playerNameFormat().replace("%name%", name);
        ChunkLoaderData d = new ChunkLoaderData(name, world.getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), radius, true, playerName,
                LoaderSchedule.always());
        loaders.put(d.key(), d);
        save();
        refresh(d);
        String err = errors.get(d.key());
        if (err != null) {
            return new Result(true, "Chunkloader '" + name + "' gespeichert, aber nicht aktiv: " + err);
        }
        return new Result(true, "Chunkloader '" + name + "' erstellt (Radius " + radius + ", "
                + ChunkLoaderData.chunkCount(radius) + " Chunks, Chunk " + d.centerChunkX() + "/"
                + d.centerChunkZ() + ").");
    }

    public Result remove(String name) {
        if (!loaded) {
            return notReady();
        }
        ChunkLoaderData d = loaders.remove(name.toLowerCase(Locale.ROOT));
        if (d == null) {
            return new Result(false, "Chunkloader '" + name + "' nicht gefunden.");
        }
        tickets.update(d.key(), Set.of());
        errors.remove(d.key());
        save();
        updateScheduleTask();
        return new Result(true, "Chunkloader '" + d.name() + "' entfernt, Tickets freigegeben.");
    }

    public Result setEnabled(ChunkLoaderData d, boolean enabled) {
        if (!loaded) {
            return notReady();
        }
        d.setEnabled(enabled);
        save();
        refresh(d);
        String err = errors.get(d.key());
        if (enabled && err != null) {
            return new Result(false, "'" + d.name() + "' ist aktiviert, aber nicht aktiv: " + err);
        }
        return new Result(true, "Chunkloader '" + d.name() + "' " + (enabled ? "aktiviert." : "deaktiviert."));
    }

    public Result setRadius(ChunkLoaderData d, int radius) {
        if (!loaded) {
            return notReady();
        }
        Result check = checkRadius(radius);
        if (!check.ok()) {
            return check;
        }
        d.setRadius(radius);
        save();
        refresh(d);
        String err = errors.get(d.key());
        return err == null
                ? new Result(true, "Radius von '" + d.name() + "' auf " + radius + " gesetzt ("
                + ChunkLoaderData.chunkCount(radius) + " Chunks).")
                : new Result(false, "Radius gesetzt, aber Loader nicht aktiv: " + err);
    }

    public Result setSchedule(ChunkLoaderData d, LoaderSchedule schedule) {
        if (!loaded) {
            return notReady();
        }
        d.setSchedule(schedule);
        save();
        refresh(d);
        updateScheduleTask();
        return new Result(true, "Zeitplan von '" + d.name() + "': " + schedule.describe());
    }

    private Result checkRadius(int radius) {
        if (radius < 0) {
            return new Result(false, "Der Radius darf nicht negativ sein.");
        }
        int chunks = ChunkLoaderData.chunkCount(radius);
        if (chunks > settings.maxChunksPerLoader()) {
            return new Result(false, "Radius " + radius + " ergibt " + chunks + " Chunks; erlaubt sind "
                    + settings.maxChunksPerLoader() + " (maximaler Radius: "
                    + ChunkLoaderData.maxRadiusFor(settings.maxChunksPerLoader()) + ").");
        }
        return new Result(true, "");
    }

    private Result notReady() {
        return new Result(false, "Die Chunkloader werden noch geladen. Bitte kurz warten.");
    }

    // ------------------------------------------------------------ Welt-Ereignisse

    public void onWorldLoad(World world) {
        // eine Tick später, damit die Welt vollständig bereit ist
        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (ChunkLoaderData d : loaders.values()) {
                    if (d.world().equals(world.getName())) {
                        refresh(d);
                    }
                }
            });
        } catch (IllegalPluginAccessException ignored) {
            // Plugin wird gerade deaktiviert
        }
    }

    public void onWorldUnload(World world) {
        for (ChunkLoaderData d : loaders.values()) {
            if (d.world().equals(world.getName())) {
                tickets.update(d.key(), Set.of());
                errors.put(d.key(), "Welt '" + world.getName() + "' wurde entladen");
            }
        }
    }

    // ------------------------------------------------------------ Aktivierung

    public void refreshAll() {
        for (ChunkLoaderData d : loaders.values()) {
            refresh(d);
        }
    }

    /** Bringt die Tickets eines Loaders in den Sollzustand. Fehler bleiben auf diesen Loader begrenzt. */
    private void refresh(ChunkLoaderData d) {
        String key = d.key();
        try {
            errors.remove(key);
            boolean wanted = d.enabled() && d.schedule().isActive(LocalTime.now());
            if (!wanted) {
                tickets.update(key, Set.of());
                return;
            }
            World world = Bukkit.getWorld(d.world());
            if (world == null) {
                tickets.update(key, Set.of());
                errors.put(key, "Welt '" + d.world() + "' ist nicht geladen");
                return;
            }
            Set<ChunkKey> desired = d.chunkKeys();
            if (desired.size() > settings.maxChunksPerLoader()) {
                tickets.update(key, Set.of());
                errors.put(key, desired.size() + " Chunks überschreiten limits.max-chunks-per-loader ("
                        + settings.maxChunksPerLoader() + ")");
                return;
            }
            if (tickets.registry().uniqueCountAfter(key, desired) > settings.maxTotalChunks()) {
                tickets.update(key, Set.of());
                errors.put(key, "Gesamtlimit limits.max-total-chunks (" + settings.maxTotalChunks()
                        + ") würde überschritten");
                return;
            }
            TicketRegistry.Change c = tickets.update(key, desired);
            if (settings.logTicketChanges() && !c.isEmpty()) {
                plugin.getLogger().info("'" + d.name() + "': +" + c.added().size() + " / -" + c.removed().size()
                        + " Chunk-Tickets");
            }
        } catch (RuntimeException e) {
            errors.put(key, "Interner Fehler: " + e.getMessage());
            plugin.getLogger().log(Level.SEVERE, "Fehler bei Chunkloader '" + d.name() + "'", e);
        }
    }

    // ------------------------------------------------------------ Zeitplan

    private void updateScheduleTask() {
        boolean needed = loaders.values().stream()
                .anyMatch(d -> d.schedule().mode() == LoaderSchedule.Mode.TIME_WINDOW);
        if (needed && scheduleTask == null) {
            long period = settings.scheduleCheckSeconds() * 20L;
            scheduleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshScheduled, period, period);
        } else if (!needed && scheduleTask != null) {
            scheduleTask.cancel();
            scheduleTask = null;
        }
    }

    private void refreshScheduled() {
        for (ChunkLoaderData d : loaders.values()) {
            if (d.schedule().mode() == LoaderSchedule.Mode.TIME_WINDOW) {
                refresh(d);
            }
        }
    }

    // ------------------------------------------------------------ Abfragen

    private void save() {
        storage.writeAsync(LoaderStorage.serialize(loaders.values()));
    }

    public Collection<ChunkLoaderData> loaders() {
        return Collections.unmodifiableCollection(loaders.values());
    }

    public ChunkLoaderData find(String name) {
        return name == null ? null : loaders.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean isHolding(ChunkLoaderData d) {
        return tickets.registry().holds(d.key());
    }

    public int heldChunks(ChunkLoaderData d) {
        return tickets.registry().heldCount(d.key());
    }

    public int uniqueChunks() {
        return tickets.registry().uniqueCount();
    }

    public String error(ChunkLoaderData d) {
        return errors.get(d.key());
    }

    public PluginSettings settings() {
        return settings;
    }

    public String describeState(ChunkLoaderData d) {
        if (!d.enabled()) {
            return "deaktiviert";
        }
        if (isHolding(d)) {
            return "aktiv";
        }
        String err = error(d);
        if (err != null) {
            return "inaktiv (" + err + ")";
        }
        if (!d.schedule().isActive(LocalTime.now())) {
            return "wartet auf Zeitplan";
        }
        return "inaktiv";
    }
}
