package me.analyzers.scs.game;

import me.analyzers.scs.primitiveComponents.PrimitiveIO;
import me.analyzers.scs.utilities.Rotation;

import java.awt.*;

import static me.analyzers.scs.game.MainPanel.realTileSize;
import static me.analyzers.scs.utilities.GraphicalUtilities.pinSize;
import static me.analyzers.scs.utilities.MathUtils.*;
import static me.analyzers.scs.utilities.Rotation.*;

public class ComponentHolder implements Placeable{
    private final int realWidth;
    private final int realHeight;
    private int wireSize;
    private final int[] relativeInputs;
    private final int[] relativeOutputs;
    private Rotation rotation;
    private final AbstractComponentMap acm;
    private int[] position;
    private final String name;


    public ComponentHolder(String name, int[] snappedPosition, int tileWidth, int tileHeight, int wireSize, int[] relativeInputs,
                            int[] relativeOutputs, Rotation startingRotation, AbstractComponentMap acm) {
        if (startingRotation == null) {
            startingRotation = NORTH; //Only for half-assed inits
        }
        this.name = name;
        this.realWidth = tileWidth* realTileSize;
        this.realHeight = tileHeight* realTileSize;
        this.position = snappedPosition;
        this.wireSize = wireSize;
        this.relativeInputs = relativeInputs;
        this.relativeOutputs = relativeOutputs;
        this.rotation = startingRotation;
        this.acm = acm;
    }

    @Override
    public ComponentHolder clone() throws CloneNotSupportedException {
        return (ComponentHolder) super.clone();
    }

    public void setWireSize(int wireSize) {
        this.wireSize = wireSize;
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
        int[] position = getSnappedPosition();
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

    public int getWireSize() {
        return wireSize;
    }

    public int[] getRelativeInputs() {
        return relativeInputs;
    }

    public int[] getRelativeOutputs() {
        return relativeOutputs;
    }

    public Rotation getRotation() {
        return rotation;
    }
}
