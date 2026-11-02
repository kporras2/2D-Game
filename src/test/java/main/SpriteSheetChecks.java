package main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

public class SpriteSheetChecks {
    public static void main(String[] args) throws Exception {
        BufferedImage source = AstronautSprite.generatedSheet();
        if (source.getRGB(0,0) != 0) throw new AssertionError("Export must retain transparency");
        Path file = Files.createTempFile("astronaut-roundtrip-", ".png");
        try {
            ImageIO.write(source, "png", file.toFile());
            BufferedImage loaded = ImageIO.read(file.toFile());
            AstronautSprite fromPng = new AstronautSprite(loaded);
            AstronautSprite original = new AstronautSprite((BufferedImage) null);
            for (int direction=0; direction<4; direction++) for(int action=0;action<3;action++) for(int frame=0;frame<4;frame++) {
                BufferedImage a=render(original,direction,action,frame), b=render(fromPng,direction,action,frame);
                for(int y=0;y<96;y++) for(int x=0;x<96;x++)
                    if(a.getRGB(x,y)!=b.getRGB(x,y)) throw new AssertionError("PNG frame changed during round-trip");
            }
            loaded.setRGB(48, 48*6, 0xffff00ff); // walking right, frame 2 in the editor
            BufferedImage edited=render(new AstronautSprite(loaded),AstronautSprite.RIGHT,AstronautSprite.WALK,1);
            if(edited.getRGB(0,0)!=0xffff00ff) throw new AssertionError("Artist's edited pixel was not used");
            try { new AstronautSprite(new BufferedImage(10,10,BufferedImage.TYPE_INT_ARGB)); throw new AssertionError("Invalid dimensions accepted"); }
            catch(IllegalArgumentException expected) { }
            new AstronautSprite(); // Check the real local/bundled asset as well.
            System.out.println("48 sprite poses round-trip exactly; edited pixels, transparency and dimension validation passed.");
        } finally { Files.deleteIfExists(file); }
    }
    private static BufferedImage render(AstronautSprite sprite,int direction,int action,int frame) {
        BufferedImage image=new BufferedImage(96,96,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();sprite.draw(g,48,64,direction,action,frame);g.dispose();return image;
    }
}
