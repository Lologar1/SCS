package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.utilities.IncompleteInputsException;
import me.analyzers.scs.utilities.Rotation;
import me.analyzers.scs.utilities.Tuple;
import me.analyzers.scs.utilities.UnmatchingWiresException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PrimitiveAND extends ComponentHolder implements PrimitiveComponent {
    public PrimitiveAND(int[] position, int wireSize, Rotation startingRotation) {
        super("AND " + wireSize, position, 3, 1, wireSize, new int[]{0, 2}, new int[]{1}, startingRotation, null);

    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        if (inputs.length < 2) {
            throw new IncompleteInputsException();
        }

        boolean[] a = inputs[0];
        boolean[] b = inputs[1];

        if (a.length != b.length) { throw new UnmatchingWiresException(); }
        boolean[] result = new boolean[a.length];
        
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] & b[i];
        }
        
        return new boolean[][]{result};
    }
}
