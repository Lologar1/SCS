package me.analyzers.scs.game;

import me.analyzers.scs.panels.BuildingMenu;
import me.analyzers.scs.panels.InventoryMenu;
import me.analyzers.scs.panels.OptionsMenu;
import me.analyzers.scs.panels.SavingMenu;
import me.analyzers.scs.utilities.*;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainPanel extends JPanel {
    //Main class for all display-related stuff (and input management)

    //Constants for mouse-related endeavors
    public static final int LEFT_CLICK = 1;
    public static final int MIDDLE_CLICK = 2;
    public static final int RIGHT_CLICK = 3;

    //Defining basic blocks. Scaling so (maybe) later it's possible to make it bigger ? Not sure.
    public static final int tileSize = 16; //In pixels
    public static final int scaling = 2;
    public static final int realTileSize = tileSize * scaling;

    //Derived constants.
    public static final int inventoryComponentSize = 3*realTileSize;
    public static final int pinSize = realTileSize/6;
    public static final int sixteenth = realTileSize/16;

    //In tiles.
    /*
    Important tile nomenclature as of V0.2
    real : absolute size, in pixels
    snapped : absolute size, in pixels, but snapped to its tile's upper right-hand corner
    tile : snapped then tile-adjusted size

    Normally, we want to use tile positions as much as possible. This includes objects such as components or wires.
     */

    public static int UPS = 10;

    public static final int widthX = 40;
    public static final int widthY = 24;

    //Constant for the inventory directory
    public static final Path inventoryDirectory = Paths.get("inventory");

    public static final int screenWidth = realTileSize * widthX;
    public static final int screenHeight = realTileSize * widthY;

    //Current GameState. Either Building, Inventory, or Saving
    public static GameState gameState = GameState.BUILDING;

    //Current wire size for base primitive components (True, In, etc.), changed by the option handler.
    public static int baseWireSize = 1;

    //Current intervals for primitives splitter and merger (changed by the option handler). Initialized to give first bit only.
    public static int[][] splitterIntervals = new int[][]{new int[]{0, 0}};
    public static int[][] mergerIntervals = new int[][]{new int[]{0, 0}};

    //Holds the current saving menu and its state when in saving mode
    private SavingMenu currentSavingMenu;

    //Holds the current inventory (always reset to top level when going back in)
    private InventoryMenu inventoryMenu;

    //Holds the unique BuildingMenu (saving can wipe it though, but never changes the object itself. It's a singleton.)
    private BuildingMenu buildingMenu = new BuildingMenu();

    //Current options menu, just like buildingMenu it is a singleton
    private final OptionsMenu optionsMenu = new OptionsMenu();

    //Peripherals handling
    KeyPressHandler keyPressHandler = new KeyPressHandler();
    MouseClickHandler mouseClickHandler = new MouseClickHandler();

    public MainPanel() {
        //Mostly for hooking up to pre-existing java swing stuff
        super();
        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.WHITE);
        this.addKeyListener(keyPressHandler);
        this.addMouseListener(mouseClickHandler);
        this.addMouseMotionListener(mouseClickHandler);
        this.setFocusable(true);

        if (!Files.exists(inventoryDirectory)) {
            try {
                Files.createDirectory(inventoryDirectory);
            } catch (IOException e) {
                System.err.println("Error : failed to initialize base inventory directory (" + inventoryDirectory.getFileName() + ")");
            }
        }

        if (!Files.isDirectory(inventoryDirectory)) {
            System.err.println("Error : base inventory path is not a directory (" + inventoryDirectory.getFileName() + ")");
        }
    }

    public void startGameLoop() {
        //Max. UPS is 1 million ticks per second. Also, 1/FPS handle user inputs.
        ScheduledExecutorService tpd = Executors.newSingleThreadScheduledExecutor();
        tpd.scheduleWithFixedDelay(this::update, 0, 1, TimeUnit.MILLISECONDS);
    }

    //Stuff for benchmarking and scheduling ticks/frames

    long lastTickTime = System.nanoTime();
    long lastInfoTime = System.nanoTime();

    int lastDisplay = 0;

    long avg = 0L;
    int c = 0;

    public void tick() {
        //Tick the ACM
        lastTickTime = System.nanoTime();
        try {
            buildingMenu.getGameACM().tick();
        } catch (Exception e) {
            e.printStackTrace();
        }

        avg += System.nanoTime() - lastTickTime;
        c++;
    }

    public void update() {
        //The main game loop, that gets called 1 million times per second.
        if (System.nanoTime() - lastInfoTime >= 5_000_000_000L) {
            //Once per second, display stats about UPS and performance (DEBUG only)
            long leeway = (1_000_000_000/UPS-(avg/c));
            int nanosPerUPS = 1_000_000_000/UPS;

            System.out.println("[DEBUG] Average nanoseconds per update : " + (avg/c) + " with a limit of " + nanosPerUPS + " nanos per update ("
                    +  leeway + " nanoseconds, or " + ((float) leeway/nanosPerUPS) * 100 + "% leeway.");
            lastInfoTime = System.nanoTime();
            avg = 0L;
            c=0;
        }

        if (System.nanoTime()-lastTickTime >= 1_000_000_000/UPS && UPS <= 1000) {
            tick();
        } else if (System.nanoTime()-lastTickTime >= 1_000_000_000/UPS) {
            int ticksToRun = Math.max(UPS/1000, 1);
            for (int i = 0; i < ticksToRun; i++) {
                tick();
            }
        }

        if (lastDisplay < 10) { //100 FPS.
            //Don't handle user inputs if not on a display frame.
            lastDisplay++;
            return;
        }
        lastDisplay = 0;

        //Check for important key presses (changing state) before passing onto specialized handlers.
        int currentKey = keyPressHandler.getCurrentKey();

        if (keyPressHandler.isNew()) {
            //A key got pressed
            switch (currentKey) {
                //ESCAPE always returns to Building mode by default
                case '\u001B' -> {
                    gameState = GameState.BUILDING;
                    keyPressHandler.setNew(false);
                    return;
                }
                case 'e' -> {
                    //Toggle inventory between inventory/building states
                    if (gameState == GameState.BUILDING) {
                        inventoryMenu = new InventoryMenu();
                        gameState = GameState.INVENTORY;
                    } else if (gameState == GameState.INVENTORY){
                        gameState = GameState.BUILDING;
                    } else {
                        break;
                    }
                    keyPressHandler.setNew(false);
                    return;
                }
                case 19 -> { //CONTROL-19 for CTRL + S
                    if (gameState != GameState.BUILDING) {
                        break;
                    }
                    keyPressHandler.setNew(false);
                    //Be sure we're up-to-date
                    buildingMenu.makeNewACM();

                    //Make a new saving menu
                    currentSavingMenu = new SavingMenu(buildingMenu.getPresenceMap(), buildingMenu.getWireQueue(),
                            buildingMenu.getComponentQueue(), buildingMenu.getGameACM());

                    //Open saving menu
                    gameState = GameState.SAVING;
                    return;
                }
                case 'o' -> {
                    //Toggle options between options/building states
                    if (gameState == GameState.BUILDING) {
                        gameState = GameState.OPTIONS;
                    } else if (gameState == GameState.OPTIONS){
                        gameState = GameState.BUILDING;
                    } else {
                        break;
                    }
                    keyPressHandler.setNew(false);
                    return;
                }
            }
        }

        //If not a state-changing input, pass it on to its respective handler

        switch (gameState) {
            case BUILDING -> buildingUpdate();
            case INVENTORY -> {
                try {
                    inventoryUpdate();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
            case SAVING -> savingUpdate();
            case OPTIONS -> optionsUpdate();
            default -> throw new RuntimeException("Illegal game state");
        }

        //Refresh the screen by re-displaying stuff on screen.
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        //Not quite sure what this does. Just plugging into Swing's API before building my own rendering on top.
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        //All graphics logic goes here !
        switch (gameState) {
            //mouseHandler for drawing ghost.
            case BUILDING -> buildingMenu.buildingGraphics(g2d, mouseClickHandler);

            //Inventory displaying.
            case INVENTORY -> inventoryMenu.render(g2d);

            //Interactive complex component construction ! Yay !
            case SAVING -> currentSavingMenu.render(g2d);

            //InteractiveTextFields for various options
            case OPTIONS -> optionsMenu.render(g2d);
        }

        //Really not sure why I need this. But there is stutter without it. Swing is weird.
        getToolkit().sync();
    }

    public void inventoryUpdate() throws IOException {
        //Test if a component was successfully chosen, and return to building state.
        inventoryMenu.update(mouseClickHandler, keyPressHandler, buildingMenu);
    }

    public void buildingUpdate() {
        //See if the map was modified, and recalculate the ACM if so.
        //Merely changing elements (e.g. wire orientations) will not trip this, which is why we have forceNewACM.
        int presenceSumHash = Arrays.deepHashCode(buildingMenu.getPresenceMap());

        buildingMenu.forceNewACM = false;

        //Check for user inputs on BUILDING mode.
        try {
            buildingMenu.update(mouseClickHandler, keyPressHandler);
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }

        if (buildingMenu.getGameACM() == null || presenceSumHash != Arrays.deepHashCode(buildingMenu.getPresenceMap())
                || buildingMenu.forceNewACM) {
            buildingMenu.makeNewACM();
        }
        //ACM ticks itself in buildingMenu.
    }

    public void savingUpdate() {
        //True : saved
        if (currentSavingMenu.update(mouseClickHandler, keyPressHandler)) {
            gameState = GameState.BUILDING;
            //Empty board !
            buildingMenu = new BuildingMenu();
        }
    }

    public void optionsUpdate() {
        optionsMenu.update(mouseClickHandler, keyPressHandler);
    }
}
