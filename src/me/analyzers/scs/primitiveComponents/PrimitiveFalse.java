package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.utilities.IncompleteInputsException;
import me.analyzers.scs.utilities.Rotation;
import me.analyzers.scs.utilities.Tuple;
import me.analyzers.scs.utilities.UnmatchingWiresException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class PrimitiveFalse extends ComponentHolder implements PrimitiveComponent{
    public PrimitiveFalse(int[] position, int wireSize, Rotation startingRotation) {
        super("False " + wireSize, position, 1, 1, wireSize, new int[]{0}, new int[]{0}, startingRotation, null);
    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        boolean[] result = new boolean[getWireSize()];

        for (int i = 0; i < getWireSize(); i++) {
            result [i] = false;
        }

        return new boolean[][]{result};
    }
}