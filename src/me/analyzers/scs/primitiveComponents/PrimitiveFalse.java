package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.Main;
import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.game.MainPanel;
import me.analyzers.scs.panels.BuildingMenu;
import me.analyzers.scs.utilities.Rotation;

public class PrimitiveFalse extends ComponentHolder implements PrimitiveComponent{
    private final int wireSize;

    public PrimitiveFalse(int[] position, Rotation startingRotation) {
        super("False " + MainPanel.baseWireSize, position, 1, 1, new int[]{}, new int[]{0}, startingRotation, null);
        wireSize = MainPanel.baseWireSize;
    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        boolean[] result = new boolean[wireSize];

        return new boolean[][]{result}; //Array of bool initialized to false by default
    }
}