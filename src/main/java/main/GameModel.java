package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The game rules are independent of Swing so they can be tested without a window. */
public class GameModel {
    public static final int WIDTH = 960, HEIGHT = 640, CAPACITY = 8;
    public static final int STATION_X = 145, STATION_Y = 310;
    public static final int BASE_X = 142, BASE_Y = 210;
    public enum Ore {
        DUST("Moonstone", "ceramic", 1, 5, 0.55, 0), COPPER("Copper", "circuit wire", 2, 12, 0.95, 1),
        CRYSTAL("Prism", "optical lenses", 3, 25, 1.35, 2), NOVA("Nova", "star cores", 4, 55, 1.8, 3);
        public final String label, product; public final int tier, value, color; public final double seconds;
        Ore(String label, String product, int tier, int value, double seconds, int color) {
            this.label = label; this.product = product; this.tier = tier; this.value = value; this.seconds = seconds; this.color = color;
        }
    }
    public static class Rock {
        public final double x, y; public final Ore ore; public double progress;
        Rock(double x, double y, Ore ore) { this.x = x; this.y = y; this.ore = ore; }
    }
    public final List<Rock> rocks = new ArrayList<>();
    public final int[] inventory = new int[Ore.values().length];
    public final int[] materials = new int[Ore.values().length];
    public double x = 240, y = 310, elapsed, spawnTimer;
    public int credits, tool = 1, totalSold, discoveries;
    public int baseStage;
    public boolean started;
    public String message = "ASTER: Six moonstones should yield enough ceramic to seal that abandoned habitat.";
    public double messageTime = 8;
    private final Random random;
    public GameModel() { this(new Random()); }
    public GameModel(Random random) {
        this.random = random;
        // A reliable first trip, plus rarer rocks to invite exploration.
        rocks.add(new Rock(330, 300, Ore.DUST));
        rocks.add(new Rock(430, 220, Ore.DUST));
        rocks.add(new Rock(500, 400, Ore.COPPER));
        rocks.add(new Rock(705, 250, Ore.CRYSTAL));
        rocks.add(new Rock(790, 465, Ore.NOVA));
        while (rocks.size() < 19) spawn();
    }
    public int cargo() { int n = 0; for (int amount : inventory) n += amount; return n; }
    public int capacity() { return baseStage >= 1 ? 12 : CAPACITY; }
    public int cargoValue() { int n = 0; for (Ore ore : Ore.values()) n += inventory[ore.ordinal()] * ore.value; return n; }
    public int processingValue() { return baseStage >= 2 ? cargoValue() * 120 / 100 : cargoValue(); }
    public int upgradeCost() { return tool == 1 ? 30 : tool == 2 ? 90 : 200; }
    public boolean atStation() { return distance(x, y, STATION_X, STATION_Y) < 95; }
    public boolean atBase() { return distance(x, y, BASE_X, BASE_Y) < 100; }
    public String objective() {
        if (baseStage == 0) return "01 / SHELTER   Process 6 moonstones. Visit the habitat and press B.";
        if (baseStage == 1) return tool < 2 ? "02 / POWER   Upgrade to drill II at the processor (U)." : "02 / POWER   Refine 4 copper. Bring 40 CR to the habitat (B).";
        if (baseStage == 2) return tool < 3 ? "03 / SIGNAL   Upgrade to drill III to mine prism." : "03 / SIGNAL   Refine 3 prism. Bring 80 CR to the habitat (B).";
        return "CHAPTER 01 COMPLETE   Your outpost is online. Keep exploring the moon.";
    }
    public String projectName() { return new String[]{"Seal the habitat", "Restore solar power", "Build a survey beacon", "Outpost established"}[baseStage]; }
    public int materialCost() { return baseStage == 0 ? 6 : baseStage == 1 ? 4 : baseStage == 2 ? 3 : 0; }
    public int buildingCost() { return baseStage == 1 ? 40 : baseStage == 2 ? 80 : 0; }
    public boolean canBuild() { return baseStage < 3 && materials[baseStage] >= materialCost() && credits >= buildingCost(); }
    public boolean build() {
        if (!atBase()) { say("Visit the habitat to build your outpost.", 3); return false; }
        if (baseStage >= 3) return false;
        if (!canBuild()) { say("ASTER: We still need the materials shown in the outpost plan.", 4); return false; }
        materials[baseStage] -= materialCost(); credits -= buildingCost(); baseStage++;
        say(switch (baseStage) {
            case 1 -> "ASTER: Airtight. A place to call home. Your new cargo harness carries 12 rocks.";
            case 2 -> "ASTER: Lights on! Powered processing now pays 20% more credits.";
            default -> "ASTER: Beacon online... wait. There is another signal beneath the moon. Chapter 01 complete.";
        }, 12);
        return true;
    }
    public Rock nearestRock() {
        Rock best = null; double dist = 48;
        for (Rock rock : rocks) { double d = distance(x, y, rock.x, rock.y); if (d < dist) { dist = d; best = rock; } }
        return best;
    }
    private static double distance(double x1, double y1, double x2, double y2) { return Math.hypot(x1-x2, y1-y2); }
    public void update(double dt, double dx, double dy, boolean mining) {
        dt = Math.min(dt, 0.05); elapsed += dt; messageTime -= dt;
        double length = Math.hypot(dx, dy);
        if (length > 0) {
            double nx = Math.max(24, Math.min(WIDTH-24, x + dx / length * 175 * dt));
            double ny = Math.max(130, Math.min(HEIGHT-70, y + dy / length * 175 * dt));
            if (!blocked(nx, y)) x = nx;
            if (!blocked(x, ny)) y = ny;
        }
        Rock target = nearestRock();
        for (Rock rock : rocks) if (rock != target || !mining) rock.progress = 0;
        if (mining && target != null && target.ore.tier <= tool && cargo() < capacity()) {
            target.progress += dt;
            if (target.progress >= target.ore.seconds) {
                inventory[target.ore.ordinal()]++; discoveries |= 1 << target.ore.ordinal();
                say("Collected " + target.ore.label + "!", 2); rocks.remove(target);
            }
        }
        spawnTimer += dt;
        if (spawnTimer >= 2.5) { spawnTimer = 0; if (rocks.size() < 25) spawn(); }
    }
    public void interact() {
        if (!atStation()) { say("Return to the processor to sell your cargo.", 2.5); return; }
        int value = processingValue();
        if (value == 0) { say("Cargo empty. Mine some rocks first!", 2.5); return; }
        int count = cargo(); credits += value; totalSold += count;
        Ore rarest = Ore.DUST;
        for (Ore ore : Ore.values()) if (inventory[ore.ordinal()] > 0) { rarest = ore; materials[ore.ordinal()] += inventory[ore.ordinal()]; }
        java.util.Arrays.fill(inventory, 0);
        say("+" + value + " CR and " + count + " materials stored. " + rarest.label + " becomes " + rarest.product + ".", 5);
    }
    public void upgrade() {
        if (!atStation()) { say("Upgrade at the processing station.", 2.5); return; }
        if (tool >= 4) { say("Your drill is fully upgraded!", 2.5); return; }
        int cost = upgradeCost();
        if (credits < cost) { say("You need " + (cost - credits) + " more credits.", 2.5); return; }
        credits -= cost; tool++; say("Drill upgraded! You can mine " + Ore.values()[tool-1].label + ".", 3.5);
    }
    private void say(String text, double seconds) { message = text; messageTime = seconds; }
    private boolean blocked(double px, double py) {
        return (px > 54 && px < 224 && py > 95 && py < 210)
            || (px > 73 && px < 209 && py > 226 && py < 355);
    }
    private void spawn() {
        for (int attempt = 0; attempt < 30; attempt++) {
            double rx = 260 + random.nextInt(WIDTH-300), ry = 155 + random.nextInt(HEIGHT-250);
            boolean clear = distance(rx, ry, x, y) > 65;
            for (Rock rock : rocks) if (distance(rx, ry, rock.x, rock.y) < 48) { clear = false; break; }
            if (!clear) continue;
            int common = 0; for (Rock existing : rocks) if (existing.ore == Ore.DUST) common++;
            int roll = common < 6 ? 0 : random.nextInt(100);
            Ore ore = roll < 53 ? Ore.DUST : roll < 80 ? Ore.COPPER : roll < 95 ? Ore.CRYSTAL : Ore.NOVA;
            rocks.add(new Rock(rx, ry, ore)); return;
        }
    }
}
