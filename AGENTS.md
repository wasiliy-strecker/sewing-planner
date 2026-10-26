# NähPlan

Eigenständiges Java-Projekt mit eigenem Git-Repository. Übergeordnete Workspace-
Regeln gelten ergänzend. BücherRegal, GarnBox und StrickProjekt nicht verändern.

## Umfang und Architektur

Stufe 4 der Java-Hobby-Reihe. Nähprojekte mit Textfeldern, einem optionalen Foto,
den drei Statuswerten Geplant, Angefangen, Fertig und einer einfachen Checkliste.
Keine weiteren Hauptfunktionen. KostümPlan bleibt die fünfte Reservestufe.
Java 21, JavaFX 21.0.11, SQLite JDBC, Maven mit Wrapper und ein Maven-Modul.
Pakete `domain`, `application`, `persistence`, `ui`. Keine gemeinsame Runtime.
Java-Bezeichner englisch, Oberfläche und Projektdokumentation deutsch.

## Befehle

Mit vollständigem JDK 21 in `JAVA_HOME` und dessen `bin` im `PATH`:

```bash
./mvnw spotless:apply
./mvnw verify
./scripts/run.sh
./scripts/package.sh app-image
bash -n scripts/run.sh scripts/package.sh
git diff --cached --check
```

Die Skripte finden bei fehlendem `JAVA_HOME` auch `$HOME/.local/jdk-21`.
Windows verwendet `mvnw.cmd` und `scripts/package.ps1`. Native Paketierung
und Grenzen sind in `docs/packaging.md` beschrieben.

## Regeln für Daten und Entwürfe

Texte, Foto, Status und Schritte bilden einen gemeinsamen Entwurf. Speichern
ist eine SQLite-Transaktion. Fehler erhalten den vollständigen Entwurf.
Abbrechen, Escape und Fensterschließen schützen auch reine Schrittänderungen.
„Weiterbearbeiten“ bleibt die sichere Vorauswahl. Keine sofort speichernde
Bearbeitungsvariante im Hauptfenster.

Schritte verwenden stabile UUIDs, niemals Text oder Listenindex als Identität.
Positionen sind explizit gespeichert. Gleichlautende Schritte sind erlaubt.
Keine leeren Texte speichern. Keine automatischen Statusübergänge oder Häkchen.
Fremdschlüssel auf jeder JDBC-Verbindung aktivieren. Projektlöschung entfernt
die zugehörigen Schritte. Unbekannte Statuswerte oder beschädigte Schrittwerte
melden, niemals still reparieren. Statusschlüssel speichern, keine Ordinalzahlen.

Bildprüfung: JPEG/PNG nach Inhalt, 10 MiB, 6000 Pixel je Seite, 16 Millionen
Pixel insgesamt. Originaldateien unverändert lassen. Datenbankzugriff und
Bilddecodierung außerhalb des JavaFX-Threads. Veraltete Vorschauen verwerfen.
Benutzerdaten liegen außerhalb von Programm und Quellcode.

Vor Commit Formatter und `verify` mit Compiler-Lint, Enforcer, Spotless,
SpotBugs sowie erforderliche GUI-Abnahme gemäß `docs/verification.md` ausführen.
Nur synthetische Testdaten verwenden. Changelog im Workspace ergänzen.
Keine Datenbanken, Build-Ausgaben oder Secrets committen. Kein Push oder
Veröffentlichen ohne Auftrag.
