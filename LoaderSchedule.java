package dev.virtualchunkloader.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Zeitplan eines Chunkloaders (immer aktiv oder tägliches Zeitfenster in Serverzeit). */
public record LoaderSchedule(Mode mode, LocalTime start, LocalTime end) {

    public enum Mode { ALWAYS, TIME_WINDOW }

    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public static LoaderSchedule always() {
        return new LoaderSchedule(Mode.ALWAYS, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT);
    }

    public static LoaderSchedule window(LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            throw new IllegalArgumentException("Start und Ende dürfen nicht identisch sein");
        }
        return new LoaderSchedule(Mode.TIME_WINDOW, start, end);
    }

    /** Fenster über Mitternacht (z. B. 22:00-06:00) werden unterstützt. Ende ist exklusiv. */
    public boolean isActive(LocalTime now) {
        if (mode == Mode.ALWAYS) {
            return true;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }

    public String describe() {
        return mode == Mode.ALWAYS ? "immer" : start.format(FORMAT) + " - " + end.format(FORMAT) + " (Serverzeit)";
    }
}
