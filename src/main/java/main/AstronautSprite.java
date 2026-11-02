package main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** A small, cached pixel sprite atlas. All poses share the same one-pixel grid. */
public final class AstronautSprite {
    public static final int FRAME_SIZE = 48, SHEET_WIDTH = 192, SHEET_HEIGHT = 576;
    public static final Path EDITABLE_SHEET = Path.of("src", "main", "resources", "sprites", "astronaut.png");
    public static final int DOWN = 0, LEFT = 1, RIGHT = 2, UP = 3;
    public static final int IDLE = 0, WALK = 1, MINE = 2;
    private static final Color INK = new Color(10, 23, 35);
    private static final Color SHADE = new Color(91, 130, 145);
    private static final Color SUIT = new Color(204, 222, 216);
    private static final Color LIGHT = new Color(244, 244, 222);
    private static final Color GOLD = new Color(231, 160, 84);
    private final BufferedImage[][][] frames = new BufferedImage[4][3][4];

    public AstronautSprite() {
        this(loadSheet());
    }

    AstronautSprite(BufferedImage sheet) {
        if (sheet != null && (sheet.getWidth() != SHEET_WIDTH || sheet.getHeight() != SHEET_HEIGHT))
            throw new IllegalArgumentException("astronaut.png must be 192 x 576 pixels, with 48 x 48 frames. Keep the original canvas size when exporting.");
        for (int direction = 0; direction < 4; direction++)
            for (int action = 0; action < 3; action++)
                for (int frame = 0; frame < 4; frame++)
                    frames[direction][action][frame] = sheet == null ? create(direction, action, frame)
                        : sheet.getSubimage(frame * FRAME_SIZE, (action * 4 + direction) * FRAME_SIZE, FRAME_SIZE, FRAME_SIZE);
    }

    private static BufferedImage loadSheet() {
        try {
            // Local edits take effect on restart; the same image is bundled in distributable JARs.
            if (Files.exists(EDITABLE_SHEET)) {
                try (InputStream in = Files.newInputStream(EDITABLE_SHEET)) { return readSheet(in); }
            }
            try (InputStream in = AstronautSprite.class.getResourceAsStream("/sprites/astronaut.png")) {
                return in == null ? null : readSheet(in);
            }
        } catch (IOException e) { throw new IllegalArgumentException("Cannot read astronaut.png. Export it as a PNG image and try again.", e); }
    }

    private static BufferedImage readSheet(InputStream in) throws IOException {
        BufferedImage image = ImageIO.read(in);
        if (image == null) throw new IOException("Not a PNG image");
        return image;
    }

    /** One-time source export. Normal builds never regenerate or overwrite the editable artwork. */
    static BufferedImage generatedSheet() {
        AstronautSprite original = new AstronautSprite((BufferedImage) null);
        BufferedImage sheet = new BufferedImage(SHEET_WIDTH, SHEET_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        for (int action = 0; action < 3; action++)
            for (int direction = 0; direction < 4; direction++)
                for (int frame = 0; frame < 4; frame++)
                    g.drawImage(original.frames[direction][action][frame], frame * FRAME_SIZE, (action * 4 + direction) * FRAME_SIZE, null);
        g.dispose();
        return sheet;
    }

    public void draw(Graphics2D g, int x, int y, int direction, int action, int frame) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(frames[direction][action][frame % 4], x - 48, y - 64, 96, 96, null);
    }

    private BufferedImage create(int direction, int action, int frame) {
        BufferedImage image = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(new Color(4, 13, 24, 100)); g.fillOval(14, 42, 21, 4);
        int bob = action == WALK && (frame == 1 || frame == 3) ? -1 : 0;
        g.translate(0, bob);
        if (direction == UP && action == MINE) tool(g, direction, frame);
        boolean side = direction == LEFT || direction == RIGHT;
        int stride = action == WALK ? new int[]{0, -2, 0, 2}[frame] : 0;
        // Rounded boot tips, a clear crotch gap, and an alternating gait.
        boot(g, side ? 21 : 18, 37 + stride);
        boot(g, side ? 25 : 26, 37 - stride);
        suit(g, direction, action, stride);
        // The helmet seats directly in the padded shoulders; no exposed neck or collar band.
        Graphics2D head = (Graphics2D) g.create();
        head.translate(0, 5);
        helmet(head, direction);
        head.dispose();
        if (direction != UP && action == MINE) tool(g, direction, frame);
        g.dispose();
        return image;
    }

    private void helmet(Graphics2D g, int direction) {
        if (direction == LEFT) {
            // Mirror the finished pixels so both profiles have identical contours.
            BufferedImage profile = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
            Graphics2D right = profile.createGraphics();
            helmet(right, RIGHT);
            right.dispose();
            g.drawImage(profile, 49, 0, 1, 48, 0, 0, 48, 48, null);
            return;
        }
        // Round shell: shaded bands follow a sphere, instead of stacking horizontal bars.
        g.setColor(INK); g.fillOval(15, 6, 19, 19);
        g.setColor(SHADE); g.fillOval(16, 7, 17, 17);
        g.setColor(SUIT); g.fillOval(16, 7, 16, 15);
        g.setColor(LIGHT); g.fillOval(17, 7, 14, 13);
        if (direction == UP) {
            // Solid rear shell, central seam and a small service connector.
            g.setColor(SUIT); g.fillOval(18, 9, 13, 13);
            rect(g, SHADE, 22, 19, 5, 3); rect(g, LIGHT, 23, 19, 3, 1);
            rect(g, new Color(160, 192, 193), 24, 9, 1, 8);
            rect(g, LIGHT, 19, 9, 2, 2);
            return;
        }
        // A broad, curved sun visor sits under the shell brow and above the chin rim.
        // Its flattened top and wrapped sides avoid the appearance of a circular porthole.
        Color glassShadow = new Color(73, 55, 40);
        Color glass = new Color(164, 112, 49);
        Color reflection = new Color(222, 177, 88);
        Graphics2D visor = (Graphics2D) g.create();
        if (direction == DOWN) {
            shape(visor, INK,
                new int[]{20,29,32,32,30,27,22,18,17,17},
                new int[]{10,10,12,17,20,21,21,19,16,12});
            shape(visor, glassShadow,
                new int[]{20,29,31,31,29,26,22,19,18,18},
                new int[]{11,11,13,17,19,20,20,18,16,13});
            shape(visor, glass,
                new int[]{20,29,31,30,27,22,19,18},
                new int[]{11,12,14,17,18,18,16,13});
            shape(visor, reflection,
                new int[]{21,27,29,27,21,19},
                new int[]{12,12,13,15,16,14});
            rect(visor, new Color(249, 230, 175), 21, 12, 5, 1);
            rect(visor, new Color(241, 211, 143), 19, 13, 2, 2);
            rect(visor, reflection, 29, 17, 1, 1);
        } else {
            // Reference profile: a circular temple pivot, with the upper and lower
            // visor rims sweeping outward from it to the front of the spherical shell.
            shape(visor, glassShadow,
                new int[]{24,27,30,32,33,33,32,30,27,24},
                new int[]{14,13,11,11,13,17,20,21,20,17});
            shape(visor, glass,
                new int[]{26,28,31,32,32,30,28,26},
                new int[]{14,13,12,14,17,20,19,17});
            shape(visor, reflection,
                new int[]{27,30,31,31,29,27},
                new int[]{14,13,14,16,17,16});
            rect(visor, new Color(249, 230, 175), 29, 13, 2, 1);
            // Rims flare apart toward the face, matching the drawn side silhouette.
            visor.setStroke(new BasicStroke(1));
            visor.setColor(LIGHT);
            visor.drawPolyline(new int[]{24,27,29,31}, new int[]{14,13,12,11}, 4);
            visor.setColor(SUIT);
            visor.drawPolyline(new int[]{24,26,28,30}, new int[]{17,19,20,21}, 4);
            // A round recessed hinge replaces the former rectangular fitting.
            String[] hinge = {"..sss..", ".swwws.", "swwhwws", "swhwhws", "swwhwws", ".swwws.", "..sss.."};
            for (int row = 0; row < hinge.length; row++) {
                for (int col = 0; col < hinge[row].length(); col++) {
                    char pixel = hinge[row].charAt(col);
                    if (pixel != '.') rect(visor, pixel == 'w' ? LIGHT : SHADE, 21 + col, 13 + row, 1, 1);
                }
            }

        }
        visor.dispose();
    }

    private void boot(Graphics2D g, int x, int y) {
        rect(g, INK, x - 1, y, 6, 7); rect(g, SUIT, x, y, 4, 3);
        rect(g, SHADE, x, y + 3, 4, 2); rect(g, INK, x - 1, y + 6, 7, 1);
    }
    private void suit(Graphics2D g, int direction, int action, int stride) {
        Graphics2D body = (Graphics2D) g.create();
        if (direction == LEFT || direction == RIGHT) {
            // A narrower profile, with the life-support pack following the curved back.
            if (direction == LEFT) { body.translate(49, 0); body.scale(-1, 1); }
            body.setColor(INK); body.fillRoundRect(14, 27, 8, 12, 4, 4);
            body.setColor(SHADE); body.fillRoundRect(15, 28, 6, 10, 3, 3);
            rect(body, SUIT, 16, 29, 3, 6); rect(body, GOLD, 16, 36, 3, 1);
            shape(body, INK, new int[]{23,27,30,31,30,29,30,28,22,21,22,21}, new int[]{26,26,28,31,33,35,38,40,40,38,34,29});
            shape(body, SHADE, new int[]{24,27,29,30,28,28,29,23,22,23}, new int[]{27,27,29,31,35,37,38,39,37,30});
            shape(body, SUIT, new int[]{24,27,28,29,27,27,28,24,23}, new int[]{27,28,30,32,35,37,38,38,30});
            rect(body, LIGHT, 24, 28, 2, 3);
            rect(body, GOLD, 27, 35, 2, 2);
            sleeve(body, 26, 29, 27, 32, action == MINE ? 31 : 27 + stride, action == MINE ? 33 : 37);
        } else {
            // The pressure suit widens at the shoulder, narrows at the waist and rounds into the hips.
            shape(body, INK,
                new int[]{21,28,31,32,31,30,31,29,20,18,19,17,18},
                new int[]{26,26,28,31,34,36,38,40,40,38,35,32,29});
            shape(body, SHADE,
                new int[]{21,28,30,31,29,29,30,28,21,20,21,19,19},
                new int[]{27,27,29,31,35,37,38,39,39,37,35,32,29});
            shape(body, SUIT,
                new int[]{22,27,29,29,28,28,29,22,21,22,20,20},
                new int[]{27,27,29,32,35,37,38,38,36,34,31,29});
            shape(body, LIGHT, new int[]{21,23,22,22,20,20}, new int[]{28,28,30,33,32,30});
            // A small utility belt and hip pockets, without a straight stripe across the torso.
            rect(body, GOLD, 22, 35, 5, 2); rect(body, LIGHT, 24, 35, 1, 1);
            rect(body, SHADE, 20, 37, 2, 2); rect(body, SHADE, 28, 37, 2, 2);
            if (direction == UP) {
                body.setColor(INK); body.fillRoundRect(20, 28, 10, 10, 4, 4);
                body.setColor(SHADE); body.fillRoundRect(21, 29, 8, 8, 3, 3);
                body.setColor(SUIT); body.fillRoundRect(21, 29, 3, 6, 2, 2); body.fillRoundRect(26, 29, 3, 6, 2, 2);
                rect(body, LIGHT, 21, 30, 1, 3); rect(body, LIGHT, 26, 30, 1, 3);
                rect(body, new Color(100, 223, 177), 24, 30, 2, 1);
            } else {
                // Small chest controls follow the suit instead of forming another rectangular torso.
                rect(body, SHADE, 25, 30, 3, 3);
                rect(body, new Color(114, 218, 200), 26, 30, 1, 1);
            }
            int swing = action == WALK ? stride : 0;
            sleeve(body, 19, 29, 17, 32, 17, 36 - swing);
            sleeve(body, 30, 29, 32, 32, 32, action == MINE ? 33 : 36 + swing);
        }
        body.dispose();
    }
    private void sleeve(Graphics2D g, int sx, int sy, int ex, int ey, int hx, int hy) {
        // Rounded shoulders stay attached while the elbow and glove move independently.
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(INK); g.drawPolyline(new int[]{sx,ex,hx}, new int[]{sy,ey,hy}, 3);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(SUIT); g.drawPolyline(new int[]{sx,ex,hx}, new int[]{sy,ey,hy-1}, 3);
        rect(g, LIGHT, sx-1, sy-1, 2, 2);
        rect(g, GOLD, hx-1, hy-2, 3, 1);
        rect(g, SHADE, hx-1, hy-1, 3, 2);
    }
    private static void shape(Graphics2D g, Color color, int[] x, int[] y) {
        g.setColor(color); g.fillPolygon(x, y, x.length);
    }
    private void tool(Graphics2D g, int direction, int frame) {
        double targetAngle = switch (direction) { case LEFT -> Math.PI; case RIGHT -> 0; case UP -> -Math.PI / 2; default -> Math.PI / 2; };
        double swing = new double[]{-.95, -.45, .12, .35}[frame];
        double angle = targetAngle + (direction == UP ? -swing : swing);
        int hx = direction == LEFT ? 16 : direction == UP ? 34 : 32, hy = direction == UP ? 28 : 33;
        int tx = hx + (int)Math.round(Math.cos(angle) * 11), ty = hy + (int)Math.round(Math.sin(angle) * 11);
        g.setStroke(new BasicStroke(3)); g.setColor(INK); g.drawLine(hx, hy, tx, ty);
        g.setStroke(new BasicStroke(1)); g.setColor(GOLD); g.drawLine(hx, hy, tx, ty);
        int px = (int)Math.round(-Math.sin(angle) * 5), py = (int)Math.round(Math.cos(angle) * 5);
        g.setStroke(new BasicStroke(3)); g.setColor(INK); g.drawLine(tx - px, ty - py, tx + px, ty + py);
        g.setStroke(new BasicStroke(1)); g.setColor(new Color(137, 225, 214)); g.drawLine(tx - px, ty - py, tx + px, ty + py);
        rect(g, LIGHT, hx - 1, hy - 1, 3, 3);
    }
    private static void rect(Graphics2D g, Color color, int x, int y, int w, int h) { g.setColor(color); g.fillRect(x, y, w, h); }
}
