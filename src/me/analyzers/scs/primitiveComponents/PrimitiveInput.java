package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.game.Interactable;
import me.analyzers.scs.utilities.Rotation;
import me.analyzers.scs.utilities.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PrimitiveInput extends ComponentHolder implements PrimitiveComponent, PrimitiveIO, Interactable {
    private boolean value;
    public PrimitiveInput(int[] position, int wireSize, Rotation startingRotation) {
        super("In " + wireSize, position, 1, 1, wireSize, new int[]{}, new int[]{0}, startingRotation, null);
        value = false;
    }

    public void toggle() {
        this.value = !value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        boolean[] result = new boolean[getWireSize()];

        for (int i = 0; i < getWireSize(); i++) {
            result[i] = value;
        }

        return new boolean[][]{result};
    }

    @Override
    public void interact() {
        toggle();
    }
}
