# Changelog

Alle wichtigen Änderungen an diesem Projekt. Format nach [Keep a Changelog](https://keepachangelog.com/de/1.1.0/),
Versionierung nach [SemVer](https://semver.org/lang/de/).

## [1.0.0] - 2026-10-23
### Hinzugefügt
- Chunkloader über Paper-Plugin-Chunk-Tickets, referenzgezählt (überlappende Loader geben Tickets sicher frei).
- Virtueller Farm-Spieler als logisches Objekt (Modus `TICKET_ONLY`), Namen und Radius konfigurierbar.
- Persistenz in `loaders.yml` (asynchron, atomar), Wiederherstellung beim Serverstart und beim späten Laden von Welten.
- Zeitpläne (`/vcl schedule`), Limits für Loader und Chunks, Aktivieren/Deaktivieren.
- Befehle `/vcl help|create|remove|list|info|enable|disable|reload|status|farm|schedule|radius|types` mit Tab-Vervollständigung.
- Ironfarm-Diagnose auf Basis tatsächlich gelesener Zustände (Chunks, Entity-Ticking, Dorfbewohner, Schlaf, Spawnfläche, Server-Pause).
- Übersicht, welche Farmtypen ohne echten Spieler laufen können.
- GitHub Actions für Build (JAR als Artefakt) und Release (bei Tag `vX.Y.Z`).
