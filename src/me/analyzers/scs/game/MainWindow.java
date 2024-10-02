package me.analyzers.scs.game;

import me.analyzers.scs.game.MainPanel;

import javax.swing.*;

public class MainWindow {
    private final JFrame window;

    public MainWindow() {
        //Setting misc. window-related stuff
        window = new JFrame();
        window.setIconImage(new ImageIcon("chip.png").getImage());
        window.setTitle("SCS V0.2");
        window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        window.setSize(800, 500);
        window.setLocationRelativeTo(null);
    }

    public void show() {
        //Is called by the main method
        MainPanel mainPanel = new MainPanel();
        //Since we're using our own graphics handling, we simply "plug" a panel and use its drawing methods
        //This means there is only ever one component, and it is the MainPanel
        window.add(mainPanel);
        window.setVisible(true);
        window.pack();
        //Everything happens here. Also runs on the EDT.
        mainPanel.startGameLoop();
    }
}
