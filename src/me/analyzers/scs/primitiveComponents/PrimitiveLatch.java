package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.utilities.IllegalInputsException;
import me.analyzers.scs.utilities.Rotation;

import java.util.Random;
import java.util.stream.IntStream;

public class PrimitiveLatch extends ComponentHolder implements PrimitiveComponent {
    private boolean[] contents = new boolean[0];
    public PrimitiveLatch(int[] position, Rotation startingRotation) {
        super("Latch", position, 3, 1, new int[]{0, 2}, new int[]{1}, startingRotation, null);
    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        if (inputs.length != 2) {
            throw new IllegalInputsException();
        }

        boolean[] a = inputs[0];
        boolean[] b = inputs[1];

        if (a.length == 0) {
            return new boolean[1][0]; //Empty array: floating output.
        }

        //RNG feature for undefined data input ! Yay !
        if (b.length == 0) {
            Random random = new Random();
            b = new boolean[64]; //Hard coded 64 bits
            for (int i = 0; i < b.length; i++) {
                b[i] = random.nextBoolean();
            }
        }

        if (a[0]) {
            contents = b.clone();
        }

        return new boolean[][]{contents};
    }
}
