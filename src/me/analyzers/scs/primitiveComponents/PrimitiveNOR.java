package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.utilities.IllegalInputsException;
import me.analyzers.scs.utilities.Rotation;
import me.analyzers.scs.utilities.UnmatchingWiresException;

public class PrimitiveNOR extends ComponentHolder implements PrimitiveComponent {
    public PrimitiveNOR(int[] position, Rotation startingRotation) {
        super("NOR", position, 3, 1, new int[]{0, 2}, new int[]{1}, startingRotation, null);

    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        if (inputs.length != 2) {
            throw new IllegalInputsException();
        }

        boolean[] a = inputs[0];
        boolean[] b = inputs[1];

        if (a.length == 0 && b.length == 0) {
            return new boolean[1][0]; //Empty array: floating output.
        }

        //Initialized to zero matching the other input's length
        a = a.length == 0 ? new boolean[b.length] : a;
        b = b.length == 0 ? new boolean[a.length] : b;

        if (a.length != b.length) { throw new UnmatchingWiresException(); }
        boolean[] result = new boolean[a.length];

        for (int i = 0; i < a.length; i++) {
            result[i] = !(a[i] | b[i]);
        }

        return new boolean[][]{result};
    }
}
