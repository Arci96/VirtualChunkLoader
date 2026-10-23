package dev.virtualchunkloader.core;

import dev.virtualchunkloader.model.ChunkKey;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketRegistryTest {

    private static ChunkKey k(int x, int z) {
        return new ChunkKey("world", x, z);
    }

    @Test
    void firstOwnerAddsTicketAndDuplicatesAreIgnored() {
        TicketRegistry r = new TicketRegistry();
        assertEquals(2, r.update("a", Set.of(k(0, 0), k(1, 0))).added().size());
        assertTrue(r.update("a", Set.of(k(0, 0), k(1, 0))).isEmpty());
        assertEquals(2, r.uniqueCount());
    }

    @Test
    void overlappingTicketIsKeptUntilLastOwnerReleases() {
        TicketRegistry r = new TicketRegistry();
        r.update("a", Set.of(k(0, 0), k(1, 0)));
        TicketRegistry.Change second = r.update("b", Set.of(k(1, 0), k(2, 0)));
        assertEquals(1, second.added().size()); // nur (2,0) ist neu

        TicketRegistry.Change releaseA = r.update("a", Set.of());
        assertEquals(1, releaseA.removed().size()); // nur (0,0); (1,0) gehört noch b
        assertEquals(k(0, 0), releaseA.removed().get(0));
        assertFalse(r.holds("a"));
        assertTrue(r.holds("b"));

        TicketRegistry.Change releaseB = r.update("b", Set.of());
        assertEquals(2, releaseB.removed().size());
        assertEquals(0, r.uniqueCount());
    }

    @Test
    void shrinkingRadiusRemovesOnlyDroppedChunks() {
        TicketRegistry r = new TicketRegistry();
        r.update("a", Set.of(k(0, 0), k(1, 0), k(2, 0)));
        TicketRegistry.Change c = r.update("a", Set.of(k(0, 0)));
        assertEquals(2, c.removed().size());
        assertEquals(1, r.heldCount("a"));
    }

    @Test
    void uniqueCountAfterDoesNotModifyState() {
        TicketRegistry r = new TicketRegistry();
        r.update("a", Set.of(k(0, 0), k(1, 0)));
        assertEquals(3, r.uniqueCountAfter("b", Set.of(k(1, 0), k(2, 0))));
        assertEquals(1, r.uniqueCountAfter("a", Set.of(k(5, 5))));
        assertEquals(2, r.uniqueCount());
    }

    @Test
    void clearReturnsAllKeys() {
        TicketRegistry r = new TicketRegistry();
        r.update("a", Set.of(k(0, 0)));
        r.update("b", Set.of(k(0, 0), k(1, 1)));
        assertEquals(2, r.clear().size());
        assertEquals(0, r.uniqueCount());
        assertFalse(r.holds("a"));
    }
}
