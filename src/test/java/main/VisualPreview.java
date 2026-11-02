package main;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Renders the real sprites and UI for visual inspection without opening a game. */
public class VisualPreview {
    static void flag(GamePanel p,String field,boolean value) throws Exception { Field f=GamePanel.class.getDeclaredField(field);f.setAccessible(true);f.setBoolean(p,value); }
    static void screen(GamePanel p,String name) throws Exception {
        BufferedImage image=new BufferedImage(960,640,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();p.paint(g);g.dispose();ImageIO.write(image,"png",new File("artifacts/"+name+".png"));
    }
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                new File("artifacts").mkdirs();
                GameModel model=new GameModel(new Random(4));GamePanel p=new GamePanel(model,null);p.setSize(960,640);
                screen(p,"intro");flag(p,"intro",false);screen(p,"outpost-start");
                flag(p,"outpostOpen",true);screen(p,"construction");flag(p,"outpostOpen",false);
                model.baseStage=3;model.discoveries=7;model.materials[1]=6;model.tool=3;model.credits=14;model.message="ASTER: That return signal... it is not coming from the sky.";screen(p,"outpost-complete");
                flag(p,"journalOpen",true);screen(p,"journal");p.removeNotify();
                AstronautSprite sprite=new AstronautSprite();
                BufferedImage sheet=new BufferedImage(768,680,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();g.setColor(new Color(16,27,43));g.fillRect(0,0,768,680);
                String[] labels={"FRONT","LEFT","RIGHT","BACK"};
                for(int row=0;row<3;row++) for(int dir=0;dir<4;dir++) {
                    Graphics2D cell=(Graphics2D)g.create();cell.translate(dir*192,row*224);cell.scale(2,2);
                    sprite.draw(cell,48,73,dir,row,row==2?2:1);cell.dispose();
                    g.setColor(new Color(170,207,204));g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12));g.drawString(labels[dir]+" / "+new String[]{"IDLE","WALK","MINE"}[row],dir*192+24,row*224+216);
                }
                g.dispose();ImageIO.write(sheet,"png",new File("artifacts/astronaut-turnaround.png"));
                ImageIO.write(sheet.getSubimage(0,0,768,224),"png",new File("artifacts/astronaut-directions.png"));
            } catch(Exception e) { throw new RuntimeException(e); }
        });
    }
}
