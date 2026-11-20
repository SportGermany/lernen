# Lernen

Begleit-App für Schulaufgaben (Realschule Bayern, 9./10. Klasse). Der Schüler arbeitet auf Papier, die App liegt daneben und führt Schritt für Schritt durch die Aufgabe. Inhalte kommen als Themenpakete (JSON) aus dem öffentlichen Repo [`lernen-pakete`](https://github.com/SportGermany/lernen-pakete).

Native Android-App: Kotlin, Jetpack Compose, Material 3. minSdk 26, targetSdk 35.

## Stand: Version 1.0.0

Alle sechs Meilensteine aus dem Auftrag sind umgesetzt: Deutsch (Begleiter, Arbeitsheft-Modus, Üben, Wortkiste, Korrekturzeichen),
Mathe und Physik (Mitrechnen, Rechen- und Einheiten-Engine, Taschenrechner, Umrechner, Plotter, Formelsammlung),
Englisch (Useful phrases, Grammatik-Rezept), Katalog und Sync, Fragen an Claude, Erinnerung, Datensicherung und App-Update.
Alle Pakete aus `lernen-pakete` sind auch in der App mitgeliefert und funktionieren ab dem ersten Start offline.

### Rechen-Engine
`core-math` ist eine eigene, schlanke Engine statt einer CAS-Bibliothek (Symja wäre sehr groß, und ob es auf Android läuft, ist unsicher).
Sie liest Schulschreibweise (Komma, ·, :, ², √, π, weggelassene Malpunkte) und prüft:
- Terme: Auswertung an vielen zufälligen Stellen,
- Gleichungen: Umformung (f₁ = c·f₂) oder gleiche Lösungsmenge (meldet „Lösung verloren/dazu“),
- Gleichungssysteme: exakt gelöst (Gauß), jeder Schritt muss zur Lösung passen.

## Neue Version veröffentlichen

Die App sucht Updates in `update/version.json` dieses Repos (Branch `main`). Dafür sind keine GitHub-Einstellungen nötig.

1. `versionName` und `versionCode` in `app/build.gradle.kts` erhöhen.
2. `LERNEN_KEYSTORE=/pfad/lernen.jks LERNEN_KEYSTORE_PASSWORT=… werkzeuge/veroeffentlichen.sh "Was ist neu"`
3. `update/` committen und auf `main` pushen. Die App zeigt „Neue Version“ und installiert sie.

Der Schlüssel `lernen.jks` und sein Passwort liegen **nicht** im Repo. Ohne sie lassen sich keine Updates bauen.
Optional geht es auch über GitHub Releases (Workflow `release.yml`, braucht vier Secrets); die App prüft beide Wege.

## Module

| Modul | Aufgabe |
|---|---|
| `app` | Activity, Navigation (untere Leiste bzw. seitliche Leiste auf dem Tablet), Hilt-Einstieg |
| `core-design` | Theme (Fachfarben, hell/dunkel, kein Dynamic Color), Bausteine (Karte, Abhakzeile, Schritt-Leiste, Hinweiskästen, Wortkiste), Paket-HTML → AnnotatedString |
| `core-data` | Paket-Modell, Parser, Prüfer, Mini-HTML-Leser, Fortschritt, Room, DataStore, Repositories, Schnittstelle `LernplanQuelle` (für später) |
| `core-sync` | Sync mit `lernen-pakete`, GitHub-API für Fragen, Token, WorkManager, App-Update |
| `core-math`, `core-units` | Rechen- und Einheiten-Engine (reines Kotlin, getestet) |
| `feature-*` | Bildschirme: Start, Fächer, Thema, Katalog, Fragen, Werkzeuge, Einstellungen |

`schema/paket.schema.json` ist das JSON-Schema für alle Pakete. Es wird auch im Paket-Repo von der GitHub Action benutzt.

## Bauen und testen

```bash
./gradlew assembleDebug                 # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew test                          # Unit-Tests (Parser, Schema, Fortschritt, App-Rauchtest)
./gradlew :feature-thema:verifyPaparazziDebug   # Bildschirmfotos vergleichen
./gradlew :feature-thema:recordPaparazziDebug   # Bildschirmfotos neu aufnehmen
```

Die Bildschirmfotos liegen unter `feature-thema/src/test/snapshots/images/`.

Keine Geheimnisse ins Repo: Der Token wird nur verschlüsselt auf dem Gerät gespeichert (Android Keystore), der Release-Keystore nur als GitHub-Secret.
