package com.vbforge.sample.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that this module's tests run as part of the reactor.
 */
class GreeterTest {

    private final Greeter greeter = new Greeter();

    @Test
    void greetsByName() {
        assertThat(greeter.greet("reactor")).isEqualTo("Hello, reactor!");
    }

    @Test
    void greetsNullAsStranger() {
        assertThat(greeter.greet(null)).isEqualTo("Hello, stranger!");
    }

    @Test
    void greetsBlankAsStranger() {
        assertThat(greeter.greet("  ")).isEqualTo("Hello, stranger!");
    }
}