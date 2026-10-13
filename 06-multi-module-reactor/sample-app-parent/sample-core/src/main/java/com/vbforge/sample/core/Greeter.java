package com.vbforge.sample.core;

/**
 * A trivial library class — its only purpose is to exist in {@code sample-core}
 * so that {@code sample-service} can depend on it, which is what gives the
 * reactor its build order.
 */
public class Greeter {

    public String greet(String name) {
        if (name == null || name.isBlank()) {
            return "Hello, stranger!";
        }
        return "Hello, " + name + "!";
    }
}