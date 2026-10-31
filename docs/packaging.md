# Paketierung und spätere Stores

## Voraussetzungen

Sichtbarer Produktname: NähPlan. Die Launcher- und Paketnamen verwenden
`NaehPlan` beziehungsweise `naehplan`. App-Kennung: `de.appfabrik.naehplan`.

Zuerst `./mvnw verify` beziehungsweise `.\mvnw.cmd verify` erfolgreich ausführen.
Paketierung kompiliert den aktuellen Stand, wiederholt jedoch nicht die Tests.
Ein vollständiges Eclipse Temurin JDK 21.0.13+9 mit `javac`, `jlink` und `jpackage` ist erforderlich.
JavaFX-Bibliotheken und JDK müssen zur Zielarchitektur passen.

Das App-Image enthält die App, SQLite JDBC und eine erzeugte Java-Laufzeit.
JavaFX wird als Modul in die Laufzeit eingebunden. Die eigene App bleibt ein
gewöhnliches Java-Projekt ohne `module-info.java`. Der Anwender benötigt kein
separates Java. Die vollständigen Runtime-Verzeichnisse müssen zusammenbleiben. `java.desktop`
ist über `java.se` enthalten und stellt die ImageIO-Decoder bereit.

## Lokales Linux-App-Image

```bash
./scripts/package.sh app-image
./target/app-image/NaehPlan/bin/NaehPlan
```

Linux dient hier als Entwicklungs- und Prüfplattform. Ein App-Image ist ein
startbares Verzeichnis mit Laufzeit. Ein Linux-Store-Release ist nicht geplant.

## Windows x64

Auf Windows mit einem x64-Temurin-JDK 21.0.13+9 und PowerShell ausführen. Für die EXE-Erzeugung
wird WiX Toolset 3.14.1 benötigt, dessen `bin`-Verzeichnis im `PATH` liegt.

```powershell
.\mvnw.cmd verify
.\scripts\package.ps1 -PackageType app-image
.\target\app-image\NaehPlan\NaehPlan.exe
.\scripts\package.ps1 -PackageType exe
```

Die EXE liegt unter `target/dist/naehplan-0.1.0-windows-x64.exe`.
Die Installation ist pro Benutzer vorgesehen. Die eigene Upgrade-ID `777707fe-ad17-42ce-bc60-95ac92520de7` ermöglicht
spätere Installer-Updates mit derselben Produktidentität. Die Sammlung liegt
außerhalb des Installationsverzeichnisses.

## macOS Intel und Apple Silicon

Auf der jeweiligen Mac-Architektur mit passendem Temurin-JDK 21.0.13+9 ausführen:

```bash
./mvnw verify
./scripts/package.sh app-image
open target/app-image/NaehPlan.app
./scripts/package.sh dmg
```

Das Ergebnis heißt `target/dist/naehplan-0.1.0-macos-x64.dmg` oder
`target/dist/naehplan-0.1.0-macos-arm64.dmg`.
Die Bundle-Kennung ist `de.appfabrik.naehplan`.
Die aktuellen Pakete haben keine Distributionssignatur und sind als lokale
Testpakete vorbereitet. Plattform-Sicherheitsdialoge sind bei der späteren
nativen Abnahme zu berücksichtigen.

## Nachweis und CI

Die Skripte starten den erzeugten Launcher mit `--version`. Unter Windows
wird dessen Exitcode geprüft, weil ein GUI-Launcher keine verlässliche
Konsolenausgabe liefert. Danach muss auf jeder Plattform der vollständige
sichtbare Ablauf aus `verification.md` folgen, einschließlich Datenbankzugriff, Fotoimport, Bildanzeige, Statuswechsel, Checklistenänderungen
und erneutem Start. Eine Launcher-Prüfung allein belegt keine funktionierende GUI.

Vorhandene versionierte EXE- und DMG-Dateien werden nicht still überschrieben.
Die generierten Zwischenverzeichnisse unter `target` werden neu aufgebaut.

Der GitHub-Workflow `Verify` prüft Java, SQLite, Bildimport, Status und Checklisten auf Linux, Windows und macOS.
`Build native test packages` wird manuell gestartet und erzeugt Windows-x64-,
macOS-x64- und macOS-arm64-Testartefakte. Er erstellt keine GitHub-Releases
und lädt nichts in einen Store. Auf Linux führt `Verify` auch die JavaFX-Dialogtests unter Xvfb aus.
Die Workflows sind vorbereitet, aber noch
nicht auf GitHub ausgeführt. Eine Remote-Adresse ist noch nicht gesetzt.

## Offene Store-Schritte

Windows und macOS sind lokal noch nicht nativ geprüft. Vor einer Veröffentlichung
sind Installation, Start, Schrift, Tastenkürzel, Foto-, Status- und Checklistenspeicherung und Updates auf
Zielsystemen abzunehmen. Eine EXE oder DMG allein belegt keine Store-Eignung.

Für den Microsoft Store ist MSIX vorgesehen, für den kostenlosen GitHub-Download
bleibt die EXE aus demselben Anwendungscode vorgesehen. Die MSIX-Paketierung
ist noch nicht implementiert. Reale Partner-Center-Identität, Publisher,
Signierungsnachweise, Windows-Installation, Updates, WACK, finale Windows-Bilder
und Store-Angaben bleiben offen. Es erfolgt keine automatische Migration
zwischen den beiden Installationswegen. MSIX-Datenpfade und Deinstallation
müssen mit synthetischen Daten separat geprüft werden.

Für den Mac App Store bleiben Apple-Entwickleridentität, App-Sandbox,
Entitlements für Java und native Bibliotheken, Signierung des vollständigen
Bundles und ein geeignetes Einreichungspaket offen. Insbesondere sind der
containerbezogene Datenpfad, die Dateiauswahl mit kurzzeitigem Lesezugriff
auf externe Fotos und das Laden der nativen SQLite-Bibliothek in der
Sandbox auf einem Mac zu prüfen. Eine notarialisierte Direktdistribution ist
ein eigener Weg und ersetzt keine Mac-App-Store-Abnahme.

Quellen für die später erneut zu prüfenden Vorgaben:

- [JDK 21 Packaging Overview](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html)
- [Apple: Distribution und App Sandbox](https://help.apple.com/xcode/mac/current/en.lproj/dev91fe7130a.html)
- [Microsoft: Store-Überblick](https://learn.microsoft.com/en-us/windows/apps/publish/)
- [GitHub: Verfügbare Runner](https://docs.github.com/en/actions/how-tos/write-workflows/choose-where-workflows-run/choose-the-runner-for-a-job)

## Internationale Vorschau

`jpackage` bündelt `jdk.localedata` für alle sechs Sprachen. Die native
Paketbezeichnung bleibt `NaehPlan`, der sichtbare Produktname `NähPlan`.
Eigenes PNG/ICO und sechs UTF-8-Kataloge sind enthalten. Der isolierte
Pakettest startet ohne Entwicklungs-JDK und prüft Bestandsschema, Foto, Status,
Schritt-UUIDs und Sprachwechsel. Siehe [Vorschauablauf](internationalization.md).

## Lokale Vorbereitung vom 31. Oktober 2026

Maven und beide CI-Workflows verwenden Temurin 21.0.13+9. Die global vorhandene
Java-Installation bleibt bestehen. App-Images enthalten zusätzlich
`legal/USER_GUIDE.md` mit Datenpfaden und `legal/BUILD_JAVA_RELEASE.txt` mit der
originalen Herkunftsdatei des Build-JDK. Der bestehende explizite Windows-ICO-Pfad
bleibt erhalten. Ein vorbereitetes Skript ist kein Windows-Ausführungsnachweis.
Die geänderten CI-Workflows wurden nicht gestartet.

Vor einer öffentlichen Binärlieferung die genauen vollständigen Laufzeitquellen
und Begleitinformationen gemäß `THIRD_PARTY_NOTICES.md` zuordnen und zugänglich
bereitstellen. Diese Vorbereitung veröffentlicht weder Pakete noch Quellen.

Der Offline-Dialog enthält die Datenschutzerklärung in sechs Sprachen. Der Helfer
`scripts/export_privacy.sh` erzeugt die gleichen Texte für die Store-Einreichung.
Die MSIX-Anleitung erlaubt eine URL oder den direkten Datenschutztext.
Bei der tatsächlichen Einreichung die angebotenen Felder prüfen. Die Angaben
zur Verarbeitung müssen auch die lokal ausgewählten Fotos berücksichtigen.

Quellen, erneut gelesen am 31. Oktober 2026:

- [Microsoft: Distributionswege, Signierung und Updates](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/choose-distribution-path)
- [Microsoft: Datenschutztext und Supportangaben](https://learn.microsoft.com/en-us/windows/apps/publish/publish-your-app/msix/support-info)

Nächste Schritte: aktuelle Windows-Builds und echte Store-Identität vorbereiten,
MSIX erstellen, Installation und Update mit Datenbestand prüfen, danach WACK,
Windows-Aufnahmen, Lizenzbegleitmaterial und Store-Metadaten abschließen.
