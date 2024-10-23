package me.analyzers.scs.game;

import me.analyzers.scs.utilities.Activation;

import java.io.Serializable;
import java.util.ArrayList;

public class ComponentConnection implements Serializable {
    private final ComponentHolder component;
    private final ArrayList<Wire> wireLine;
    private final int inPort;
    private boolean isTopLevel;

    public ComponentConnection(ComponentHolder component, ArrayList<Wire> wireLine, int inPort, boolean isTopLevel) {
        this.component = component;
        this.wireLine = wireLine;
        this.inPort = inPort;
        this.isTopLevel = isTopLevel;
    }

    public ArrayList<Wire> getWireLine() {
        return wireLine;
    }

    public int getInPort() {
        return inPort;
    }

    public boolean isTopLevel() {
        return isTopLevel;
    }

    public ComponentHolder getComponent() {
        return component;
    }

    public void setTopLevel(boolean topLevel) {
        isTopLevel = topLevel;
    }

    public void setState(Activation state) {
        if (!isTopLevel) {
            return;
        }
        wireLine.forEach(w -> w.setState(state));
    }
}