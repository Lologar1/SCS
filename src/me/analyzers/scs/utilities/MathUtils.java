package me.analyzers.scs.utilities;

import me.analyzers.scs.game.*;

import java.util.*;

import static me.analyzers.scs.game.MainPanel.*;

public class MathUtils {
    public static int[] getCombinatorIO(int size) {
        int[] inputs = new int[size];
        for (int i = 0; i < size; i++) {inputs[i] = i;}
        return inputs;
    }
    public static boolean isPolar(Rotation r) {
        return r == Rotation.NORTH || r == Rotation.SOUTH;
    }

    public static Rotation complement(Rotation r) {
        switch (r) {
            case NORTH -> {
                return Rotation.SOUTH;
            }
            case SOUTH -> {
                return Rotation.NORTH;
            }
            case WEST -> {
                return Rotation.EAST;
            }
            case EAST -> {
                return Rotation.WEST;
            }
        }
        throw new RuntimeException("Illegal rotation for complement method");
    }

    public static int[] toCardinalDirection(Rotation rotation) {
        switch (rotation) {
            case NORTH -> {
                return new int[]{0, -1};
            }
            case WEST -> {
                return new int[]{-1, 0};
            }
            case EAST -> {
                return new int[]{1, 0};
            }
            case SOUTH -> {
                return new int[]{0, 1};
            }
            default -> throw new RuntimeException("Illegal rotation for cardinal direction.");
        }
    }

    public static Rotation snapToComponentIO(ComponentHolder component, Wire wire) {
        /*
        Gives the rotation the wire should take to connect to a component, given it is right next to an input.
        Else, null.
         */

        //Define some variables
        //Component snapped and tile positions
        int[] componentSnappedPosition = component.getSnappedPosition();
        int[] tileComponentPosition = new int[]{convertToTileNotation(componentSnappedPosition[0]), convertToTileNotation(componentSnappedPosition[1])};

        //Relative I/O of componentHolder.

        /*
        Now, relative I/O is a rather tricky thing.
        They are defined for a north-facing version of the component holder, starting at 0
        Component I/O is always at the back and from of the componentHolder. A standard XOR gate might have
        Inputs : int[]{0, 2} -> for first and third tile on the bottom
        Output : int[]{1} -> for middle tile on top
         */

        int[] in = component.getRelativeInputs();
        int[] out = component.getRelativeOutputs();
        Rotation componentRotation = component.getRotation();

        //Wire snapped and tile positions
        int[] wireSnappedPosition = wire.getSnappedPosition();
        int[] tileWirePosition = convertToTileNotation(wireSnappedPosition);

        //Getting difference of X and Y levels
        int yDiff = Math.abs(tileComponentPosition[1] - tileWirePosition[1]);
        int xDiff = Math.abs(tileComponentPosition[0] - tileWirePosition[0]);

        //Wire is right next to possible I/O.
        //Difference act as the relative offset. This code is a bit ugly and bruteforcey, but I don't see the point in changing that.
        if (componentRotation == Rotation.NORTH) {
            if (tileComponentPosition[1] - tileWirePosition[1] < 0) { //Inputs (downside)
                if (Arrays.stream(in).anyMatch(i -> i == xDiff)) {
                    return Rotation.NORTH;
                }
            } else { //Outputs (upside)
                if (Arrays.stream(out).anyMatch(i -> i == xDiff)) {
                    return Rotation.SOUTH;
                }
            }
        } else if (componentRotation == Rotation.EAST) {
            if (tileComponentPosition[0] - tileWirePosition[0] > 0) { //Inputs (to the left)
                if (Arrays.stream(in).anyMatch(i -> i == yDiff)) {
                    return Rotation.EAST;
                }
            } else { //Outputs (to the right)
                if (Arrays.stream(out).anyMatch(i -> i == yDiff)) {
                    return Rotation.WEST;
                }
            }
        } else if (componentRotation == Rotation.SOUTH) {
            if (tileComponentPosition[1] - tileWirePosition[1] < 0) { //Outputs (downside)
                //Magic -1 value: example, reflecting over 0, 1, 2, 3 needs "maxvalue" of 3; so 3-1 = 2 which is a successful reflection.
                if (Arrays.stream(reflectOverAxis(out, component.getRelativeTileWidth()-1)).anyMatch(i -> i == xDiff)) {
                    return Rotation.NORTH;
                }
            } else { //Inputs (upside)
                if (Arrays.stream(reflectOverAxis(in, component.getRelativeTileWidth()-1)).anyMatch(i -> i == xDiff)) {
                    return Rotation.SOUTH;
                }
            }
        } else { //WEST
            if (tileComponentPosition[0] - tileWirePosition[0] < 0) { //Inputs (to the right)
                if (Arrays.stream(reflectOverAxis(in, component.getRelativeTileHeight()-1)).anyMatch(i -> i == yDiff)) {
                    return Rotation.WEST;
                }
            } else { //Outputs (to the left)
                if (Arrays.stream(reflectOverAxis(out, component.getRelativeTileHeight()-1)).anyMatch(i -> i == yDiff)) {
                    return Rotation.EAST;
                }
            }
        }
        return null;
    }

    public static int[] reflectOverAxis(int[] toReflect, int maxValue) {
        //Reflects an integer array according to a certain axis. Used for SOUTH and WEST I/O reflections (for asymmetrical components)
        //Takes into account the zero !
        int[] result = new int[toReflect.length];

        for (int i = 0; i < toReflect.length; i++) {
            result[i] = maxValue - toReflect[i];
        }

        return result;
    }

    public static int[] getDirectionalLocation(int[] position, Rotation direction, int unit) {
        switch (direction) {
            case NORTH -> {
                return new int[]{position[0], position[1]-unit};
            }
            case EAST -> {
                return new int[]{position[0]+unit, position[1]};
            }
            case SOUTH -> {
                return new int[]{position[0], position[1]+unit};
            }
            case WEST -> {
                return new int[]{position[0]-unit, position[1]};
            }
            default -> {throw new RuntimeException("Illegal rotation direction in getDirectionalLocation()");}
        }
    }

    public static Rotation bendClockwise(Rotation rotation) {
        switch (rotation) {
            case NORTH -> {
                return Rotation.EAST;
            }
            case EAST -> {
                return Rotation.SOUTH;
            }
            case SOUTH -> {
                return Rotation.WEST;
            }
            case WEST -> {
                return Rotation.NORTH;
            }
            default -> throw new RuntimeException("Illegal rotation direction in bendClockwise()");
        }
    }

    public static int[] getSnappedRelativeTopLeft(ComponentHolder componentHolder) {
        //Snapped coordinates of the tile representing this component's rotationally relative top-left corner.

        int[] topLeft = componentHolder.getSnappedPosition();

        switch (componentHolder.getRotation()) {
            case EAST -> topLeft[0] += componentHolder.getRealHeight()-realTileSize; //Move to the appropriate snapped tile.
            case SOUTH -> {
                topLeft[0] += componentHolder.getRealWidth()-realTileSize;
                topLeft[1] += componentHolder.getRealHeight()-realTileSize;
            }
            case WEST -> topLeft[1] += componentHolder.getRealWidth()-realTileSize;
        }
        return topLeft;
    }

    public static int[] getSnappedRelativeBottomLeft(ComponentHolder componentHolder) {
        //Snapped coordinates of the tile representing this component's rotationally relative bottom-left corner.

        int[] bottomLeft = componentHolder.getSnappedPosition();

        switch (componentHolder.getRotation()) {
            case NORTH -> bottomLeft[1] += componentHolder.getRealHeight()-realTileSize;
            case SOUTH -> bottomLeft[0] += componentHolder.getRealWidth()-realTileSize;
            case WEST -> {
                bottomLeft[0] += componentHolder.getRealHeight()-realTileSize;
                bottomLeft[1] += (componentHolder.getRealWidth()-realTileSize);
            }
        }
        return bottomLeft;
    }

    public static int[][] getTileOutputPositions(ComponentHolder componentHolder) {
        //Get the tile location directly in front of each output pin.

        int outputCount = componentHolder.getRelativeOutputs().length;
        int[][] outputPositions = new int[outputCount][2];

        for (int i = 0; i < outputCount; i++) {
            //Component's position is always in its top-left corner, regardless of orientation. We need the relative top-left.
            int[] snappedPosition = getSnappedRelativeTopLeft(componentHolder);
            int[] tilePosition = convertToTileNotation(snappedPosition);
            Rotation rotation = componentHolder.getRotation();

            //Relative bending and directional locations conserve proper order of outputs, even on upside-down components.
            //Order should always be measured as per the north-facing component.

            int relativeOutputDistance = componentHolder.getRelativeOutputs()[i];

            //Offset by relative-clockwise direction (starting from relative top-left, so want to "descend" along outputs) then by 1 out (get in back of the component)
            outputPositions[i] = getDirectionalLocation(getDirectionalLocation(tilePosition, bendClockwise(rotation), relativeOutputDistance), rotation, 1);
        }

        return outputPositions;
    }

    public static int[][] getTileInputPositions(ComponentHolder componentHolder) {
        //Get the tile location directly in front of each input pin.

        int inputCount = componentHolder.getRelativeInputs().length;
        int[][] inputPositions = new int[inputCount][2];

        for (int i = 0; i < inputCount; i++) {
            //Component's position is always in its top-left corner, regardless of orientation. We need the relative bottom-left, not the absolute.
            int[] snappedPosition = getSnappedRelativeBottomLeft(componentHolder);
            int[] tilePosition = convertToTileNotation(snappedPosition);
            Rotation rotation = componentHolder.getRotation();

            //Relative bending and directional locations conserve proper order of outputs, even on upside-down components.
            //Order should always be measured as per the north-facing component.

            int relativeInputDistance = componentHolder.getRelativeInputs()[i];

            //Offset by relative-clockwise direction (starting from relative top-left, so want to "descend" along outputs) then by 1 out (get in front of the component)
            inputPositions[i] = getDirectionalLocation(getDirectionalLocation(tilePosition, bendClockwise(rotation), relativeInputDistance), rotation, -1);
        }

        return inputPositions;
    }

    public static int convertToTileNotation(int x) {
        return (x-x%realTileSize)/realTileSize;
    }

    public static int convertToSnappedNotation(int x) {
        return x-x%realTileSize;
    }

    public static int[] convertToTileNotation(int[] x) {
        int[] tileArray = new int[x.length];
        for (int i = 0; i < x.length; i++) {
            tileArray[i] = convertToTileNotation(x[i]);
        }
        return tileArray;
    }

    public static int[] convertToSnappedNotation(int[] x) {
        int[] tileArray = new int[x.length];
        for (int i = 0; i < x.length; i++) {
            tileArray[i] = convertToSnappedNotation(x[i]);
        }
        return tileArray;
    }

    public static InventoryComponent getClickedInventoryComponent(MouseClickHandler clickHandler, ArrayList<InventoryComponent> loadedComponents) {
        //Tiles are 3x3 in inventory, 8 tiles starting from 1, 1 with 1 spacing between
        if((convertToTileNotation(clickHandler.getLastClickY()))%4==0
                || (convertToTileNotation(clickHandler.getLastClickX()))%4==0) {return null; }

        //Isn't in-between tiles
        int x = convertToTileNotation(clickHandler.getLastClickX())/4; //Grid is 3x3 + 1 inbetween
        int y = convertToTileNotation(clickHandler.getLastClickY())/4;
        int index = y*(widthX/4) + x;
        return index < loadedComponents.size() ? loadedComponents.get(index) : null;
    }

    public static void pushToPresence(Placeable[][] presence, Placeable component){
        int x = convertToTileNotation(component.getSnappedPosition()[0]);
        int y = convertToTileNotation(component.getSnappedPosition()[1]);

        for (int i = x; i < x+component.getRelativeTileWidth(); i++) {
            for (int j = y; j < y+component.getRelativeTileHeight(); j++) {
                if (presence[i][j] != null) { //Trying to place somewhere that's already there ?!
                    throw new RuntimeException("Illegal placeable component placement !");
                }
                presence[i][j] = component; //Putting a reference
            }
        }
    }
    public static void removeFromPresence(Placeable[][] presence, Placeable component){
        int x = convertToTileNotation(component.getSnappedPosition()[0]);
        int y = convertToTileNotation(component.getSnappedPosition()[1]);

        for (int i = x; i < x+component.getRelativeTileWidth(); i++) {
            for (int j = y; j < y+component.getRelativeTileHeight(); j++) {
                if (presence[i][j] == null) { //Trying to remove somewhere that's already there ?!
                    throw new RuntimeException("Illegal placeable component removal !");
                }
                //Null always represents air in presence.
                presence[i][j] = null;
            }
        }
    }

    public static boolean overlaps(Placeable[][] presence, Placeable component) {
        //Checks if a placeable overlaps with one from the presenceMap (it can't already be there !)
        int x = convertToTileNotation(component.getSnappedPosition()[0]);
        int y = convertToTileNotation(component.getSnappedPosition()[1]);
        if (x < 0 || y < 0 || x+component.getRelativeTileWidth() > widthX || y+component.getRelativeTileHeight() > widthY) {
            return true; //"Overlaps" with the Out-Of-Bounds !
        }
        for (int i = x; i < x+component.getRelativeTileWidth(); i++) {
            for (int j = y; j < y+component.getRelativeTileHeight(); j++) {
                if (presence[i][j] != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isMutualComplement(Set<Rotation> rotations) {
        List<Rotation> rotationList = rotations.stream().toList();
        return rotationList.size() == 2 && rotationList.get(0) == complement(rotationList.get(1));
    }

    public static boolean any(boolean[] array) {
        for (boolean b : array) {
            if (b) {
                return true;
            }
        }
        return false;
    }
}
