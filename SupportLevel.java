package dev.virtualchunkloader.farm;

/** Ob ein Farmtyp ohne echten Spieler laufen kann. */
public enum SupportLevel {
    FULL("Vollständig"),
    PARTIAL("Teilweise"),
    NONE("Gar nicht");

    private final String label;

    SupportLevel(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
