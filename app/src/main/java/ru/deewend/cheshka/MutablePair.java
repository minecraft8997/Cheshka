package ru.deewend.cheshka;

public class MutablePair<A, B> {
    public A first;
    public B second;

    public MutablePair() {
    }

    public MutablePair(A first, B second) {
        this.first = first;
        this.second = second;
    }
}
