package me.analyzers.scs.utilities;

public class IllegalInputsException extends RuntimeException {
    public IllegalInputsException(String message) {
        super(message);
    }

    public IllegalInputsException() {}
}
