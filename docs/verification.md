# Prüfbericht

## Mac-Beenden und Exportzugriff, 1. November 2026

Cmd+Q kann für Hauptfenster und Editor mehrere Schließanfragen auslösen.
Diese werden vor der Rückfrage gebündelt. „Weiterbearbeiten“ erhält den gesamten
Entwurf und verwirft auch den ausstehenden Beenden-Wunsch. „Verwerfen“ schließt
Editor und Hauptfenster. Ein unveränderter Editor benötigt keine Rückfrage.
Das alleinige Schließen des Editors lässt das Hauptfenster geöffnet. Laufendes
Speichern wird nicht unterbrochen. Implementierung und Tests bleiben app-lokal.

Beim JSON-Export auf macOS zuerst einen Zielordner im nativen Dialog wählen,
danach einen Dateinamen eingeben. Die Ordnerfreigabe umfasst auch temporäre
Dateien für den atomaren Export. Der Ablauf gilt für jeden Mac-Start und benötigt
keinen besonderen Store-Schalter. Unter Windows und Linux bleibt die bisherige
Dateiauswahl erhalten. Pfadbestandteile, Kontrollzeichen und zu lange Dateinamen
werden abgelehnt. Abbruch erzeugt keine Datei. Die neuen Texte sind in allen
sechs Sprachen vorhanden. Bestehende Dateien behalten den bisherigen Schutz.

Grundlage ist Apples dokumentierte Freigabe eines ausgewählten Ordners samt
seinem Inhalt: [Dateizugriff in der App Sandbox](https://developer.apple.com/documentation/security/accessing-files-from-the-macos-app-sandbox).

Der aktuelle Nutzerauftrag umfasst ausdrücklich die zuvor gefundenen Fehler.
Die frühere Testzurückstellung einzelner Aufgaben gilt nicht für diese Korrektur.
Mit Eclipse Temurin 21.0.13+9 auf einem eigenen Xvfb-/Openbox-Bildschirm ausgeführt:

```bash
./mvnw --batch-mode --no-transfer-progress spotless:apply verify -Dnaehplan.uiTests=true
```

196 Tests bestanden. Keine Fehler und keine übersprungenen Tests.
Compiler-Lint, Enforcer, Spotless und SpotBugs bestanden. Protokoll:
`target/mac-workflows-verify.log`. Sechs neue Beenden-Regressionen rufen den
JavaFX-Glass-Handler auf, den macOS für Cmd+Q verwendet. Sie prüfen Verwerfen,
Weiterbearbeiten, wiederholte Anfragen, unveränderte Entwürfe, das einzelne
Editorfenster und einen gezielt verzögerten Speichervorgang.

Weitere Regressionen prüfen den Dateinamendialog in sechs Sprachen, Abbruch,
Korrektur ungültiger Namen, Unicode und die Bindung an den ausgewählten Ordner.
Die bestehenden Sammlungstransfer- und Dateifehlerprüfungen bestehen weiterhin.

Manuelle Mac-Abnahme bleibt erforderlich: Cmd+Q mit offenem Entwurf abbrechen
und bestätigen. Während eines Speichervorgangs erneut versuchen. Beim Export
Ordner und Dateiname auswählen, beide Abbruchwege prüfen und das Ergebnis
außerhalb des App-Containers öffnen. Bei Store-Ausführung die Ordnerfreigabe
mit einem frischen App-Start überprüfen. Linux-Prüfungen sind keine native
Mac- oder Sandbox-Abnahme. Keine Store-Pakete, Veröffentlichung oder Änderung
persönlicher Sammlungen. Bereits laufende Benutzerprozesse wurden nicht beendet.

## Deutsche Mac-Store-Inhalte ohne App-Build, 31. Oktober 2026

Nach dem Vorbild von Knitting Projects 13 deutsche Felddateien und vier echte
Aufnahmen der vorhandenen App vorbereitet. Die Version 1.0 ist der geplante
Store-Eintrag. Die laufende App-Version und die Paketierung bleiben unverändert.
Die englische Apple-Kennung de.appfabrik.sewingplanner ist vorgesehen, ihre
Registrierung wurde nicht geprüft.

Fünf erfundene Nähprojekte enthalten alle drei Statuswerte, fünf Farbcodes,
drei vorhandene eigene Illustrationen und 15 Schritte mit sechs erledigten
Häkchen. Import und erneuter Import erhalten den vollständigen Bestand.
Der echte UI-Export bei aktiver Suche enthält alle fünf Projekte einschließlich
unveränderter Texte, Fotos, Statuswerte, Schritt-IDs, Positionen und Häkchen.
Die gezielten Prüfungen dienen der Asset-Erstellung, nicht einer Release-Abnahme.

Aufnahmen auf eigenem Xvfb in 2560 × 1600 RGB. Übersicht, Checklisten-Editor,
aktive Suche und erfolgreicher Export wurden visuell geprüft. Die bestehenden
App-JARs und JavaFX-Module werden gelesen und vor sowie nach der Aufnahme
per SHA256 verglichen. Kein App-Build, Maven-Aufruf oder Paketbau ausgeführt.
Der separate Java-Aufnahmehelfer wurde im Quelltextmodus ausgeführt.
Google Java Format, Ruff, Python-Syntax, Feldgrenzen und Dateihashes geprüft.
Beide öffentlichen Links antworten mit HTTP 200. Keine komplette Testsuite.

Aufnahme und Nachweise liegen unter
`target/mac-store-content-drafts-20261031-2327/`.
`scripts/mac_store_content.py` liefert ausschließlich die ausgewählten Assets
mit Quellcommit, Prüfnachweisen und SHA256-Inventar. Anleitung:
`docs/mac-app-store-fields.md`. Linux-Bildentwürfe, keine finalen Mac-Screenshots.
Keine persönlichen Daten als Beispiele, kein Upload, kein Push und keine ZIP-Datei.

## Vollständiger Custom-Color-Dialog, 31. Oktober 2026

Der native JavaFX-Farbdialog verwendet jetzt 50 × 34 em bevorzugte Inhaltsgröße,
bei der App-Schrift 700 × 476 Pixel. Der rechte Bedienbereich erhält mindestens
26 em für vollständige übersetzte Beschriftungen. Das Fenster ist vergrößerbar.
Die Mindestgröße berücksichtigt nach dem Layout die tatsächliche Titelleiste
und den Fensterrahmen. Der Dialog wird über dem Editor innerhalb des Bildschirms
positioniert und erst anschließend eingeblendet. Die Umsetzung verwendet
öffentliche JavaFX-APIs und bleibt vollständig app-lokal.

Geprüft mit `./mvnw --batch-mode --no-transfer-progress spotless:apply verify`.
165 gemeldete Tests, davon 11 übersprungen, keine Fehler. Compiler-Lint,
Enforcer, Spotless und SpotBugs bestanden. Protokoll:
`target/color-dialog-verify.log`.
Die standardmäßig deaktivierten älteren GUI-Suiten wurden hierbei nicht erneut aktiviert.

Ein separater temporärer GUI-Prüfhelfer verwendet das echte Farbfeld und den
nativen Dialog ohne Sammlung oder Datenbank. Auf einem eigenen Xvfb mit Openbox
bei 800 × 600 Pixeln wurden en-US, de-DE, es-ES, fr-FR, it-IT und pt-BR geprüft.
Alle 272 Prüfbedingungen je Sprache bestanden. HSB, RGB und Web, vollständig
sichtbare Schaltflächen und Beschriftungen, Verkleinerung auf die Mindestgröße,
Speichern, Verwenden, Abbrechen, Escape, Fensterschließen und erneutes Öffnen.
Protokoll: `target/color-dialog-gui.log`. Synthetische Aufnahmen normaler und
minimaler Fenstergröße: `target/custom-color-review/`. Repräsentative Aufnahmen
wurden zusätzlich visuell geprüft.

Manuelle Wiederholungsprüfung: Ein Farbfeld öffnen, „Custom Color“ wählen und
alle drei Register durchgehen. Die unteren Aktionen müssen vollständig sichtbar
sein. Die Fenstergröße bis zur Mindestgröße ändern und erneut prüfen. Eine
andere Farbe wählen und jeweils Verwenden, Speichern sowie die Abbruchwege
testen. Der Farbfeldwert darf sich nur beim Übernehmen ändern. Den Dialog
zwischen den Schritten erneut öffnen.

Das vorhandene lokale Linux-App-Image mit dem geprüften App-JAR aktualisiert.
JAR-Hashes stimmen überein. Der eingebettete Launcher meldet
`Sewing Planner 0.1.0`. Native Windows- und macOS-Abnahmen sind separat.


## Checklisten und nachgeholte Regressionstests, 31. Oktober 2026

Der neue Nutzerauftrag umfasst die Korrektur der Prüfbefunde und die Übertragung
auf die vierte und fünfte Java-App. Damit wird auch die zuvor zurückgestellte
Funktionsprüfung durchgeführt. Nur synthetische Daten und eigene virtuelle
X11-Bildschirme wurden verwendet.

Der Editorinhalt und die gespeicherte Checkliste halten rechts 16 Pixel Abstand
zum Scrollbalken. Der Erledigt-Zähler ist in allen sechs Sprachen vollständig.
Enter im Schritttext wechselt zum nächsten Schritt oder ergänzt am Ende eine
leere Zeile, ohne das Projekt zu speichern. Leeres Enter ergänzt keine weitere
Zeile. Entfernen erhält einen sinnvollen Fokus, fokussierte Eingaben scrollen
bei Bedarf in den sichtbaren Bereich.

Die höchste gespeicherte Schrittposition löst beim Hinzufügen keinen Überlauf
mehr aus. Die Schaltfläche bleibt mit übersetzter Erklärung deaktiviert.
Dasselbe gilt bei 10.000 Schritten. Bestehende IDs, Positionen, Texte und
Häkchen bleiben erhalten. Die Screenreader-Beschriftung beschreibt in jeder
Sprache das richtige Projektfoto.

Tatsächlich bestanden:

- Vollständiges `./mvnw --batch-mode --no-transfer-progress verify -Dnaehplan.uiTests=true`
  mit 179 Tests, ohne Fehler oder übersprungene Tests. Compiler-Lint,
  Enforcer, Spotless und SpotBugs ohne Befunde. Protokoll:
  `target/checklist-verify.log`.
- Die 25 JavaFX-Prüfungen nach Präzisierung der asynchronen Fenstergrößenprüfung
  erneut bestanden. Protokoll `target/checklist-ui-final.log`. Sie prüfen unter
  anderem sechs Sprachen, importierte Maximalpositionen, Bearbeiten und Speichern,
  Enter, Fokus, Verwerfschutz, Scrollabstand und fünf Spalten mit je sechs echten
  Mausklicks. Suche erhält Sortierung und die Zuordnung zum richtigen Editor.
- JSON-Rückimport mit kollidierenden Schritt-IDs, gleichen Projektinhalten,
  verschiedenen Statuswerten und Häkchen. Mehrfachimport ergänzt keine weiteren
  Kopien. Späte JSON- und SQLite-Fehler hinterlassen keine Teilimporte.
  10.000 Schritte und die höchste Position bleiben beim Rückimport unverändert.
- Fotooptimierung einschließlich aller acht EXIF-Ausrichtungen für JPEG und PNG,
  Transparenz, unveränderter Quelldateien und gespeicherter Fotobytes.
  Der gemerkte Bilderordner überlebt einen Neustart und behandelt Fehler.
- Synthetische Aufnahmen in `target/checklist-review/` erzeugt. Der verkleinerte
  Dialog wurde zusätzlich visuell geprüft. Die sechs Sprachen wurden geometrisch
  auf Abstand und Sichtbarkeit des fokussierten Schritts geprüft.

Veraltete Test-Konstruktoraufrufe wurden an die aktuellen Abhängigkeiten angepasst.

Das vorhandene Linux-App-Image wurde mit dem geprüften App-JAR und der Anleitung
aktualisiert. JAR-Hashes stimmen überein, der eingebettete Launcher meldet
`Sewing Planner 0.1.0`. Laufzeit und Abhängigkeiten wurden nicht ersetzt.
Die frühere Sewing-Planner-Instanz wurde regulär geschlossen und die aktualisierte
App für den Nutzer geöffnet. Persönliche Sammlungen wurden nicht als Testdaten
verwendet. Keine Store-Pakete, kein Upload und kein Push. Native Windows- und
macOS-Funktionsabnahmen bleiben separat.


## Sortierbare Projektliste, 31. Oktober 2026

Die vollständige Sortierfunktion aus Knitting Projects bis Commit `ca8d75e`
wurde app-lokal übernommen. Alle fünf Spalten besitzen dauerhafte Pfeilpaare
und alphabetische Sortierung in der App-Sprache ohne Groß-/Kleinschreibung.
Die aktive Richtung ist hervorgehoben. Klicks wechseln zwischen A–Z und Z–A,
ohne den nativen dritten Zustand ohne Sortierung beizubehalten. Die Reihenfolge
Projektname, Status, Material, Farbe, Aufbewahrungsort ist gegen Verschieben
gesperrt. Pfeile und Hoverfarbe passen zur bestehenden App-Farbe.

Initial wird nach Projektname aufsteigend sortiert. Nach Suche, Speichern,
Löschen und Import wird die aktive Sortierung erneut angewendet, bevor eine
weiterhin sichtbare Auswahl anhand der Projekt-ID wiederhergestellt wird.
Notiz, Foto und Checkliste bleiben an das ausgewählte Projekt gebunden.

Manuelle Abnahme durch den Nutzer, noch nicht ausgeführt:

- Jede Überschrift mindestens sechsmal anklicken. A–Z und Z–A wechseln,
  alle Pfeilpaare bleiben sichtbar und die hervorgehobene Richtung stimmt.
- Zwischen allen fünf Spalten wechseln. Status alphabetisch nach dem
  angezeigten Text und Farbe nach Farbcode beziehungsweise Farbtext prüfen.
- Überschriften seitlich ziehen. Keine Spalte darf ihre Position wechseln.
- Ein Projekt auswählen, umsortieren und Notiz, Foto sowie Checkliste prüfen.
  Bearbeiten muss das ausgewählte Projekt mit seinen gespeicherten Schritten öffnen.
- Absteigend sortieren, suchen, null Treffer erzeugen und die Suche leeren.
  Die Sortierung muss auch nach Hinzufügen, Bearbeiten, Löschen und Import gelten.
- Leere Felder, gleiche Werte, gemischte Großschreibung, Umlaute, Akzente und
  schmale Fenster prüfen.

Gemäß app-lokaler Vorgabe keine Test- oder GUI-Suite. Formatter, Compiler-Lint,
Enforcer, Paketbau und SpotBugs bestanden mit
`./mvnw --batch-mode --no-transfer-progress spotless:apply package spotbugs:check -Dmaven.test.skip=true`.
Protokoll: `target/column-sorting-build.log`. Die manuelle Bedienprüfung bleibt offen.
Das lokale Linux-App-Image wurde mit App-JAR und Anleitung aktualisiert und
für den Nutzer geöffnet. Laufzeit, Abhängigkeiten und Datenformat bleiben gleich.

## Übertragung des Yarn-Box-Stands, 31. Oktober 2026

Übernommen wurden die Verbesserungen dieses Chats und die Nachkorrekturen
bis Yarn-Box-Commit `7fc3363`: Dialogzentrierung, Editor ohne Tabellenblinken,
Farbauswahl mit lesbaren Farbcodes und Alttext-Erhalt, Fotooptimierung,
gemerkter Fotoordner, ergänzender JSON-Transfer, Sicherungsgrenzen,
entfernte Neuladen-Schaltfläche, englischer Produktname und englischer Erststart.
Status und Checklistendaten werden im app-eigenen JSON-Format gesichert.

Auf ausdrücklichen Nutzerwunsch keine Tests und keine automatisierten
GUI-Abläufe. Formatter, Compiler-Lint, Enforcer, Paketbau und SpotBugs bestanden mit
`./mvnw --no-transfer-progress spotless:apply package spotbugs:check -Dmaven.test.skip=true`.
Protokoll: `target/yarn-parity-build.log`. Das Linux-App-Image wurde
mit gebündelter Laufzeit und den benötigten JSON-Bibliotheken erstellt.
Paketprotokoll: `target/yarn-parity-package.log`. Native Windows- und macOS-Abnahme bleiben offen.
Vorhandene Testquellen und Vorschauhelfer sind nur angepasst, nicht ausgeführt.
Die nachfolgenden Ergebnisse beschreiben frühere Stände.

Manuelle Abnahme durch den Nutzer, in diesem Auftrag nicht ausgeführt:

- Hinzufügen und Bearbeiten auf Zentrierung und fehlendes Tabellenblinken prüfen.
- Farbe wählen, speichern, in der Liste ansehen und wieder entfernen. Alttexte
  bei einer reinen Notizänderung erhalten. Ausgewählte Zeilen müssen lesbar sein.
- Großes Foto, kleines Foto, transparentes PNG und gedrehtes Handyfoto auswählen.
  Originaldateien bleiben gleich, neue gespeicherte Kopien haben höchstens
  1600 Pixel Kantenlänge. Abbrechen oder Fehler erhalten den Entwurf.
- Die Fotoauswahl in einem anderen Eintrag und nach Neustart erneut öffnen.
  Der bestätigte Ordner soll erhalten bleiben, Abbrechen darf ihn nicht ändern.
- Ganze Sammlung bei aktivem Filter exportieren. Status und Checklisten
  kontrollieren, wieder importieren und mehrfachen Import ohne zusätzliche
  Duplikate durchführen. Ungültige Dateien dürfen keine Teilimporte hinterlassen.
- Einträge mit gleichen Texten und Fotos, aber anderem Status oder anderen
  Checklisten, einschließlich leerer Listen, getrennt erhalten.
- Eine Sicherung mit bereits vergebenen Schrittkennungen ergänzen. Bestehende
  Projekte und ihre Schritte dürfen sich nicht ändern. Mehrfachimport bleibt stabil.
- Überschrittene Text-, Eintrags-, Foto- und Dateigrenzen müssen eine Erklärung
  liefern. Bereits vorhandene größere Bestände dürfen nicht gekürzt werden.
- Erststart ohne Spracheinstellung bei deutscher Systemsprache soll Englisch
  verwenden. Gespeicherte Sprache und ausdrücklich gewähltes Automatisch erhalten.


## App-Icons auf den drei Desktop-Plattformen, 31. Oktober 2026

Vorhandene Motive in zusätzliche Fenstergrößen, Windows-ICO und macOS-ICNS
überführt. Fenster und Startfehler verwenden die App-Icons. Linux erhält einen
benutzerspezifischen Anwendungseintrag mit passender JavaFX-Fensterklasse und
Icons. Windows- und macOS-Pakethelfer verknüpfen die jeweiligen Icon-Dateien
explizit mit Launcher/Bundle und Installer.

Tatsächlich ausgeführt: Icon-Erzeugung mit Pillow, Formatter und Java-Paketbau
mit `-Dmaven.test.skip=true`. Protokoll: `target/desktop-icons-build.log`.
Vorhandene Linux-App-Images mit App-JAR, Icons, Desktop-Installer und Anleitung
aktualisiert. Die Linux-Desktop-Registrierung wurde für das aktuelle Benutzerkonto
ausgeführt. Keine vollständigen nativen Installer neu gebaut.

Die Anwendung wurde in diesem Auftrag nicht gestartet. Der neue Menüeintrag
verwendet den bisherigen Standarddatenpfad. Visuelle Abnahme bleibt offen.

Auf Nutzerwunsch keine Tests, kein `verify` und keine automatisierten GUI-Prüfungen.
Manuelle Abnahme: App über die Ubuntu-Anwendungsübersicht starten, eigenes Icon
im Dock und beim Fensterwechsel ansehen, Dialoge öffnen und nach einem Neustart
die Zuordnung kontrollieren. Windows separat in Explorer, Startmenü und Taskleiste
prüfen, macOS separat im Finder, Dock und App-Umschalter. Native Windows-/macOS-
Pakete und Signierungen wurden hier nicht erzeugt oder geprüft.

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
