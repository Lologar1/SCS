package me.analyzers.scs.panels;

import me.analyzers.scs.game.MainPanel;
import me.analyzers.scs.utilities.InteractiveTextField;
import me.analyzers.scs.utilities.KeyPressHandler;
import me.analyzers.scs.utilities.MouseClickHandler;

import java.awt.*;
import java.util.Optional;

import static me.analyzers.scs.game.MainPanel.*;
import static me.analyzers.scs.utilities.MathUtils.isInsideRectangle;
import static me.analyzers.scs.utilities.MathUtils.parseCombinatorSpecifications;

public class OptionsMenu {
    public final InteractiveTextField baseWireSize;
    public final InteractiveTextField splitterIntervals;
    public final InteractiveTextField mergerIntervals;
    public final InteractiveTextField ups;

    private InteractiveTextField selectedField;

    public OptionsMenu() {
        baseWireSize = new InteractiveTextField(new int[]{screenWidth/8, screenHeight/5}, 10*realTileSize, realTileSize, 16);
        splitterIntervals = new InteractiveTextField(new int[]{screenWidth/8, 2*screenHeight/5}, 10*realTileSize, realTileSize, 40);
        mergerIntervals = new InteractiveTextField(new int[]{screenWidth/8, 3*screenHeight/5}, 10*realTileSize, realTileSize, 40);
        ups = new InteractiveTextField(new int[]{screenWidth/8, 4*screenHeight/5}, 10*realTileSize, realTileSize, 8);
        selectedField = baseWireSize;
    }

    public void update(MouseClickHandler mouseClickHandler, KeyPressHandler keyPressHandler) {
        if (keyPressHandler.isNew()) {
            keyPressHandler.setNew(false);
            selectedField.access(keyPressHandler.getCurrentKey());
        }

        if (baseWireSize.getContents().matches("\\d+")) {
            MainPanel.baseWireSize = Integer.parseInt(baseWireSize.getContents());
            baseWireSize.setColor(Color.GREEN);
        } else {
            baseWireSize.setColor(Color.BLACK);
        }

        try {
            MainPanel.splitterIntervals = parseCombinatorSpecifications(splitterIntervals.getContents());
            splitterIntervals.setColor(Color.GREEN);
        } catch (Exception ignored) {
            splitterIntervals.setColor(Color.BLACK);
        }

        try {
            MainPanel.mergerIntervals = parseCombinatorSpecifications(mergerIntervals.getContents());
            mergerIntervals.setColor(Color.GREEN);
        } catch (Exception ignored) {
            mergerIntervals.setColor(Color.BLACK);
        }

        if (ups.getContents().matches("\\d+")) {
            UPS = Integer.parseInt(ups.getContents());
            ups.setColor(Color.GREEN);
        } else {
            ups.setColor(Color.BLACK);
        }

        if (!mouseClickHandler.isNew()) {
            return;
        }

        mouseClickHandler.setNew(false);

        int[] realPressPosition = mouseClickHandler.getPressFirstPosition();

        //Select the field
        Optional<InteractiveTextField> selectedOptional = InteractiveTextField.getTextFields(this).filter(t ->
                        isInsideRectangle(realPressPosition, t.getRealPosition(), t.getRealWidth(), t.getRealHeight()))
                .findFirst();
        selectedOptional.ifPresent(interactiveTextField -> selectedField = interactiveTextField);
    }

    public void render(Graphics2D g2d) {
        g2d.setColor(Color.black);
        g2d.drawString("Changes are saved automatically.", 0, realTileSize);

        //Render text fields
        InteractiveTextField.getTextFields(this).forEach(t -> t.render(g2d));
        baseWireSize.drawTitle("Base component wire size :", g2d);
        splitterIntervals.drawTitle("Splitter intervals (Intervals using -, separated by _) :", g2d);
        mergerIntervals.drawTitle("Merger intervals (Intervals using -, separated by _) :", g2d);
        ups.drawTitle("Updates per second :", g2d);
    }
}
