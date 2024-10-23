package me.analyzers.scs.utilities;

import java.awt.*;
import java.io.Serializable;

public interface Placeable extends Cloneable, Serializable {
    int[] getSnappedPosition();
    int getRealWidth();
    int getRealHeight();
    int getRelativeTileWidth();
    int getRelativeTileHeight();
    void render(Graphics2D g2d);
}
