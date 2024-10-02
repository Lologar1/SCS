package me.analyzers.scs.game;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static me.analyzers.scs.game.MainPanel.realTileSize;
import static me.analyzers.scs.game.MainPanel.widthX;
import static me.analyzers.scs.utilities.GraphicalUtilities.inventoryComponentSize;

public class InventoryComponent {
    private ComponentHolder component;
    private Path directoryPath;
    private String description; //Also used as name in the case of being a directory.
    private boolean isDirectory;

    public InventoryComponent(ComponentHolder component, String path, String description, boolean isDir) {
        //For the special "OPTIONS" InventoryComponent, a hard-coded check will have to be added if its index is 0.
        //This way it's possible to have dirs or components named "Options"... for whatever reason.

        this.isDirectory = isDir;
        this.directoryPath = path == null ? null : Path.of(path);
        this.component = component;
        this.description = description;
    }

    public InventoryComponent(Path filePath) {
        //Instantiate a component from an existing directory or file.
        //TODO: Make it support actual files once I'm done with SAVING ! Mweh heh heh.

        if(!(Files.exists(filePath) && Files.isDirectory(filePath))) {
            //Error; all fields are left as null. This shouldn't happen.
            System.err.println("Error instantiating InventoryComponent from directory path " + filePath);
            return;
        }

        this.isDirectory = true;
        this.directoryPath = filePath;
        this.component = null;
        this.description = filePath.getFileName().toString();
    }

    public Path getDirectoryPath() {
        return directoryPath;
    }

    public void render(Graphics2D g2d, int index) {
        //Renders the InventoryComponent at the specified index in the 4x4 tile grid
        //For directories, don't display the current wireSize after description.
        int wireSize = getComponent() == null ? 0 : getComponent().getWireSize();
        String desc = getDescription() + " " + (wireSize <= 1 ? "" : wireSize);

        //getting its index relative to a 4x4 tile grid (-1 and + to offset by 1)
        //Then * 4 (to get real tile) and * realtilesize (to get real pixel)
        int realComponentX = (index%(widthX/4)) * 4 * realTileSize + realTileSize;
        int realComponentY = (index/(widthX/4)) * 4 * realTileSize + realTileSize;

        //Appropriate color. TODO: set green for OPTIONS, later.
        if (isDirectory()) {
            g2d.setColor(Color.YELLOW);
        } else {
            g2d.setColor(Color.BLACK);
        }

        g2d.fillRect(realComponentX, realComponentY, inventoryComponentSize, inventoryComponentSize);
        g2d.setColor(Color.WHITE);
        int sixteenth = realTileSize/16; //Borders
        g2d.fillRect(realComponentX+sixteenth, realComponentY+sixteenth, inventoryComponentSize -2*sixteenth, inventoryComponentSize -2*sixteenth);
        int nameWidth = g2d.getFontMetrics().stringWidth(desc);
        int offset = (inventoryComponentSize)/2 - nameWidth/2;
        g2d.setColor(Color.BLACK);
        g2d.drawString(desc, realComponentX + offset, realComponentY+ inventoryComponentSize /2);
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public ComponentHolder getComponent() {
        return component;
    }

    public String getDescription() {
        return description;
    }
}
