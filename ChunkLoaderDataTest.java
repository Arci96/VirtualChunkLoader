package dev.virtualchunkloader.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkLoaderDataTest {

    private static ChunkLoaderData data(int x, int z, int radius) {
        return new ChunkLoaderData("test", "world", x, 64, z, radius, true, "FarmLoader_test", null);
    }

    @Test
    void chunkCountAndMaxRadius() {
        assertEquals(1, ChunkLoaderData.chunkCount(0));
        assertEquals(25, ChunkLoaderData.chunkCount(2));
        assertEquals(2, ChunkLoaderData.maxRadiusFor(25));
        assertEquals(1, ChunkLoaderData.maxRadiusFor(24));
        assertEquals(0, ChunkLoaderData.maxRadiusFor(1));
    }

    @Test
    void chunkKeysCoverSquareAroundCenter() {
        ChunkLoaderData d = data(40, 40, 1); // Chunk 2/2
        assertEquals(9, d.chunkKeys().size());
        assertTrue(d.chunkKeys().contains(new ChunkKey("world", 1, 1)));
        assertTrue(d.chunkKeys().contains(new ChunkKey("world", 3, 3)));
    }

    @Test
    void negativeCoordinatesUseFloorDivision() {
        ChunkLoaderData d = data(-1, -17, 0);
        assertEquals(-1, d.centerChunkX());
        assertEquals(-2, d.centerChunkZ());
    }

    @Test
    void nameValidationAndKey() {
        assertTrue(ChunkLoaderData.isValidName("ironfarm_1"));
        assertFalse(ChunkLoaderData.isValidName("a b"));
        assertFalse(ChunkLoaderData.isValidName("a.b"));
        assertFalse(ChunkLoaderData.isValidName(""));
        assertEquals("ironfarm", new ChunkLoaderData("IronFarm", "w", 0, 0, 0, 0, true, "p", null).key());
        assertThrows(IllegalArgumentException.class, () -> data(0, 0, -1));
    }
}
