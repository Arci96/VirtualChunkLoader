# VirtualChunkLoader

Paper-Plugin für **Minecraft Java 1.21.11**. Es hält ausgewählte Chunks per Plugin-Chunk-Ticket geladen und
tickend und **diagnostiziert**, ob Farmen (vor allem Ironfarmen) auch ohne echten Spieler weiterlaufen können.

> **Ehrlich vorab:** Das Plugin simuliert **keinen echten Spieler**. Der "virtuelle Spieler" ist ein logisches
> Objekt (Name, Position, Radius, Chunks). Chunkloading allein macht nicht jede Farm funktionsfähig.

## Voraussetzungen
- Paper 1.21.11, Java 21
- Für Farmen ohne Spieler in `server.properties`: `pause-when-empty-seconds=-1` (sonst pausiert der Server komplett, sobald niemand online ist; Neustart nötig)
- In `spigot.yml` darf `entity-activation-range.tick-inactive-villagers` nicht `false` sein

## Installation
1. JAR aus **Releases** (oder aus dem Actions-Artefakt) laden.
2. In `plugins/` des Paper-Servers legen, Server neu starten.
3. `/vcl status` ausführen.

## Befehle
| Befehl | Beschreibung | Recht |
|---|---|---|
| `/vcl help` | Zeigt alle Befehle | – |
| `/vcl create <name> [radius]` | Chunkloader an deiner Position | `create` |
| `/vcl remove <name>` | Entfernt einen Chunkloader, gibt Tickets frei | `remove` |
| `/vcl list` | Listet alle Chunkloader | `list` |
| `/vcl info <name>` | Details, Chunks, Entity-Ticking, Dorfbewohner | `list` |
| `/vcl enable <name>` / `disable <name>` | Aktivieren / Deaktivieren | `admin` |
| `/vcl reload` | Konfiguration neu laden | `reload` |
| `/vcl status` | Aktive Loader, gehaltene Chunks, Server-Pause | `list` |
| `/vcl farm <name> <typ>` | Farm analysieren (`iron` voll, andere: Einschätzung + Chunk-Diagnose) | `list` |
| `/vcl schedule <name> always` oder `<HH:mm> <HH:mm>` | Zeitplan (Serverzeit) | `admin` |
| `/vcl radius <name> <radius>` | Radius in Chunks ändern | `admin` |
| `/vcl types` | Welche Farmtypen ohne Spieler laufen | `list` |

## Berechtigungen
`virtualchunkloader.admin` (Standard: OP, enthält alle anderen), `.create`, `.remove`, `.list`, `.reload`.

## Konfiguration
Alle Optionen stehen kommentiert in `plugins/VirtualChunkLoader/config.yml`: Limits (Loader, Chunks pro Loader, Chunks gesamt,
Standardradius), Wiederherstellung beim Start, Speicherdatei, Zeitplan-Intervall, virtueller Spieler, Farmdiagnose, Debug, Protokollierung.
Standard: max. 5 Loader, 25 Chunks pro Loader (Radius 2), 100 Chunks gesamt.

## Was unterstützt wird (und was nicht)
| Farmtyp | Ohne echten Spieler |
|---|---|
| Pflanzen, Zuckerrohr, Bambus, Kaktus | Vollständig (Random Ticks in tickenden Chunks, Redstone-Ernte) |
| Lager-/Sortiersysteme | Vollständig, solange der Weg in geladenen Chunks liegt |
| Eisenfarm | Teilweise: Golem-Spawn braucht nach Vanilla-Logik keinen Spieler, aber Server-Pause, inaktive Dorfbewohner, Schlaf, Spawnfläche müssen stimmen (`/vcl farm <name> iron`) |
| Mobfarmen (natürliches Spawnen, Spawner) | Gar nicht – benötigt einen echten Spieler |

Diese Einschätzungen stützen sich auf Vanilla-Mechaniken und sind **kein Garantieversprechen** für ein konkretes Design.
Die Diagnose meldet nur, was gelesen werden kann; Unklares wird als "unbekannt" angezeigt. Es werden **keine** Golems oder Items erzeugt.

### Grenzen
- Kein Fake-Player-Entity: Die Paper-API bietet keinen offiziellen Weg dafür. Eine NMS-/Packet-Lösung wäre versionsabhängig und ist nicht enthalten (`SimulationMode` ist dafür erweiterbar).
- Folia wird nicht unterstützt.

## Fehlerbehebung
- **Loader "inaktiv (Welt nicht geladen)"**: Die Welt wird später geladen; der Loader aktiviert sich automatisch beim Laden der Welt.
- **"Limit erreicht"**: Limits in `config.yml` prüfen, dann `/vcl reload`.
- **Farm steht, obwohl Chunks geladen**: `/vcl farm <name> iron` ausführen und rote `[FEHLT]`-Zeilen beheben; häufigster Fall ist `pause-when-empty-seconds`.
- **Defekte `loaders.yml`**: wird als `loaders.yml.broken-<zeit>` gesichert, das Plugin startet leer.
- Debug: `debug: true` in der `config.yml`.

## Entwicklung
`mvn verify` baut `target/VirtualChunkLoader-<version>.jar`. Die Version steht nur in der `pom.xml`.
Details zu GitHub: [docs/GITHUB-ANLEITUNG.md](docs/GITHUB-ANLEITUNG.md).
