package dev.virtualchunkloader.model;

/** Eindeutiger Schlüssel eines Chunks (Weltname + Chunk-Koordinaten). */
public record ChunkKey(String world, int x, int z) {
}
