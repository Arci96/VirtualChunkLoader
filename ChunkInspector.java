package dev.virtualchunkloader.core;

import dev.virtualchunkloader.model.ChunkKey;
import dev.virtualchunkloader.model.ChunkLoaderData;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.List;

/** Liest den tatsächlichen Chunk-Zustand (Hauptthread). Lädt selbst KEINE Chunks. */
public final class ChunkInspector {

    private ChunkInspector() {
    }

    public record ChunkStats(int total, int loaded, int entityTicking, int entitiesLoaded,
                             List<Villager> villagers, boolean truncated) {
    }

    public static ChunkStats inspect(ChunkLoaderData d, World world, int maxVillagers) {
        int total = 0;
        int loaded = 0;
        int ticking = 0;
        int entitiesLoaded = 0;
        boolean truncated = false;
        List<Villager> villagers = new ArrayList<>();

        for (ChunkKey k : d.chunkKeys()) {
            total++;
            if (!world.isChunkLoaded(k.x(), k.z())) {
                continue;
            }
            loaded++;
            Chunk chunk = world.getChunkAt(k.x(), k.z()); // bereits geladen, lädt nichts nach
            if ("ENTITY_TICKING".equals(chunk.getLoadLevel().name())) {
                ticking++;
            }
            if (chunk.isEntitiesLoaded()) {
                entitiesLoaded++;
                for (Entity e : chunk.getEntities()) {
                    if (e instanceof Villager v) {
                        if (villagers.size() < maxVillagers) {
                            villagers.add(v);
                        } else {
                            truncated = true;
                        }
                    }
                }
            }
        }
        return new ChunkStats(total, loaded, ticking, entitiesLoaded, villagers, truncated);
    }
}
