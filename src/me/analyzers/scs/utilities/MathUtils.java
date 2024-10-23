package me.analyzers.scs.utilities;

import me.analyzers.scs.game.*;

import java.util.*;

import static me.analyzers.scs.game.MainPanel.*;

public class MathUtils {

    public static boolean isInsideRectangle(int[] testPosition, int[] rectTopLeftPosition, int width, int height) {
        return testPosition[0] >= rectTopLeftPosition[0] && testPosition[0] <= rectTopLeftPosition[0] + width
                && testPosition[1] >= rectTopLeftPosition[1] && testPosition[1] <= rectTopLeftPosition[1] + height;
    }

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

        //Wire snapped and tile positions
        int[] wireSnappedPosition = wire.getSnappedPosition();
        int[] tileWirePosition = convertToTileNotation(wireSnappedPosition);

        int[][] inputPositions = getTileInputPositions(component);
        int[][] outputPositions = getTileOutputPositions(component);

        for (int[] inputPosition : inputPositions) {
            if (Arrays.equals(inputPosition, tileWirePosition)) {
                return component.getRotation();
            }
        }

        for (int[] outputPosition : outputPositions) {
            if (Arrays.equals(outputPosition, tileWirePosition)) {
                return complement(component.getRotation());
            }
        }

        //Did not match
        return null;
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
                    System.err.println("Illegal placeable component placement at [" + i + ", " + j + "]");
                    continue;
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
                    System.err.println("Illegal placeable component removal at [" + i + ", " + j + "]");
                    continue;
                }
                presence[i][j] = null; //Null represents air.
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

    public static int[][] parseCombinatorSpecifications(String raw) {
        String specs = raw.replaceAll("\\s+", ""); //Replace all whitespaces
        int[][] formatted = new int[specs.length() - specs.replace("_", "").length() + 1][2];

        //Will throw exceptions for bad input strings.
        int index = 0;
        for (String substring : specs.split("_")) {
            int firstPart;
            int secondPart;

            if (!substring.contains("-")) {
                firstPart = Integer.parseInt(substring);
                secondPart = firstPart;
            } else {
                firstPart = Integer.parseInt(substring.split("-")[0]);
                secondPart = Integer.parseInt(substring.split("-")[1]);
            }

            formatted[index] = new int[]{firstPart, secondPart};
            index++;
        }

        return formatted;
    }

    public static String getSpecificationAsString(int[][] specs) {
        StringBuilder mergerIntervals = new StringBuilder();
        for (int[] interval : specs) {
            if (interval[0] == interval[1]) {
                mergerIntervals.append(interval[0]).append(" ");
            } else {
                mergerIntervals.append(interval[0]).append("-").append(interval[1]).append(" ");
            }
        }
        mergerIntervals.setLength(mergerIntervals.length() - 1); //Remove last 2 chars

        return mergerIntervals.toString();
    }

}
