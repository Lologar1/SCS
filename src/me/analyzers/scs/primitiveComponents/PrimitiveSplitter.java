package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.game.MainPanel;
import me.analyzers.scs.utilities.IllegalInputsException;
import me.analyzers.scs.utilities.Rotation;

import java.util.Arrays;

import static me.analyzers.scs.utilities.MathUtils.getCombinatorIO;
import static me.analyzers.scs.utilities.MathUtils.getSpecificationAsString;

public class PrimitiveSplitter extends ComponentHolder implements PrimitiveComponent {
    int[][] splitterIntervals = new int[MainPanel.splitterIntervals.length][];

    public PrimitiveSplitter(int[] position, Rotation startingRotation) {
        super("Splitter " + getSpecificationAsString(MainPanel.splitterIntervals), position, MainPanel.splitterIntervals.length, 1,
                new int[]{MainPanel.splitterIntervals.length/2}, getCombinatorIO(MainPanel.splitterIntervals.length), startingRotation, null);

        //Copy of present intervals into this component
        for (int i = 0; i < MainPanel.splitterIntervals.length; i++) {
            splitterIntervals[i] = MainPanel.splitterIntervals[i].clone();
        }
    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        if (inputs.length != 1) {
            throw new IllegalInputsException();
        }

        boolean[] input = inputs[0];
        if (input.length == 0) {
            return new boolean[splitterIntervals.length][0]; //Floating
        }

        boolean[][] result = new boolean[splitterIntervals.length][];

        for (int i = 0; i < splitterIntervals.length; i++) {
            int[] interval = splitterIntervals[i];

            //Don't allow "backwards" intervals... Maybe one day ? TODO
            int start = Math.min(interval[0], interval[1]);
            int endInclusive = Math.max(interval[0], interval[1]);

            if (start < 0) {
                throw new IllegalInputsException(); //Badly formatted interval
            }

            result[i] = new boolean[endInclusive - start + 1];

            int relativeOutputIndex = 0; //We're mapping from absolute input (interval) to 0-indexed output.
            for (int wireIndex = start; wireIndex <= endInclusive; wireIndex++) {
                result[i][relativeOutputIndex] = input[wireIndex];
                relativeOutputIndex++;
            }
        }

        return result;
    }
}
