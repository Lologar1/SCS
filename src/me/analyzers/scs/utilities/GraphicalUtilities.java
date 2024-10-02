package me.analyzers.scs.utilities;

import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.game.InventoryComponent;
import me.analyzers.scs.game.Wire;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;

import static me.analyzers.scs.game.MainPanel.*;

public class GraphicalUtilities {
    public static final int inventoryComponentSize = 3*realTileSize;

    public static void savingGraphics(Graphics2D g2d, LinkedList<ComponentHolder> drawQueue) {
        //Special interactive GUI for saving components, or deleting them.
    }

    public static void inventoryGraphics(Graphics2D g2d, ArrayList<InventoryComponent> inventoryComponents) {
        for (InventoryComponent inventoryComponent : inventoryComponents) {
            //Rendering by its own method
            int index = inventoryComponents.indexOf(inventoryComponent);
            inventoryComponent.render(g2d, index);
        }
    }

    //The wire connection pins, also the I/O pins.
    public static final int pinSize = realTileSize/6;

    public static void buildingGraphics(Graphics2D g2d, LinkedList<Wire> wireQueue, LinkedList<ComponentHolder> componentQueue) {
        //Drawing grid
        g2d.setColor(Color.LIGHT_GRAY);
        for (int ran = 0; ran < screenHeight; ran += realTileSize) {
            g2d.drawLine(0, ran, screenWidth, ran);
        }
        for (int col = 0; col < screenWidth; col += realTileSize) {
            g2d.drawLine(col, 0, col, screenHeight);
        }
        g2d.setColor(Color.BLACK);

        //Drawing wires (so get painted over by I/O)
        for (Wire wire : wireQueue) {
            wire.render(g2d);
        }

        //Drawing components. Different components may need to override the method.
        for (ComponentHolder toDraw : componentQueue) {
            toDraw.render(g2d);
        }
    }
}
