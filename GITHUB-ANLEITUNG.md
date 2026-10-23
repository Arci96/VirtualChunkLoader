# GitHub-Anleitung für Windows

Ziel: Du änderst Code, pushst ihn, und GitHub baut die fertige JAR automatisch.

## 1. Konto
Auf https://github.com ein Konto erstellen oder anmelden. Unter https://git-scm.com/download/win **Git für Windows** installieren.

## 2. Repository erstellen
Auf GitHub: **New repository** → Name `VirtualChunkLoader` → **Create repository** (ohne README anlegen, wenn du die Dateien selbst hochlädst).
Der Hauptbranch muss `main` heißen (die Workflows sind darauf eingestellt).

## 3. Projekt hinzufügen
**Variante A – Git (empfohlen):** in der PowerShell im entpackten Projektordner:
```powershell
git init -b main
git add .
git commit -m "Erster Stand"
git remote add origin https://github.com/DEIN-NAME/VirtualChunkLoader.git
git push -u origin main
```
(Beim ersten Push meldest du dich im Browserfenster bei GitHub an. Gib nie Passwörter oder Tokens in Dateien ein.)

**Variante B – Upload im Browser:** Im Repository **Add file → Upload files**, den *Inhalt* des Projektordners hineinziehen
(auch den versteckten Ordner `.github`, sonst laufen keine Workflows) → **Commit changes**.

## 4. Änderungen pushen
```powershell
git add .
git commit -m "Beschreibung der Änderung"
git push
```

## 5. Build kontrollieren
Im Repository den Reiter **Actions** öffnen. Grüner Haken = Kompilieren und Tests erfolgreich, rotes Kreuz = Fehler (Klick auf den Lauf zeigt das Log).

## 6. JAR aus den Artefakten laden
Actions → gewünschten Lauf anklicken → unten bei **Artifacts** `VirtualChunkLoader-jar` herunterladen (ZIP entpacken).

## 7. Release per Tag
Zuerst die Version in `pom.xml` und `CHANGELOG.md` anheben (z. B. `1.0.0`), pushen, dann:
```powershell
git tag v1.0.0
git push origin v1.0.0
```
Der Tag muss zur `pom.xml`-Version passen und auf `main` liegen, sonst bricht der Release-Workflow ab.

## 8. JAR aus Releases laden
Im Repository rechts **Releases** → neueste Version → `VirtualChunkLoader-1.0.0.jar`.

## 9. Installation
JAR in den Ordner `plugins` des Paper-Servers (1.21.11, Java 21) kopieren.

## 10. Testen
Server neu starten, dann `/vcl status` ausführen. Für Farmen ohne Spieler zusätzlich `pause-when-empty-seconds=-1` in `server.properties` setzen und neu starten.
