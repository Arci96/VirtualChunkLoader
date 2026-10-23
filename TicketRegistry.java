package dev.virtualchunkloader.core;

import dev.virtualchunkloader.model.ChunkKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Referenzzählende Buchführung über Chunk-Tickets.
 * Paper-Plugin-Tickets existieren pro Chunk nur einmal je Plugin. Überlappen sich zwei Chunkloader,
 * darf das Ticket daher erst entfernt werden, wenn der letzte Besitzer es freigibt.
 * Diese Klasse enthält keine Bukkit-Abhängigkeit und ist einzeln testbar.
 */
public final class TicketRegistry {

    /** Ergebnis einer Aktualisierung: Chunks, die neu ein Ticket brauchen / keins mehr brauchen. */
    public record Change(List<ChunkKey> added, List<ChunkKey> removed) {
        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty();
        }
    }

    private final Map<ChunkKey, Set<String>> owners = new HashMap<>();
    private final Map<String, Set<ChunkKey>> held = new HashMap<>();

    /** Setzt die gewünschten Chunks eines Loaders (leere Menge = Freigabe). Doppelte Einträge sind unmöglich. */
    public synchronized Change update(String loader, Set<ChunkKey> desired) {
        Set<ChunkKey> current = held.getOrDefault(loader, Set.of());
        List<ChunkKey> added = new ArrayList<>();
        List<ChunkKey> removed = new ArrayList<>();

        for (ChunkKey key : current) {
            if (!desired.contains(key)) {
                Set<String> set = owners.get(key);
                set.remove(loader);
                if (set.isEmpty()) {
                    owners.remove(key);
                    removed.add(key);
                }
            }
        }
        for (ChunkKey key : desired) {
            if (!current.contains(key)) {
                Set<String> set = owners.computeIfAbsent(key, k -> new HashSet<>());
                boolean first = set.isEmpty();
                set.add(loader);
                if (first) {
                    added.add(key);
                }
            }
        }
        if (desired.isEmpty()) {
            held.remove(loader);
        } else {
            held.put(loader, new HashSet<>(desired));
        }
        return new Change(added, removed);
    }

    /** Anzahl eindeutiger Chunks, falls der Loader auf {@code desired} gesetzt würde (ohne Änderung). */
    public synchronized int uniqueCountAfter(String loader, Set<ChunkKey> desired) {
        Set<ChunkKey> result = new HashSet<>();
        for (Map.Entry<ChunkKey, Set<String>> e : owners.entrySet()) {
            for (String owner : e.getValue()) {
                if (!owner.equals(loader)) {
                    result.add(e.getKey());
                    break;
                }
            }
        }
        result.addAll(desired);
        return result.size();
    }

    /** Entfernt alle Einträge und liefert alle Chunks, die ein Ticket hatten. */
    public synchronized Set<ChunkKey> clear() {
        Set<ChunkKey> all = new HashSet<>(owners.keySet());
        owners.clear();
        held.clear();
        return all;
    }

    public synchronized boolean holds(String loader) {
        return held.containsKey(loader);
    }

    public synchronized int heldCount(String loader) {
        return held.getOrDefault(loader, Set.of()).size();
    }

    public synchronized int uniqueCount() {
        return owners.size();
    }
}
