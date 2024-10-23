package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.utilities.IllegalInputsException;
import me.analyzers.scs.utilities.Rotation;

public class PrimitiveNOT extends ComponentHolder implements PrimitiveComponent {
    public PrimitiveNOT(int[] position, Rotation startingRotation) {
        super("NOT", position, 1, 1, new int[]{0}, new int[]{0}, startingRotation, null);

    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        if (inputs.length != 1) {
            throw new IllegalInputsException();
        }

        boolean[] a = inputs[0];

        if (a.length == 0) {
            return new boolean[1][0]; //Empty array: floating output.
        }

        boolean[] result = new boolean[a.length];

        for (int i = 0; i < a.length; i++) {
            result[i] = !a[i];
        }

        return new boolean[][]{result};
    }
}
