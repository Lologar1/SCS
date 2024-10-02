package me.analyzers.scs;

import me.analyzers.scs.game.MainWindow;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        //Creates an instance of the window (Java Swing) and runs it on the EventDispatchThread
        MainWindow mainWindow = new MainWindow();
        SwingUtilities.invokeLater(mainWindow::show);
    }
}

