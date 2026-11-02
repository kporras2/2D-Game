package main;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/** Only progression is persisted; the rock field is regenerated for a new expedition. */
public final class SaveGame {
    public static Path defaultPath() { return Path.of(System.getProperty("user.home"), ".starfall-salvage", "progress.properties"); }

    public static void save(GameModel game, Path path) throws IOException {
        Properties p = new Properties();
        p.setProperty("version", "1");
        p.setProperty("started", Boolean.toString(game.started));
        p.setProperty("credits", "" + game.credits); p.setProperty("tool", "" + game.tool);
        p.setProperty("base", "" + game.baseStage); p.setProperty("sold", "" + game.totalSold);
        p.setProperty("discoveries", "" + game.discoveries);
        for (int i = 0; i < 4; i++) {
            p.setProperty("cargo." + i, "" + game.inventory[i]); p.setProperty("material." + i, "" + game.materials[i]);
        }
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temp = Files.createTempFile(path.toAbsolutePath().getParent(), "progress-", ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp)) { p.store(out, "Starfall Salvage progress"); }
            try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }

    public static boolean load(GameModel game, Path path) throws IOException {
        if (!Files.exists(path)) return false;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) { p.load(in); }
        try {
            // Validate the entire file before mutating the model.
            int version = number(p, "version", 1, 1);
            int credits = number(p, "credits", 0, 100_000_000), tool = number(p, "tool", 1, 4);
            int stage = number(p, "base", 0, 3), sold = number(p, "sold", 0, 10_000_000);
            int discoveries = number(p, "discoveries", 0, 15);
            int[] cargo = new int[4], materials = new int[4]; int total = 0;
            for (int i = 0; i < 4; i++) {
                cargo[i] = number(p, "cargo." + i, 0, 12); total += cargo[i];
                materials[i] = number(p, "material." + i, 0, 10_000_000);
            }
            if (total > (stage > 0 ? 12 : 8)) throw new IllegalArgumentException("Cargo exceeds capacity");
            game.credits = credits; game.tool = tool; game.baseStage = stage; game.totalSold = sold;
            game.discoveries = discoveries; game.started = Boolean.parseBoolean(p.getProperty("started", "false"));
            System.arraycopy(cargo, 0, game.inventory, 0, 4); System.arraycopy(materials, 0, game.materials, 0, 4);
            game.message = "ASTER: Welcome back. Your outpost and supplies are just as you left them.";
            game.messageTime = 8;
            return true;
        } catch (IllegalArgumentException e) { throw new IOException("Save file is not valid", e); }
    }
    private static int number(Properties p, String key, int min, int max) {
        int value = Integer.parseInt(p.getProperty(key));
        if (value < min || value > max) throw new IllegalArgumentException("Invalid " + key);
        return value;
    }
}
