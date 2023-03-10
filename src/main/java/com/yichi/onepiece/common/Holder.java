package com.yichi.onepiece.common;

public class Holder<T> {
    private T element;

    public T get() {
        return element;
    }

    public void set(T e) {
        element = e;
    }
}
