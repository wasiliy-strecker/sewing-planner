# Verwendete Komponenten

NähPlan verwendet OpenJFX unter GPLv2 mit Classpath Exception und SQLite
JDBC von Xerial unter Apache License 2.0 und BSD-2-Clause. SQLite selbst ist
Public Domain. Die eingebettete Java-Laufzeit unterliegt den Lizenzbedingungen
der beim Bauen gewählten JDK-Distribution. Die aktuelle Build-Basis ist
Eclipse Temurin 21.0.13+9 unter GPLv2 mit Classpath Exception. Maven prüft
Hersteller, Version und Buildnummer. Das App-Image enthält zusätzlich die
unveränderte JDK-Datei `release` als `legal/BUILD_JAVA_RELEASE.txt`, sodass
Herkunft, Architektur und vollständige Laufzeitversion nachvollziehbar bleiben.

Die vollständigen Hinweise der Laufzeit bleiben im Verzeichnis `legal` des
Runtime-Images erhalten. SQLite JDBC bleibt als unverändertes Archiv mit den
mitgelieferten Lizenzdateien enthalten. Die OpenJFX-Lizenztexte werden aus
`third-party-licenses` in das Paket übernommen. Diese Datei ergänzt diese Hinweise.

## Versionsbezogene Herkunft und Quellcode

| Komponente | Version | Herkunft und zugehöriger Quellcode |
| --- | --- | --- |
| Eclipse Temurin | 21.0.13+9 | [Binärrelease und vollständiges Quellarchiv](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.13%2B9), Datei `OpenJDK21U-jdk-sources_21.0.13_9.tar.gz` |
| OpenJFX | 21.0.11+4 | [OpenJFX-Quellen](https://github.com/openjdk/jfx21u/tree/21.0.11%2B4), Module `javafx.base`, `javafx.graphics`, `javafx.controls` |
| Xerial SQLite JDBC | 3.53.4.0 | [Unverändertes JDBC-Archiv und Quellen](https://github.com/xerial/sqlite-jdbc/tree/3.53.4.0), Lizenzen unter `META-INF/maven/org.xerial/sqlite-jdbc` im JAR |

Das JavaFX-Artefakt meldet die Laufzeitversion `21.0.11+4`. Die mitgelieferten
Linux-Nativhinweise decken JPEG und Mesa ab. Die JDK-Lizenzhinweise bleiben
im Runtime-Image erhalten. SQLite selbst ist [Public Domain](https://sqlite.org/copyright.html).
Die Anwendung verändert die Fremdkomponenten nicht.

Die [GPLv2 mit Classpath Exception](https://openjdk.org/legal/gplv2+ce.html)
enthält weiterhin Pflichten für die weitergegebenen Laufzeitkomponenten.
Vor einer öffentlichen Binärlieferung müssen die genau zu den ausgelieferten
Temurin- und OpenJFX-Binärdateien gehörenden vollständigen Quellarchive samt
Buildinformationen und Lizenzhinweisen als Release-Begleitmaterial verfügbar
gemacht werden. Ein bloßer allgemeiner Projektlink ersetzt diese Aufgabe nicht.
Für die spätere Store-Lieferung ist die Bereitstellung aus dem Store-Eintrag
zugänglich zu machen. Für GitHub gehören die Begleitarchive zum zugehörigen
Release. Diese lokale Vorbereitung veröffentlicht weder Binärdateien noch
Quellarchive.

Die zusätzlichen nativen Komponenten des endgültigen Windows-Pakets sind
erst am tatsächlich gebauten Windows-Artefakt abschließend zu inventarisieren.
Die Linux-Prüfung ist keine Lizenz- oder Signaturabnahme eines Windows-Pakets.

Maven, Spotless, SpotBugs und JUnit sind Entwicklungswerkzeuge und werden nicht
in das App-Image aufgenommen. Dieses Dokument vergibt keine Lizenz für den
eigenen NähPlan-Quellcode.
