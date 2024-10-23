package me.analyzers.scs.panels;

import me.analyzers.scs.Main;
import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.game.InventoryComponent;
import me.analyzers.scs.game.MainPanel;
import me.analyzers.scs.game.StoredState;
import me.analyzers.scs.primitiveComponents.*;
import me.analyzers.scs.utilities.GameState;
import me.analyzers.scs.utilities.KeyPressHandler;
import me.analyzers.scs.utilities.MouseClickHandler;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

import static me.analyzers.scs.game.MainPanel.*;
import static me.analyzers.scs.utilities.MathUtils.getClickedInventoryComponent;

public class InventoryMenu {
    private final ArrayList<InventoryComponent> inventoryComponents;
    private Path currentLocation = inventoryDirectory;

    public InventoryMenu() {
        inventoryComponents = new ArrayList<>();
        try {
            initializeInventory();
        } catch (IOException e) {
            System.err.println("Error while initializing inventory.");
        }
    }

    private void initializeInventory() throws IOException {
        inventoryComponents.clear();
        if (currentLocation.equals(inventoryDirectory)) {
            //Primitive components.
            addComponent(new InventoryComponent(new PrimitiveAND(null, null), "AND Gate"));
            addComponent(new InventoryComponent(new PrimitiveOR(null, null), "OR Gate"));
            addComponent(new InventoryComponent(new PrimitiveXOR(null, null), "XOR Gate"));
            addComponent(new InventoryComponent(new PrimitiveNAND(null, null), "NAND Gate"));
            addComponent(new InventoryComponent(new PrimitiveNOR(null, null), "NOR Gate"));
            addComponent(new InventoryComponent(new PrimitiveXNOR(null, null), "XNOR Gate"));
            addComponent(new InventoryComponent(new PrimitiveSplitter(null, null), "Splitter"));
            addComponent(new InventoryComponent(new PrimitiveMerger(null, null), "Merger"));
            addComponent(new InventoryComponent(new PrimitiveTrue(null, null), "True"));
            addComponent(new InventoryComponent(new PrimitiveFalse(null, null), "False"));
            addComponent(new InventoryComponent(new PrimitiveLatch(null, null), "D Latch"));
            addComponent(new InventoryComponent(new PrimitiveNOT(null, null), "Not Gate"));
            addComponent(new InventoryComponent(new PrimitiveOutput(null, null), "Out"));
            addComponent(new InventoryComponent(new PrimitiveInput(null, null), "In"));
            //Delays just spit out whatever was given to them. In truth, wireSize is mostly there to guarantee coherency between >1 input gates, as a check is done.
            addComponent(new InventoryComponent(new PrimitiveDelay(null, null), "Delay"));
        } else {
            addComponent(new InventoryComponent(currentLocation.getParent()));
        }

        //Adding directories & other files
        try (Stream<Path> paths = Files.list(currentLocation)) {
            paths.forEach(f -> {
                if (Files.isDirectory(f)) {
                    addComponent(new InventoryComponent(f));
                } else {
                    try {
                        //We're assuming only directories and states are in here. Don't pollute !
                        StoredState storedState = StoredState.readFromFile(f);
                        if (storedState == null) {
                            System.err.println("Error : null state for file " + f.getFileName() + " when initializing inventory.");
                        } else {
                            addComponent(new InventoryComponent(storedState.getRepresentation(), null, storedState.getRepresentation().getName(), storedState, false));
                        }
                    } catch (IOException e) {
                        System.err.println("Error : unable to retrieve file " + f.getFileName() + " when initializing inventory. (Deprecated format ?)");
                    }
                }
            });
        }
    }

    private void addComponent(InventoryComponent inventoryComponent) {
        int maxComponents = (widthX / 4) * (widthY / 4);
        if (inventoryComponents.size() == maxComponents) {
            System.err.println("Warning : could not add inventory component " + inventoryComponent.getDescription() + " : max size reached for this window size)");
            return;
        }
        inventoryComponents.add(inventoryComponent);
    }

    public void render(Graphics2D g2d) {
        for (InventoryComponent inventoryComponent : inventoryComponents) {
            //Rendering by its own method
            int index = inventoryComponents.indexOf(inventoryComponent);
            inventoryComponent.render(g2d, index);
        }
    }

    public void update(MouseClickHandler mouseClickHandler, KeyPressHandler keyPressHandler,
                                  BuildingMenu buildingMenu) throws IOException {
        if (mouseClickHandler.isNew()) {
            int clickType = mouseClickHandler.getType();

            mouseClickHandler.setNew(false);
            InventoryComponent clickedComponent = getClickedInventoryComponent(mouseClickHandler, inventoryComponents);

            //Only proceed if the user clicked a component
            if (clickedComponent == null) {
                return;
            }

            if (clickType == LEFT_CLICK) {
                //Change held component
                if (!clickedComponent.isDirectory()) {
                    ComponentHolder template = clickedComponent.getComponent();
                    buildingMenu.setCurrentComponent(template);
                    gameState = GameState.BUILDING;
                    return;
                }
                //Is a directory, reload
                currentLocation = clickedComponent.getDirectoryPath();
                initializeInventory();
            } else if (clickType == RIGHT_CLICK) {
                //Set state and remake ACM
                buildingMenu.setBoardState(clickedComponent.getGameState());
                gameState = GameState.BUILDING;
            }
        }
    }
}
