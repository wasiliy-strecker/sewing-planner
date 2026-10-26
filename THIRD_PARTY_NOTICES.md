# Verwendete Komponenten

NähPlan verwendet OpenJFX unter GPLv2 mit Classpath Exception und SQLite
JDBC von Xerial unter Apache License 2.0 und BSD-2-Clause. SQLite selbst ist
Public Domain. Die eingebettete Java-Laufzeit unterliegt den Lizenzbedingungen
der beim Bauen gewählten JDK-Distribution. Für die dokumentierten Builds wird
OpenJDK 21 von Eclipse Temurin unter GPLv2 mit Classpath Exception verwendet.

Die vollständigen Hinweise der Laufzeit bleiben im Verzeichnis `legal` des
Runtime-Images erhalten. SQLite JDBC bleibt als unverändertes Archiv mit den
mitgelieferten Lizenzdateien enthalten. Die OpenJFX-Lizenztexte werden aus
`third-party-licenses` in das Paket übernommen. Diese Datei ergänzt diese Hinweise.

Quellen:

- https://openjdk.org/legal/gplv2+ce.html
- https://github.com/openjdk/jfx/blob/jfx21/LICENSE
- https://github.com/xerial/sqlite-jdbc
- https://sqlite.org/copyright.html
- https://adoptium.net/about/

Maven, Spotless, SpotBugs und JUnit sind Entwicklungswerkzeuge und werden nicht
in das App-Image aufgenommen. Dieses Dokument vergibt keine Lizenz für den
eigenen NähPlan-Quellcode.
