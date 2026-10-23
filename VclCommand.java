package dev.virtualchunkloader.command;

import dev.virtualchunkloader.VirtualChunkLoaderPlugin;
import dev.virtualchunkloader.config.PluginSettings;
import dev.virtualchunkloader.core.ChunkInspector;
import dev.virtualchunkloader.core.LoaderManager;
import dev.virtualchunkloader.farm.Check;
import dev.virtualchunkloader.farm.DiagnosisReport;
import dev.virtualchunkloader.farm.FarmType;
import dev.virtualchunkloader.farm.IronFarmAnalyzer;
import dev.virtualchunkloader.model.ChunkLoaderData;
import dev.virtualchunkloader.model.LoaderSchedule;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

/** Befehl /vcl mit Tab-Vervollständigung. */
public final class VclCommand implements TabExecutor {

    private record Help(String sub, String usage, String description) {
    }

    private static final List<Help> HELP = List.of(
            new Help("help", "/vcl help", "Zeigt alle Befehle"),
            new Help("create", "/vcl create <name> [radius]", "Erstellt einen Chunkloader an deiner Position"),
            new Help("remove", "/vcl remove <name>", "Entfernt einen Chunkloader"),
            new Help("list", "/vcl list", "Listet alle Chunkloader"),
            new Help("info", "/vcl info <name>", "Zeigt Details und Status"),
            new Help("enable", "/vcl enable <name>", "Aktiviert einen Chunkloader"),
            new Help("disable", "/vcl disable <name>", "Deaktiviert einen Chunkloader"),
            new Help("reload", "/vcl reload", "Lädt die Konfiguration neu"),
            new Help("status", "/vcl status", "Zeigt aktive Chunkloader und geladene Chunks"),
            new Help("farm", "/vcl farm <name> <typ>", "Analysiert den Bereich als Farm (Typ iron wird analysiert)"),
            new Help("schedule", "/vcl schedule <name> always|<HH:mm> <HH:mm>", "Setzt einen Zeitplan"),
            new Help("radius", "/vcl radius <name> <radius>", "Ändert den Radius in Chunks"),
            new Help("types", "/vcl types", "Zeigt je Farmtyp, ob er ohne Spieler läuft"));

    private final VirtualChunkLoaderPlugin plugin;
    private final LoaderManager manager;
    private final IronFarmAnalyzer ironAnalyzer = new IronFarmAnalyzer();

    public VclCommand(VirtualChunkLoaderPlugin plugin, LoaderManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private static String permissionFor(String sub) {
        return switch (sub) {
            case "create" -> "virtualchunkloader.create";
            case "remove" -> "virtualchunkloader.remove";
            case "list", "info", "status", "farm", "types" -> "virtualchunkloader.list";
            case "reload" -> "virtualchunkloader.reload";
            case "enable", "disable", "schedule", "radius" -> "virtualchunkloader.admin";
            default -> null;
        };
    }

    private static boolean allowed(CommandSender s, String sub) {
        String perm = permissionFor(sub);
        return perm == null || s.hasPermission(perm);
    }

    // ------------------------------------------------------------ Ausführung

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] a) {
        if (a.length == 0) {
            help(sender);
            return true;
        }
        String sub = a[0].toLowerCase(Locale.ROOT);
        if (HELP.stream().noneMatch(h -> h.sub().equals(sub))) {
            Msg.err(sender, "Unbekannter Unterbefehl '" + a[0] + "'. Mit /vcl help siehst du alle Befehle.");
            return true;
        }
        if (!allowed(sender, sub)) {
            Msg.err(sender, "Dafür fehlt dir die Berechtigung (" + permissionFor(sub) + ").");
            return true;
        }
        try {
            switch (sub) {
                case "help" -> help(sender);
                case "create" -> create(sender, a);
                case "remove" -> remove(sender, a);
                case "list" -> list(sender);
                case "info" -> info(sender, a);
                case "enable" -> toggle(sender, a, true);
                case "disable" -> toggle(sender, a, false);
                case "reload" -> reload(sender);
                case "status" -> status(sender);
                case "farm" -> farm(sender, a);
                case "schedule" -> schedule(sender, a);
                case "radius" -> radius(sender, a);
                case "types" -> types(sender);
                default -> help(sender);
            }
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Fehler bei /vcl " + sub, e);
            Msg.err(sender, "Interner Fehler. Details stehen in der Serverkonsole.");
        }
        return true;
    }

    private void help(CommandSender s) {
        Msg.ok(s, "Befehle:");
        for (Help h : HELP) {
            if (allowed(s, h.sub())) {
                Msg.info(s, h.usage() + " - " + h.description());
            }
        }
    }

    private void create(CommandSender s, String[] a) {
        if (!(s instanceof Player p)) {
            Msg.err(s, "Dieser Befehl ist nur für Spieler, da er deine Position verwendet.");
            return;
        }
        if (a.length < 2) {
            Msg.err(s, "Verwendung: /vcl create <name> [radius]");
            return;
        }
        Integer radius = null;
        if (a.length >= 3) {
            radius = parseInt(a[2]);
            if (radius == null) {
                Msg.err(s, "'" + a[2] + "' ist keine gültige Zahl für den Radius.");
                return;
            }
        }
        reply(s, manager.create(a[1], p.getLocation(), radius));
    }

    private void remove(CommandSender s, String[] a) {
        if (a.length < 2) {
            Msg.err(s, "Verwendung: /vcl remove <name>");
            return;
        }
        reply(s, manager.remove(a[1]));
    }

    private void list(CommandSender s) {
        if (manager.loaders().isEmpty()) {
            Msg.info(s, "Es gibt noch keine Chunkloader. Erstelle einen mit /vcl create <name>.");
            return;
        }
        Msg.ok(s, "Chunkloader (" + manager.loaders().size() + "):");
        for (ChunkLoaderData d : manager.loaders()) {
            Msg.info(s, d.name() + " - " + d.world() + " (" + d.x() + ", " + d.y() + ", " + d.z() + "), Radius "
                    + d.radius() + " - " + manager.describeState(d));
        }
    }

    private void info(CommandSender s, String[] a) {
        ChunkLoaderData d = need(s, a, "info");
        if (d == null) {
            return;
        }
        Msg.ok(s, "Chunkloader: " + d.name());
        Msg.info(s, "Status: " + manager.describeState(d));
        Msg.info(s, "Welt/Position: " + d.world() + " (" + d.x() + ", " + d.y() + ", " + d.z() + "), Chunk "
                + d.centerChunkX() + "/" + d.centerChunkZ());
        Msg.info(s, "Radius: " + d.radius() + " (" + ChunkLoaderData.chunkCount(d.radius()) + " Chunks)");
        Msg.info(s, "Zeitplan: " + d.schedule().describe());
        Msg.info(s, "Virtueller Spieler: " + d.playerName() + " - Modus " + plugin.settings().simulationMode()
                + " (kein Spieler-Entity)");
        Msg.info(s, "Plugin-Tickets gehalten: " + manager.heldChunks(d));
        World w = Bukkit.getWorld(d.world());
        if (w == null) {
            Msg.warn(s, "Welt nicht geladen - keine Chunk-Auswertung möglich.");
            return;
        }
        ChunkInspector.ChunkStats st = ChunkInspector.inspect(d, w, plugin.settings().maxVillagersScanned());
        Msg.info(s, "Chunks geladen: " + (st.loaded() == st.total() ? "Ja" : "Nein") + " (" + st.loaded() + "/"
                + st.total() + ")");
        Msg.info(s, "Entity-Ticking: " + (st.entityTicking() == st.total() ? "Aktiv" : st.entityTicking() > 0
                ? "Teilweise" : "Inaktiv") + " (" + st.entityTicking() + "/" + st.total() + ")");
        Msg.info(s, "Dorfbewohner erkannt: " + st.villagers().size());
    }

    private void toggle(CommandSender s, String[] a, boolean enable) {
        ChunkLoaderData d = need(s, a, enable ? "enable" : "disable");
        if (d != null) {
            reply(s, manager.setEnabled(d, enable));
        }
    }

    private void reload(CommandSender s) {
        plugin.reloadAll();
        Msg.ok(s, "Konfiguration neu geladen. Server-Einstellungen werden im Hintergrund neu gelesen.");
    }

    private void status(CommandSender s) {
        PluginSettings cfg = manager.settings();
        long active = manager.loaders().stream().filter(manager::isHolding).count();
        Msg.ok(s, "Status:");
        Msg.info(s, "Chunkloader: " + manager.loaders().size() + " (aktiv: " + active + ", Limit "
                + cfg.maxLoaders() + ")");
        Msg.info(s, "Gehaltene Chunks (eindeutig): " + manager.uniqueChunks() + " / " + cfg.maxTotalChunks());
        Msg.info(s, "Simulationsmodus: " + cfg.simulationMode() + " - " + cfg.simulationMode().description());
        Integer pause = plugin.serverSettings().pauseWhenEmptySeconds();
        if (pause == null) {
            Msg.warn(s, "Server-Pause ohne Spieler: unbekannt");
        } else if (pause > 0) {
            Msg.warn(s, "Server pausiert nach " + pause + " s ohne Spieler (pause-when-empty-seconds) - "
                    + "Farmen laufen dann nicht weiter!");
        } else {
            Msg.info(s, "Server-Pause ohne Spieler: deaktiviert");
        }
        for (ChunkLoaderData d : manager.loaders()) {
            Msg.info(s, " - " + d.name() + ": " + manager.describeState(d) + " (" + manager.heldChunks(d)
                    + " Chunks)");
        }
    }

    private void farm(CommandSender s, String[] a) {
        if (a.length < 3) {
            Msg.err(s, "Verwendung: /vcl farm <name> <typ> - Typen: " + String.join(", ", typeKeys()));
            return;
        }
        ChunkLoaderData d = need(s, a, "farm");
        if (d == null) {
            return;
        }
        FarmType type = FarmType.fromKey(a[2]);
        if (type == null) {
            Msg.err(s, "Unbekannter Farmtyp '" + a[2] + "'. Typen: " + String.join(", ", typeKeys()));
            return;
        }
        if (!plugin.settings().diagnosticsEnabled()) {
            Msg.warn(s, "Die Farmdiagnose ist in der config.yml deaktiviert (diagnostics.enabled).");
            return;
        }
        if (type == FarmType.IRON) {
            DiagnosisReport report = ironAnalyzer.analyze(d, manager, plugin.settings(), plugin.serverSettings());
            render(s, report);
            return;
        }
        Msg.ok(s, type.displayName() + ": " + d.name());
        Msg.info(s, "Ohne echten Spieler: " + type.level().label());
        Msg.info(s, type.explanation());
        Msg.warn(s, "Für diesen Farmtyp gibt es keine automatische Analyse, nur die Chunk-Diagnose:");
        World w = Bukkit.getWorld(d.world());
        if (w == null) {
            Msg.err(s, "Welt '" + d.world() + "' ist nicht geladen.");
            return;
        }
        ChunkInspector.ChunkStats st = ChunkInspector.inspect(d, w, 0);
        Msg.info(s, "Chunks geladen: " + st.loaded() + "/" + st.total() + ", Entity-Ticking: "
                + st.entityTicking() + "/" + st.total() + ", Loader: " + manager.describeState(d));
    }

    private void render(CommandSender s, DiagnosisReport r) {
        Msg.ok(s, r.title());
        for (Check c : r.checks()) {
            NamedTextColor color;
            String tag;
            switch (c.status()) {
                case OK -> { color = NamedTextColor.GREEN; tag = "[OK] "; }
                case FAIL -> { color = NamedTextColor.RED; tag = "[FEHLT] "; }
                case WARN -> { color = NamedTextColor.YELLOW; tag = "[WARNUNG] "; }
                case UNKNOWN -> { color = NamedTextColor.GRAY; tag = "[?] "; }
                default -> { color = NamedTextColor.AQUA; tag = "[INFO] "; }
            }
            Msg.send(s, color, tag + c.label() + ": " + c.detail());
        }
        DiagnosisReport.Verdict v = r.verdict();
        Msg.send(s, v == DiagnosisReport.Verdict.MET ? NamedTextColor.GREEN
                : v == DiagnosisReport.Verdict.PARTIAL ? NamedTextColor.YELLOW : NamedTextColor.RED,
                "Status: " + v.text());
    }

    private void types(CommandSender s) {
        Msg.ok(s, "Farmtypen ohne echten Spieler:");
        for (FarmType t : FarmType.values()) {
            Msg.info(s, t.key() + " (" + t.displayName() + "): " + t.level().label() + " - " + t.explanation());
        }
    }

    private void schedule(CommandSender s, String[] a) {
        if (a.length < 3) {
            Msg.err(s, "Verwendung: /vcl schedule <name> always  ODER  /vcl schedule <name> <HH:mm> <HH:mm>");
            return;
        }
        ChunkLoaderData d = need(s, a, "schedule");
        if (d == null) {
            return;
        }
        if (a[2].equalsIgnoreCase("always")) {
            reply(s, manager.setSchedule(d, LoaderSchedule.always()));
            return;
        }
        if (a.length < 4) {
            Msg.err(s, "Bitte Start und Ende angeben, z. B. /vcl schedule " + d.name() + " 22:00 06:00");
            return;
        }
        try {
            LoaderSchedule sc = LoaderSchedule.window(LocalTime.parse(a[2]), LocalTime.parse(a[3]));
            reply(s, manager.setSchedule(d, sc));
        } catch (DateTimeParseException e) {
            Msg.err(s, "Ungültige Uhrzeit. Format: HH:mm (z. B. 22:00).");
        } catch (IllegalArgumentException e) {
            Msg.err(s, e.getMessage());
        }
    }

    private void radius(CommandSender s, String[] a) {
        if (a.length < 3) {
            Msg.err(s, "Verwendung: /vcl radius <name> <radius>");
            return;
        }
        ChunkLoaderData d = need(s, a, "radius");
        if (d == null) {
            return;
        }
        Integer r = parseInt(a[2]);
        if (r == null) {
            Msg.err(s, "'" + a[2] + "' ist keine gültige Zahl.");
            return;
        }
        reply(s, manager.setRadius(d, r));
    }

    // ------------------------------------------------------------ Hilfsmethoden

    private ChunkLoaderData need(CommandSender s, String[] a, String sub) {
        if (a.length < 2) {
            Msg.err(s, "Fehlender Parameter <name>. Verwendung: /vcl " + sub + " <name> ...");
            return null;
        }
        ChunkLoaderData d = manager.find(a[1]);
        if (d == null) {
            Msg.err(s, "Chunkloader '" + a[1] + "' nicht gefunden. Mit /vcl list siehst du alle.");
        }
        return d;
    }

    private static void reply(CommandSender s, LoaderManager.Result r) {
        if (r.ok()) {
            Msg.ok(s, r.message());
        } else {
            Msg.err(s, r.message());
        }
    }

    private static Integer parseInt(String v) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> typeKeys() {
        return Arrays.stream(FarmType.values()).map(FarmType::key).toList();
    }

    // ------------------------------------------------------------ Tab-Vervollständigung

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String alias, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            for (Help h : HELP) {
                if (allowed(s, h.sub())) {
                    out.add(h.sub());
                }
            }
            return filter(out, a[0]);
        }
        String sub = a[0].toLowerCase(Locale.ROOT);
        if (!allowed(s, sub)) {
            return out;
        }
        boolean takesLoader = List.of("remove", "info", "enable", "disable", "farm", "schedule", "radius").contains(sub);
        if (a.length == 2 && takesLoader) {
            manager.loaders().forEach(d -> out.add(d.name()));
            return filter(out, a[1]);
        }
        if (a.length == 3) {
            switch (sub) {
                case "farm" -> out.addAll(typeKeys());
                case "schedule" -> out.addAll(List.of("always", "22:00", "00:00", "06:00"));
                case "radius", "create" -> out.addAll(List.of("1", "2", "3"));
                default -> { }
            }
            return filter(out, a[2]);
        }
        if (a.length == 4 && sub.equals("schedule")) {
            out.addAll(List.of("06:00", "08:00", "12:00", "18:00"));
            return filter(out, a[3]);
        }
        return out;
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }
}
