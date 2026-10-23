package dev.virtualchunkloader.farm;

import dev.virtualchunkloader.config.PluginSettings;
import dev.virtualchunkloader.config.ServerSettings;
import dev.virtualchunkloader.core.ChunkInspector;
import dev.virtualchunkloader.core.LoaderManager;
import dev.virtualchunkloader.model.ChunkLoaderData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Villager;
import org.bukkit.entity.memory.MemoryKey;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Analysiert eine Ironfarm anhand tatsächlich lesbarer Zustände (Hauptthread).
 * Es werden keine Golems oder Items erzeugt und nichts verändert.
 * Die Regeln der Golem-Spawns entsprechen dem Vanilla-Verhalten nach bestem Wissen und wurden nicht zur Laufzeit verifiziert.
 */
public final class IronFarmAnalyzer {

    private static final long SLEEP_WINDOW_TICKS = 24000L;

    public DiagnosisReport analyze(ChunkLoaderData d, LoaderManager manager,
                                   PluginSettings settings, ServerSettings server) {
        List<Check> checks = new ArrayList<>();
        String title = "Ironfarm: " + d.name();

        World w = Bukkit.getWorld(d.world());
        if (w == null) {
            checks.add(Check.fail("Welt", "Welt '" + d.world() + "' ist nicht geladen"));
            return new DiagnosisReport(title, checks);
        }

        // 1) Chunkloader / Chunks / Entity-Ticking
        if (manager.isHolding(d)) {
            checks.add(Check.ok("Chunkloader aktiv", manager.heldChunks(d) + " Plugin-Tickets gehalten"));
        } else {
            checks.add(Check.fail("Chunkloader aktiv", "Nein - " + manager.describeState(d)));
        }

        ChunkInspector.ChunkStats st = ChunkInspector.inspect(d, w, settings.maxVillagersScanned());
        String loadedText = st.loaded() + "/" + st.total();
        checks.add(st.loaded() == st.total()
                ? Check.ok("Chunks geladen", "Ja (" + loadedText + ")")
                : Check.fail("Chunks geladen", "Nein (" + loadedText + ")"));

        String tickText = st.entityTicking() + "/" + st.total();
        if (st.entityTicking() == st.total()) {
            checks.add(Check.ok("Entity-Ticking", "Aktiv (" + tickText + ")"));
        } else if (st.entityTicking() > 0) {
            checks.add(Check.warn("Entity-Ticking", "Teilweise (" + tickText + ")"));
        } else {
            checks.add(Check.fail("Entity-Ticking", "Inaktiv (" + tickText + ")"));
        }

        // 2) Dorfbewohner
        List<Villager> villagers = st.villagers();
        long game = w.getGameTime();
        if (st.entitiesLoaded() < st.loaded()) {
            checks.add(Check.warn("Entities geladen", "Nur in " + st.entitiesLoaded() + " von " + st.loaded()
                    + " Chunks - Zählung evtl. unvollständig"));
        }
        int total = villagers.size();
        checks.add(total == 0
                ? Check.fail("Dorfbewohner erkannt", "0")
                : Check.ok("Dorfbewohner erkannt", total + (st.truncated() ? "+ (Scan-Limit erreicht)" : "")));

        Villager center = null;
        if (total > 0) {
            int beds = 0;
            int jobs = 0;
            int slept = 0;
            for (Villager v : villagers) {
                if (v.getMemory(MemoryKey.HOME) != null) {
                    beds++;
                }
                if (hasJob(v)) {
                    jobs++;
                }
                if (sleptRecently(v, game)) {
                    slept++;
                }
            }
            checks.add(levelCheck("Betten (HOME-Speicher)", beds, total, false));
            checks.add(levelCheck("Arbeitsplätze (JOB_SITE)", jobs, total, true));

            int min = settings.golemMinVillagers();
            checks.add(slept >= min
                    ? Check.ok("Geschlafen (letzte 24000 Ticks)", slept + "/" + total)
                    : Check.fail("Geschlafen (letzte 24000 Ticks)", slept + "/" + total
                    + " - mindestens " + min + " nötig"));

            // größte Gruppe im 10-Block-Umkreis
            int best = 0;
            for (Villager v : villagers) {
                if (!sleptRecently(v, game)) {
                    continue;
                }
                BoundingBox box = v.getBoundingBox().expand(10.0);
                int n = 0;
                for (Villager o : villagers) {
                    if (sleptRecently(o, game) && box.overlaps(o.getBoundingBox())) {
                        n++;
                    }
                }
                if (n > best) {
                    best = n;
                    center = v;
                }
            }
            checks.add(best >= min
                    ? Check.ok("Gruppe im 10-Block-Umkreis", best + " ausgeschlafene Dorfbewohner (nötig: " + min + ")")
                    : Check.fail("Gruppe im 10-Block-Umkreis", "höchstens " + best
                    + " ausgeschlafene Dorfbewohner (nötig: " + min + ")"));
        }

        // 3) Spawnfläche
        if (!settings.spawnAreaScan()) {
            checks.add(Check.info("Spawnfläche", "Prüfung in config.yml deaktiviert"));
        } else if (center == null) {
            checks.add(Check.unknown("Spawnfläche", "Nicht prüfbar - keine passende Dorfbewohner-Gruppe gefunden"));
        } else {
            int positions = countSpawnPositions(w, center.getLocation());
            if (positions == 0) {
                checks.add(Check.fail("Spawnfläche", "0 gültige Positionen im Bereich +-8/+-6 Blöcke (Näherung)"));
            } else if (positions < 3) {
                checks.add(Check.warn("Spawnfläche", positions + " gültige Positionen (Näherung)"));
            } else {
                checks.add(Check.ok("Spawnfläche", positions + " gültige Positionen (Näherung)"));
            }
        }

        // 4) Spielerbedingung: tickt der Server auch ohne Spieler?
        Integer pause = server.pauseWhenEmptySeconds();
        Check.Status pauseStatus;
        if (pause == null) {
            pauseStatus = Check.Status.UNKNOWN;
            checks.add(Check.unknown("Server-Pause ohne Spieler", "unbekannt (server.properties nicht lesbar)"));
        } else if (pause <= 0) {
            pauseStatus = Check.Status.OK;
            checks.add(Check.ok("Server-Pause ohne Spieler", "deaktiviert"));
        } else {
            pauseStatus = Check.Status.FAIL;
            checks.add(Check.fail("Server-Pause ohne Spieler", "Server pausiert nach " + pause
                    + " s ohne Spieler - pause-when-empty-seconds=-1 in server.properties setzen (Neustart nötig)"));
        }

        Boolean tickInactive = server.tickInactiveVillagers(w.getName());
        Check.Status villagerStatus;
        if (tickInactive == null) {
            villagerStatus = Check.Status.UNKNOWN;
            checks.add(Check.unknown("Inaktive Dorfbewohner ticken", "nicht in spigot.yml gefunden (Standard der Version gilt)"));
        } else if (tickInactive) {
            villagerStatus = Check.Status.OK;
            checks.add(Check.ok("Inaktive Dorfbewohner ticken", "tick-inactive-villagers: true"));
        } else {
            villagerStatus = Check.Status.FAIL;
            checks.add(Check.fail("Inaktive Dorfbewohner ticken",
                    "tick-inactive-villagers: false in spigot.yml - Dorfbewohner ticken ohne Spieler nicht"));
        }

        Check.Status player;
        if (pauseStatus == Check.Status.FAIL || villagerStatus == Check.Status.FAIL) {
            player = Check.Status.FAIL;
        } else if (pauseStatus == Check.Status.UNKNOWN || villagerStatus == Check.Status.UNKNOWN) {
            player = Check.Status.UNKNOWN;
        } else {
            player = Check.Status.OK;
        }
        checks.add(new Check(player, "Spielerbedingung (Server tickt ohne Spieler)",
                switch (player) {
                    case OK -> "Erfüllt";
                    case FAIL -> "Nicht erfüllt";
                    default -> "Nicht eindeutig prüfbar";
                }));

        // 5) Hinweise
        checks.add(Check.info("Simulationsmodus", settings.simulationMode() + " - " + settings.simulationMode().description()
                + "; virtueller Spieler: " + d.playerName()));
        checks.add(Check.info("Hinweis", "Ein Spieler in der Nähe ist für den Golem-Spawn nach Vanilla-Logik nicht nötig "
                + "(nicht zur Laufzeit verifiziert). Raids, Spawner und natürliches Mob-Spawnen bleiben ohne Spieler inaktiv."));

        return new DiagnosisReport(title, checks);
    }

    private static boolean hasJob(Villager v) {
        Villager.Profession p = v.getProfession();
        return !p.equals(Villager.Profession.NONE) && !p.equals(Villager.Profession.NITWIT);
    }

    private static boolean sleptRecently(Villager v, long gameTime) {
        Long slept = v.getMemory(MemoryKey.LAST_SLEPT);
        return slept != null && gameTime - slept < SLEEP_WINDOW_TICKS;
    }

    private static Check levelCheck(String label, int count, int total, boolean infoOnly) {
        String detail = count + "/" + total;
        if (count == total) {
            return Check.ok(label, detail);
        }
        if (infoOnly) {
            return Check.info(label, detail);
        }
        return count == 0 ? Check.fail(label, detail) : Check.warn(label, detail);
    }

    /** Näherung der Vanilla-Spawnsuche (+-8 horizontal, +-6 vertikal): fester Boden, 3 freie Blöcke, keine Flüssigkeit. */
    private static int countSpawnPositions(World w, Location c) {
        int cx = c.getBlockX();
        int cy = c.getBlockY();
        int cz = c.getBlockZ();
        int count = 0;
        for (int x = cx - 8; x <= cx + 8; x++) {
            for (int z = cz - 8; z <= cz + 8; z++) {
                if (!w.isChunkLoaded(x >> 4, z >> 4)) {
                    continue;
                }
                for (int y = cy - 6; y <= cy + 6; y++) {
                    if (y <= w.getMinHeight() || y + 2 >= w.getMaxHeight()) {
                        continue;
                    }
                    if (!w.getBlockAt(x, y - 1, z).getType().isSolid()) {
                        continue;
                    }
                    if (free(w.getBlockAt(x, y, z)) && free(w.getBlockAt(x, y + 1, z))
                            && free(w.getBlockAt(x, y + 2, z))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static boolean free(Block b) {
        return b.isPassable() && !b.isLiquid();
    }
}
