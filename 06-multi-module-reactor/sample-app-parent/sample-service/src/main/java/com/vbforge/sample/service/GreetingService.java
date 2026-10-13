package com.vbforge.sample.service;

import com.vbforge.sample.core.Greeter;

import java.util.Locale;

/**
 * Uses {@link Greeter} from {@code sample-core}. The fact that this module
 * compiles at all is the proof that the reactor built {@code sample-core}
 * first.
 */
public class GreetingService {

    private final Greeter greeter;

    public GreetingService() {
        this(new Greeter());
    }

    public GreetingService(Greeter greeter) {
        this.greeter = greeter;
    }

    public String greet(String name) {
        return greeter.greet(name).toUpperCase(Locale.ROOT);
    }
}