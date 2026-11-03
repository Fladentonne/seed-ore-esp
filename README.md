# Seed Ore ESP (Fabric, Minecraft 26.3)

Client-Mod: Du gibst den Welt-Seed ein, die Mod rechnet die Erzgenerierung nach und zeichnet
die Erze als Outline (durch Wände sichtbar) um dich herum.

## Bauen
Voraussetzung: JDK 25 und Gradle 9.4+ (der Gradle-Wrapper liegt nicht bei, weil er hier ohne
Internet nicht erzeugt werden konnte).

    gradle wrapper --gradle-version 9.5.1   # einmalig
    ./gradlew build

Alternativ das Projekt in IntelliJ IDEA (2025.3+) öffnen, es richtet Gradle selbst ein.

Die Mod liegt danach in `build/libs/seed-ore-esp-1.0.0.jar` (nicht die `-sources`-Datei).
In den `mods`-Ordner legen, zusammen mit Fabric Loader 0.19.5+ und Fabric API 0.165.2+26.3.
Falls das Loom-Plugin nicht gefunden wird: `loom_version` in `gradle.properties` anpassen
(aktuelle Werte: https://fabricmc.net/develop/).

## Bedienung
- **O**: Menü öffnen (Seed, Sichtweite, Erze an/aus, Farben, Kalibrieren)
- **P**: ESP an/aus
- Beide Tasten sind unter Steuerung > "Seed Ore ESP" umbelegbar.
- Seed: Zahl oder Text (Text wird wie im Spiel gehasht).

## Kalibrieren (wichtig)
Welches Erz in der Generierungsreihenfolge an welcher Stelle steht, hängt von der Version ab.
Die Standardwerte in `OreDefs.java` sind Schätzungen. Im Menü auf **Kalibrieren** klicken,
während du in einer Gegend mit sichtbaren Erzen (Höhlen, Schluchten) stehst: Die Mod testet
pro Feature die Indizes 0-63 gegen die echten Blöcke in den geladenen Chunks und speichert den besten.
In `config/seedoresp.json` landet das Ergebnis.

## Bekannte Grenzen
- Biom-abhängige Erze (Smaragd, Extra-Gold in Badlands, großes Kupfer) fehlen.
- Nether: nur Ancient Debris. End hat keine Erze.
- Die Prüfung "liegt der Vein überhaupt im Gelände" (Heightmap) ist weggelassen;
  gezeichnet wird nur, wo der echte Block ein passendes Wirtsgestein oder Erz ist.
- Nicht kompiliert/getestet, siehe Hinweise im Chat.
