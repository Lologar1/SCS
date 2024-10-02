package me.analyzers.scs.game;

import me.analyzers.scs.primitiveComponents.*;
import me.analyzers.scs.utilities.*;
import org.codehaus.groovy.util.HashCodeHelper;

import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

import static me.analyzers.scs.utilities.MathUtils.*;

public class MainPanel extends JPanel implements ActionListener{
    //Main class for all display-related stuff (and input management)

    //Refresh rate
    public static final int FPS = 120;

    //Constants for mouse-related endeavors
    public static final int LEFT_CLICK = 1;
    public static final int MIDDLE_CLICK = 2;
    public static final int RIGHT_CLICK = 3;

    //Defining basic blocks. Scaling so (maybe) later it's possible to make it bigger ? Not sure.
    public static final int tileSize = 16; //In pixels
    public static final int scaling = 2;
    public static final int realTileSize = tileSize * scaling;

    //In tiles.
    /*
    Important tile nomenclature as of V0.2
    real : absolute size, in pixels
    snapped : absolute size, in pixels, but snapped to its tile's upper right-hand corner
    tile : snapped then tile-adjusted size

    Normally, we want to use tile positions as much as possible. This includes objects such as components or wires.
     */
    public static final int widthX = 60;
    public static final int widthY = 32;
    public static final int screenWidth = realTileSize * widthX;
    public static final int screenHeight = realTileSize * widthY;

    //Current GameState. Either Building, Inventory, or Saving
    public static GameState gameState = GameState.BUILDING;

    //(Building) Current held component data
    private Rotation currentRotation = Rotation.NORTH;
    //ComponentHolder object as template to the one we're using. Position is inconsequential.
    private ComponentHolder currentComponent = new PrimitiveMerger(null, 8, currentRotation);
    private int currentComponentWireSize = 8;

    //For updates that don't change the board (i.e. single-wire redirection)
    private boolean forceNewACM = false;

    //ACM representing the board. Updated on presenceMap modification.
    private AbstractComponentMap gameACM;

    //Current state of the drawn board.
    //presenceMap holds all Placeables according to their position, even for ones spanning multiple tiles. Null for air.
    //componentQueue is a disordered list of all drawn components
    //wireQueue is a disordered list of all drawn wires (separately, not inside groups)

    private final Placeable[][] presenceMap = new Placeable[widthX][widthY];
    public LinkedList<ComponentHolder> componentQueue = new LinkedList<>();
    public LinkedList<Wire> wireQueue = new LinkedList<>();

    //Contains all components in the inventory.
    //TODO : an InventoryComponent can either be a directory (redirects to more) or a component (either Primitive or ACM-driven)
    public ArrayList<InventoryComponent> inventoryComponents = new ArrayList<>();

    //Peripherials handling
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
    }

    public void startGameLoop() {
        //Every 1/FPS second, call this object's actionPerformed() method (as implemented in the ActionListener interface)
        Timer timer = new Timer(1000/FPS, this);
        timer.start();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //The main game loop, that gets called once per 1/FPS second. (Default: 120)
        switch (gameState) {
            case BUILDING -> {
                //See if the map was modified, and recalculate the ACM if so.
                //Merely changing elements (e.g. wire orientations) will not trip this, which is why we have forceNewACM.
                int presenceSumHash = Arrays.deepHashCode(presenceMap);

                forceNewACM = false;

                //Check for user inputs on BUILDING mode.
                try {
                    update();
                } catch (CloneNotSupportedException ex) {
                    throw new RuntimeException(ex);
                }

                if (gameACM == null || presenceSumHash != Arrays.deepHashCode(presenceMap) || forceNewACM) {
                    makeNewACM();
                }

                //Tick the ACM. Tick handles itself automatically as to not tick once per frame.
                //Warning: the tick delay should share a common multiple with the FPS, otherwise it might not be exact !
                //TODO: change this, perhaps ? Another thread ?
                tick();

            }
            case INVENTORY -> inventoryUpdate();
            case SAVING -> savingUpdate();
            default -> throw new RuntimeException("Illegal game state");
        }

        //Refresh the screen by re-displaying stuff on screen.
        repaint();
    }

    public void update() throws CloneNotSupportedException {
        //Get key that is being pressed. 0 if no key is pressed.
        char currentKey = keyPressHandler.getCurrentKey();

        //Last click position handling IMPORTANT: getting the click is really just the last time the mouse was pressed down
        //To get current mouse position, refer to MouseClickHandler.getHoveringPosition()
        int[] realPressLocation = new int[]{mouseClickHandler.getLastClickX(), mouseClickHandler.getLastClickY()};
        int[] snappedLocation = new int[]{convertToSnappedNotation(realPressLocation[0]),
                convertToSnappedNotation(realPressLocation[1])};
        int tileX = snappedLocation[0]/realTileSize;
        int tileY = snappedLocation[1]/realTileSize;

        if (mouseClickHandler.isNew()) { //isNew is only true on a new mouse click.
            int clickType = mouseClickHandler.getType(); //RIGHT, LEFT, or MIDDLE click mostly.

            //A middle or left click might be in the middle of making a wireLine, and a right click might be in the middle of deleting one.
            //In both the latter cases, we want to keep querying until the user stops holding down the button so we can handle that.

            if (clickType == MIDDLE_CLICK && !mouseClickHandler.isDragging()
                    || clickType == LEFT_CLICK && keyPressHandler.isShiftDown()) {

                //Click type was wire, and we stopped making the line. Alternatively, we used the pipette tool.
                if (clickType == LEFT_CLICK && mouseClickHandler.isDragging()) {
                    //We'll just wait, and prevent the left click from getting caught later on as a component placing/interaction
                    return;
                }

                mouseClickHandler.setNew(false);
                //Pipette tool handling. Omit left-clicks.
                Placeable placeable = presenceMap[tileX][tileY]; //The placeable we might've clicked, or null.
                if (placeable instanceof ComponentHolder && clickType == MIDDLE_CLICK) {
                    //Valid component; pipette it !
                    currentComponent = ((ComponentHolder) placeable).clone(); //Set a new template. Again, pos is inconsequential.
                    setCurrentRotation(currentComponent.getRotation()); //Set rotation to the pipetted component's
                    currentComponentWireSize = currentComponent.getWireSize(); //Set wireSize to the pipetted component's
                    //We may only handle one event per loop, so we exit.
                    return;
                }

                //Try to create a wireLine
                int[] positionFirst = mouseClickHandler.getPressFirstPosition(); //This is equal to getLastClickX and Y
                int[] positionLast = mouseClickHandler.getPressReleasePosition();//This is the actual, current mouse cursor when releasing

                //Make the wireLine's position line iterator. Because this is dealing with input from the mouse, positions are absolute.
                PositionLineIterator positionLineIterator = new PositionLineIterator(positionFirst, positionLast);
                if (!positionLineIterator.isValid() || positionLineIterator.getLength() < 2) {
                    //Not straight, and at least 2 must be selected !
                    return;
                }

                //Test if both ends exist
                boolean existsFirst = presenceMap[convertToTileNotation(positionFirst[0])][convertToTileNotation(positionFirst[1])] != null;
                boolean existsLast = presenceMap[convertToTileNotation(positionLast[0])][convertToTileNotation(positionLast[1])] != null;

                //The actual wireLine
                LinkedList<Wire> toAdd = new LinkedList<>();

                Rotation direction = positionLineIterator.getDirection();

                //Omit first, as first and last are dealt with specially (autosnap)
                positionLineIterator.next();

                while (positionLineIterator.hasNext()) {
                    if (positionLineIterator.getLength() == 1) {
                        break; //Omit last for the same reason as above
                    }

                    int[] position = positionLineIterator.next();

                    //Add middle wire to the wireLine
                    Wire justPut = new Wire(position, complement(direction), direction);

                    //Test presence of a Placeable where the wireLine is being layed down
                    int[] justPutPosition = justPut.getSnappedPosition();
                    Placeable potentialPartner = presenceMap[convertToTileNotation(justPutPosition[0])]
                            [convertToTileNotation(justPutPosition[1])];

                    if (potentialPartner != null) {
                        //Test for making a new Crossover instead of returning. Requirements:
                        //Is a complete 2-way wire which does not bend (isMutualComplement)
                        //Is not in the same direction as the wireLine being made (noneMatch rotation partners)

                        //TODO : autosnap to stub wires
                        if (potentialPartner instanceof Wire && isMutualComplement(((Wire) potentialPartner).getPartners())
                            && ((Wire) potentialPartner).getPartners().stream().noneMatch(r -> justPut.getPartners().contains(r))) {

                            ((Wire) potentialPartner).setCrossover(true);
                            ((Wire) potentialPartner).setShowConnected(false);
                            ((Wire) potentialPartner).getPartners().addAll(Arrays.stream(Rotation.values()).toList());
                            //Set the crossover to be: a crossover, don't show connected (little dot), and has all partners.
                        } else {
                            //A componentHolder, or a wireLine ending. Don't crossover then; abort.
                            return;
                        }
                    } else {
                        //Nothing there.
                        toAdd.add(justPut);
                    }
                }

                int[] snappedPositionFirst = positionLineIterator.getSnappedPositionFirst();
                if (existsFirst) {
                    //Check for adjusting for the first end.
                    Placeable wireToAdjust = presenceMap[convertToTileNotation(snappedPositionFirst[0])]
                            [convertToTileNotation(snappedPositionFirst[1])];
                    if (wireToAdjust instanceof Wire && !((Wire) wireToAdjust).isCrossover()) {
                        //There is a wire at posFirst, and it's not a crossover.
                        //Crossovers handle themselves, as their natural rotations will match with this wireLine.
                        //Simply add the proper direction to connect it with the rest of the wireLine.
                        ((Wire) wireToAdjust).setShowConnected(true);
                        ((Wire) wireToAdjust).getPartners().add(direction);
                    }
                } else {
                    //Check for snapping to component I/O
                    //Getting a unique set of all close by components to snap to
                    Set<ComponentHolder> closeComponents = new HashSet<>();
                    int[] tilePositionFirst = new int[]{convertToTileNotation(snappedPositionFirst[0]),
                            convertToTileNotation(snappedPositionFirst[1])};

                    for (Rotation r : Rotation.values()) {
                        int[] cardinal = toCardinalDirection(r);
                        int i = cardinal[0];
                        int j = cardinal[1];
                        if (tilePositionFirst[0]+i > widthX-1 || tilePositionFirst[0]+i < 0
                                || tilePositionFirst[1]+j > widthY-1 || tilePositionFirst[1]+j < 0) {
                            //Trying to test componentHolder out of bounds of presenceMap; it's not there.
                            continue;
                        }
                        if (presenceMap[tilePositionFirst[0]+i][tilePositionFirst[1]+j] instanceof ComponentHolder) {
                            closeComponents.add((ComponentHolder) presenceMap[tilePositionFirst[0]+i][tilePositionFirst[1]+j]);
                        }

                    }
                    boolean didSnapToComponent = false;
                    for (ComponentHolder componentHolder : closeComponents) {
                        //Since there is nothing, create the wire as a stub.
                        Wire firstPositionWire = new Wire(snappedPositionFirst, direction);
                        Rotation connect = snapToComponentIO(componentHolder, firstPositionWire);
                        if (connect==null) {
                            //Nothing to snap to.
                            continue;
                        }
                        //Don't add the fallback stub wire at the end
                        didSnapToComponent = true;
                        firstPositionWire.getPartners().add(connect);
                        toAdd.add(firstPositionWire);
                    }
                    if (!didSnapToComponent) {
                        //Add a fallback stub wire, since it did not connect to any components.
                        toAdd.add(new Wire(snappedPositionFirst, direction));
                    }
                }

                int[] snappedPositionLast = positionLineIterator.getSnappedPositionLast();
                if (existsLast) {
                    //Check for adjusting for the last end.
                    Placeable wireToAdjust = presenceMap[convertToTileNotation(snappedPositionLast[0])]
                            [convertToTileNotation(snappedPositionLast[1])];
                    if (wireToAdjust instanceof Wire && !((Wire) wireToAdjust).isCrossover()) {
                        ((Wire) wireToAdjust).setShowConnected(true);
                        //Since it is last, add the complement instead !
                        ((Wire) wireToAdjust).getPartners().add(complement(direction));
                    }
                } else {
                    Set<ComponentHolder> closeComponents = new HashSet<>();
                    int[] tilePositionLast = new int[]{convertToTileNotation(snappedPositionLast[0]),
                            convertToTileNotation(snappedPositionLast[1])};
                    for (Rotation r : Rotation.values()) {
                        int[] cardinal = toCardinalDirection(r);
                        int i = cardinal[0];
                        int j = cardinal[1];
                        if (tilePositionLast[0]+i > widthX-1 || tilePositionLast[0]+i < 0
                                || tilePositionLast[1]+j > widthY-1 || tilePositionLast[1]+j < 0) {
                            continue;
                        }
                        if (presenceMap[tilePositionLast[0]+i][tilePositionLast[1]+j] instanceof ComponentHolder) {
                            closeComponents.add((ComponentHolder) presenceMap[tilePositionLast[0]+i][tilePositionLast[1]+j]);
                        }
                    }
                    boolean didSnapToComponent = false;
                    for (ComponentHolder componentHolder : closeComponents) {
                        Wire lastPositionWire = new Wire(snappedPositionLast, complement(direction));
                        Rotation connect = snapToComponentIO(componentHolder, lastPositionWire);
                        if (connect==null) {
                            //Nothing to snap to.
                            continue;
                        }
                        //Don't add the fallback stub wire at the end
                        didSnapToComponent = true;
                        lastPositionWire.getPartners().add(connect);
                        toAdd.add(lastPositionWire);
                    }
                    if (!didSnapToComponent) {
                        //Add a fallback stub wire, since it did not connect to any components.
                        toAdd.add(new Wire(snappedPositionLast, complement(direction)));
                    }
                }

                if (existsLast || existsFirst) {
                    //For single-wire redirection
                    forceNewACM = true;
                }

                for (Wire wire : toAdd) {
                    //Reaching this point, we commit to changing the board state with our new wireLine.
                    int[] position = wire.getSnappedPosition();
                    presenceMap[convertToTileNotation(position[0])][convertToTileNotation(wire.getSnappedPosition()[1])] = wire;
                }
                //Adding our wires to the wireQueue
                wireQueue.addAll(toAdd);

                //Cull and maintain wires.
                wireQueue.forEach(this::cleanupWire);

                //TODO: Legacy and pending discontinuation method. (Low priority)
                mergeWires(); //An o(n^2) algorithm (could be made faster) Only call when creating wires.
                return;
            }

            //The next clicks should always interact with objects, except swathe-deleting wires in a straight line.

            //As with above occurrences, accesses to the presenceMap through user-provided positions are always unsafe.
            //Please don't click out of bounds !
            //TODO: find a way to stop window resizing. And, while you're at it, automatic full screen/monitor size detection ?

            boolean exists = presenceMap[tileX][tileY] != null;

            if (clickType == RIGHT_CLICK) {
                //Single-object removal as well as swathe-removal handling occurs here.
                if (exists) {
                    mouseClickHandler.setNew(false);
                    Placeable toRemove = presenceMap[tileX][tileY];

                    //Must remove all tiles of the component; not all components are single-tile.
                    //For this case, I'm O.K. with iterating as presence is always small.
                    removeFromPresence(presenceMap, toRemove);

                    //Remove from their respective queues, as to now draw nor ACM them.
                    if(toRemove instanceof ComponentHolder) {
                        componentQueue.remove((ComponentHolder) toRemove);
                    } else {
                        wireQueue.remove((Wire) toRemove);
                    }
                } else if (!mouseClickHandler.isDragging()) {
                    //This means swathe-deleting only works when finishing on an empty tile.
                    //TODO: perhaps add some test that discriminates between normal-deleting a wire and when you just finished dragging ?
                    mouseClickHandler.setNew(false);

                    //Setting up tools for iterating through the line
                    int[] positionFirst = mouseClickHandler.getPressFirstPosition();
                    int[] positionLast = mouseClickHandler.getPressReleasePosition();
                    PositionLineIterator posIterator = new PositionLineIterator(positionFirst, positionLast);
                    ArrayList<Wire> potentialRemove = new ArrayList<>();

                    if (!posIterator.isValid()) {
                        //An invalid swathe-delete line; abort
                        return;
                    }

                    while (posIterator.hasNext()) {
                        int[] position = posIterator.next();
                        Placeable there = presenceMap[convertToTileNotation(position[0])][convertToTileNotation(position[1])];
                        if (there == null) {
                            continue;
                        }
                        //A swathe-delete can go *through* stuff like componentHolders.
                        if (there instanceof Wire) {
                            potentialRemove.add((Wire) there);
                        }
                    }
                    for (Wire wire : potentialRemove) {
                        removeFromPresence(presenceMap, wire);
                    }
                    wireQueue.removeAll(potentialRemove);
                }
                //Of course, cleanup after any deletion.
                wireQueue.forEach(this::cleanupWire);
                return;
            }

            if (clickType == LEFT_CLICK) {
                //This is a left click that didn't make a wireLine, as shift was not caught upwards.

                mouseClickHandler.setNew(false);

                if (!exists) {
                    //Component creation handling

                    /*
                    This only makes sure of the data consistency of the components.
                    The practical stuff gets done when compiling a board into an ACM.
                    ACM elements have a "toplevel" property that states whether or not they are part of the visible board.
                    The wire lighting up and I/O changing states is done by poking the presenceMap and/or queues from the ACM's tick() method.
                    This is done after listening for user inputs.
                    Last of all is the graphical painting. This is also mentioned in the aforementioned actionPerformed() dictator method.
                    */

                    //Clone the template. The template is also modified by other methods; can't use it directly !
                    ComponentHolder componentHolder = currentComponent.clone();
                    componentHolder.setSnappedPosition(snappedLocation);
                    componentHolder.setWireSize(currentComponentWireSize);
                    componentHolder.setRotation(currentRotation);

                    boolean willHit = overlaps(presenceMap, componentHolder);

                    if (!willHit) {
                        pushToPresence(presenceMap, componentHolder);
                        componentQueue.push(componentHolder);
                    }
                } else {
                    //Interacting with a component. As of V0.2, that's only for toggling interactable I/O
                    //TODO: Have this open files for ROMs, or displaying hardware screens ?

                    //Force prime to read updated values
                    gameACM.primeAll();

                    Placeable placeable = presenceMap[tileX][tileY];

                    if (placeable instanceof Interactable) {
                        ((Interactable) placeable).interact();
                    }
                }
                return;
            }
        }

        //If no mouse buttons got pressed, listen for key presses instead. Change mode, rotation, etc.
        if (currentKey != 0 && keyPressHandler.isNew()) {
            //A key got pressed, and it isn't zero.
            keyPressHandler.setNew(false);
            switch (currentKey) {
                //Scroll to next rotation
                case 'r' -> setCurrentRotation(Rotation.values()[(currentRotation.ordinal()+1)%Rotation.values().length]);
                //Shift + r
                case 'R' -> setCurrentRotation(Rotation.values()[(currentRotation.ordinal()-1+Rotation.values().length)%Rotation.values().length]);
                //Switch to inventory (dictator will know to switch to other handler) and initialize it.
                case 'e' -> {
                    gameState = GameState.INVENTORY;
                    initializeInventory();
                }

                //Save the ACM. Development is underway.
                //TODO: well, this.
                case 19 -> { //CONTROL-19 for CTRL + S
                    //Be sure we're up-to-date
                    makeNewACM();

                    //Open saving menu
                    gameState = GameState.SAVING;
                }

                case '\u001B' -> gameState = GameState.BUILDING; //Escape returns to default building mode
            }
        }
    }
    @Override
    public void paintComponent(Graphics g) {
        //Not quite sure what this does. Just plugging into Swing's API before building my own rendering on top.
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        //All graphics logic goes here !
        switch (gameState) {
            case BUILDING -> {
                //Will draw the wires and components using this g2d, so it will appear on screen.
                GraphicalUtilities.buildingGraphics(g2d, wireQueue, componentQueue);

                //Drawing the ghost of the held component holder
                int[] hoveringPosition = mouseClickHandler.getHoveringPosition();
                int hx = convertToSnappedNotation(hoveringPosition[0]);
                int hy = convertToSnappedNotation(hoveringPosition[1]);

                /*
                The template is also used as the ghost, storing its position, rotation etc.
                This is why a clone should always be made when copying from the template.
                Values changed here are always non-consequential to the template's use, such as position or rotation.
                 */

                currentComponent.setSnappedPosition(new int[]{hx, hy});
                currentComponent.setRotation(currentRotation);

                //Change ghost color for invalid placements
                if (overlaps(presenceMap, currentComponent)) {
                    g2d.setColor(Color.RED);
                } else {
                    //How pretty !
                    g2d.setColor(Color.decode("#32CD32"));
                }

                //Drawing the ghost. Special case for drawing a component without its render() method; we handle it manually.
                if (isPolar(currentRotation)) {
                    g2d.drawRect(hx, hy, currentComponent.getRealWidth(), currentComponent.getRealHeight());
                } else {
                    g2d.drawRect(hx, hy, currentComponent.getRealHeight(), currentComponent.getRealWidth());
                }

                //Figuring out the 'output' side and drawing a nice line to point it out.
                g2d.setColor(Color.decode("#008000"));

                int topLeftX = currentRotation == Rotation.EAST ? hx + currentComponent.getRealHeight() : hx;
                int topLeftY = currentRotation == Rotation.SOUTH ? hy + currentComponent.getRealHeight() : hy;
                int topLeftX2 = isPolar(currentRotation) ? topLeftX + currentComponent.getRealWidth() : topLeftX;
                int topLeftY2 = isPolar(currentRotation) ? topLeftY : topLeftY + currentComponent.getRealWidth();
                g2d.drawLine(topLeftX, topLeftY, topLeftX2, topLeftY2);
            }

            //Inventory displaying.
            case INVENTORY -> GraphicalUtilities.inventoryGraphics(g2d, inventoryComponents);
            case SAVING -> GraphicalUtilities.savingGraphics(g2d, componentQueue);
        }

        //Really not sure why I need this. But there is stutter without it. Swing is weird.
        getToolkit().sync();
    }

    public void setCurrentRotation(Rotation currentRotation) {
        this.currentRotation = currentRotation;
    }

    public void setCurrentComponent(ComponentHolder currentComponent) {
        this.currentComponent = currentComponent;
    }

    static long lastTime = 0L;
    public void tick() {
        long time = System.currentTimeMillis();
        //Setting to 1 second per tick.
        if (time - lastTime > 500 && gameACM != null) {
            lastTime = time;
            try {
                gameACM.tick();
            } catch (UnmatchingWiresException e) {
                System.out.println("Unmatching wires somewhere !");
            }
        }
    }

    public void initializeInventory() {
        //Add the primitive InventoryComponents.
        //TODO : Later, make primitives into actual files so the user can move them (button to regen in OPTIONS). Also, add options and ".." when not toplevel dir.
        //This method is called as means of "refreshing" the inventory, too. Perhaps keep current working dir in a variable (destroy when going out of inv?) and load from there.

        inventoryComponents.clear();
        inventoryComponents.add(new InventoryComponent(new PrimitiveAND(null, currentComponentWireSize, null), null, "AND Gate", false));
        inventoryComponents.add(new InventoryComponent(new PrimitiveOR(null, currentComponentWireSize, null), null, "OR Gate", false));
        inventoryComponents.add(new InventoryComponent(new PrimitiveXOR(null, currentComponentWireSize, null), null, "XOR Gate", false));
        inventoryComponents.add(new InventoryComponent(new PrimitiveNAND(null, currentComponentWireSize, null), null, "NAND Gate", false));
        inventoryComponents.add(new InventoryComponent(new PrimitiveNOR(null, currentComponentWireSize, null), null, "NOR Gate", false));
        inventoryComponents.add(new InventoryComponent(new PrimitiveXNOR(null, currentComponentWireSize, null), null, "XNOR Gate", false));
        inventoryComponents.add(new InventoryComponent(new PrimitiveSplitter(null, currentComponentWireSize, null), null, "Splitter", false));
        inventoryComponents.add(new InventoryComponent(new PrimitiveMerger(null, currentComponentWireSize, null), null, "Merger", false));
        inventoryComponents.add(new InventoryComponent(new PrimitiveTrue(null, currentComponentWireSize, null), null, "True", false));
        inventoryComponents.add(new InventoryComponent(new PrimitiveFalse(null, currentComponentWireSize, null), null, "False", false));
        inventoryComponents.add(new InventoryComponent(new PrimitiveLatch(null, currentComponentWireSize, null), null, "D Latch", false));
        inventoryComponents.add(new InventoryComponent(new PrimitiveNOT(null, currentComponentWireSize, null), null, "Not Gate", false));
        inventoryComponents.add(new InventoryComponent(new PrimitiveOutput(null, currentComponentWireSize, null), null, "Out", false));
        inventoryComponents.add(new InventoryComponent(new PrimitiveInput(null, currentComponentWireSize, null), null, "In", false));

        //Delays just spit out whatever was given to them. In truth, wireSize is mostly there to guarantee coherency between >1 input gates, as a check is done.
        inventoryComponents.add(new InventoryComponent(new PrimitiveDelay(null, null), null, "Delay", false));
    }

    public void inventoryUpdate() {
        if (mouseClickHandler.isNew()) {
            mouseClickHandler.setNew(false);
            InventoryComponent component = getClickedInventoryComponent(mouseClickHandler, inventoryComponents);

            //Testing if component is OPTIONS, or if loaded dir will go here.

            if (component != null) {
                //TODO: test if directory.
                setCurrentComponent(component.getComponent());
                gameState = GameState.BUILDING;
            }
        }

        //Handling quick-switch of wireSize (2^n with number keys)
        int currentKey = keyPressHandler.getCurrentKey();

        if (keyPressHandler.isNew() && currentKey != 0 ) {
            keyPressHandler.setNew(false);
            //A key got pressed
            switch (currentKey) {
                case 'e', '\u001B' -> gameState = GameState.BUILDING;
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
                    currentComponentWireSize = (int) Math.pow(2, Integer.parseInt(String.valueOf((char) currentKey)));
                    initializeInventory();
                }
            }
        }
    }

    public void savingUpdate() {
        int currentKey = keyPressHandler.getCurrentKey();
        if (keyPressHandler.isNew() && currentKey != 0 ) {
            keyPressHandler.setNew(false);
            //A key got pressed
            switch (currentKey) {
                case 'e', '\u001B' -> gameState = GameState.BUILDING;
            }
        }
    }

    public void makeNewACM() {
        //Reset graphical state of wires. New ACM assumes everything is blank.
        wireQueue.forEach(w -> w.setState(Activation.OFF));

        //The actually important bit.
        gameACM = new AbstractComponentMap(presenceMap, componentQueue);
    }

    public void cleanupWire(Wire wire) {
        //Utility method to cull and "cleanup" (force normalization) on a wire. E.g. stub where no neighbors are found.
        if (wire.isCrossover()) {
            return; //Don't cull crossovers as it can create ambiguity.
        }
        //Branches to cull
        ArrayList<Rotation> toRemove = new ArrayList<>();

        int[] position = wire.getSnappedPosition();
        for (Rotation r : wire.getPartners()) {
            int[] partnerPosition = getDirectionalLocation(position, r, realTileSize);
            Placeable placeable = presenceMap[convertToTileNotation(partnerPosition[0])][convertToTileNotation(partnerPosition[1])];

            //Remove if : there is nothing (bad component connections would be too much work to check for. They don't happen anyway... Well, shouldn't !
            //Also remove illegal wire placements (pointing into an un-reciprocating wire
            if (placeable == null || (placeable instanceof Wire && !((Wire) placeable).getPartners().contains(complement(r)))) {
                toRemove.add(r);
            }
        }

        //Don't destroy single dots ! (They'll keep an "illegal" state until they're reconnected)
        if (toRemove.size() != wire.getPartners().size()) {
            toRemove.forEach(wire.getPartners()::remove);
        }

        List<Rotation> rotationList = wire.getPartners().stream().toList();  //Cull useless "show connected" dots
        if (!rotationList.isEmpty() && (rotationList.size() == 1 ||
                (rotationList.size() == 2 && isPolar(rotationList.get(0)) == isPolar(rotationList.get(1))))) {
            wire.setShowConnected(false);
        }
    }

    public void mergeWires() {
        /*
        An O(n^2) algorithm, for crunching stacked wires into one.
        This algorithm is a legacy code remnant and should never, EVER be needed.
        Pending discontinuation once I can prove no wire ever gets stacked. For now though best to keep it !
         */
        ArrayList<Wire> toRemove = new ArrayList<>();
        for (Wire wire : wireQueue) {
            if (wire.isCrossover()) {
                continue;
            }
            List<Rotation> rotationList = wire.getPartners().stream().toList();
            if (toRemove.contains(wire)) {
                continue;
            }
            if (rotationList.size() > 2 || (rotationList.size() == 2 &&
                    !(isPolar(rotationList.get(0))==isPolar(rotationList.get(1))))) { wire.setShowConnected(true); }
            Set<Wire> without = new HashSet<>(wireQueue);
            without.remove(wire);
            for (Wire partner : without) {
                if (partner.getSnappedPosition() == wire.getSnappedPosition()){
                    wire.getPartners().addAll(partner.getPartners());
                    toRemove.add(partner);
                }
            }
            //Merging into 1 on the presencemap
            presenceMap[convertToTileNotation(wire.getSnappedPosition()[0])][convertToTileNotation(wire.getSnappedPosition()[1])] = wire;
        }
        wireQueue.removeAll(toRemove);
    }
}
