# Lernen

Begleit-App für Schulaufgaben (Realschule Bayern, 9./10. Klasse). Der Schüler arbeitet auf Papier, die App liegt daneben und führt Schritt für Schritt durch die Aufgabe. Inhalte kommen als Themenpakete (JSON) aus dem öffentlichen Repo [`lernen-pakete`](https://github.com/SportGermany/lernen-pakete).

Native Android-App: Kotlin, Jetpack Compose, Material 3. minSdk 26, targetSdk 35.

## Stand

| Meilenstein | Inhalt | Stand |
|---|---|---|
| 1 | Gerüst, Design, Navigation, Room, Paket-Parser + Schema, Deutsch offline (Überblick, Begleiter, Merkzettel), Bildschirm anlassen, Tablet zweispaltig | fertig |
| 2 | `lernen-pakete`, Sync, Katalog (Hinzufügen / Entfernen / Update), Fächer-Einstellungen | fertig |
| 3 | Üben (alle Übungstypen), Wiederholen, Wortkiste, Korrekturzeichen | fertig |
| 4 | Fragen an Claude, Token, Auto-Löschen, Erinnerung, Datensicherung, App-Update, Release-Workflow | fertig (erste Release: siehe unten) |
| 5 | Rechen-Engine, Mathe-Tastatur, Mitrechnen, Einheiten, Taschenrechner, Umrechner, Plotter, Formelsammlung | fertig |
| 6 | Englisch: Useful phrases, Grammatik als Rezept, Beispielpakete | fertig |

### Rechen-Engine
`core-math` ist eine eigene, schlanke Engine statt einer CAS-Bibliothek (Symja wäre sehr groß, und ob es auf Android läuft, ist unsicher).
Sie liest Schulschreibweise (Komma, ·, :, ², √, π, weggelassene Malpunkte) und prüft:
- Terme: Auswertung an vielen zufälligen Stellen,
- Gleichungen: Umformung (f₁ = c·f₂) oder gleiche Lösungsmenge (meldet „Lösung verloren/dazu“),
- Gleichungssysteme: exakt gelöst (Gauß), jeder Schritt muss zur Lösung passen.

## Erste Release

1. Keystore erzeugen (einmalig, gut aufbewahren):
   `keytool -genkeypair -v -keystore lernen.jks -alias lernen -keyalg RSA -keysize 2048 -validity 10000`
2. Im Repo unter Settings → Secrets → Actions anlegen: `KEYSTORE_BASE64` (`base64 -w0 lernen.jks`), `KEYSTORE_PASSWORT`, `KEY_ALIAS`, `KEY_PASSWORT`.
3. Auf GitHub: Actions → Release → „Run workflow“ (oder einen Tag `vX.Y.Z` pushen, der zu `versionName` passt).
   Die Action baut die signierte APK und legt die Release an. Die App findet sie dann über „Nach Update suchen“.

Hinweis: Eine mit dem Debug-Schlüssel signierte APK lässt sich nicht durch eine Release-APK ersetzen (anderer Schlüssel). Beim Wechsel einmal deinstallieren; vorher die Datensicherung exportieren.

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
