package me.analyzers.scs.primitiveComponents;

import me.analyzers.scs.utilities.Tuple;

import java.util.ArrayList;
import java.util.List;

public interface PrimitiveComponent {
    boolean[][] evaluate(boolean[]... inputs);
}
