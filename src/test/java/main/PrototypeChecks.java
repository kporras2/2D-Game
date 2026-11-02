package main;

import java.io.IOException;
import java.nio.file.*;
import java.util.Random;

/** Dependency-free integration checks: execute with scripts/check.sh. */
public class PrototypeChecks {
    private static int checks;
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    private static void tick(GameModel g, int frames, boolean mining) { for (int i=0;i<frames;i++) g.update(.05,0,0,mining); }
    private static void collect(GameModel g, GameModel.Ore ore, int count) {
        for (int i=0;i<count;i++) {
            // Put an ore node within reach; use the actual mining rules to collect it.
            g.x=400; g.y=300; g.rocks.clear(); g.rocks.add(new GameModel.Rock(420,300,ore));
            int before=g.cargo(); tick(g, (int)Math.ceil(ore.seconds/.05)+2, true);
            check(g.cargo()==before+1, "Ore mined into cargo: " + ore);
        }
    }
    private static void process(GameModel g) { g.x=230; g.y=310; g.interact(); }
    private static void build(GameModel g) { g.x=230; g.y=220; check(g.build(), "Construction completed"); }
    public static void main(String[] args) throws Exception {
        GameModel g=new GameModel(new Random(10));
        check(g.rocks.size()==19,"Initial rock field");
        check(!g.build(),"Cannot build remotely");
        g.x=230;g.y=220; check(!g.build(),"Cannot build without supplies");
        collect(g,GameModel.Ore.DUST,6); process(g);
        check(g.credits==30 && g.materials[0]==6 && g.cargo()==0,"Refining yields credits and construction materials");
        build(g); check(g.baseStage==1 && g.capacity()==12 && g.materials[0]==0,"Shelter consumes supplies and upgrades cargo");
        g.x=230;g.y=310;g.upgrade(); check(g.tool==2 && g.credits==0,"First drill upgrade affordable from first expedition");
        collect(g,GameModel.Ore.COPPER,4);process(g);build(g);
        check(g.baseStage==2 && g.credits==8 && g.materials[1]==0,"Solar cost deducted once");
        collect(g,GameModel.Ore.COPPER,6);process(g);
        check(g.credits==94,"Powered processing bonus");g.upgrade();check(g.tool==3,"Prism unlocked");
        collect(g,GameModel.Ore.CRYSTAL,3);process(g);build(g);
        check(g.baseStage==3 && g.materials[2]==0 && g.credits==14,"Chapter completed through collection and processing");
        check(!g.build() && g.credits==14,"Completed construction cannot charge again");
        check(g.objective().contains("COMPLETE"),"Completion objective");
        GameModel starter=new GameModel(new Random(2));starter.x=400;starter.y=300;starter.rocks.clear();starter.rocks.add(new GameModel.Rock(410,300,GameModel.Ore.NOVA));
        tick(starter,80,true);check(starter.cargo()==0,"Drill cannot mine locked ore");
        starter.rocks.clear();starter.rocks.add(new GameModel.Rock(410,300,GameModel.Ore.DUST));starter.inventory[0]=8;
        tick(starter,30,true);check(starter.cargo()==8,"Cargo limit respected");
        starter.x=230;starter.y=310;starter.update(.05,-1,0,false);starter.update(.05,-1,0,false);starter.update(.05,-1,0,false);
        check(starter.x>=209,"Cannot walk through processor");
        starter.x=960;starter.y=640;starter.update(.05,1,1,false);check(starter.x<=936 && starter.y<=570,"World bounds");
        Path dir=Files.createTempDirectory("starfall-check-");Path save=dir.resolve("progress.properties");
        try {
            g.started=true;g.inventory[3]=2;SaveGame.save(g,save);
            GameModel restored=new GameModel(new Random(3));check(SaveGame.load(restored,save),"Save loaded");
            check(restored.baseStage==3 && restored.credits==14 && restored.tool==3 && restored.inventory[3]==2 && restored.started,"Base, tools, credits and cargo survive restart");
            check(java.util.Arrays.equals(g.materials,restored.materials),"Stored materials survive restart");
            Files.writeString(save,"version=1\ncredits=999\ntool=5\n");
            try { SaveGame.load(restored,save);throw new AssertionError("Invalid save accepted"); } catch(IOException expected) { check(restored.credits==14,"Invalid save cannot partially mutate progress"); }
        } finally { Files.deleteIfExists(save);Files.deleteIfExists(dir); }
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            GameModel uiModel = new GameModel(new Random(9));
            GamePanel panel = new GamePanel(uiModel, null);
            try {
                press(panel, java.awt.event.KeyEvent.VK_ENTER); release(panel, java.awt.event.KeyEvent.VK_ENTER);
                check(uiModel.started, "Opening transmission starts the expedition");
                press(panel, java.awt.event.KeyEvent.VK_B); release(panel, java.awt.event.KeyEvent.VK_B);
                check(!panelFlag(panel, "outpostOpen"), "Construction requires proximity to habitat");
                uiModel.x=230; uiModel.y=220; uiModel.materials[0]=6;
                press(panel, java.awt.event.KeyEvent.VK_B);
                check(panelFlag(panel, "outpostOpen"), "B opens construction");
                press(panel, java.awt.event.KeyEvent.VK_B);
                check(panelFlag(panel, "outpostOpen"), "Held B does not repeatedly toggle menu");
                release(panel, java.awt.event.KeyEvent.VK_B);
                press(panel, java.awt.event.KeyEvent.VK_ENTER); release(panel, java.awt.event.KeyEvent.VK_ENTER);
                check(uiModel.baseStage==1 && !panelFlag(panel, "outpostOpen"), "Enter builds and closes plan");
                press(panel, java.awt.event.KeyEvent.VK_J); release(panel, java.awt.event.KeyEvent.VK_J);
                check(panelFlag(panel, "journalOpen"), "Journal accessible");
                press(panel, java.awt.event.KeyEvent.VK_ESCAPE); release(panel, java.awt.event.KeyEvent.VK_ESCAPE);
                check(!panelFlag(panel, "journalOpen"), "Escape closes journal");
            } finally { panel.removeNotify(); }
        });
        System.out.println(checks+" progression, economy, collision, controls and save checks passed.");
    }
    private static void press(GamePanel p, int key) {
        p.getKeyListeners()[0].keyPressed(new java.awt.event.KeyEvent(p, java.awt.event.KeyEvent.KEY_PRESSED, 0, 0, key, java.awt.event.KeyEvent.CHAR_UNDEFINED));
    }
    private static void release(GamePanel p, int key) {
        p.getKeyListeners()[0].keyReleased(new java.awt.event.KeyEvent(p, java.awt.event.KeyEvent.KEY_RELEASED, 0, 0, key, java.awt.event.KeyEvent.CHAR_UNDEFINED));
    }
    private static boolean panelFlag(GamePanel p, String name) {
        try { var field=GamePanel.class.getDeclaredField(name); field.setAccessible(true); return field.getBoolean(p); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

}
