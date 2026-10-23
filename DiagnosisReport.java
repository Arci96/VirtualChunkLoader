package dev.virtualchunkloader.farm;

import java.util.List;

/** Ergebnis einer Farm-Diagnose. */
public record DiagnosisReport(String title, List<Check> checks) {

    public enum Verdict {
        MET("Alle prüfbaren Voraussetzungen erfüllt"),
        PARTIAL("Voraussetzungen teilweise nicht prüfbar oder mit Warnungen"),
        MISSING("Weitere Voraussetzungen erforderlich");

        private final String text;

        Verdict(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }
    }

    public Verdict verdict() {
        boolean partial = false;
        for (Check c : checks) {
            if (c.status() == Check.Status.FAIL) {
                return Verdict.MISSING;
            }
            if (c.status() == Check.Status.WARN || c.status() == Check.Status.UNKNOWN) {
                partial = true;
            }
        }
        return partial ? Verdict.PARTIAL : Verdict.MET;
    }
}
