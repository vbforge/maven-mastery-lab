package com.vbforge.profiles;

import java.util.Map;
import java.util.Set;

/**
 * The environment against which profile activation rules are evaluated.
 *
 * <p>{@code jdk} is a version string like {@code "17.0.2"} — profile rules
 * match on prefix. {@code osName} is a plain string like {@code "Linux"} or
 * {@code "Mac OS X"}. {@code properties} are the {@code -D} user properties
 * plus any project properties. {@code cliActive} is the set of profile ids
 * passed with {@code -P}.
 */
public record ActivationEnv(String jdk, String osName,
                            Map<String, String> properties, Set<String> cliActive) {

    public ActivationEnv {
        properties = properties == null ? Map.of() : Map.copyOf(properties);
        cliActive = cliActive == null ? Set.of() : Set.copyOf(cliActive);
    }

    public static ActivationEnv of(String jdk, String osName) {
        return new ActivationEnv(jdk, osName, Map.of(), Set.of());
    }
}