package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.utilities.IncompleteInputsException;
import me.analyzers.scs.utilities.Rotation;
import me.analyzers.scs.utilities.Tuple;
import me.analyzers.scs.utilities.UnmatchingWiresException;

import java.util.List;

import static me.analyzers.scs.utilities.MathUtils.getCombinatorIO;

public class PrimitiveMerger extends ComponentHolder implements PrimitiveComponent {
    public PrimitiveMerger(int[] position, int wireSize, Rotation startingRotation) {
        super("Merger " + wireSize, position, wireSize, 1, wireSize, getCombinatorIO(wireSize),
                new int[]{wireSize/2}, startingRotation, null);
    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        if (inputs.length < 1) {
            throw new IncompleteInputsException();
        }

        boolean[] result = new boolean[getWireSize()];

        //Contract: when setting up a merger/splitter, I/O should be properly tested !
        int i = 0;
        for(boolean[] input : inputs) {
            for (boolean value : input) {
                result[i] = value;
                i++;
            }
        }

        return new boolean[][]{result};
    }
}