package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.utilities.IncompleteInputsException;
import me.analyzers.scs.utilities.Rotation;
import me.analyzers.scs.utilities.Tuple;
import me.analyzers.scs.utilities.UnmatchingWiresException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class PrimitiveLatch extends ComponentHolder implements PrimitiveComponent {
    private boolean[] contents = new boolean[getWireSize()];
    public PrimitiveLatch(int[] position, int wireSize, Rotation startingRotation) {
        super("Latch " + wireSize, position, 3, 1, wireSize, new int[]{0, 2}, new int[]{1}, startingRotation, null);
        IntStream.range(0, wireSize).forEach(n -> contents[n] = false);
    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        if (inputs.length < 2) {
            throw new IncompleteInputsException();
        }

        boolean[] a = inputs[0];
        boolean[] b = inputs[1];

        if (a[0]) {
            contents = b;
        }

        return new boolean[][]{contents};
    }
}
