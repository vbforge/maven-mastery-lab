package com.vbforge.sample.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs in the reactor after {@code sample-core}'s tests, exercising a class
 * that lives in a different module.
 */
class GreetingServiceTest {

    private final GreetingService service = new GreetingService();

    @Test
    void uppercasesTheGreeting() {
        assertThat(service.greet("reactor")).isEqualTo("HELLO, REACTOR!");
    }

    @Test
    void stillGreetsStrangers() {
        assertThat(service.greet(null)).isEqualTo("HELLO, STRANGER!");
    }
}