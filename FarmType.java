package dev.virtualchunkloader.farm;

import java.util.Locale;

/**
 * Farmtypen mit Einschätzung, ob sie ohne echten Spieler laufen können.
 * Die Einschätzungen beruhen auf den Vanilla-Mechaniken (siehe README) und sind keine Garantie für ein konkretes Farm-Design.
 */
public enum FarmType {
    IRON("iron", "Eisenfarm", SupportLevel.PARTIAL,
            "Der Golem-Spawn der Dorfbewohner braucht nach Vanilla-Logik keinen Spieler. Voraussetzung sind "
                    + "entity-tickende Chunks, ein nicht pausierender Server und aktive, ausgeschlafene Dorfbewohner. "
                    + "Designs mit Zombies, Raids oder Spieler-Interaktion funktionieren nicht."),
    CROP("crop", "Pflanzenfarm", SupportLevel.FULL,
            "Wachstum läuft über Random Ticks in tickenden Chunks. Redstone-Ernte funktioniert. "
                    + "Villager-Bauern brauchen zusätzlich aktive Dorfbewohner."),
    SUGARCANE("sugarcane", "Zuckerrohrfarm", SupportLevel.FULL,
            "Wachstum über Random Ticks; Observer/Redstone-Ernte läuft in geladenen Chunks."),
    BAMBOO("bamboo", "Bambusfarm", SupportLevel.FULL,
            "Wachstum über Random Ticks; Observer/Redstone-Ernte läuft in geladenen Chunks."),
    CACTUS("cactus", "Kaktusfarm", SupportLevel.FULL,
            "Wachstum über Random Ticks; Redstone-Ernte läuft in geladenen Chunks."),
    MOB("mob", "Mobfarm", SupportLevel.NONE,
            "Natürliches Spawnen und Spawner-Blöcke benötigen einen echten Spieler in der Nähe. "
                    + "Ein Plugin-Chunk-Ticket ersetzt das nicht."),
    STORAGE("storage", "Lager- und Sortiersystem", SupportLevel.FULL,
            "Trichter, Redstone und Item-Transport laufen in geladenen, tickenden Chunks. "
                    + "Der gesamte Transportweg muss innerhalb geladener Chunks liegen.");

    private final String key;
    private final String displayName;
    private final SupportLevel level;
    private final String explanation;

    FarmType(String key, String displayName, SupportLevel level, String explanation) {
        this.key = key;
        this.displayName = displayName;
        this.level = level;
        this.explanation = explanation;
    }

    public String key() { return key; }
    public String displayName() { return displayName; }
    public SupportLevel level() { return level; }
    public String explanation() { return explanation; }

    /** Liefert null bei unbekanntem Schlüssel. */
    public static FarmType fromKey(String key) {
        if (key == null) {
            return null;
        }
        String k = key.toLowerCase(Locale.ROOT);
        for (FarmType t : values()) {
            if (t.key.equals(k)) {
                return t;
            }
        }
        return null;
    }
}
