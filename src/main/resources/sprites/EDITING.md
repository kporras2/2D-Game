# Editing the astronaut

Open **astronaut.png** in a pixel-art editor. This is the actual artwork the game loads, with a transparent background and no labels.

Good choices:
- **Piskel** (https://www.piskelapp.com/): free, runs in your browser, convenient for quick pixel changes.
- **Pixelorama** (https://orama-interactive.itch.io/pixelorama): free/open source, with a Mac download and animation tools.
- **Aseprite** (https://www.aseprite.org/): a paid editor with layers, animation and sprite-sheet workflows.

## Simple pixel touch-ups

1. Open the PNG as one image, keeping its **192 × 576 pixel** canvas.
2. Zoom to 800% or higher and use a **1-pixel pencil**. The game doubles each source pixel, so draw on the original image rather than an enlarged screenshot.
3. Keep the background transparent. Avoid blur, smoothing, trimming, automatic packing, or resizing the canvas.
4. Save/export as PNG to the same `astronaut.png` location. Browser editors download a new copy; move that copy over this file.
5. Close and restart the game **from the project folder** to see edits. A rebuild is unnecessary for local PNG changes. Run `mvn package` when you want to include the edited image in the distributable JAR.

Normal builds and the preview tool never overwrite this PNG. The procedural Java drawing is retained only as a fallback and an optional original-art exporter.

## Frame layout

Each frame occupies **48 × 48 pixels**, with no spacing or margins. The sheet has four columns (frames 1–4) and twelve rows:

| Row | Action | Direction |
| --- | --- | --- |
| 1 | Idle | Front |
| 2 | Idle | Left |
| 3 | Idle | Right |
| 4 | Idle | Back |
| 5 | Walk | Front |
| 6 | Walk | Left |
| 7 | Walk | Right |
| 8 | Walk | Back |
| 9 | Mine | Front |
| 10 | Mine | Left |
| 11 | Mine | Right |
| 12 | Mine | Back |

Rows/columns in this table start at 1. Idle currently uses the first frame in each row. Walking and mining use all four. Apply helmet changes across the relevant walking/mining frames too; walking has a one-pixel bob in frames 2 and 4.

If importing as an animation instead of a single image, use 48 × 48 cells in row order, then export with **four columns, twelve rows, no padding, no trimming, original scale**. You can keep a separate layered editor project alongside this PNG, then export to PNG for the game.

Preview changes after a build with:

```sh
java -Djava.awt.headless=true -cp target/classes:target/test-classes main.VisualPreview
```

This produces enlarged, labeled previews in `artifacts/`. Those previews are for inspection; edit the transparent source PNG here.
