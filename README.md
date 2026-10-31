# NähPlan

Deine Ideen. Stich für Stich. Verwalte kleine Nähprojekte mit Projektname,
Stoff oder Material, Farbe, Aufbewahrungsort, mehrzeiliger Notiz, einem
optionalen Foto und dem Status **Geplant**, **Angefangen** oder **Fertig**.
Die Checkliste hält deine Arbeitsschritte fest. Alles bleibt lokal.

![NähPlan mit ausschließlich synthetischen Testdaten](docs/screenshots/naehplan.png)

Stufe 4 der Java-Reihe BücherRegal → GarnBox → StrickProjekt → NähPlan.
KostümPlan bleibt die fünfte Reservestufe. Gegenüber StrickProjekt kommt
genau eine Hauptfunktion hinzu: die Checkliste pro Projekt.
Material ist Freitext ohne Verbindung zu anderen Apps.

## Starten

Build und CI verwenden Eclipse Temurin 21.0.13+9. Maven prüft Hersteller,
Version und Buildnummer. Für direkte Maven-Aufrufe auf diesem Rechner:

```bash
export JAVA_HOME="$HOME/.local/share/naehplan/jdk-21.0.13+9"
export PATH="$JAVA_HOME/bin:$PATH"
```

Ein vollständiges JDK 21 in `JAVA_HOME` und dessen `bin` im `PATH` verwenden.
Der Maven Wrapper lädt Maven und Abhängigkeiten beim ersten Start.
Die fertige App benötigt weder Konto noch Netzwerkzugriff.

Linux und macOS, aus diesem Verzeichnis:

```bash
./mvnw javafx:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd javafx:run
```

Im vorhandenen Linux-Workspace findet `./scripts/run.sh` auch das JDK unter
`$HOME/.local/share/naehplan/jdk-21.0.13+9`. Das lokale Paket mit eingebetteter Laufzeit startet so:

```bash
./target/app-image/NaehPlan/bin/NaehPlan
```

Der sichtbare Produktname lautet NähPlan. Paketdateien, Launcher und
Datenverzeichnisse verwenden die ASCII-Schreibweise `NaehPlan` beziehungsweise
`naehplan`. Die App-Kennung ist `de.appfabrik.naehplan`.

## Bedienung

„Projekt hinzufügen“ und „Bearbeiten“ öffnen denselben Dialog. Nur der
Projektname ist erforderlich. Gleichnamige Projekte sind unabhängig.
Neue Projekte beginnen mit „Geplant“ und einer leeren Checkliste.
Alle drei Statuswerte lassen sich jederzeit ineinander ändern.
Löschen verlangt eine Bestätigung und entfernt nur das ausgewählte Projekt
einschließlich seines Fotos und seiner Schritte.

„Arbeitsschritt hinzufügen“ ergänzt eine neue, noch offene Zeile am Ende.
Den Text direkt in der Zeile bearbeiten, das Kästchen abhaken oder wieder
öffnen und einzelne Zeilen mit „Entfernen“ entfernen. Gleichlautende Schritte
bleiben unabhängig. Leere Schritttexte werden mit einem verständlichen Hinweis
abgelehnt. Eingaben bleiben dabei erhalten. Eine vollständig leere Checkliste
ist gültig. Die Reihenfolge bleibt nach Speichern und Neustart erhalten.

**Erst „Speichern“ übernimmt Texte, Foto, Status und Checkliste gemeinsam.**
Abbrechen, Escape und Fensterschließen schützen auch ausschließliches Abhaken,
Wiederöffnen, Umbenennen, Hinzufügen oder Entfernen eines Schritts.
„Weiterbearbeiten“ ist die sichere Vorauswahl. „Verwerfen“ erhält den gespeicherten
Stand. Ein Speicherfehler lässt den Dialog mit dem gesamten Entwurf offen.
Die Checkliste im Hauptfenster zeigt ausschließlich den gespeicherten Stand.

Status und Checkliste bleiben unabhängig. Eine vollständig abgehakte Liste
setzt den Status nicht auf Fertig. Ein fertiges Projekt darf offene Schritte
haben. Statusänderungen beeinflussen keine Häkchen.

Die Suche durchsucht Projektname, Material, Farbe, Aufbewahrungsort, Notiz und
alle Schritttexte. Sie ignoriert Groß- und Kleinschreibung einschließlich Umlauten
und Unicode-Schreibvarianten. Mehrere passende Schritte erzeugen keine doppelten
Projekttreffer. Eine leere Suche zeigt alle Projekte.

## Foto

Genau ein JPEG oder PNG auswählen, ansehen, ersetzen oder entfernen.
NähPlan prüft den tatsächlichen Bildinhalt und speichert eine eigene Kopie
in SQLite. Die Quelldatei wird niemals verändert oder gelöscht. Das Foto bleibt
nach Verschieben oder Löschen der Quelle erhalten.

Grenzen: 10 MiB (10.485.760 Bytes), 6000 Pixel je Seite und 16 Millionen Pixel
insgesamt. Abgebrochene Dateiauswahl oder fehlgeschlagener Import erhalten das
bisherige Entwurfsfoto. Verspätete Vorschauen werden nach Auswahlwechseln
verworfen. Während Import und Speichern wartet der Dialog auf den Abschluss.

## Tastatur

| Taste | Aktion |
| --- | --- |
| Strg+N / Cmd+N | Projekt hinzufügen |
| Strg+F / Cmd+F | Suche auswählen |
| Enter oder Doppelklick in der Projektliste | Projekt bearbeiten |
| Entf in der Projektliste | Nach Bestätigung löschen |
| Strg+S / Cmd+S | Gesamten Dialog speichern |
| Escape | Abbrechen mit Verwerfschutz |
| Leertaste auf einem Kästchen | Schritt abhaken oder wieder öffnen |
| Enter im Notizfeld | Neue Zeile |
| Tab / Umschalt+Tab | Steuerelement wechseln |

## Lokale Daten

`naehplan.db` enthält Projekte, Fotobytes und Arbeitsschritte.
Die Standardpfade liegen außerhalb von Programm und Quellcode.

| System | Datenverzeichnis |
| --- | --- |
| Windows | `%LOCALAPPDATA%\NaehPlan`, sonst `~/AppData/Local/NaehPlan` |
| macOS | `~/Library/Application Support/NaehPlan` |
| Linux | `$XDG_DATA_HOME/naehplan`, sonst `~/.local/share/naehplan` |

Zum Sichern die App schließen und `naehplan.db` kopieren. SQLite kann nach dem
Entfernen von Fotos freien Platz intern behalten. Mehrere gleichzeitig
bearbeitende Instanzen werden nicht unterstützt.

Isolierter Teststart mit einem eigenen absoluten Pfad:

```bash
./scripts/run.sh '-Djavafx.args=--data-dir=/tmp/naehplan-test'
./target/app-image/NaehPlan/bin/NaehPlan --data-dir=/tmp/naehplan-test
```

## Prüfen und verstehen

```bash
./mvnw spotless:apply
./mvnw verify
```

`verify` umfasst JUnit, Compiler-Lint mit Warnungen als Fehlern, Enforcer,
Spotless und SpotBugs. Die zusätzlichen echten JavaFX-Dialogtests benötigen
einen grafischen Bildschirm und werden ausdrücklich aktiviert:

```bash
./mvnw verify -Dnaehplan.uiTests=true
```

Tests verwenden temporäre SQLite-Dateien und synthetische Bilder.
[Prüfbericht und manuelle Abnahme](docs/verification.md) dokumentieren Ergebnisse
und Grenzen. [Architektur und Bewerbungsgespräch](docs/architecture.md) erklären
insbesondere stabile Schritt-IDs, gemeinsame Entwürfe und Transaktionen.

Java 21, JavaFX 21.0.11, SQLite JDBC 3.53.4.0, Maven 3.9.16 über Wrapper.
Ein Maven-Modul und ein eigenes Git-Repository. Keine gemeinsame Runtime mit
anderen Apps. [Paketierung](docs/packaging.md) bereitet Windows und macOS vor.
Native Zielplattformtests, Signierung und Store-Einreichungen stehen noch aus.
[Hinweise zu Fremdkomponenten](THIRD_PARTY_NOTICES.md).

Keine Unteraufgaben, Vorlagen, Abhängigkeiten, Termine, Verantwortlichen,
manuelle Umsortierung, automatischen Übergänge, PDFs, Cloud oder weitere
Hauptfunktionen. Es wurde nichts gepusht oder veröffentlicht.

## Sprachen und Store-Vorschau

Die Oberfläche unterstützt Deutsch, Englisch, Spanisch, Französisch, Italienisch
und brasilianisches Portugiesisch. Unter „Sprache“ wählbar, wirksam ab Neustart.
Bestehende Daten, Fotos, Status und Checklisten bleiben erhalten.
[Internationalisierung](docs/internationalization.md) beschreibt sechs Store-Textfassungen,
echte Bildschirmaufnahmen und geprüfte Vorschauordner ohne Lieferarchive.
Windows-Abnahme und Veröffentlichung bleiben separat.

## Datenschutz und Fokusrahmen

„Info“ öffnet die vollständige Datenschutzerklärung offline in der ausgewählten
App-Sprache. Der Dialog ist scrollbar, schreibgeschützt und kopierbar.
Escape oder „Schließen“ führen zurück. Er wird erst nach der Positionierung
sichtbar, damit die anfängliche Korrektur der Fensterposition verborgen bleibt.
Die Erklärung beschreibt die tatsächlich gespeicherten Texte, Fotos und die
jeweiligen Projektfunktionen. Originalfotos werden nicht verändert.

Löschschaltflächen erhalten einen roten Fokusrahmen. Die übrigen Schaltflächen
behalten die jeweilige App-Farbe. Die Auswahlfläche der Tabelle bleibt sichtbar,
der zusätzliche Rahmen der fokussierten Tabellenzeile ist transparent.

Die sechs identischen Texte für Partner Center lassen sich aus den Ressourcen
in einen neuen Ausgabeordner exportieren:

```bash
./scripts/export_privacy.sh target/privacy-texts
```

Ein vorhandener Ausgabeordner wird nicht überschrieben. Die vorhandenen
Prüfbedingungen für Vorschau-Lieferungen bleiben bestehen. Der neue Datenschutz-
ordner wird erst mit einer erneut geprüften Lieferung erzeugt.
