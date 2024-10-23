package me.analyzers.scs.utilities;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class MouseClickHandler implements MouseListener, MouseMotionListener {
    private int lastClickX = 0;
    private int lastClickY = 0;
    private int type;
    private boolean isNew = false;
    private boolean dragging;
    private int[] pressReleasePosition;
    private int[] hoveringPosition = new int[]{0, 0};

    @Override
    public void mouseClicked(MouseEvent e) {} // All handled in pressed and released

    public int getType() {
        return type;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public int getLastClickX() {
        return lastClickX;
    }

    public int getLastClickY() {
        return lastClickY;
    }

    public boolean isDragging() {
        return dragging;
    }

    public int[] getHoveringPosition() {
        return hoveringPosition;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        dragging = true;
        this.lastClickX = e.getX();
        this.lastClickY = e.getY();
        this.type = e.getButton();
        isNew = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
        pressReleasePosition = new int[]{e.getX(), e.getY()};
    }

    public int[] getPressFirstPosition() {
        return new int[]{lastClickX, lastClickY};
    }

    public int[] getPressReleasePosition() {
        return pressReleasePosition;
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        hoveringPosition = new int[]{e.getX(), e.getY()};
    }
}
