package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.utilities.Rotation;
import me.analyzers.scs.utilities.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class PrimitiveTrue extends ComponentHolder implements PrimitiveComponent {
    public PrimitiveTrue(int[] position, int wireSize, Rotation startingRotation) {
        super("True " + wireSize, position, 1, 1, wireSize, new int[]{0}, new int[]{0}, startingRotation, null);
    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        boolean[] result = new boolean[getWireSize()];

        for (int i = 0; i < getWireSize(); i++) {
            result [i] = true;
        }

        return new boolean[][]{result};
    }
}