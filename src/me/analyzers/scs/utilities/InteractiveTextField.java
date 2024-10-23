package me.analyzers.scs.utilities;

import java.awt.*;
import java.util.Arrays;
import java.util.stream.Stream;

import static me.analyzers.scs.game.MainPanel.realTileSize;
import static me.analyzers.scs.game.MainPanel.sixteenth;

public class InteractiveTextField {
    private final StringBuffer contents;
    private final int[] realPosition;
    private final int realWidth;
    private final int realHeight;
    private final int limit;
    private Color color = Color.BLACK;

    public InteractiveTextField(int[] realPosition, int realWidth, int realHeight, int limit) {
        this.contents = new StringBuffer(limit);
        this.realPosition = realPosition;
        this.realWidth = realWidth;
        this.realHeight = realHeight;
        this.limit = limit;
    }

    public void render(Graphics2D g2d) {
        //Making visible bounding box
        g2d.setColor(color);
        g2d.fillRect(realPosition[0], realPosition[1], realWidth, realHeight);
        g2d.setColor(Color.white);
        g2d.fillRect(realPosition[0]+sixteenth/2, realPosition[1] + sixteenth/2, realWidth - sixteenth, realHeight - sixteenth);
        g2d.setColor(color);

        //Drawing the string !
        g2d.drawString(contents.toString(), realPosition[0] + 2*sixteenth, realPosition[1] + realTileSize/2);
    }

    public void access(char c) {
        //Only let alphanumerics in !
        if (c == 8) {
            if (!contents.isEmpty()) {
                contents.deleteCharAt(contents.length() - 1);
            }
            return;
        }

        //Only allow alphanumerics, and abort if at limit.
        if (!(Character.isDigit(c) || Character.isAlphabetic(c) || c == '_' || c == '-') || contents.length() == limit) {
            return;
        }

        contents.append(c);
    }

    public void drawTitle(String title, Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.drawString(title, realPosition[0], realPosition[1]);
    }

    public String getContents() {
        return contents.toString();
    }

    public int[] getRealPosition() {
        return realPosition;
    }

    public int getRealHeight() {
        return realHeight;
    }

    public int getRealWidth() {
        return realWidth;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public static Stream<InteractiveTextField> getTextFields(Object object) {
        return Arrays.stream(object.getClass().getFields())
                .filter(f -> f.getType().equals(InteractiveTextField.class))
                .map(field -> {
                    try {
                        return (InteractiveTextField) field.get(object);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
