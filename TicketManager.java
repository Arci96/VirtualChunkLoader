package dev.virtualchunkloader.core;

import dev.virtualchunkloader.model.ChunkKey;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Wendet die Buchführung der {@link TicketRegistry} auf echte Paper-Plugin-Chunk-Tickets an (Hauptthread). */
public final class TicketManager {

    private final Plugin plugin;
    private final Logger log;
    private final TicketRegistry registry = new TicketRegistry();

    public TicketManager(Plugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public TicketRegistry registry() {
        return registry;
    }

    /** Setzt die Chunks eines Loaders; Tickets werden nur für wirklich neue/frei gewordene Chunks geändert. */
    public TicketRegistry.Change update(String loader, Set<ChunkKey> desired) {
        TicketRegistry.Change change = registry.update(loader, desired);
        for (ChunkKey k : change.added()) {
            World w = Bukkit.getWorld(k.world());
            if (w == null) {
                log.warning("Kein Ticket gesetzt, Welt nicht geladen: " + k.world());
                continue;
            }
            try {
                w.addPluginChunkTicket(k.x(), k.z(), plugin);
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "Ticket konnte nicht gesetzt werden (" + k + "): " + e.getMessage());
            }
        }
        for (ChunkKey k : change.removed()) {
            removeTicket(k);
        }
        return change;
    }

    /** Gibt ALLE Tickets dieses Plugins frei (Herunterfahren). */
    public void releaseAll() {
        for (ChunkKey k : registry.clear()) {
            removeTicket(k);
        }
        for (World w : Bukkit.getWorlds()) {
            try {
                w.removePluginChunkTickets(plugin);
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "Restliche Tickets in " + w.getName() + " nicht entfernt: " + e.getMessage());
            }
        }
    }

    private void removeTicket(ChunkKey k) {
        World w = Bukkit.getWorld(k.world());
        if (w == null) {
            return;
        }
        try {
            w.removePluginChunkTicket(k.x(), k.z(), plugin);
        } catch (RuntimeException e) {
            log.log(Level.WARNING, "Ticket konnte nicht entfernt werden (" + k + "): " + e.getMessage());
        }
    }
}
