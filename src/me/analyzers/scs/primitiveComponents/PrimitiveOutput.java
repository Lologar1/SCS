package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.game.MainPanel;
import me.analyzers.scs.utilities.IllegalInputsException;
import me.analyzers.scs.utilities.Rotation;

import java.util.stream.IntStream;

public class PrimitiveOutput extends ComponentHolder implements PrimitiveComponent, PrimitiveIO {
    private boolean value;

    public PrimitiveOutput(int[] position, Rotation startingRotation) {
        super("Out", position, 1, 1, new int[]{0}, new int[]{}, startingRotation, null);
        value = false;
    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        if (inputs.length != 1) {
            throw new IllegalInputsException();
        }

        boolean[] a = inputs[0];

        value = IntStream.range(0, a.length).anyMatch(i -> a[i]);

        return new boolean[0][0]; //Shouldn't be connected to anything.
    }

    @Override
    public boolean getValue() {
        return value;
    }
}
