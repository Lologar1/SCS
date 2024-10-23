package me.analyzers.scs.game;

import me.analyzers.scs.primitiveComponents.PrimitiveIO;
import me.analyzers.scs.primitiveComponents.PrimitiveInput;
import me.analyzers.scs.primitiveComponents.PrimitiveOutput;
import me.analyzers.scs.utilities.Placeable;
import me.analyzers.scs.utilities.Rotation;

import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static me.analyzers.scs.game.MainPanel.realTileSize;
import static me.analyzers.scs.game.MainPanel.pinSize;
import static me.analyzers.scs.utilities.MathUtils.*;
import static me.analyzers.scs.utilities.Rotation.*;

public class ComponentHolder implements Placeable {
    private final int realWidth;
    private final int realHeight;
    private final int[] relativeInputs;
    private final int[] relativeOutputs;
    private Rotation rotation;
    private AbstractComponentMap acm;
    private int[] position;
    private final String name;


    public ComponentHolder(String name, int[] snappedPosition, int tileWidth, int tileHeight, int[] relativeInputs,
                            int[] relativeOutputs, Rotation startingRotation, AbstractComponentMap acm) {
        if (startingRotation == null) {
            startingRotation = NORTH; //Only for half-assed inits
        }
        this.name = name;
        this.realWidth = tileWidth* realTileSize;
        this.realHeight = tileHeight* realTileSize;
        this.position = snappedPosition;
        this.relativeInputs = relativeInputs;
        this.relativeOutputs = relativeOutputs;
        this.rotation = startingRotation;
        this.acm = acm;
    }

    public void cloneACM() throws CloneNotSupportedException {
        ConcurrentHashMap<ComponentHolder, boolean[][]> dynamicComponentInputs = new ConcurrentHashMap<>();
        ConcurrentHashMap<ComponentHolder, ComponentConnection[][]> componentLinks = new ConcurrentHashMap<>();
        LinkedHashSet<ComponentHolder> primed = new LinkedHashSet<>();
        ArrayList<PrimitiveInput> inputPins = new ArrayList<>();
        ArrayList<PrimitiveOutput> outputPins = new ArrayList<>();

        //Will modify all ComponentHolder references in this ACM to be clones to avoid duplicates.
        //Wires won't be touched, so activation signals will "teleport" but since they're not top level it does not matter.

        HashMap<ComponentHolder, ComponentHolder> replacements = new HashMap<>();
        for (ComponentHolder componentHolder : acm.getDynamicComponentInputs().keySet()) {
            replacements.put(componentHolder, componentHolder.clone());
        }

        //Replace in dynamicComponentInputs. Keeping boolean[][] reference as it gets shallow-copied on ACM flattening anyway.
        for (ComponentHolder componentHolder : acm.getDynamicComponentInputs().keySet()) {
            dynamicComponentInputs.put(replacements.get(componentHolder), acm.getDynamicComponentInputs().get(componentHolder));
        }

        //Replace in componentLinks, without replacing ComponentConnection[][] reference (later)
        for (ComponentHolder componentHolder : acm.getComponentLinks().keySet()) {
            //Make new componentConnections
            ComponentConnection[][] newConnections = new ComponentConnection[acm.getComponentLinks().get(componentHolder).length][];

            //Swap out the connection for the replacement, and make a new array so as to not modify the existing, template one !
            for (int batch = 0; batch < acm.getComponentLinks().get(componentHolder).length; batch++) {
                newConnections[batch] = new ComponentConnection[acm.getComponentLinks().get(componentHolder)[batch].length];

                for (int connection = 0; connection < acm.getComponentLinks().get(componentHolder)[batch].length; connection++) {
                    ComponentConnection oldConnection = acm.getComponentLinks().get(componentHolder)[batch][connection];

                    newConnections[batch][connection] = new ComponentConnection(replacements.get(oldConnection.getComponent()),
                            oldConnection.getWireLine(), oldConnection.getInPort(), oldConnection.isTopLevel());
                }
            }
            componentLinks.put(replacements.get(componentHolder), newConnections);
        }

        acm.getOutputPins().forEach(p -> outputPins.add((PrimitiveOutput) replacements.get(p)));
        acm.getInputPins().forEach(p -> inputPins.add((PrimitiveInput) replacements.get(p)));
        acm.getPrimed().forEach(p -> primed.add(replacements.get(p)));

        //Change ACM reference.
        acm = new AbstractComponentMap(dynamicComponentInputs, componentLinks, primed, inputPins, outputPins);
    }

    @Override
    public ComponentHolder clone() throws CloneNotSupportedException {
        return (ComponentHolder) super.clone();
    }

    public void setSnappedPosition(int[] position) {
        this.position = position;
    }

    public void setRotation(Rotation rotation) {
        this.rotation = rotation;
    }

    @Override
    public int[] getSnappedPosition() {
        //It would appear I fell asleep and directly modified the array in a number of math methods.
        //Kinda forgot primitives array aren't primitives and as such have a reference instead of being copy/pasted around.
        return position.clone();
    }

    public String getName() {
        return name;
    }

    public AbstractComponentMap getAcm() {
        return acm;
    }

    public int getRealHeight() {
        return realHeight;
    }

    public int getRealWidth() {
        return realWidth;
    }

    @Override
    public int getRelativeTileWidth() {
        return isPolar(rotation) ? convertToTileNotation(realWidth) : convertToTileNotation(realHeight);
    }

    @Override
    public int getRelativeTileHeight() {
        return isPolar(rotation) ? convertToTileNotation(realHeight) : convertToTileNotation(realWidth);
    }

    @Override
    public void render(Graphics2D g2d) {
        //Rendering
        int posA = position[0];
        int posB = position[1];

        //Swap width/height on non-polar rotations (rectangle is on the side)
        int width = isPolar(rotation) ? getRealWidth() : getRealHeight();
        int height = isPolar(rotation) ? getRealHeight() : getRealWidth();

        //Removing background grid to white, or green/red for I/O Screens and such will get rendered separately, overriding the method.
        if (this instanceof PrimitiveIO) {
            if (((PrimitiveIO) this).getValue()) {
                g2d.setColor(Color.GREEN);
            } else {
                g2d.setColor(Color.RED);
            }
        } else {
            g2d.setColor(Color.WHITE);
        }

        g2d.fillRect(posA, posB, width, height);

        /*
        Drawing component outline and display name
        Known 'bug' : names can overflow to next tiles.
        TODO: One day, find a clever way to fix this without limiting size or doing ugly wraparounds. Low priority.
        */

        g2d.setColor(Color.BLACK);
        g2d.drawRect(posA, posB, width, height);
        g2d.drawString(getName(), posA, posB + getRealHeight() / 2);

        //Drawing I/O for component. Relative I/O is mapped from 0,0 at bottom.left

        //Drawing outputs
        g2d.setColor(Color.RED);
        int[][] outwardsOutputs = getTileOutputPositions(this);

        for (int[] outputPosition : outwardsOutputs) {
            outputPosition[0] *= realTileSize;
            outputPosition[1] *= realTileSize;

            //Setting position to center of tile (snapped is in top-left)
            outputPosition[0] += realTileSize/2;
            outputPosition[1] += realTileSize/2;

            int[] pinCenter = getDirectionalLocation(outputPosition, complement(rotation), realTileSize/2);
            g2d.fillOval(pinCenter[0]-pinSize/2, pinCenter[1]-pinSize/2, pinSize, pinSize);
        }

        //Drawing inputs
        g2d.setColor(Color.BLUE);
        int[][] outwardsInputs = getTileInputPositions(this);

        for (int[] inputPosition : outwardsInputs) {

            inputPosition[0] *= realTileSize;
            inputPosition[1] *= realTileSize;

            //Setting position to center of tile (snapped is in top-left)
            inputPosition[0] += realTileSize/2;
            inputPosition[1] += realTileSize/2;

            int[] pinCenter = getDirectionalLocation(inputPosition, rotation, realTileSize/2);
            g2d.fillOval(pinCenter[0]-pinSize/2, pinCenter[1]-pinSize/2, pinSize, pinSize);
        }
    }

    public int[] getRelativeInputs() {
        return Arrays.copyOf(relativeInputs, relativeInputs.length);
    }

    public int[] getRelativeOutputs() {
        return Arrays.copyOf(relativeOutputs, relativeOutputs.length);
    }

    public Rotation getRotation() {
        return rotation;
    }

}
