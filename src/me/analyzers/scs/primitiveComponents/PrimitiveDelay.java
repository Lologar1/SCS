package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.utilities.IllegalInputsException;
import me.analyzers.scs.utilities.Rotation;

public class PrimitiveDelay extends ComponentHolder implements PrimitiveComponent{
    public PrimitiveDelay(int[] position, Rotation startingRotation) {
        super("Delay", position, 1, 1, new int[]{0}, new int[]{0}, startingRotation, null);

    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        if (inputs.length != 1) {
            throw new IllegalInputsException();
        }

        if (inputs[0].length == 0) {
            return new boolean[1][0]; //Empty array: floating output.
        }

        return new boolean[][]{inputs[0].clone()};
    }
}
