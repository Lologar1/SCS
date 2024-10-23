package me.analyzers.scs.utilities;

public record Tuple<T, U>(T a, U b) {

    public T getA() {
        return a;
    }

    public U getB() {
        return b;
    }
}
