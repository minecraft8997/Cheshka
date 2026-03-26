package com.deewend.cheshka;

public class Either<A, B> {
    private final Object value;

    public Either(Object value) {
        this.value = value;
    }

    public static <A, B> Either<A, B> of(Object value) {
        return new Either<>(value);
    }

    /** @noinspection unchecked*/
    public A first() {
        return (A) value;
    }

    /** @noinspection unchecked*/
    public B second() {
        return (B) value;
    }
}
