# Paketierung und spätere Stores

## Voraussetzungen

Sichtbarer Produktname: NähPlan. Die Launcher- und Paketnamen verwenden
`NaehPlan` beziehungsweise `naehplan`. App-Kennung: `de.appfabrik.naehplan`.

Zuerst `./mvnw verify` beziehungsweise `.\mvnw.cmd verify` erfolgreich ausführen.
Paketierung kompiliert den aktuellen Stand, wiederholt jedoch nicht die Tests.
Ein vollständiges JDK 21 mit `javac`, `jlink` und `jpackage` ist erforderlich.
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

Auf Windows mit einem x64-JDK 21 und PowerShell ausführen. Für die EXE-Erzeugung
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

Auf der jeweiligen Mac-Architektur mit passendem JDK 21 ausführen:

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

Für den Microsoft Store bleiben Produktregistrierung, konkrete Distributionsform,
Signierung, Metadaten und Zertifizierung offen. Die Entscheidung zwischen einem
unterstützten Desktop-Installer-Weg und MSIX gehört zum späteren Releaseauftrag.

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
