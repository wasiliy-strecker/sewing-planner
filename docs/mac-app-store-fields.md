# Sewing Planner: deutsche Mac-Store-Inhalte

Vorbereitet für **Sewing Planner**, Store-Version **1.0**, Primärsprache Deutsch.
Vorlage ist die Inhaltslieferung von Knitting Projects. Die Lieferung enthält
vier Bildentwürfe, direkt kopierbare Feldtexte, synthetische Beispieldaten und
die kanonische englische Datenschutzerklärung. Die App wurde dafür nicht gebaut.

Die vorgesehene neue englische Bundle-ID ist `de.appfabrik.sewingplanner`.
Eine Registrierung dieser Kennung wurde nicht geprüft oder vorgenommen.
`bundle-id.txt` dokumentiert die vorgesehene Kennung, keinen bestätigten
App-Store-Connect-Eintrag. Bestehende Registrierungen nicht ungeprüft ersetzen.

## Feldtexte

Die Dateien unter `Store/de-DE` im Übergabeordner enthalten ausschließlich den
kopierbaren Inhalt. Im Repository liegen sie unter
`store-listings/mac-app-store/de-DE`. Der Produktname bleibt in jeder Sprache
Sewing Planner. Die Store-Sprache ändert die App-Standardsprache nicht.

| Feld | Datei |
| --- | --- |
| Name | `name.txt` |
| Untertitel | `subtitle.txt` |
| Werbetext | `promotional-text.txt` |
| Beschreibung | `description.txt` |
| Schlüsselwörter | `keywords.txt` |
| Support-URL | `support-url.txt` |
| Marketing-URL | `marketing-url.txt` |
| Copyright | `copyright.txt` |
| Geplante Store-Version | `version.txt` |
| Anmerkungen zur App-Prüfung | `review-notes.txt` |
| Vorbereitete Erstversionsnotiz | `release-notes.txt` |

Die Texte beschreiben Nähprojekte mit einem optionalen Foto, Farbe, Material,
Aufbewahrungsort, Notiz, drei Statuswerten und eigenen Checklisten. Änderungen
an Schritten und Häkchen werden gemeinsam mit dem Projekt gespeichert.
Die Checkliste ändert den Status nicht automatisch. Die Suche umfasst die fünf
Projekttextfelder und die Arbeitsschritte. Der JSON-Transfer erhält Fotos,
Status und sämtliche Schritte.

Die App braucht kein Konto und keine Anmeldung. Für die App-Prüfung ist deshalb
kein Demo-Zugang nötig. Die erreichbaren Prüferkontaktdaten im Konto ergänzen.
Kategorie Produktivität ist vorgesehen. Game Center wird nicht verwendet.
Die Entscheidung zur Veröffentlichung erfolgt später in App Store Connect.
Dieser Auftrag nimmt dort keine Änderungen vor.

Stand 3. November 2026: Die aktuellen Store-Dateien verwenden die englische
GitHub-Datenschutzerklärung. Die Beschreibung unterscheidet die offline
nutzbare Projektverwaltung vom Browseraufruf. Für das Portal die Texte aus
der neuen Übergabe verwenden. Frühere Lieferungen bleiben unverändert.
„Anmeldung erforderlich“ ausschalten und eigene erreichbare Prüferkontakte
eintragen. Nach der Eingabe auf der Versionsseite „Sichern“ wählen.
Den Build nach dem Mac-Bau und Upload zuordnen. Die optionalen Sandbox-Angaben
mit dem tatsächlich signierten Paket abgleichen. Auf „App-Datenschutz“ die
`privacy-url.txt` eintragen, die Datenerfassung für die beschriebene lokale
Anwendung beantworten und die Datenschutzangaben veröffentlichen.
Inhaltsrechte, Altersfreigabe, Preis und Länder im Portal ergänzen.

Für die erste Version wird die vorbereitete Erstversionsnotiz nicht als
„Neu in dieser Version“ benötigt. Keine zusätzlichen Features, Cloud-Dienste,
PDF-Funktionen oder technische Mac-Freigabe aus den Texten ableiten.

## Vier Bildentwürfe

Die Dateien unter `Screenshot-Drafts-Linux/de-DE` sind echte Aufnahmen der
vorhandenen JavaFX-App mit den enthaltenen synthetischen Beispieldaten.
Sie haben 2560 × 1600 Pixel, RGB und keine Transparenz. Die Oberfläche wurde
weder nachgebaut noch mit nachgeahmter macOS-Fensterdekoration versehen.

| Datei | Ansicht | Bildtext |
| --- | --- | --- |
| `01-overview.png` | Fünf Nähprojekte, drei Statuswerte, Foto und gespeicherte Checkliste | Deine Nähprojekte mit Foto und Status im Überblick |
| `02-edit-checklist.png` | Projekteditor mit offenen und erledigten Arbeitsschritten | Arbeitsschritte festhalten und Erledigtes abhaken |
| `03-search.png` | Aktive Suche nach Innentasche mit einem passenden Projekt | Nähideen und Projektnotizen schnell wiederfinden |
| `04-json-export.png` | Erfolgreicher Export der gesamten Sammlung mit Bestätigung | Projekte mit Fotos und Checklisten als JSON sichern |

`captions.txt` enthält diese vier redaktionellen Bildtexte. Sie sind keine
separaten Pflichtfelder in App Store Connect. Ein Vorschauvideo wurde nicht erstellt.

Die Aufnahmen stammen von Linux und verwenden die eigene JavaFX-Oberfläche.
Vor der Einreichung mit dem tatsächlich laufenden Mac-Build vergleichen und
abweichende Ansichten auf dem Mac erneut aufnehmen. Dafür einen separaten
Testbenutzer und `Demo/sewing-planner-de-DE.json` verwenden.
`Screenshots-Final-Mac/de-DE` bleibt bis zum Vergleich leer. Die vorhandenen
Texte, Illustrationen und Ansichten bleiben nutzbar. Die Bildentwürfe belegen
keine native Mac-Funktionsprüfung.

## Datenschutz und technische Vorbereitung

Für die lokale Anwendung ist weiterhin „Es werden keine Daten erfasst“ vorgesehen.
Die App besitzt keine Konten, Werbung, Telemetrie oder eigene Cloud-Synchronisation.
Der ausdrücklich angeforderte Browseraufruf überträgt keine Sammlung.

Datenschutz-URL für alle Store-Sprachen:

```text
https://github.com/wasiliy-strecker/sewing-planner/blob/main/PRIVACY.md
```

Die vollständige englische Erklärung steht in `PRIVACY.md`. Neue Übergaben
enthalten eine unveränderte Kopie und `privacy-url.txt` unter `Privacy`.
Vor der Einreichung die URL ohne Anmeldung öffnen und den App-Namen prüfen.
Die optionale URL für Datenschutzoptionen bleibt leer.

Händlerstatus, Inhaltsrechte und Vertragszustimmungen sind persönliche rechtliche
Erklärungen. Diese Vorbereitung trifft oder ändert sie nicht automatisch.

Die aktuelle Paketierung verwendet noch die historische Kennung `de.appfabrik.naehplan`.
Ihre Ausrichtung auf die gewählte englische Store-Kennung gehört zum späteren
technischen Releaseauftrag. Diese Inhaltslieferung ändert keine Kennungen,
Datenpfade, App-Versionen oder Installationen und enthält keine Mac-App,
PKG-Datei, Signierungshelfer oder Uploads.

## Ohne App-Build erneut erzeugen

Benötigt werden die bereits geprüfte `target/naehplan-0.1.0.jar` mit ihrem
unveränderten `target/classes`, die Abhängigkeiten unter
`target/app-image/Sewing Planner/lib/app`, die JavaFX-Module unter
`target/javafx-modules`, JDK 21, Python mit Pillow, Xvfb und xdotool.
Die ältere App-JAR im App-Image wird nicht verwendet oder ersetzt. Fehlende
oder veraltete Artefakte führen zum Abbruch, nicht zu einem automatischen Build.
`SEWINGPLANNER_XVFB` kann auf ein vorhandenes Xvfb zeigen.

```bash
export JAVA_HOME="$HOME/.local/share/naehplan/jdk-21.0.13+9"
python3 scripts/mac_store_content.py check --check-links
python3 scripts/mac_store_content.py capture --output target/mac-store-content-drafts-neu
```

Der Helfer startet einen eigenen X-Bildschirm und eine temporäre Sammlung.
Der Java-Quelltextmodus führt ausschließlich den separaten Aufnahmehelfer aus.
Die vorhandene Anwendung und ihre JARs werden nicht kompiliert oder verändert.
Es gibt keinen Maven-Aufruf, keine Paketierung und keine vollständige Testsuite.
Fehlen die benötigten Artefakte, beendet sich der Helfer ohne automatischen Build.

Die gezielten Inhaltsprüfungen bestätigen synthetische Projekte, Fotobytes,
Farben, Status, Schritte, Häkchen und einen wiederholten Import ohne Duplikate.
Bild 4 zeigt einen echten Export über die Oberfläche. Eine aktive Suche beim
Export darf die gesicherten Projekte nicht einschränken.

Nach Sichtprüfung und lokalem Commit die Übergabe erstellen:

```bash
python3 scripts/mac_store_content.py assemble --captures target/mac-store-content-drafts-neu --output build/releases/mac-store-content/Sewing-Planner-1.0-de-DE-neu
```

Beide Ausgabeordner müssen neu sein. Es werden nur ausgewählte Store-Dateien
übernommen. Persönliche Exporte werden nicht eingelesen. Die Übergabe prüft
Feldgrenzen, Bildmaße, Quellstand, unveränderte vorhandene JARs und Dateihashes.
`SHA256SUMS.txt` erfasst die gelieferten Dateien. Es wird kein ZIP erzeugt.

## Quellen

[Apple: Versionsfelder](https://developer.apple.com/help/app-store-connect/reference/app-information/platform-version-information/)

[Apple: Screenshot-Größen](https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/)

[Apple: App-Informationen](https://developer.apple.com/help/app-store-connect/reference/app-information/app-information/)
