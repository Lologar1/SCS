package me.analyzers.scs.game;

import me.analyzers.scs.utilities.Placeable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;

public class StoredState implements Serializable {
    private final ComponentHolder representation;
    private final Placeable[][] presenceMap;
    private final LinkedList<Wire> wireQueue;
    private final LinkedList<ComponentHolder> componentQueue;


    public StoredState(ComponentHolder representation, Placeable[][] presenceMap, LinkedList<Wire> wireQueue, LinkedList<ComponentHolder> componentQueue) {
        this.representation = representation;
        this.presenceMap = presenceMap;
        this.wireQueue = wireQueue;
        this.componentQueue = componentQueue;
    }

    public ComponentHolder getRepresentation() {
        return representation;
    }

    public LinkedList<ComponentHolder> getComponentQueue() {
        return componentQueue;
    }

    public LinkedList<Wire> getWireQueue() {
        return wireQueue;
    }

    public Placeable[][] getPresenceMap() {
        return presenceMap;
    }

    public void writeToFile(Path file) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(this);
            Files.write(file, byteArrayOutputStream.toByteArray());
        }
    }

    public static StoredState readFromFile(Path file) throws IOException{
        if (!Files.exists(file)) {
            return null;
        }

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Files.readAllBytes(file));
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return (StoredState) objectInputStream.readObject();
        } catch (ClassNotFoundException | ClassCastException e) {
            System.err.println("Error deserializing stored state in file " + file.getFileName());
            return null;
        }
    }
}
