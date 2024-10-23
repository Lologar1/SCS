package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.Main;
import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.game.MainPanel;
import me.analyzers.scs.utilities.IllegalInputsException;
import me.analyzers.scs.utilities.Rotation;

import java.util.Arrays;

import static me.analyzers.scs.utilities.MathUtils.getCombinatorIO;
import static me.analyzers.scs.utilities.MathUtils.getSpecificationAsString;

public class PrimitiveMerger extends ComponentHolder implements PrimitiveComponent {
    int[][] mergerIntervals = new int[MainPanel.mergerIntervals.length][];

    public PrimitiveMerger(int[] position, Rotation startingRotation) {
        super("Merger " + getSpecificationAsString(MainPanel.mergerIntervals), position, MainPanel.mergerIntervals.length, 1,
                getCombinatorIO(MainPanel.mergerIntervals.length), new int[]{MainPanel.mergerIntervals.length/2}, startingRotation, null);

        //Copy of present intervals into this component
        for (int i = 0; i < MainPanel.mergerIntervals.length; i++) {
            mergerIntervals[i] = MainPanel.mergerIntervals[i].clone();
        }
    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        if (inputs.length != mergerIntervals.length) {
            throw new IllegalInputsException();
        }

        //Sum all (inclusive) intervals
        boolean[] result = new boolean[Arrays.stream(mergerIntervals).mapToInt(i -> Math.abs(i[0] - i[1]) + 1).sum()];

        for (int i = 0; i < mergerIntervals.length; i++) {
            int[] interval = mergerIntervals[i];

            //Don't allow "backwards" intervals... Maybe one day ? TODO
            int start = Math.min(interval[0], interval[1]);
            int endInclusive = Math.max(interval[0], interval[1]);

            if (start < 0) {
                throw new IllegalInputsException(); //Badly formatted interval
            }

            //Throws an error if the inputs are illegally connected. (ArrayIndexOutOfBounds)
            int relativeInputIndex = 0; //We're mapping from the inputs (indexed at 0) to the absolute output (indexed by wireIndex)
            for (int wireIndex = start; wireIndex <= endInclusive; wireIndex++) {

                if (inputs[i].length == 0) {
                    return new boolean[1][0]; //Floating
                }
                result[wireIndex] = inputs[i][relativeInputIndex];
                relativeInputIndex++;
            }
        }

        return new boolean[][]{result};
    }
}