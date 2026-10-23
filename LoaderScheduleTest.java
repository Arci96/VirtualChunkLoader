package dev.virtualchunkloader.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoaderScheduleTest {

    @Test
    void alwaysIsAlwaysActive() {
        assertTrue(LoaderSchedule.always().isActive(LocalTime.of(3, 0)));
    }

    @Test
    void sameDayWindow() {
        LoaderSchedule s = LoaderSchedule.window(LocalTime.of(8, 0), LocalTime.of(12, 0));
        assertTrue(s.isActive(LocalTime.of(8, 0)));
        assertTrue(s.isActive(LocalTime.of(11, 59)));
        assertFalse(s.isActive(LocalTime.of(12, 0)));
        assertFalse(s.isActive(LocalTime.of(7, 59)));
    }

    @Test
    void windowAcrossMidnight() {
        LoaderSchedule s = LoaderSchedule.window(LocalTime.of(22, 0), LocalTime.of(6, 0));
        assertTrue(s.isActive(LocalTime.of(23, 30)));
        assertTrue(s.isActive(LocalTime.of(0, 0)));
        assertTrue(s.isActive(LocalTime.of(5, 59)));
        assertFalse(s.isActive(LocalTime.of(6, 0)));
        assertFalse(s.isActive(LocalTime.of(12, 0)));
    }

    @Test
    void identicalStartAndEndIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> LoaderSchedule.window(LocalTime.of(1, 0), LocalTime.of(1, 0)));
    }
}
