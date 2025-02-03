package main;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {

    //Screen settings
    final int originalTitleSIze = 16; // 16x16
    final int scale = 3;

    //Tile size is scaled due to new screen resolutions being much larger in modern days
    final int tileSize= originalTitleSIze * scale; // 48x48
    final int maxScreenCol = 16;
    final int maxScreenRow = 12;
    final int screenWidth = tileSize * maxScreenCol; // 768 pixels (48*16)
    final int screenHeight = tileSize * maxScreenRow; // 576 pixels (48*12)


    public GamePanel(){

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
    }
}
