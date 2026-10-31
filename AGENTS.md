# Sewing Planner

Eigenständiges Java-Projekt mit eigenem Git-Repository. Übergeordnete Workspace-
Regeln gelten ergänzend. BücherRegal, GarnBox und StrickProjekt nicht verändern.

## Umfang und Architektur

Stufe 4 der Java-Hobby-Reihe. Nähprojekte mit Textfeldern, einem optionalen Foto,
den drei Statuswerten Geplant, Angefangen, Fertig und einer einfachen Checkliste.
Keine weiteren Hauptfunktionen. KostümPlan bleibt die fünfte Reservestufe.
Java 21, JavaFX 21.0.11, SQLite JDBC, Maven mit Wrapper und ein Maven-Modul.
Pakete `domain`, `application`, `persistence`, `ui`. Keine gemeinsame Runtime.
Java-Bezeichner und GitHub-Metadaten englisch, Projektdokumentation deutsch.
Die Oberfläche unterstützt de-DE, en-US, es-ES, fr-FR, it-IT und pt-BR.
Sämtliche neuen UI-Texte in allen sechs UTF-8-Katalogen ergänzen.
Sprachwahl gilt ab Neustart. Daten und stabile Status-/Schrittwerte nie übersetzen.
`docs/internationalization.md` beschreibt isolierte GUI-Prüfungen und Ordnerlieferung.

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

Die Skripte finden bei fehlendem `JAVA_HOME` auch `$HOME/.local/share/naehplan/jdk-21.0.13+9`.
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

## Datenschutz und lokale Release-Vorbereitung

Build und CI sind auf Eclipse Temurin 21.0.13+9 festgelegt. Eigene Installation
je App verwenden. Datenschutzdialog und Store-Textexport verwenden dieselben
sechs Ressourcen. Foto-, Status-, Checklisten- und PDF-Aussagen müssen dem
Funktionsumfang dieser App entsprechen. Der Sammlungstransfer ist durch den ausdrücklichen Auftrag vom 31. Oktober
2026 autorisiert. App-eigenes Format verwenden und zusätzliche Projektfelder erhalten.

Der Dialog verwendet die tatsächlichen Fenstermaße nach nativen Größenkorrekturen
und wird erst nach der Positionierung sichtbar. Destruktive Schaltflächen tragen
die CSS-Klasse `danger` und einen roten Fokusrahmen.

Microsoft Store: MSIX. Kostenloser GitHub-Download: EXE. Identitäten, Signierung,
Windows-Abnahme und Veröffentlichung bleiben separate Release-Schritte.

## Übertragene Verbesserungen, 31. Oktober 2026

Vorlage ist Yarn Box einschließlich der Nachkorrekturen bis `7fc3363`.
Farbwähler mit Erhalt alter Texte, lesbare Farbcodes, zentrierte Dialoge ohne
Tabellenblinken, gemerkter Fotoordner und kein Neuladen-Button.
Neue Fotos höchstens 1600 Pixel längste Seite ohne Vergrößerung, JPEG 0,85
beziehungsweise PNG bei Transparenz. EXIF-Ausrichtung anwenden, Quellmetadaten
nicht übernehmen. Datenbank- und JSON-Fotos nur prüfen, niemals neu codieren.

Öffentlicher Name ist **Sewing Planner**, Paket-ID und Datenpfade bleiben erhalten.
Englisch beim Erststart, bestehende Sprachwahl behalten. Erste Store-Sprache
de-DE. Alle sechs UI-Kataloge einschließlich Fehlermeldungen vollständig halten.
JSON-Format `sewing-planner.collection`, Version 1. Import atomar ergänzend,
keine Bestandsüberschreibung. Duplikatanzahl aus den Inhalten berücksichtigen.
Status und sämtliche Checklistendaten müssen vollständig übertragen werden.
Höchstens 10.000 Projekte und 1.048.576 Zeichen je Textfeld, 256 MiB je JSON.
Höchstens 10.000 Schritte je Projekt. Doppelte Schrittkennungen beim Import
nur für die ergänzte Kopie ersetzen. Kopienvergleich anhand Inhalt und
Positionen, unabhängig von intern neu vergebenen Kennungen.
Neuanlagen und Importe atomar begrenzen. Größere Altbestände nicht kürzen.
Exportgrenzen vor Veröffentlichung prüfen, temporäre Dateien bereinigen.

Desktop-Start aus Agentensitzungen über `systemd-run --user --property=Type=exec`
mit übernommenem DISPLAY und XAUTHORITY, damit die App nach Tool-Ende offen
bleibt. Zuerst nach vorhandenen Fenstern suchen und keine zweite Instanz der
Sammlung öffnen. Ungespeicherte Entwürfe vor Neustart schützen.

Der Nutzer übernimmt in diesem Auftrag die Tests selbst. Paketbau mit
`-Dmaven.test.skip=true` und statische Analyse, keine Test- oder GUI-Suite.

## Mac-Store-Inhalte vom 31. Oktober 2026

Für die ausdrücklich beauftragte Inhaltslieferung nach dem Vorbild von
Knitting Projects keine App-Builds, Maven-Aufrufe, Paketierung oder Uploads.
`scripts/mac_store_content.py` verwendet die vorhandenen geprüften App-JARs.
Der separate Aufnahmehelfer läuft im Java-Quelltextmodus mit eigenen temporären
Daten und eigenem Xvfb. Seine gezielten Import-/Exportprüfungen gehören zur
Asset-Erstellung. Keine vollständige Release-Testserie für diese Inhaltsänderung.

Vier deutsche Linux-Bildentwürfe, kopierbare Storetexte, synthetischer Beispielbestand
und bestehende Datenschutzerklärung werden in einem neuen Ordner ohne ZIP geliefert.
Persönliche Sammlungen und die geöffnete Nutzer-App bleiben unberührt.
Die vorgesehene neue Apple-Kennung ist `de.appfabrik.sewingplanner`.
Ihre Registrierung ist nicht bestätigt. Eine Umstellung bestehender technischer
Kennungen ist kein Teil der Inhaltslieferung. Native Mac-Aufnahmen folgen separat.
