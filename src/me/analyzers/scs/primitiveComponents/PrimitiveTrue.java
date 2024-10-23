package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.game.MainPanel;
import me.analyzers.scs.utilities.Rotation;
import me.analyzers.scs.utilities.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class PrimitiveTrue extends ComponentHolder implements PrimitiveComponent {
    private final int wireSize;

    public PrimitiveTrue(int[] position, Rotation startingRotation) {
        super("True " + MainPanel.baseWireSize, position, 1, 1, new int[]{}, new int[]{0}, startingRotation, null);
        wireSize = MainPanel.baseWireSize;
    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        boolean[] result = new boolean[wireSize];
        Arrays.fill(result, true);

        return new boolean[][]{result};
    }
}