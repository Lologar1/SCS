package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.utilities.IncompleteInputsException;
import me.analyzers.scs.utilities.Rotation;
import me.analyzers.scs.utilities.Tuple;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static me.analyzers.scs.game.MainPanel.realTileSize;
import static me.analyzers.scs.utilities.GraphicalUtilities.pinSize;
import static me.analyzers.scs.utilities.MathUtils.isPolar;

public class PrimitiveOutput extends ComponentHolder implements PrimitiveComponent, PrimitiveIO {
    private boolean value;
    public PrimitiveOutput(int[] position,
                          int wireSize, Rotation startingRotation) {
        super("Out " + wireSize, position, 1, 1, wireSize, new int[]{0}, new int[]{0}, startingRotation, null);
        value = false;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    @Override
    public boolean[][] evaluate(boolean[]... inputs) {
        if (inputs.length < 1) {
            throw new IncompleteInputsException();
        }

        boolean[] a = inputs[0];
        value = IntStream.range(0, a.length).anyMatch(i -> a[i]);

        return new boolean[][]{a};
    }

    @Override
    public boolean getValue() {
        return value;
    }
}
