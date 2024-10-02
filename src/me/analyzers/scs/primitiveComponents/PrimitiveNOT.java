package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.utilities.IncompleteInputsException;
import me.analyzers.scs.utilities.Rotation;
import me.analyzers.scs.utilities.Tuple;
import me.analyzers.scs.utilities.UnmatchingWiresException;

import java.util.List;
import java.util.stream.Collectors;

public class PrimitiveNOT extends ComponentHolder implements PrimitiveComponent {
    public PrimitiveNOT(int[] position, int wireSize, Rotation startingRotation) {
        super("NOT " + wireSize, position, 1, 1, wireSize, new int[]{0}, new int[]{0}, startingRotation, null);

    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        if (inputs.length < 1) {
            throw new IncompleteInputsException();
        }

        boolean[] a = inputs[0];

        boolean[] result = new boolean[a.length];

        for (int i = 0; i < a.length; i++) {
            result[i] = !a[i];
        }

        return new boolean[][]{result};
    }
}
