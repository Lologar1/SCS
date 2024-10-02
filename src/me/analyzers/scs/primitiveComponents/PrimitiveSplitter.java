package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.utilities.IncompleteInputsException;
import me.analyzers.scs.utilities.Rotation;
import me.analyzers.scs.utilities.Tuple;
import me.analyzers.scs.utilities.UnmatchingWiresException;

import java.util.List;

import static me.analyzers.scs.utilities.MathUtils.getCombinatorIO;

public class PrimitiveSplitter extends ComponentHolder implements PrimitiveComponent {
    public PrimitiveSplitter(int[] position, int wireSize, Rotation startingRotation) {
        super("Splitter " + wireSize, position, wireSize, 1, wireSize, new int[]{wireSize/2},
                getCombinatorIO(wireSize), startingRotation, null);
    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        if (inputs.length < 1) {
            throw new IncompleteInputsException();
        }

        boolean[] input = inputs[0];

        //For now, only support splitting into a whole bunch.
        //TODO: rework splitter and merger later !
        boolean[][] result = new boolean[getWireSize()][1];

        int i = 0;
        for(boolean value : input) {
            result[i][0] = value;
            i++;
        }

        return result;
    }
}
