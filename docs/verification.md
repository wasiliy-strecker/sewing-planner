# Prüfbericht

## CJK-Schriftdarstellung, 31. Oktober 2026

Der gemeldete Beispieltitel „Bücher 日本語 📚“ ist in der synthetischen
BücherRegal-Datenbank vollständig gespeichert. Die drei Kästchen betreffen die
Darstellung. Alle fünf Apps verwenden jetzt eine app-lokal mitgelieferte,
explizit geladene Schrift Noto Sans CJK JP 2.004 mit Regular und Bold.
Die Änderung gilt für Tabellen, Eingaben und die übrige gestylte Oberfläche.
Gespeicherte Texte und Sprachwahl werden nicht umgeschrieben.

Die eingesehene native Linux-Schriftauswahl von OpenJFX 21.0.11 filtert bei der
Fallback-Ermittlung auf TrueType. Die hier installierten Noto-CJK-Schriften
verwenden CFF-Outlines. Die primäre, direkt geladene OTF-Schrift umgeht diesen
Fallback für die enthaltenen Zeichen. Andere Unicode-Zeichen und die Darstellung
von Emojis hängen weiterhin von der JavaFX-Ersatzschrift des Zielsystems ab.

Quelle: [OpenJFX 21.0.11, fontpath_linux.c](https://github.com/openjdk/jfx21u/blob/21.0.11%2B4/modules/javafx.graphics/src/main/native-font/fontpath_linux.c).
Die Einbindung nutzt die öffentliche API
[Font.loadFont](https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/text/Font.html).

Formatter und Java-Kompilierung mit Paketbau erfolgreich. Pro App wurde
`./mvnw spotless:apply package -Dmaven.test.skip=true` mit dem eigenen
Temurin 21.0.13+9 ausgeführt. Protokoll: `target/ui-font-build.log`.
Die vorhandenen Linux-App-Images wurden mit dem neuen App-JAR und den
zusätzlichen Lizenzhinweisen aktualisiert. Die Java-Laufzeit blieb unverändert.

Auf Nutzerwunsch keine Tests, kein `verify` und keine automatisierte GUI-Abnahme.
Manuell abzunehmen: den obigen Beispieltitel in Tabelle und Bearbeitungsdialog
lesen, zusätzlich Umlaute und Akzente in den sechs UI-Sprachen ansehen und
Dialoggrößen sowie fette Überschriften kontrollieren. In den anderen Apps dafür
nur eigene temporäre Beispieldaten verwenden. Windows und macOS bleiben offen.

## Datenschutz und lokale Paketvorbereitung, 31. Oktober 2026

Aus BücherRegal übernommen: Info-Menü, vollständige Offline-Datenschutzerklärung
in sechs Sprachen, größerer scrollbar-kopierbarer Dialog, Zentrierung nach nativen
Größenkorrekturen und verzögertes Einblenden. Die Texte beschreiben die tatsächlichen
Daten dieser App. Fotos bleiben lokale Datenbankkopien. Status und Arbeitsschritte
werden nur in den Apps mit diesen Funktionen genannt. KostümPlan beschreibt
zusätzlich seinen PDF-Export. Es wurde kein JSON-Import/-Export ergänzt.

Destruktive Schaltflächen haben einen roten Fokusrahmen. Die übrigen Aktionen
behalten die jeweilige App-Farbe. Die Auswahlfläche der Tabelle bleibt erhalten,
der zusätzliche Fokusrahmen der Tabellenzeile ist transparent.

Tatsächlich ausgeführt: Formatter, Java-Compiler und Linux-App-Image-Bau mit
Temurin 21.0.13+9. Tests waren durch `-Dmaven.test.skip=true` ausdrücklich
ausgeschaltet. Der Pakethelfer führte nur seinen Launcher-Versionsaufruf aus.
Der Datenschutztextexport erzeugte sechs Dateien aus den produktiven Ressourcen.
Keine Tests, kein `verify`, keine Fehleranalyse und kein GUI-Prüflauf dieses
Standes. Frühere Prüfergebnisse gelten ausschließlich für die damaligen Stände.

Buildprotokoll: `target/portfolio-privacy-build.log`.
Datenschutztexte: `build/releases/privacy/2026-10-31`.
Die Pakete enthalten die eigene Laufzeit, Lizenzhinweise, Bedienungsanleitung
und die originale Herkunftsdatei des Build-JDK. Die geänderten CI-Workflows und
native Windows-/macOS-Paketierung wurden nicht ausgeführt. Keine neuen Store-
Screenshots erzeugt. Frühere Lieferungen wurden nicht überschrieben.

Manuelle Abnahme durch den Nutzer bleibt offen:

1. Einen Eintrag wählen und mit Tab „Bearbeiten“ sowie „Löschen“ fokussieren.
   Nur der destruktive Fokusrahmen ist rot. Die Zeilenauswahl bleibt sichtbar.
2. Unter „Info“ die Erklärung öffnen. Positionierung und Einblenden beurteilen,
   scrollen, Text kopieren und über Escape sowie „Schließen“ zurückkehren.
3. Den Dialog nach einem Sprachwechsel und Neustart in den sechs Sprachen prüfen.
   Texte, Fotos und die vorhandenen Status-/Schrittdaten müssen erhalten bleiben.
4. Die bisherigen Kernfunktionen der App einschließlich Fotoauswahl und
   gegebenenfalls Status, Checkliste oder PDF manuell abnehmen.

Eine Store-Freigabe ist damit nicht belegt. Identität, MSIX, Signierung,
Installation, Updates, WACK und Windows-Aufnahmen bleiben offen.

## Internationalisierung, 29. Oktober 2026

`./mvnw verify -Dnaehplan.uiTests=true` besteht auf einem eigenen Xvfb-Display
mit 134 Tests ohne Fehler oder übersprungene Fälle. Compiler-Lint, Enforcer,
Spotless und SpotBugs bestehen. Ruff, Shell-Syntax und Diff-Prüfung bestehen.
110 UTF-8-Meldungen je Sprache decken Oberfläche, Checkliste und Fehler ab.

18 echte JavaFX-Prozesse prüfen sechs Sprachen mit 618 Prüfbedingungen.
Die Checklistenprüfung umfasst leere Schritte, gleiche Texte mit unabhängigen
UUIDs, Positionslücken, Häkchen, Verwerfen, SQLite-Sperre, Wiederholung und
unveränderten Neustart. Fotos, Unicode und alle drei Statuswerte bleiben erhalten.
24 echte Screenshots wurden visuell geprüft. Der Bearbeitungsdialog wird für
die Aufnahme auf 1040 Pixel vergrößert und zeigt die Checkliste vollständig.

Das eigenständige Linux-Paket startet außerhalb des Projekts ohne `JAVA_HOME`
und mit `PATH=/nonexistent`. Die eingebettete JVM enthält `jdk.localedata`.
1234 synthetische Projekte im Bestandsschema prüfen regionale Zahlengruppierung.
Native JPEG-Auswahl, gemeinsame Speicherung und englischer Neustart erhalten
Fotobytes, Status sowie Schritt-UUIDs, Häkchen und Positionslücken.

Nachweise: `target/international-ui-verify-v2.log`,
`target/international-captures-final-v2/`, `target/international-packaged-final-v2/`.
Die geprüfte Paketlaufzeit liegt in `target/app-image/NaehPlan/`.
Die Ordnerlieferung bindet Quellen, Paket und Nachweise per Hash an den Commit.
Windows-Paketabnahme und Store-Einreichung bleiben offen.

## Historischer Erststand

Stand: 26. Oktober 2026, NähPlan 0.1.0.

## Automatische Prüfungen

Linux x64, Eclipse Temurin JDK 21.0.11, JavaFX 21.0.11,
SQLite JDBC 3.53.4.0 und Maven 3.9.16 über Maven Wrapper.

```bash
./mvnw spotless:apply
./mvnw verify
./mvnw verify -Dnaehplan.uiTests=true
bash -n scripts/run.sh scripts/package.sh
git diff --cached --check
```

93 JUnit-Testfälle ohne Oberfläche und 10 zusätzliche JavaFX-Testfälle bestanden.
Der vollständige Lauf mit aktivierten UI-Tests meldet **103 Tests, keine Fehler,
keine Fehlschläge und keine übersprungenen Tests**. Compiler-Lint mit `-Werror`,
Maven Enforcer, Spotless und SpotBugs bestanden ohne Befunde.
Shellsyntax, POM-XML und Workflow-YAML wurden geprüft.

Die UI-Tests sind bei normalem `verify` deaktiviert. Für den vollständigen Lauf
ist ein grafischer Bildschirm erforderlich. Hier liefen sie auf einem separaten
Xvfb-Bildschirm. Auf einem Linux-System mit installiertem Xvfb:

```bash
xvfb-run --auto-servernum ./mvnw verify -Dnaehplan.uiTests=true
```

| Bereich | Nachgewiesenes Verhalten |
| --- | --- |
| Grundfunktionen | Anlegen, Bearbeiten, Löschen, stabile Projekt-IDs, gleichnamige Projekte, Pflichtfeld und optionale Texte |
| Suche | Alle Projekttextfelder, Schritttexte, Umlaute, NFC-Schreibvarianten, leere Suche, keine Treffer und genau ein Projekttreffer bei mehreren passenden Schritten |
| Schrittmodell | Stabile UUIDs, unabhängige gleiche Texte, explizite aufsteigende Positionen, unveränderliche Listen und Unicode-Leerraumprüfung |
| Checkliste | Anlegen, Umbenennen, Abhaken, Wiederöffnen, Entfernen, Anhängen und leere Liste nach erneutem Öffnen der Datenbank |
| Unabhängigkeit | Alle Statuswechsel erhalten Häkchen. Vollständig erledigte oder offene Listen ändern bei keinem der drei Statuswerte den Status |
| Transaktion | Fehler beim zweiten Schritt-INSERT rollt Texte, Foto, Status und alle Schritte zurück. Fehlgeschlagenes Anlegen hinterlässt kein Teilprojekt. Derselbe Entwurf lässt sich danach speichern |
| SQLite | CHECK-Regeln, Fremdschlüssel, Cascade-Löschung, eindeutige Positionen, kollidierende Schritt-ID eines anderen Projekts, Datenbanksperren und ungültige gespeicherte Werte |
| Status | Standard Geplant, genau drei stabile Schlüssel, alle Wechsel, keine Ordinalzahlen, ungültige gespeicherte Werte werden gemeldet |
| Fotos | JPEG/PNG nach Inhalt, eigene Bytekopie, Quellenlöschung, Ersetzen, Entfernen, unberührte Quelle, defensive Kopien, Größen- und Dimensionsgrenzen, beschädigte Dateien |
| Fehler | Ungültiger Pfad, beschädigte Datenbank, neuere Schema-Version, gesperrte Datenbank und fehlende Projekte |

Die zehn echten JavaFX-Testfälle prüfen neue leere Projekte, Abhaken,
Wiederöffnen, Umbenennen, Hinzufügen und Entfernen als alleinige Änderungen,
Abbrechen/Escape/Fensterschließen, sichere Standardtaste, Erhalt nach
„Weiterbearbeiten“, Verwerfen ohne Schreiben, ungültige Texte mit Korrektur,
späten Datenbankfehler mit erneutem Speichern, unveränderten Dialogabbruch und
veraltete Foto-Callbacks. Sie verwenden dieselben Steuerelemente, Hintergrund-
Tasks und SQLite-Implementierung wie die App. Die Schließereignisse werden
programmgesteuert an die echten JavaFX-Fenster gesendet.

Alle Tests erzeugen ausschließlich temporäre Datenbanken und synthetische Bilder.
Normale Benutzersammlungen werden nicht geöffnet.

## Sichtbar geprüfte paketierte Anwendung

```bash
./scripts/package.sh app-image
./target/app-image/NaehPlan/bin/NaehPlan --version
./target/app-image/NaehPlan/bin/NaehPlan --data-dir=/absoluter/testpfad
```

Das Linux-x64-App-Image wurde erfolgreich gebaut. Der Launcher meldet
`NähPlan 0.1.0`. Die echte Oberfläche wurde ohne `JAVA_HOME` und mit
`PATH=/nonexistent` gestartet. Die geladenen Prozessbibliotheken bestätigen
`target/app-image/NaehPlan/lib/runtime/lib/server/libjvm.so`.

Zusätzlich zu den automatischen UI-Tests wurde das Paket auf einem separaten
Xvfb-Bildschirm mit Maus und Tastatur über `xdotool` bedient. Screenshots wurden
visuell geprüft. Eine unabhängige Python-SQLite-Verbindung kontrollierte Texte,
Fotobytes, Status, Schritt-IDs, Positionen und Häkchen. Die Testdaten liegen
ausschließlich unter dem ignorierten `target/gui-test-data`.

| Ablauf im Paket | Ergebnis |
| --- | --- |
| Leere Sammlung öffnen | Verständlicher Leerzustand, Projekt kann hinzugefügt werden |
| Projekt mit Textfeldern, mehrzeiliger Notiz, PNG, Angefangen und drei Schritten anlegen | Gemeinsamer Stand gespeichert, ein Schritt abgehakt, weitere Schritte offen |
| Hauptansicht ansehen | Foto, Notiz, Status und schreibgeschützte Checkliste sichtbar |
| Nach NÄHEN suchen, ohne Treffer suchen, Suche leeren | Passender Schritt findet das Projekt, eigener Leerzustand, Sammlung wieder sichtbar |
| Nur einen Schritt abhaken und Escape drücken | Rückfrage. Enter wählt Weiterbearbeiten und erhält den Entwurf |
| Danach erneut Escape und Verwerfen | Gespeicherter Haken bleibt unverändert |
| Schritttext leeren und Speichern | Verständlicher Hinweis, kein Speichern und kein Verlust des Entwurfs |
| Dialog auf 660 × 540 verkleinern | Formular scrollbar, Schritttext, Hinweis und beide Dialogaktionen lesbar |
| Fotoquelle entfernen und Paket neu starten | Texte, Foto, Status, Schritt-IDs, Reihenfolge und Häkchen erhalten |
| Hauptfenster auf 860 × 700 verkleinern | Status Angefangen und Detailbereich lesbar |
| Dateiauswahl abbrechen | Vorhandenes Entwurfsfoto erhalten |
| Textdatei als PNG importieren | Verständlicher Fehler, bisherige Vorschau und Entwurf erhalten |
| PNG durch JPEG ersetzen und speichern | JPEG-Bytes übernommen, Schritte erhalten, Quelle unverändert |
| Löschen mit Enter abbrechen | Projekt und Schritte erhalten |
| Löschung ausdrücklich bestätigen | Projekt, Foto und alle zugehörigen Schritte entfernt |

Der Versionsaufruf allein wäre kein GUI-Nachweis. Die oben genannten Abläufe
belegen JavaFX, ImageIO und natives SQLite in der eingebetteten Laufzeit.

Screenshots mit ausschließlich synthetischen Daten:
[Hauptfenster](screenshots/naehplan.png),
[gemeinsamer Dialog](screenshots/editor.png),
[Verwerfschutz](screenshots/discard.png),
[Pflichtfeldhinweis bei kleinem Dialog](screenshots/validation.png).

## Wiederholbare native Abnahme

Auf dem Zielsystem ein eigenes leeres Verzeichnis über `--data-dir` verwenden.
Die Abläufe aus der Pakettabelle wiederholen. Zusätzlich gleiche Projektnamen
und gleiche Schritttexte anlegen, verschiedene Zeilen umbenennen und entfernen,
neue Zeilen hinten ergänzen und nach Neustart IDs und Reihenfolge kontrollieren.
Fertig mit offenen Schritten und Geplant mit vollständig erledigter Liste testen.
Jede alleinige Schrittaktion mit allen drei Schließwegen prüfen.

Für einen Speicherfehler ausschließlich die Testdatenbank über eine zweite
SQLite-Verbindung mit `BEGIN EXCLUSIVE` sperren. Den vollständigen Projektentwurf
ändern und speichern. Nach der Fehlermeldung die Sperre mit `ROLLBACK` lösen.
Der unveränderte Entwurf muss sich erneut speichern lassen. Dieser Fall ist
bereits automatisiert geprüft, einschließlich eines Fehlers nach Beginn der
Schrittspeicherung. Auf dem Zielsystem zusätzlich die Fehlerdarstellung prüfen.

## Noch offen

Windows und macOS stehen in dieser Umgebung nicht zur Verfügung. EXE-, DMG-
und PowerShell-Skripte sowie GitHub-Workflows sind vorbereitet, dort jedoch noch
nicht ausgeführt. Native Installation, Update, Deinstallation ohne Datenverlust,
Skalierung, Dateiauswahl, Tastenkürzel und Sicherheitsdialoge sind auf Windows,
Intel-Mac und Apple Silicon zu prüfen.

Signierung, Store-Registrierung, Mac-App-Sandbox, Entitlements und konkrete
Einreichungspakete gehören zu einem späteren Releaseauftrag. Ein normaler
Installer ist keine geprüfte Store-Version. Siehe [Paketierung](packaging.md).
Kein Push, Upload oder Veröffentlichen wurde durchgeführt.
