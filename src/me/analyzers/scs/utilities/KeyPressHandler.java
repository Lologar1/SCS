package me.analyzers.scs.utilities;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyPressHandler implements KeyListener {
    private Character currentKey = 0;
    private boolean isNew = false;
    private boolean isShiftDown = false;

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.isShiftDown()) {
            isShiftDown = true;
        }
        currentKey = e.getKeyChar();
        isNew = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        isShiftDown = false;
        currentKey = 0;
    }

    public char getCurrentKey() {
        return currentKey;
    }

    public boolean isShiftDown() {
        return isShiftDown;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }
}
