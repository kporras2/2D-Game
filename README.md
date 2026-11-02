# Starfall Salvage

A Java pixel-art mining prototype inspired by the structure of RyiSnow's Java 2D game tutorial. A storm has stranded your astronaut. With help from the survey core ASTER, turn the moon's rocks into a place to call home.

## Run

Install JDK 17 or later and Maven, then run from this directory:

```sh
mvn package
java -jar target/starfall-salvage-1.0-SNAPSHOT.jar
```

Press **Enter** at the opening transmission. The game pauses while its window is unfocused or a journal/construction screen is open. The resizable window scales to fit.

## Controls

- **WASD / arrows:** walk; the astronaut faces movement direction.
- **Hold Space:** mine a nearby rock; the astronaut turns toward the target.
- **E near processor:** refine cargo into credits **and** one stored building material per rock.
- **U near processor:** upgrade the drill (30 / 90 / 200 credits).
- **B near habitat:** inspect the next outpost project; **Enter** builds when affordable.
- **J:** read ASTER's field journal. **Escape** closes the journal or construction screen.

## Chapter 01: A place to call home

1. **Seal the habitat:** process 6 moonstones into ceramic panels, then build at the abandoned habitat above the processor. Your cargo capacity increases from 8 to 12.
2. **Restore solar power:** unlock the copper drill, then bring 4 circuit wires and 40 credits to the habitat. Lights and a solar array appear; processing pays 20% more credits.
3. **Build the survey beacon:** unlock the prism drill, then bring 3 optical lenses and 80 credits. Your outpost is established, and ASTER picks up a mysterious signal from beneath the moon.

Every rock produces both a material and credits. Building spends stored materials and the listed credits; upgrading the drill spends only credits. Moonstone, copper, prism, and nova have increasing value and require progressively higher drill tiers. Rocks replenish, with common ore replenishment prioritized to keep starter resources available.

This chapter has a fixed habitat location. Interiors, freely placed buildings, and the next story chapter are not implemented yet. You can keep mining and upgrading after the beacon is built.

## Saved progress

Base construction, credits, materials, cargo, discoveries and drill tier are saved to `~/.starfall-salvage/progress.properties` on processing, purchases, building, window close, and every 15 seconds of active play. Relaunching places you near the processor with a refreshed rock field. Close the game and move this file elsewhere to begin a fresh expedition while retaining the old save. An unreadable save is left intact and a warning appears in-game.

## Edit the artwork

The astronaut's editable source is [`src/main/resources/sprites/astronaut.png`](src/main/resources/sprites/astronaut.png). Open it in Piskel, Pixelorama, or Aseprite, edit with a 1-pixel pencil, and save the same PNG. Restart the game from this project folder to see the changes; rebuild to bundle them in the JAR. The [editing guide](src/main/resources/sprites/EDITING.md) explains the 48 × 48 frames and sheet layout. Normal builds preserve your pixel edits.

## Checks and visual previews

```sh
./scripts/check.sh
java -Djava.awt.headless=true -cp target/classes:target/test-classes main.VisualPreview
```

The dependency-free checks cover the complete first-chapter economy, locked ore, capacity, construction costs, collision, menu controls, save/load, and sprite-sheet import/export. The preview command renders the actual UI and directional sprite poses into `artifacts/` for visual inspection. It uses an isolated game model and never touches your save.

## Implementation direction

Game rules live in `GameModel`, persistence in `SaveGame`, and the cached pixel sprite poses in `AstronautSprite`. Swing handles the desktop display. Sprites share a one-pixel source grid and use nearest-neighbor scaling. No external runtime libraries or downloaded artwork are needed.

Java/Swing supports this desktop prototype. Browser/mobile distribution would require another front end or an engine migration; these are not current build targets.
