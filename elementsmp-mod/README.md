# Elemente-SMP (Fabric-Mod, rein serverseitig)

Minecraft 1.21.11 | Fabric | Java 21 | **Clients brauchen keine Mod** (Vanilla-Clients koennen joinen).

## Installation
1. `./gradlew build`
2. `build/libs/elementsmp-1.0.0.jar` in den `mods`-Ordner des Servers legen (zusammen mit Fabric API).

## Spielprinzip
Beim ersten Join erhaelt jeder Spieler zufaellig eines von sechs Elementen (Wind, Sand, Lava, Wasser, Erde, Void)
und ein **Fokus-Item**. Je Element gibt es 3 Faehigkeiten mit Cooldown.

- Rechtsklick mit Fokus-Item: ausgewaehlte Faehigkeit wirken
- Shift + Rechtsklick: Faehigkeit wechseln
- Action-Bar zeigt Faehigkeit und Cooldown

## Befehle
| Befehl | Rechte | Beschreibung |
|---|---|---|
| `/element` | alle | Eigenes Element und Faehigkeiten, oeffnet das GUI |
| `/element gui` | alle | GUI oeffnen |
| `/element cast <1-3>` | alle | Faehigkeit direkt wirken |
| `/element focus` | alle | Neues Fokus-Item |
| `/element info <spieler>` | Admin | Element eines Spielers |
| `/element set <spieler> <element>` | Admin | Element aendern |
| `/element reset <spieler>` | Admin | Zuweisung zuruecksetzen (wuerfelt sofort neu) |
| `/element reload` | Admin | Konfiguration neu laden |

## Konfiguration
`config/elementsmp/config.json` wird beim ersten Start erzeugt:
- `probabilities`: Basis-Wahrscheinlichkeit pro Element
- `balanceStrength`: je hoeher, desto seltener werden bereits oft vergebene Elemente gewuerfelt (0 = reiner Zufall)
- `giveFocusItem`: Fokus-Item automatisch geben
- `abilities`: pro Faehigkeit `cooldownSeconds`, `power`, `radius`, `durationSeconds`, `damage`, `selfDamage`

Spielerdaten (UUID -> Element): `config/elementsmp/players.json`.

## Erweiterung
Neue Faehigkeit: in der passenden `*Abilities`-Klasse (Paket `com.example.ability`) einen `Ability`-Eintrag ergaenzen.
Die Config-Werte werden automatisch hinzugefuegt.
