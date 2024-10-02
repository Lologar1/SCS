package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.utilities.IncompleteInputsException;
import me.analyzers.scs.utilities.Rotation;
import me.analyzers.scs.utilities.Tuple;
import me.analyzers.scs.utilities.UnmatchingWiresException;

import java.util.List;

public class PrimitiveDelay extends ComponentHolder implements PrimitiveComponent{
    public PrimitiveDelay(int[] position, Rotation startingRotation) {
        super("Delay", position, 1, 1, 1, new int[]{0}, new int[]{0}, startingRotation, null);

    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        if (inputs.length < 1) {
            throw new IncompleteInputsException();
        }

        return new boolean[][]{inputs[0]};
    }
}
