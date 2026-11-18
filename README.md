# CrossClick (Fabric, Minecraft 1.21.11)

Eine Taste auf Account 1 löst auf dem zweiten Client (Account 2) einen Rechtsklick aus.
Beide Clients laufen auf demselben PC und reden über UDP auf 127.0.0.1 (Port 25599).

## Bauen
1. Ordner in IntelliJ IDEA öffnen (JDK 21), Gradle importieren lassen.
2. Testen: Gradle-Task `Tasks > fabric > runClient`. Fertige Mod: Task `build`, Datei in `build/libs/crossclick-1.0.0.jar`.
3. Jar zusammen mit Fabric API (und optional Mod Menu) in den `mods`-Ordner **beider** Instanzen legen.

## Einrichten
- Zwei getrennte Instanzen (z. B. Prism Launcher), damit jede ihre eigene `config/` hat.
- Instanz Account 1: Rolle `SENDER` (Standard). Taste: Mod Menu > CrossClick > Konfigurieren (Standard: R).
- Instanz Account 2: Rolle `RECEIVER` (im Menü oder `config/crossclick.json`), Spiel neu starten.
- Beide Spiele laufen lassen. Taste auf Account 1 drücken -> Account 2 macht Rechtsklick.

## Config (`config/crossclick.json`)
- `role`: `SENDER` / `RECEIVER`
- `key`: GLFW-Keycode (-1 = nicht belegt)
- `port`: UDP-Port, bei beiden gleich
- `hold`: `false` = Einzelklick, `true` = Rechtsklick gehalten, solange die Taste unten ist (Bogen, Essen, Schild)

## Hinweise
- Der Sender reagiert nur, wenn kein Menü/Chat offen ist; der Empfänger klickt nur ohne offenes Menü.
- Account 2 darf unfokussiert sein (Pause bei Fokusverlust wird abgeschaltet), aber nicht minimiert.
- Viele Server verbieten Multiboxing/Automatisierung. Regeln beachten.
