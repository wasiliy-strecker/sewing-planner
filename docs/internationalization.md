Aktueller öffentlicher Name: **Sewing Planner**. Erstes Store-Release auf Deutsch.
Die App startet bei fehlender Spracheinstellung auf Englisch. Bestehende
Vorschauordner dokumentieren den damaligen Stand und wurden nicht neu erzeugt.

# Internationalisierung und Linux-Vorschau

NähPlan unterstützt de-DE, en-US, es-ES, fr-FR, it-IT und pt-BR. Der Umfang
bleibt Stufe 4 mit fünf Textfeldern, genau einem optionalen Foto und drei
Statuswerten und einer Checkliste. Produktname, App-Kennung, Datenpfade und SQLite-Schema bleiben
unverändert. Es gibt keine gemeinsame Runtime mit anderen Portfolio-Apps.

## Sprache und bestehende Daten

Die Schaltfläche „Sprache“ bietet die sechs Sprachen und die automatische Wahl
nach Systemsprache. `language.txt` liegt neben `naehplan.db`. Die Auswahl
wird beim nächsten App-Start wirksam. Nicht unterstützte Systemsprachen und
unbekannte gespeicherte Werte verwenden Englisch. Ein Lesefehler verwendet die
Systemsprache und zeigt einen übersetzten Hinweis. Ein Schreibfehler lässt den
Dialog samt Auswahl offen. Die Einstellungsdatei wird atomar ersetzt.

Die sechs UTF-8-Kataloge enthalten jeweils 110 Schlüssel. Eigene Dialoge,
Validierungen, Foto- und Speicherfehler, Mengenangaben und Text-Kontextmenüs
sind übersetzt. Die Sortierung folgt der gewählten Sprache. Die stabilen Werte
`planned`, `started` und `finished` bleiben unabhängig von ihren Anzeigenamen.
Namen, Notizen und Foto-BLOBs werden unverändert gespeichert. Es gibt keine
Datenmigration. Native Fotoauswahl-Titel und Dateifilter sind übersetzt.
Betriebssystem-Schaltflächen der Auswahl folgen den Systemeinstellungen.

Die bisherigen JavaFX-JUnit-Tests benötigen ein eigenes Xvfb-Display und
`-Dnaehplan.uiTests=true`. Die folgenden GUI-Helfer verwalten ihr Display selbst.

## Reproduzierbare GUI-Prüfung

Benötigt werden JDK 21, Python 3, Xvfb und `xdotool`. `SEWINGPLANNER_XVFB` kann auf
ein lokal entpacktes Xvfb zeigen. Jeder Helfer startet einen eigenen Bildschirm
über `-displayfd` mit 1920 × 1080 Pixeln. Der vorhandene Desktop wird nicht benutzt.
Temporäre Daten unter `target/gui-work/` sind ausdrücklich als synthetisch markiert.

```bash
export JAVA_HOME="$HOME/.local/share/naehplan/jdk-21.0.13+9"
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw spotless:apply
./mvnw verify -Dnaehplan.uiTests=true dependency:build-classpath -Dmdep.outputFile=target/gui-classpath.txt
python3 scripts/store_assets/run_gui.py --output target/gui-international
```

Gezielte Durchläufe sind mit `--locales fr-FR` und `--mode capture` oder
`--mode verify` möglich. Die Lieferung verlangt alle sechs Sprachen im Modus
`all`. Der Ausgabeordner muss neu sein.

Je Sprache startet `GuiJourney` die produktive `ProjectsApplication` in drei
neuen JVM-Prozessen. Maus- und Tastaturereignisse bedienen die echten Fenster.
JavaFX Robot nimmt den Bildschirm unverändert auf. Die native Dateiauswahl wird
mit X11-Eingaben bedient. Die Bilder enthalten keine nachgebaute Oberfläche.
Vier Aufnahmen je Sprache zeigen Übersicht, Projekteditor mit Foto und Status,
Suche und Sprachwahl. Fünf Projekte je Sprache und drei Illustrationen sind
synthetisch. Beim normalen App-Start werden keine Beispiele eingefügt.

Die Abnahme prüft Pflichtfelder, übersetzte Kontextmenüs, Unicode, lange Notizen,
sicheren Abbruch, PNG-Import, ungültige Bilder, JPEG-Ersatz, echte exklusive
SQLite-Sperren und erneutes Speichern. Statusänderungen gehören zum gemeinsamen
Entwurf. Reiner Statusabbruch und Wechsel zwischen allen drei Statuswerten werden
über echte Eingaben geprüft und anschließend in SQLite kontrolliert.
Zusätzlich werden leere Schritte, doppelte Texte mit unabhängigen UUIDs,
Positionslücken, reine Häkchenänderungen und Verwerfen geprüft. Eine echte
SQLite-Sperre erhält den gesamten Entwurf samt Schritten. Nach Neustart bleiben
Schritt-IDs, Texte, Häkchen und Positionen unverändert.
Fotoentfernung mit Verwerfen und Speichern sowie erneuter Import sind enthalten.
Kleine Haupt- und Dialogfenster, abgebrochenes Löschen, fehlgeschlagene
Spracheinstellung und erfolgreicher Sprachwechsel werden ebenfalls geprüft.
Ein neuer Prozess bestätigt die neue Sprache, Unicode, mehrzeilige Notiz, den
unveränderten Statuscode und die exakten Fotobytes. Japanisch als abweichende
Ausgangssystemsprache prüft den Vorrang der gespeicherten Wahl.

## Paketierter Launcher

```bash
./scripts/package.sh app-image
python3 scripts/store_assets/packaged_smoke.py --output target/packaged-international
```

Zusätzlich werden ImageMagick und `xsel` benötigt. Der Launcher startet außerhalb
des Projekts mit eingebetteter JVM, ohne `JAVA_HOME` und mit `PATH=/nonexistent`.
Eine synthetische Sammlung im ursprünglichen Schema 1 enthält 1234 Projekte.
Der Ablauf prüft französischen Start, Unicode-Bearbeitung, native JPEG-Auswahl,
Statusänderung, gemeinsame Speicherung, Sprachwechsel und englischen Neustart
nach Löschen der importierten Bildquelle. Screenshots zeigen Zahlengruppierung,
übersetzte Dialoge und gespeichertes Foto samt Status. Die tatsächlich geladene
JVM einschließlich `jdk.localedata` und die unveränderten Paketdateien werden über Pfad und Hashes nachgewiesen.

## Ausgabe als Ordner

Nach Sichtprüfung, Dokumentation und lokalem App-Commit ausführen:

```bash
python3 scripts/store_assets/assemble.py target/gui-international \
  build/releases/international/naehplan-linux-preview-YYYYMMDD-HHMM \
  --packaged-checks target/packaged-international
```

Der Helfer erstellt ausschließlich einen neuen Ordner. Er prüft Quellhashes,
alle 18 GUI-Prozesse, Bildabmessungen, Nachweishashes und geprüfte Paketlaufzeit.
Vorhandene Lieferungen werden nicht überschrieben. `Store/<Sprache>/text`
enthält direkt kopierbare Textdateien ohne Listenmarker, daneben liegen vier
Screenshots. `Branding` enthält Icons, `Verification` die Prüfnachweise.
Das Manifest nennt den tatsächlich geprüften lokalen Commit. `SHA256SUMS.txt`
enthält Hashes aller gelieferten Dateien. Quellcode, Texte, Werkzeuge und Assets
dürfen nach der Aufnahme nicht geändert werden. Dokumentation kann vor dem
Commit ergänzt werden. Es wird kein ZIP oder anderes Lieferarchiv erzeugt.

## Vektorgrafiken und Grenzen

Das eigene Icon zeigt Stoff, Nadel und ein Häkchen in der
bestehenden Mauve-Palette. Die drei eigenen Illustrationen zeigen Stofftasche,
Kissen und Schürze. Alle Quellen und Ableitungen bleiben app-lokal. Es gibt keine Stockfotos oder fremden Marken.

```bash
/usr/bin/python3 scripts/store_assets/branding.py
```

Das Rendern benötigt PyGObject, Cairo, librsvg und ImageMagick. Die App selbst
benötigt diese Werkzeuge nicht. Das 256-Pixel-PNG dient als Fenster- und
Linux-Paketicon. Der PowerShell-Helfer bindet ein mehrstufiges ICO ein.
Weitere PNGs haben 44, 50, 150, 300 und 1024 Pixel.

Die 24 Bilder sind echte Linux-Vorschauen. Windows-Paketierung, Installation,
Updateprüfung, Skalierung, native Dialoge und endgültige Windows-Screenshots
bleiben offen. macOS-Abnahme, Store-Identitäten, Signierung und Einreichung
gehören zu späteren Releaseaufträgen. Diese Lieferung ist keine Store-Abnahme.

## Datenschutzübernahme vom 31. Oktober 2026

Die Info-Schaltfläche, Dialogtexte und Datenschutzerklärung stehen in allen
sechs Sprachkatalogen. App-Dialog und Datenschutztextexport verwenden dieselben
Ressourcen. `assemble.py` ergänzt nach den vorhandenen Prüfungen den Ordner
`Privacy` mit sechs UTF-8-Dateien. Frühere Vorschauordner behalten ihren damaligen
Quellstand. Für die aktuelle UI wurden auf Nutzerwunsch keine neuen Aufnahmen
oder GUI-Prüfungen erzeugt. Neue Bilder und native Abnahme bleiben offen.
