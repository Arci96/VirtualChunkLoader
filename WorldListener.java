package dev.virtualchunkloader.listener;

import dev.virtualchunkloader.core.LoaderManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/** Reagiert auf spät geladene (z. B. Multiverse) und entladene Welten. */
public final class WorldListener implements Listener {

    private final LoaderManager manager;

    public WorldListener(LoaderManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        manager.onWorldLoad(event.getWorld());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        manager.onWorldUnload(event.getWorld());
    }
}
