package me.analyzers.scs.game;

import java.awt.*;

public interface Placeable extends Cloneable {
    int[] getSnappedPosition();
    int getRealWidth();
    int getRealHeight();
    int getRelativeTileWidth();
    int getRelativeTileHeight();
    void render(Graphics2D g2d);
}
