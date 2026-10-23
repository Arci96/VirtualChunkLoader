package dev.virtualchunkloader.model;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Daten eines Chunkloaders inklusive seines virtuellen Spielers. Nur im Hauptthread verändern. */
public final class ChunkLoaderData {

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final String name;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private int radius;
    private boolean enabled;
    private final String playerName;
    private LoaderSchedule schedule;

    public ChunkLoaderData(String name, String world, int x, int y, int z, int radius,
                           boolean enabled, String playerName, LoaderSchedule schedule) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Ungültiger Name: " + name);
        }
        if (radius < 0) {
            throw new IllegalArgumentException("Radius darf nicht negativ sein");
        }
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.enabled = enabled;
        this.playerName = playerName;
        this.schedule = schedule == null ? LoaderSchedule.always() : schedule;
    }

    public static boolean isValidName(String n) {
        return n != null && NAME.matcher(n).matches();
    }

    /** Anzahl Chunks eines quadratischen Bereichs mit Radius r: (2r+1)^2. */
    public static int chunkCount(int radius) {
        int side = 2 * radius + 1;
        return side * side;
    }

    /** Größter Radius, dessen Chunk-Anzahl das Limit nicht überschreitet (mindestens 0). */
    public static int maxRadiusFor(int maxChunks) {
        int r = 0;
        while (chunkCount(r + 1) <= maxChunks) {
            r++;
        }
        return r;
    }

    public Set<ChunkKey> chunkKeys() {
        Set<ChunkKey> keys = new LinkedHashSet<>();
        int cx = centerChunkX();
        int cz = centerChunkZ();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                keys.add(new ChunkKey(world, cx + dx, cz + dz));
            }
        }
        return keys;
    }

    public int centerChunkX() { return x >> 4; }
    public int centerChunkZ() { return z >> 4; }

    /** Kleingeschriebener Schlüssel (Namen sind nicht case-sensitiv). */
    public String key() { return name.toLowerCase(Locale.ROOT); }

    public String name() { return name; }
    public String world() { return world; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    public int radius() { return radius; }
    public boolean enabled() { return enabled; }
    public String playerName() { return playerName; }
    public LoaderSchedule schedule() { return schedule; }

    public void setRadius(int radius) {
        if (radius < 0) {
            throw new IllegalArgumentException("Radius darf nicht negativ sein");
        }
        this.radius = radius;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setSchedule(LoaderSchedule schedule) { this.schedule = schedule; }
}
