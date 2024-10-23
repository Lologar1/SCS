package me.analyzers.scs.panels;

import me.analyzers.scs.game.AbstractComponentMap;
import me.analyzers.scs.game.ComponentHolder;
import me.analyzers.scs.game.StoredState;
import me.analyzers.scs.game.Wire;
import me.analyzers.scs.utilities.*;

import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;

import static me.analyzers.scs.game.MainPanel.*;
import static me.analyzers.scs.utilities.MathUtils.*;

public class SavingMenu {
    //Always set the InteractiveTextFields as public !
    public final InteractiveTextField name;
    public final InteractiveTextField relativeInputs;
    public final InteractiveTextField relativeOutputs;
    public final InteractiveTextField tileWidth;
    public final InteractiveTextField tileHeight;

    private InteractiveTextField selectedField;

    private final Placeable[][] presenceMap;
    private final LinkedList<Wire> wireQueue;
    private final LinkedList<ComponentHolder> componentQueue;
    private final AbstractComponentMap acm;

    private final int[] saveButtonPosition;
    private final int saveButtonWidth = 5*realTileSize;
    private final int saveButtonHeight = 2*realTileSize;

    private ComponentHolder previewComponent;
    private String errorMessage = "";

    public SavingMenu(Placeable[][] presenceMap, LinkedList<Wire> wireQueue, LinkedList<ComponentHolder> componentQueue, AbstractComponentMap acm) {
        //Dividing the screen in quarters with the text fields
        name = new InteractiveTextField(new int[]{screenWidth/8, screenHeight/6}, 10*realTileSize, realTileSize, 16);
        relativeInputs = new InteractiveTextField(new int[]{screenWidth/8, 2*screenHeight/6}, 10*realTileSize, realTileSize, 16);
        relativeOutputs = new InteractiveTextField(new int[]{screenWidth/8, 3*screenHeight/6}, 10*realTileSize, realTileSize, 16);
        tileWidth = new InteractiveTextField(new int[]{screenWidth/8, 4*screenHeight/6}, 10*realTileSize, realTileSize, 1);
        tileHeight = new InteractiveTextField(new int[]{screenWidth/8, 5*screenHeight/6}, 10*realTileSize, realTileSize, 1);

        //Default to selecting name (first)
        selectedField = name;

        //Setting the three state variables + ACM
        this.presenceMap = presenceMap;
        this.wireQueue = wireQueue;
        this.componentQueue = componentQueue;
        this.acm = acm;

        //Save button stuff
        saveButtonPosition = new int[]{tileHeight.getRealPosition()[0] + 15*realTileSize, tileHeight.getRealPosition()[1]};
    }

    public boolean update(MouseClickHandler mouseClickHandler, KeyPressHandler keyPressHandler) {
        //Pass pressed key to currently selected text field
        if (keyPressHandler.isNew()) {
            keyPressHandler.setNew(false);
            selectedField.access(keyPressHandler.getCurrentKey());
        }

        //Prevent looping
        if (!mouseClickHandler.isNew()) {
            return false;
        }
        mouseClickHandler.setNew(false);

        int[] realPressPosition = mouseClickHandler.getPressFirstPosition();

        //Select the field
        Optional<InteractiveTextField> selectedOptional = InteractiveTextField.getTextFields(this).filter(t ->
                        isInsideRectangle(realPressPosition, t.getRealPosition(), t.getRealWidth(), t.getRealHeight()))
                .findFirst();
        selectedOptional.ifPresent(interactiveTextField -> selectedField = interactiveTextField);


        //Test for save button press
        if (isInsideRectangle(realPressPosition, saveButtonPosition, saveButtonWidth, saveButtonHeight)) {
            //Save the component and write errors if they occur.
            if (previewComponent == null) {
                errorMessage = "Component must be fully formed !";
                return false;
            }

            //Can use relative tile width as absolute tile width here because we know that rotation is always north.
            //Testing if I/O is outside, or overlapping with itself
            if (Arrays.stream(previewComponent.getRelativeOutputs()).anyMatch(out -> previewComponent.getRelativeTileWidth() <= out)
                    || Arrays.stream(previewComponent.getRelativeOutputs()).anyMatch(e ->
                    Arrays.stream(previewComponent.getRelativeOutputs()).filter(i -> i == e).count() > 1)) {
                errorMessage = "Bad component output positions";
                return false;
            }
            if (Arrays.stream(previewComponent.getRelativeInputs()).anyMatch(in -> previewComponent.getRelativeTileWidth() <= in)
                    || Arrays.stream(previewComponent.getRelativeInputs()).anyMatch(e ->
                    Arrays.stream(previewComponent.getRelativeInputs()).filter(i -> i == e).count() > 1)) {
                errorMessage = "Bad component input positions";
                return false;
            }
            if (previewComponent.getName().isEmpty()) {
                errorMessage = "Component must have a name !";
                return false;
            }

            //Component is legal and ready to go !
            StoredState storedState = new StoredState(previewComponent, presenceMap, wireQueue, componentQueue);
            try {
                storedState.writeToFile(inventoryDirectory.resolve(previewComponent.getName()));
            } catch (IOException e) {
                System.err.println("Error : Could not write saved state for " + previewComponent.getName() + " to file !");
            }
            return true;
        }

        return false;
    }

    public void render(Graphics2D g2d) {
        g2d.setColor(Color.black);

        //Render text fields
        InteractiveTextField.getTextFields(this).forEach(t -> t.render(g2d));

        //Render helpful info
        name.drawTitle("Component Name :", g2d);
        relativeInputs.drawTitle("Relative Inputs :", g2d);
        relativeOutputs.drawTitle("Relative Outputs :", g2d);
        tileHeight.drawTitle("Tile Height :", g2d);
        tileWidth.drawTitle("Tile Width :", g2d);

        //Render preview
        int defaultWidth = Math.max(Math.max(acm.getInputPins().size(), acm.getOutputPins().size()), 1);
        previewComponent = new ComponentHolder(name.getContents(),
                convertToSnappedNotation(new int[]{2*screenWidth/3, screenHeight/3}),
                tileWidth.getContents().matches("\\d+") ? Integer.parseInt(tileWidth.getContents()) : defaultWidth,
                tileHeight.getContents().matches("\\d+") ? Integer.parseInt(tileHeight.getContents()) : 1,
                //x - '0' will convert a char to its numeric value as ASCII numbers are contiguous
                relativeInputs.getContents().matches("\\d+") && relativeInputs.getContents().length() == acm.getInputPins().size()
                        ? relativeInputs.getContents().chars().map(x -> x - '0').toArray() : getCombinatorIO(acm.getInputPins().size()),
                relativeOutputs.getContents().matches("\\d+") && relativeOutputs.getContents().length() == acm.getOutputPins().size()
                        ? relativeOutputs.getContents().chars().map(x -> x - '0').toArray() : getCombinatorIO(acm.getOutputPins().size()),
                Rotation.NORTH, acm);
        previewComponent.render(g2d);

        g2d.setColor(Color.orange);

        //Render "SAVE" button at (y = tile height text box, x = tile X + 15 tile sizes)
        g2d.fillRect(saveButtonPosition[0], saveButtonPosition[1], saveButtonWidth, saveButtonHeight);
        g2d.setColor(Color.white);
        g2d.fillRect(saveButtonPosition[0]+sixteenth, saveButtonPosition[1]+sixteenth,
                saveButtonWidth-2*sixteenth, saveButtonHeight-2*sixteenth);

        g2d.setColor(Color.black);
        g2d.setFont(new Font(Font.DIALOG, Font.PLAIN, 20));
        g2d.setColor(Color.BLACK);
        g2d.drawString("SAVE", tileHeight.getRealPosition()[0]+2*sixteenth + 15*realTileSize, tileHeight.getRealPosition()[1]+realTileSize);
        g2d.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));

        //Error message (if any)
        g2d.setColor(Color.red);
        g2d.drawString(errorMessage, 0, realTileSize);
    }
}
