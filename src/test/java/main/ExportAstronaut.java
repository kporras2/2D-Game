package main;

import java.nio.file.*;
import java.io.OutputStream;
import javax.imageio.ImageIO;

/** Explicit one-time export; refuses to overwrite an artist's existing file. */
public class ExportAstronaut {
    public static void main(String[] args) throws Exception {
        Path output = args.length == 0 ? AstronautSprite.EDITABLE_SHEET : Path.of(args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (OutputStream stream = Files.newOutputStream(output, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            ImageIO.write(AstronautSprite.generatedSheet(), "png", stream);
        }
        System.out.println("Editable sprite sheet: " + output.toAbsolutePath());
    }
}
