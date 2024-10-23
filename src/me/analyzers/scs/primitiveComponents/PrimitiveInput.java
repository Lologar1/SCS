package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.game.MainPanel;
import me.analyzers.scs.utilities.Interactable;
import me.analyzers.scs.utilities.Rotation;

import java.util.Arrays;

public class PrimitiveInput extends ComponentHolder implements PrimitiveComponent, PrimitiveIO, Interactable {
    private boolean value;
    private final int wireSize;

    public PrimitiveInput(int[] position, Rotation startingRotation) {
        super("In " + MainPanel.baseWireSize, position, 1, 1, new int[]{}, new int[]{0}, startingRotation, null);
        value = false;
        wireSize = MainPanel.baseWireSize;
    }

    public void toggle() {
        this.value = !value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        boolean[] result = new boolean[wireSize];

        Arrays.fill(result, value);

        return new boolean[][]{result};
    }

    @Override
    public void interact() {
        toggle();
    }
}
