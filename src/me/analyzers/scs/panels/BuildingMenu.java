package me.analyzers.scs.panels;

import me.analyzers.scs.game.*;
import me.analyzers.scs.primitiveComponents.PrimitiveMerger;
import me.analyzers.scs.utilities.*;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static me.analyzers.scs.game.MainPanel.*;
import static me.analyzers.scs.utilities.MathUtils.*;

public class BuildingMenu {
    //(Building) Current held component data
    private Rotation currentRotation = Rotation.NORTH;
    //ComponentHolder object as template to the one we're using. Position is inconsequential.
    private ComponentHolder currentComponent = new PrimitiveMerger(null, currentRotation);

    //For updates that don't change the board (i.e. single-wire redirection)
    public boolean forceNewACM = false;

    //ACM representing the board. Updated on presenceMap modification.
    private AbstractComponentMap gameACM;

    //Current state of the drawn board.
    //presenceMap holds all Placeables according to their position, even for ones spanning multiple tiles. Null for air.
    //componentQueue is a disordered list of all drawn components
    //wireQueue is a disordered list of all drawn wires (separately, not inside groups)

    private Placeable[][] presenceMap = new Placeable[widthX][widthY];
    public LinkedList<ComponentHolder> componentQueue = new LinkedList<>();
    public LinkedList<Wire> wireQueue = new LinkedList<>();

    //Called to handle mouse/keyboard inputs and change the map.
    public void update(MouseClickHandler mouseClickHandler, KeyPressHandler keyPressHandler) throws CloneNotSupportedException {

        //Get key that is being pressed. 0 if no key is pressed.
        char currentKey = keyPressHandler.getCurrentKey();

        //Last click position handling IMPORTANT: getting the click is really just the last time the mouse was pressed down
        //To get current mouse position, refer to MouseClickHandler.getHoveringPosition()
        int[] realPressLocation = new int[]{mouseClickHandler.getLastClickX(), mouseClickHandler.getLastClickY()};
        int[] snappedLocation = convertToSnappedNotation(realPressLocation);
        int tileX = snappedLocation[0]/realTileSize;
        int tileY = snappedLocation[1]/realTileSize;

        if (mouseClickHandler.isNew()) { //isNew is only true on a new mouse click, or ongoing drag (as we avoid setting it to false, instead letting it fall through)
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
                    currentComponent = ((ComponentHolder) placeable).clone();//Set a new template. Again, pos is inconsequential.
                    setCurrentRotation(currentComponent.getRotation()); //Set rotation to the pipetted component's
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
                boolean existsFirst = presenceMap[convertToTileNotation(positionFirst[0])][convertToTileNotation(positionFirst[1])] instanceof Wire;
                boolean existsLast = presenceMap[convertToTileNotation(positionLast[0])][convertToTileNotation(positionLast[1])] instanceof Wire;

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

                        if (potentialPartner instanceof Wire && isMutualComplement(((Wire) potentialPartner).getPartners())
                                && ((Wire) potentialPartner).getPartners().stream().noneMatch(r -> justPut.getPartners().contains(r))) {

                            ((Wire) potentialPartner).setCrossover(true);
                            ((Wire) potentialPartner).setShowConnected(false);
                            ((Wire) potentialPartner).getPartners().addAll(Arrays.stream(Rotation.values()).toList());
                            //Set the crossover to be: a crossover, don't show connected (little dot), and has all partners.
                        } else if (potentialPartner instanceof Wire && ((Wire) potentialPartner).getPartners().stream().noneMatch(justPut.getPartners()::contains)) {
                            //If wire is a stub (not mutual complement, but still doesn't contain any of the same rotations), then connect it.
                            ((Wire) potentialPartner).setShowConnected(true);
                            ((Wire) potentialPartner).getPartners().addAll(justPut.getPartners());
                        } else {
                            //A componentHolder; abort
                            return;
                        }
                    } else {
                        //Nothing there.
                        toAdd.add(justPut);
                    }
                }

                if (existsFirst) {
                    //Wire already exists, so we autosnap it.
                    Wire wireToAdjust = (Wire) presenceMap[convertToTileNotation(positionFirst[0])]
                            [convertToTileNotation(positionFirst[1])];
                    wireToAdjust.autoSnap(direction, presenceMap, false);
                } else if (presenceMap[tileX][tileY] == null){
                    //Create a stub wire and autosnap it, if there is nothing there
                    Wire newWire = new Wire(convertToSnappedNotation(positionFirst), direction);
                    newWire.autoSnap(direction, presenceMap, true);
                    toAdd.add(newWire);
                }

                //Same as above, but for last
                if (existsLast) {
                    Wire wireToAdjust = (Wire) presenceMap[convertToTileNotation(positionLast[0])]
                            [convertToTileNotation(positionLast[1])];
                    wireToAdjust.autoSnap(complement(direction), presenceMap, false);
                } else if (presenceMap[convertToTileNotation(positionLast[0])]
                        [convertToTileNotation(positionLast[1])] == null){
                    Wire newWire = new Wire(convertToSnappedNotation(positionLast), complement(direction));
                    newWire.autoSnap(complement(direction),presenceMap, true);
                    toAdd.add(newWire);
                }

                if (existsLast || existsFirst) {
                    //For single-wire redirection
                    forceNewACM = true;
                }

                for (Wire wire : toAdd) {
                    //Reaching this point, we commit to changing the board state with our new wireLine.
                    pushToPresence(presenceMap, wire);
                }
                //Adding our wires to the wireQueue
                wireQueue.addAll(toAdd);

                //Cull and maintain wires.
                wireQueue.forEach(this::cleanupWire);
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
                    //This means swathe-deleting only works when beginning on an empty tile.
                    //TODO: perhaps add some test that discriminates between normal-deleting a wire and when you start dragging ?
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
                    //Important: deepClone the ACM so there are no duplicate references when flat mapping the ACMs.
                    ComponentHolder componentHolder = currentComponent.clone();
                    componentHolder.setSnappedPosition(snappedLocation);
                    componentHolder.setRotation(currentRotation);

                    boolean willHit = overlaps(presenceMap, componentHolder);

                    if (!willHit) {
                        pushToPresence(presenceMap, componentHolder);
                        componentQueue.push(componentHolder);
                    }
                } else {
                    //Interacting with a component. As of V0.2, that's only for toggling interactable I/O
                    //TODO: Have this open files for ROMs, or displaying hardware screens ?

                    Placeable placeable = presenceMap[tileX][tileY];

                    if (placeable instanceof Interactable) {
                        ((Interactable) placeable).interact();
                        gameACM.prime((ComponentHolder) placeable);
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

    public void setBoardState(StoredState state) {
        if (state == null) {
            presenceMap = new Placeable[widthX][widthY];
            wireQueue.clear();
            componentQueue.clear();
        } else {
            Placeable[][] statePresenceMap = state.getPresenceMap();
            presenceMap = new Placeable[widthX][widthY];
            for (int x = 0; x < statePresenceMap.length; x++) {
                for (int y = 0; y < statePresenceMap[0].length; y++) {
                    if (x < widthX && y < widthY) {
                        presenceMap[x][y] = statePresenceMap[x][y];
                    }
                }
            }
            wireQueue = state.getWireQueue();
            componentQueue = state.getComponentQueue();
        }
        makeNewACM();
    }

    public void setCurrentRotation(Rotation currentRotation) {
        this.currentRotation = currentRotation;
    }

    public void setCurrentComponent(ComponentHolder currentComponent) {
        this.currentComponent = currentComponent;
    }

    public AbstractComponentMap getGameACM() {
        return gameACM;
    }

    public Placeable[][] getPresenceMap() {
        return presenceMap;
    }

    public LinkedList<Wire> getWireQueue() {
        return wireQueue;
    }

    public LinkedList<ComponentHolder> getComponentQueue() {
        return componentQueue;
    }

    //Called to render the map on-screen when in building mode.
    public void buildingGraphics(Graphics2D g2d, MouseClickHandler mouseClickHandler) {
        //Will draw the wires and components using this g2d, so it will appear on screen.

        //Draw grid
        g2d.setColor(Color.LIGHT_GRAY);
        for (int ran = 0; ran < screenHeight; ran += realTileSize) {
            g2d.drawLine(0, ran, screenWidth, ran);
        }
        for (int col = 0; col < screenWidth; col += realTileSize) {
            g2d.drawLine(col, 0, col, screenHeight);
        }
        g2d.setColor(Color.BLACK);

        //Drawing wires (so get painted over by I/O)
        for (Wire wire : wireQueue) {
            wire.render(g2d);
        }

        //Drawing components. Different components may need to override the method.
        for (ComponentHolder toDraw : componentQueue) {
            toDraw.render(g2d);
        }

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
}
