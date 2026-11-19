# Lernen

Begleit-App für Schulaufgaben (Realschule Bayern, 9./10. Klasse). Der Schüler arbeitet auf Papier, die App liegt daneben und führt Schritt für Schritt durch die Aufgabe. Inhalte kommen als Themenpakete (JSON) aus dem öffentlichen Repo [`lernen-pakete`](https://github.com/SportGermany/lernen-pakete).

Native Android-App: Kotlin, Jetpack Compose, Material 3. minSdk 26, targetSdk 35.

## Stand

| Meilenstein | Inhalt | Stand |
|---|---|---|
| 1 | Gerüst, Design, Navigation, Room, Paket-Parser + Schema, Deutsch offline (Überblick, Begleiter, Merkzettel), Bildschirm anlassen, Tablet zweispaltig | fertig |
| 2 | `lernen-pakete`, Sync, Katalog | offen |
| 3 | Üben, Wiederholen, Wortkiste, Korrekturzeichen | offen |
| 4 | Fragen an Claude, Erinnerung, Datensicherung, App-Update, erste Release | offen |
| 5 | Mathe & Physik | offen |
| 6 | Englisch | offen |

## Module

| Modul | Aufgabe |
|---|---|
| `app` | Activity, Navigation (untere Leiste bzw. seitliche Leiste auf dem Tablet), Hilt-Einstieg |
| `core-design` | Theme (Fachfarben, hell/dunkel, kein Dynamic Color), Bausteine (Karte, Abhakzeile, Schritt-Leiste, Hinweiskästen, Wortkiste), Paket-HTML → AnnotatedString |
| `core-data` | Paket-Modell, Parser, Prüfer, Mini-HTML-Leser, Fortschritt, Room, DataStore, Repositories, Schnittstelle `LernplanQuelle` (für später) |
| `core-sync` | Sync mit `lernen-pakete` (ab Meilenstein 2) |
| `core-math`, `core-units` | Rechen- und Einheiten-Engine (ab Meilenstein 5) |
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

Keine Geheimnisse ins Repo: Tokens werden nur verschlüsselt auf dem Gerät gespeichert (ab Meilenstein 4).
