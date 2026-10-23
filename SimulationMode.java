package dev.virtualchunkloader.model;

import java.util.Locale;

/** Simulationsmodus des virtuellen Spielers. */
public enum SimulationMode {
    /** Nur Plugin-Chunk-Tickets. Kein Spieler-Entity, keine Spieler-Mechaniken. */
    TICKET_ONLY("Plugin-Chunk-Tickets (kein Spieler-Entity)");

    private final String description;

    SimulationMode(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    /** Liefert null, wenn der Wert unbekannt ist. */
    public static SimulationMode parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
