package me.analyzers.scs.utilities;

public class IncompleteInputsException extends RuntimeException {
    public IncompleteInputsException(String message) {
        super(message);
    }

    public IncompleteInputsException() {}
}
