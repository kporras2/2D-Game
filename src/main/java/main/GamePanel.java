package main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

public class GamePanel extends JPanel {
    private final GameModel game;
    private final AstronautSprite astronaut = new AstronautSprite();
    private final Path savePath;
    private final Timer timer;
    private double saveTimer;
    private boolean outpostOpen, journalOpen, saveFailed;
    private final boolean[] keys = new boolean[256];
    private final int[][] stars = new int[100][3];
    private long previous = System.nanoTime();
    private boolean intro = true;
    private boolean walking, mining;
    // 0 = down, 1 = left, 2 = right, 3 = up.
    private int facing = 0;
    private static final Color[] ORE = {new Color(173,190,203), new Color(241,151,92), new Color(120,219,211), new Color(206,146,255)};
    public GamePanel() { this(new GameModel(), SaveGame.defaultPath()); }
    GamePanel(GameModel model, Path savePath) {
        this.game = model; this.savePath = savePath;
        if (savePath != null) {
            try { SaveGame.load(game, savePath); }
            catch (IOException e) { saveFailed = true; game.message = "Save could not be loaded. Existing save will be preserved."; game.messageTime = 12; }
        }
        intro = !game.started;
        setPreferredSize(new Dimension(GameModel.WIDTH, GameModel.HEIGHT));
        setBackground(new Color(12,19,35)); setFocusable(true);
        Random random = new Random(42);
        for (int[] star : stars) { star[0] = random.nextInt(960); star[1] = 95 + random.nextInt(545); star[2] = random.nextInt(3)+1; }
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k < 0 || k >= keys.length || keys[k]) return;
                keys[k] = true;
                if (intro) {
                    if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) { intro = false; game.started = true; clearExcept(k); saveProgress(); }
                    return;
                }
                if (outpostOpen || journalOpen) {
                    if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_B || k == KeyEvent.VK_J) { outpostOpen = journalOpen = false; clearExcept(k); }
                    else if (k == KeyEvent.VK_ENTER && outpostOpen && game.canBuild()) { game.build(); outpostOpen = false; clearExcept(k); saveProgress(); }
                    return;
                }
                if (k == KeyEvent.VK_E) { game.interact(); saveProgress(); }
                if (k == KeyEvent.VK_U) { game.upgrade(); saveProgress(); }
                if (k == KeyEvent.VK_B) {
                    if (game.atBase()) { outpostOpen = true; clearExcept(k); }
                    else { game.message = "Move near the abandoned habitat above the processor, then press B."; game.messageTime = 4; }
                }
                if (k == KeyEvent.VK_J) { journalOpen = true; clearExcept(k); }
            }
            public void keyReleased(KeyEvent e) { if (e.getKeyCode() < keys.length) keys[e.getKeyCode()] = false; }
        });
        addFocusListener(new FocusAdapter() { public void focusLost(FocusEvent e) { java.util.Arrays.fill(keys, false); } });
        timer = new Timer(16, e -> { long now = System.nanoTime(); double dt = (now-previous)/1e9; previous = now;
            if (intro || outpostOpen || journalOpen || !hasFocus()) { walking = mining = false; repaint(); return; }
            double dx = axis(KeyEvent.VK_D, KeyEvent.VK_RIGHT, KeyEvent.VK_A, KeyEvent.VK_LEFT), dy = axis(KeyEvent.VK_S, KeyEvent.VK_DOWN, KeyEvent.VK_W, KeyEvent.VK_UP);
            walking = dx != 0 || dy != 0;
            if (walking) facing = Math.abs(dx) > Math.abs(dy) ? (dx < 0 ? 1 : 2) : (dy < 0 ? 3 : 0);
            mining = keys[KeyEvent.VK_SPACE] && game.nearestRock() != null && game.nearestRock().ore.tier <= game.tool && game.cargo() < game.capacity();
            if (mining) { GameModel.Rock target = game.nearestRock(); double tx = target.x - game.x, ty = target.y - game.y; facing = Math.abs(tx) > Math.abs(ty) ? (tx < 0 ? 1 : 2) : (ty < 0 ? 3 : 0); }
            game.update(dt, dx, dy, keys[KeyEvent.VK_SPACE]);
            saveTimer += dt; if (saveTimer >= 15) { saveProgress(); saveTimer = 0; }
            repaint(); });
        timer.start();
    }
    private void clearExcept(int key) { java.util.Arrays.fill(keys, false); keys[key] = true; }
    public void saveProgress() {
        if (savePath == null || saveFailed) return;
        try { SaveGame.save(game, savePath); }
        catch (IOException e) { saveFailed = true; game.message = "Saving unavailable. This session is still playable."; game.messageTime = 10; }
    }
    @Override public void removeNotify() { timer.stop(); super.removeNotify(); }
    private int axis(int plus, int plus2, int minus, int minus2) { return (keys[plus] || keys[plus2] ? 1 : 0) - (keys[minus] || keys[minus2] ? 1 : 0); }
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics); Graphics2D g = (Graphics2D) graphics.create();
        double scale = Math.min(getWidth()/960.0, getHeight()/640.0);
        g.translate((getWidth()-960*scale)/2, (getHeight()-640*scale)/2); g.scale(scale, scale);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(new Color(16,27,43)); g.fillRect(0,0,960,640);
        g.setColor(new Color(29,46,60)); for (int[] s : stars) g.fillRect((s[0]/4)*4,(s[1]/4)*4,4,4);
        for (int i=0;i<26;i++) { int cx = 250 + (i*137)%680, cy = 120 + (i*83)%450; g.setColor(new Color(23,38,51)); g.fillRect(cx,cy,40,4); g.fillRect(cx-8,cy+4,56,4); g.fillRect(cx,cy+8,40,4); }
        habitat(g);
        station(g);
        for (GameModel.Rock rock : game.rocks) rock(g, rock);
        player(g);
        hud(g); if (intro) intro(g); else if (outpostOpen || journalOpen) outpost(g); g.dispose();
    }
    private void habitat(Graphics2D g) {
        boolean sealed = game.baseStage >= 1, powered = game.baseStage >= 2;
        g.setColor(new Color(9,19,29)); g.fillRect(58,190,168,22);
        g.setColor(new Color(56,77,86)); g.fillRect(66,194,152,12);
        g.setColor(new Color(45,64,77)); g.fillRect(74,140,136,56); g.fillRect(86,124,112,72); g.fillRect(106,116,72,80);
        g.setColor(new Color(111,145,145)); g.fillRect(78,142,128,50); g.fillRect(90,128,104,64); g.fillRect(106,120,72,72);
        if (sealed) {
            g.setColor(new Color(160,184,172)); g.fillRect(82,143,120,42); g.fillRect(94,132,96,53); g.fillRect(108,124,68,61);
            g.setColor(new Color(196,209,186)); g.fillRect(108,124,68,4); g.fillRect(94,132,12,4);
            g.setColor(new Color(99,128,135)); g.fillRect(100,139,4,48); g.fillRect(180,139,4,48);
            g.setColor(new Color(21,44,60)); g.fillRect(110,143,28,20); g.fillRect(146,143,28,20);
            g.setColor(powered ? new Color(131,229,202) : new Color(51,89,108));
            g.fillRect(114,147,20,12); g.fillRect(150,147,20,12);
            g.setColor(new Color(32,54,67)); g.fillRect(128,169,28,31);
            g.setColor(new Color(81,113,122)); g.fillRect(132,173,20,25);
            g.setColor(powered ? new Color(245,205,122) : new Color(81,113,122)); g.fillRect(137,177,10,6);
            g.setColor(new Color(36,68,78)); g.fillRect(144,187,3,3);
        } else {
            g.setColor(new Color(22,36,49)); g.fillRect(88,143,108,49); g.fillRect(102,132,80,60);
            g.setColor(new Color(101,135,137)); g.fillRect(100,132,5,61); g.fillRect(178,132,5,61); g.fillRect(138,122,5,71);
            g.setColor(new Color(167,127,83)); g.fillRect(80,180,42,5); g.fillRect(166,190,30,5);
        }
        if (powered) {
            g.setColor(new Color(83,121,135)); g.fillRect(23,166,34,6); g.fillRect(36,172,6,24);
            g.setColor(new Color(31,69,100)); g.fillRect(17,141,46,27);
            g.setColor(new Color(98,167,183)); g.drawRect(17,141,46,27);
            for (int i=1;i<4;i++) g.drawLine(17+i*11,141,17+i*11,168);
            g.drawLine(17,154,63,154);
            g.setColor(new Color(215,169,94)); g.drawLine(42,197,72,197);
        }
        if (game.baseStage >= 3) {
            g.setColor(new Color(109,153,163)); g.fillRect(222,125,4,73); g.fillRect(216,195,16,5);
            g.setColor(new Color(139,227,209)); g.fillRect(218,117,12,8); g.fillRect(221,112,6,5);
            if ((int)game.elapsed % 2 == 0) { g.drawRect(214,111,20,17); g.drawRect(210,107,28,25); }
        }
        text(g,sealed ? "YOUR OUTPOST" : "ABANDONED HABITAT",142,108,10,new Color(176,205,193),true);
        if (game.atBase()) text(g,"B  OUTPOST",142,226,11,new Color(241,207,138),true);
    }
    private void supplies(Graphics2D g) {
        g.setColor(new Color(8,16,28,220)); g.fillRect(18,455,215,125);
        text(g,"OUTPOST SUPPLIES",30,476,11,new Color(142,203,192),false);
        for (int i=0;i<4;i++) {
            GameModel.Ore ore = GameModel.Ore.values()[i];
            String label = (game.discoveries & (1 << i)) != 0 || game.materials[i] > 0 ? ore.product.toUpperCase() : "UNDISCOVERED";
            text(g,label,30,497+i*22,10,ORE[i],false);
            text(g,""+game.materials[i],199,497+i*22,11,Color.WHITE,false);
        }
    }
    private void outpost(Graphics2D g) {
        g.setColor(new Color(4,10,20,224)); g.fillRect(0,0,960,640);
        g.setColor(new Color(95,163,158)); g.fillRect(170,109,620,434);
        g.setColor(new Color(15,29,43)); g.fillRect(174,113,612,426);
        text(g,journalOpen ? "ASTER / FIELD JOURNAL" : "YOUR OUTPOST / CONSTRUCTION",204,148,12,new Color(135,223,196),false);
        text(g,journalOpen ? "A place to call home" : game.projectName(),204,184,24,Color.WHITE,false);
        if (journalOpen) {
            text(g,"The storm stranded us. This abandoned shelter is our second chance.",204,222,13,new Color(200,216,210),false);
            text(g,"01  Shelter: moonstone can be fired into airtight ceramic panels.",204,267,13,Color.WHITE,false);
            text(g,"02  Power: copper becomes wire for the old solar array.",204,299,13,Color.WHITE,false);
            text(g,"03  Signal: prism becomes the lens of a survey beacon.",204,331,13,Color.WHITE,false);
            text(g,game.baseStage == 3 ? "Beacon log: a faint signal is coming from beneath the moon." : "ASTER: Every rock is the beginning of something we can build.",204,380,13,new Color(245,205,122),false);
            text(g,"Projects complete: " + game.baseStage + " / 3",204,421,14,new Color(135,223,196),false);
        } else if (game.baseStage < 3) {
            String[] reason = {"The habitat walls are cracked. Ceramic panels will make it airtight.", "The shelter is sound. Now bring its solar array and lights online.", "Use prism lenses to focus a signal through the moon's interference."};
            String[] effect = {"Unlocks a cargo harness: carry 12 rocks instead of 8.", "Powers the processor: earn 20% more credits on every load.", "Completes Chapter 01 and reveals the next mystery."};
            text(g,reason[game.baseStage],204,224,13,new Color(200,216,210),false);
            text(g,"REQUIRED / IN STORAGE",204,269,11,new Color(144,174,182),false);
            text(g,game.materialCost()+" "+GameModel.Ore.values()[game.baseStage].product+"  /  "+game.materials[game.baseStage]+" stored",204,299,17,Color.WHITE,false);
            text(g,game.buildingCost()+" credits  /  "+game.credits+" available",204,327,17,Color.WHITE,false);
            text(g,effect[game.baseStage],204,371,13,new Color(245,205,122),false);
            text(g,"Processing stores one material per rock AND earns credits.",204,411,12,new Color(144,174,182),false);
            text(g,game.canBuild() ? "ENTER  BUILD" : "Gather the missing supplies, then return here.",204,463,15,game.canBuild() ? new Color(135,223,196) : new Color(245,205,122),false);
        } else {
            text(g,"Habitat sealed. Solar array running. Survey beacon transmitting.",204,237,14,new Color(200,216,210),false);
            text(g,"You came here as a stranded prospector. Now you have a home.",204,282,14,Color.WHITE,false);
            text(g,"ASTER: That return signal... it is not coming from the sky.",204,330,14,new Color(245,205,122),false);
            text(g,"CHAPTER 01 COMPLETE",204,390,17,new Color(135,223,196),false);
            text(g,"Keep mining and upgrading. The next chapter is still to come.",204,428,13,new Color(144,174,182),false);
        }
        text(g,"ESC  CLOSE",204,512,11,new Color(144,174,182),false);
    }
    private void station(Graphics2D g) {
        int x=85,y=245;
        g.setColor(new Color(7,16,26)); g.fillRect(x-8,y+10,128,105);
        g.setColor(new Color(54,83,88)); g.fillRect(x,y,112,104);
        g.setColor(new Color(102,139,132)); g.fillRect(x+8,y-8,96,12); g.fillRect(x+8,y+8,96,76);
        g.setColor(new Color(31,48,59)); g.fillRect(x+16,y+16,80,48);
        g.setColor(new Color(64,191,155)); g.fillRect(x+24,y+24,64,32);
        g.setColor(new Color(135,241,192)); g.fillRect(x+32,y+32,24,8); g.fillRect(x+64,y+32,16,8);
        g.setColor(new Color(220,177,105)); g.fillRect(x+24,y+72,64,8);
        g.setColor(new Color(34,53,65)); g.fillRect(x+16,y+88,80,8);
        text(g,"PROCESSOR",145,379,12,new Color(140,202,186),true);
        if (game.atStation()) text(g,"E  PROCESS   /   U  UPGRADE",145,400,11,Color.WHITE,true);
    }
    private void rock(Graphics2D g, GameModel.Rock r) {
        int x=(int)r.x,y=(int)r.y; Color c=ORE[r.ore.color];
        g.setColor(new Color(6,15,25)); g.fillRect(x-20,y-8,40,24); g.fillRect(x-12,y-16,24,32);
        g.setColor(c.darker().darker()); g.fillRect(x-16,y-8,32,20); g.fillRect(x-8,y-12,20,28);
        g.setColor(c.darker()); g.fillRect(x-12,y-12,20,20); g.fillRect(x-16,y-4,28,8);
        g.setColor(c); g.fillRect(x-8,y-12,12,8); g.fillRect(x-12,y-4,20,8);
        g.setColor(c.brighter()); g.fillRect(x-8,y-8,8,4);
        if (r == game.nearestRock()) {
            g.setColor(Color.WHITE); g.drawRect(x-24,y-23,48,44);
            String hint = r.ore.tier > game.tool ? "DRILL " + r.ore.tier + " REQUIRED" : game.cargo() >= game.capacity() ? "CARGO FULL" : "HOLD SPACE";
            text(g,hint,x,y-34,10,Color.WHITE,true);
            if (r.progress > 0) { g.setColor(new Color(19,27,39)); g.fillRect(x-25,y+23,50,6); g.setColor(new Color(117,244,192)); g.fillRect(x-25,y+23,(int)(50*r.progress/r.ore.seconds),6); }
        }
    }
    private void player(Graphics2D g) {
        int action = mining ? AstronautSprite.MINE : walking ? AstronautSprite.WALK : AstronautSprite.IDLE;
        int frame = action == AstronautSprite.IDLE ? 0 : (int)(game.elapsed * (mining ? 12 : 9)) % 4;
        astronaut.draw(g, (int)game.x, (int)game.y, facing, action, frame);
        GameModel.Rock target = game.nearestRock();
        if (mining && target != null && frame == 2) {
            g.setColor(new Color(232,225,172));
            g.fillRect((int)target.x-7,(int)target.y-22,3,7);
            g.fillRect((int)target.x+12,(int)target.y-14,6,3);
        }
    }
    private void intro(Graphics2D g) {
        g.setColor(new Color(4,10,20,220)); g.fillRect(0,0,960,640);
        g.setColor(new Color(104,178,167)); g.fillRect(164,108,632,426);
        g.setColor(new Color(15,29,43)); g.fillRect(170,114,620,414);
        text(g,"CHAPTER 01 / A PLACE TO CALL HOME",480,160,12,new Color(112,219,190),true);
        text(g,"STARFALL SALVAGE",480,202,27,Color.WHITE,true);
        g.setColor(new Color(112,219,190)); g.fillRect(420,220,120,4);
        text(g,"The storm took your ship. Your survey core survived.",480,263,16,Color.WHITE,true);
        text(g,"ASTER has found an abandoned habitat on this moon.",480,292,16,Color.WHITE,true);
        text(g,"ASTER: Moonstone makes ceramic. Copper carries power.",480,333,15,new Color(190,210,211),true);
        text(g,"Seal the shelter. Restore power. Make this place yours.",480,359,15,new Color(190,210,211),true);
        text(g,"WASD / ARROWS  MOVE     SPACE  MINE",480,416,12,new Color(249,208,123),true);
        text(g,"E  PROCESS    U  UPGRADE    B  OUTPOST    J  JOURNAL",480,439,12,new Color(249,208,123),true);
        text(g,"PRESS ENTER TO BEGIN",480,492,16,new Color(112,219,190),true);
    }
    private void hud(Graphics2D g) {
        g.setColor(new Color(8,16,28,235)); g.fillRect(0,0,960,90);
        text(g,"STARFALL",24,29,19,Color.WHITE,false); text(g,"SALVAGE",24,48,11,new Color(112,219,190),false);
        text(g,"CREDITS",205,26,10,new Color(145,165,174),false); text(g,""+game.credits,205,52,22,new Color(249,208,123),false);
        text(g,"CARGO",320,26,10,new Color(145,165,174),false); text(g,game.cargo()+" / "+game.capacity()+"   ·   "+game.processingValue()+" CR",320,50,16,Color.WHITE,false);
        text(g,"DRILL TIER",530,26,10,new Color(145,165,174),false); text(g,""+game.tool+" / 4",530,50,16,Color.WHITE,false);
        text(g,game.tool<4 ? "NEXT UPGRADE  "+game.upgradeCost()+" CR" : "DRILL MAXED",690,35,12,new Color(112,219,190),false);
        text(g,"WASD / ARROWS  MOVE     SPACE  MINE",690,56,10,new Color(165,183,190),false);
        text(g,game.objective(),24,78,11,new Color(165,211,204),false);
        supplies(g);
        g.setColor(new Color(8,16,28,225)); g.fillRoundRect(16,594,928,33,12,12);
        String msg = game.messageTime > 0 ? game.message : "E  Process   U  Drill upgrade   B  Outpost (at habitat)   J  Journal";
        text(g,msg,30,615,12,Color.WHITE,false);
        if (saveFailed) text(g,"SAVE UNAVAILABLE",925,585,10,new Color(249,177,123),true);
        if (game.atStation()) {
            String upgrade = game.tool < 4 ? "NEXT: " + GameModel.Ore.values()[game.tool].label + " drill · " + game.upgradeCost() + " credits" : "All drill tiers unlocked";
            text(g,upgrade,145,420,10,new Color(249,208,123),true);
        }
    }
    private void text(Graphics2D g,String s,int x,int y,int size,Color color,boolean centered) {
        g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,size)); g.setColor(color);
        if (centered) x -= g.getFontMetrics().stringWidth(s)/2; g.drawString(s,x,y);
    }
}
