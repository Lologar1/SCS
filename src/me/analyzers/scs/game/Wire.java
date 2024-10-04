package me.analyzers.scs.game;

import me.analyzers.scs.utilities.Activation;
import me.analyzers.scs.utilities.Rotation;

import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static me.analyzers.scs.game.MainPanel.*;
import static me.analyzers.scs.utilities.GraphicalUtilities.pinSize;
import static me.analyzers.scs.utilities.MathUtils.*;

public class Wire implements Placeable {
    private final Set<Rotation> partners;
    private final int[] position;
    private boolean showConnected = false;
    private boolean crossover = false;
    private Activation state = Activation.OFF;

    public Wire(int[] snappedPosition, Rotation... first) {
        this.position = snappedPosition;
        partners = new HashSet<>(4);
        partners.addAll(Arrays.asList(first));
    }

    public void setState(Activation state) {
        this.state = state;
    }

    public Activation getState() {
        return state;
    }

    public void setCrossover(boolean crossover) {
        this.crossover = crossover;
    }

    public boolean isCrossover() {
        return crossover;
    }

    @Override
    public int[] getSnappedPosition() {
        return position;
    }

    @Override
    public int getRealWidth() {
        return realTileSize;
    }

    @Override
    public int getRealHeight() {
        return realTileSize;
    }

    @Override
    public int getRelativeTileWidth() {
        return 1;
    }

    @Override
    public int getRelativeTileHeight() {
        return 1;
    }

    @Override
    public void render(Graphics2D g2d) {
        //Draw the wire on screen
                    /*
            Coloring activated wires.
            A hard 'bug' to figure out : crossovers will light up green in whole, even if just one of the "crosses is on.
            Mandatory TODO: to fix this one day. Purely visual inconvenience, low priority.
             */

        switch (getState()) {
            case OFF -> g2d.setColor(Color.GRAY);
            case ON -> g2d.setColor(Color.GREEN);
            case ERROR -> g2d.setColor(Color.RED);
        }

        //Draw every branch for wires.
        int[] position = getSnappedPosition();
        Set<Rotation> partners = getPartners();
        int halfTileSize = realTileSize/2;
        int centerX = position[0] + halfTileSize;
        int centerY = position[1] + halfTileSize;
        for (Rotation rotation : partners) {
            switch (rotation) {
                case NORTH -> g2d.drawLine(centerX, centerY, centerX, centerY-halfTileSize);
                case EAST -> g2d.drawLine(centerX, centerY, centerX+halfTileSize, centerY);
                case SOUTH -> g2d.drawLine(centerX, centerY, centerX, centerY+halfTileSize);
                case WEST -> g2d.drawLine(centerX, centerY, centerX-halfTileSize, centerY);
            }
        }

        //Draw the little node thingy
        if (isShowConnected()) {
            //Draw small connection node
            g2d.fillOval(centerX-pinSize/2, centerY-pinSize/2, pinSize, pinSize);
        }
    }

    public void autoSnap(Rotation wireLineDirection, Placeable[][] presenceMap, boolean snapToComponents) {
        //Snap to a newly created wireLine (if it exists) or to components if it's newly created

        if (snapToComponents) {
            //Getting a unique set of all close by components to snap to
            Set<ComponentHolder> closeComponents = new HashSet<>();
            int[] tilePositionFirst = convertToTileNotation(position);

            for (Rotation r : Rotation.values()) {
                int[] cardinal = toCardinalDirection(r);
                int i = cardinal[0];
                int j = cardinal[1];
                if (tilePositionFirst[0] + i > widthX - 1 || tilePositionFirst[0] + i < 0
                        || tilePositionFirst[1] + j > widthY - 1 || tilePositionFirst[1] + j < 0) {
                    //Trying to test componentHolder out of bounds of presenceMap; it's not there.
                    continue;
                }
                if (presenceMap[tilePositionFirst[0] + i][tilePositionFirst[1] + j] instanceof ComponentHolder) {
                    closeComponents.add((ComponentHolder) presenceMap[tilePositionFirst[0] + i][tilePositionFirst[1] + j]);
                }
            }

            //Adding all relevant rotations
            for (ComponentHolder componentHolder : closeComponents) {
                Rotation connect = snapToComponentIO(componentHolder, this);
                if (connect==null) {
                    //Nothing to snap to.
                    continue;
                }
                //Again, straight lines will get their setShowConnected cleared by cleanWire()
                setShowConnected(true);
                partners.add(connect);
            }
        } else {
            if (crossover) {
                //Rotation already matches, and no need to set show connected.
                return;
            }
            //Add the proper direction to connect it with the rest of the wireLine.
            //ShowConnected is set for connecting straight wires, but cleaned by cleanWire().
            setShowConnected(true);
            partners.add(wireLineDirection);
        }
    }

    public Set<Rotation> getPartners() {
        return partners;
    }

    public void setShowConnected(boolean showConnected) {
        this.showConnected = showConnected;
    }

    public boolean isShowConnected() {
        return showConnected;
    }

}
