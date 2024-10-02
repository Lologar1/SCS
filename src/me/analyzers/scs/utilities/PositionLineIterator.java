package me.analyzers.scs.utilities;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static me.analyzers.scs.game.MainPanel.realTileSize;
import static me.analyzers.scs.utilities.MathUtils.convertToSnappedNotation;

public class PositionLineIterator implements Iterator {
    /*
    Iterates through a straight line defined by two grid positions.
    Accepts only real (absolute) positions, be it snapped or not.
     */

    private int length;
    private final Rotation direction;
    private int minY;
    private int minX;
    private boolean valid = true;
    private final int[] adjustedFirst;
    private final int[] adjustedLast;

    public PositionLineIterator(int[] positionFirst, int[] positionLast) {
        int posFA = convertToSnappedNotation(positionFirst[0]);
        int posFB = convertToSnappedNotation(positionFirst[1]);
        int posLA = convertToSnappedNotation(positionLast[0]);
        int posLB = convertToSnappedNotation(positionLast[1]);

        if (!(posFA == posLA || posFB == posLB)) {
            valid = false; //Not a straight line, as !(x is on same line as other x | y is on same as other y)
        }

        //Get snapped ends of the line
        this.adjustedFirst = new int[]{posFA, posFB};
        this.adjustedLast = new int[]{posLA, posLB};

        //Self-explanatory
        this.length = 1 + Math.abs((posFA/realTileSize - posLA/realTileSize) + (posFB/realTileSize - posLB/realTileSize));

        //Small snippet to appropriately set the direction
        this.minY = posFB;
        this.minX = posFA;

        if (posFA < posLA) {
            this.direction = Rotation.EAST;
        } else if (posLA < posFA) {
            this.minX = posLA;
            this.direction = Rotation.WEST;
        } else if (posFB < posLB) {
            this.direction = Rotation.SOUTH;
        } else {
            this.minY = posLB;
            this.direction = Rotation.NORTH;
        }
    }

    public int[] getSnappedPositionFirst() {
        return adjustedFirst;
    }

    public int[] getSnappedPositionLast() {
        return adjustedLast;
    }

    public boolean isValid() {
        return valid;
    }

    public int getLength() {
        return length;
    }

    @Override
    public boolean hasNext() {
        return length != 0;
    }

    @Override
    public int[] next() {
        //Returns snapped position of next tile in line. As 0,0 is up-left, it goes downward !
        if (length == 0) {
            throw new NoSuchElementException();
        }
        length--;
        if (direction == Rotation.EAST || direction == Rotation.WEST) {
            minX += realTileSize;
            return new int[]{minX-realTileSize, minY};
        } else{
            minY += realTileSize;
            return new int[]{minX, minY-realTileSize};
        }
    }

    public Rotation getDirection() {
        return direction;
    }
}
