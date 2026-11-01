# Separate offline information dialogs in Sewing Planner

Drei lokalisierte Info-Einträge mit getrennten Fenstern ergänzt. App-Informationen
zeigen Produkt, gebaute Version und Kontakt. Datenschutz bleibt vollständig
app-spezifisch. Hinweise und Original-Lizenztexte sind offline eingebettet.
Keine gemeinsame Runtime mit anderen Apps und keine Änderung an Nutzerdaten,
technischen Kennungen, Sammlungsschema oder Versionsnummern.

Prüfung: 208 Java-Tests, Compiler-Lint, Enforcer, Spotless und SpotBugs
bestanden. Menüfolge, Inhalte, Kopierbarkeit/Schreibschutz und Schließen in allen
sechs Sprachen geprüft. Linux-App-Image isoliert gebaut, Klassen und Ressourcen
abgeglichen, nativen Start und normales Schließen geprüft. Desktop-Starter
aktualisiert und vorhandene Datenoptionen erhalten. Laufende Nutzerfenster und
persönliche Exporte nicht verändert. Keine Veröffentlichung und kein Push.

Checkpoint: `144c8a0`. Prüfbericht: `docs/verification.md`.
