package com.vbforge.profiles;

/**
 * A Maven profile, modelled only by the activation rules that matter for this
 * module.
 *
 * <p>Each non-null field is one activation rule. A profile is active if
 * <b>any</b> of its rules matches the current {@link ActivationEnv}, or if its
 * {@link #id()} is in {@link ActivationEnv#cliActive()} (the {@code -P} flag).
 *
 * <p>{@code activeByDefault} is a special rule: it only applies when no
 * profile was requested with {@code -P} — otherwise Maven would always activate
 * every default profile, which is never what you want.
 */
public record Profile(String id, boolean activeByDefault, String jdk, String osName,
                      String propertyName, String propertyValue) {

    public Profile {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }

    /** A profile with no rules at all — only {@code -P id} can activate it. */
    public static Profile of(String id) {
        return new Profile(id, false, null, null, null, null);
    }

    /** A profile that is active when no {@code -P} is given. */
    public static Profile defaultActive(String id) {
        return new Profile(id, true, null, null, null, null);
    }

    /** Activated when the JDK version starts with the given prefix (e.g. "17"). */
    public static Profile onJdk(String id, String jdkPrefix) {
        return new Profile(id, false, jdkPrefix, null, null, null);
    }

    /** Activated on a matching OS name. */
    public static Profile onOs(String id, String osName) {
        return new Profile(id, false, null, osName, null, null);
    }

    /** Activated when the given property is set (with a matching value if given). */
    public static Profile onProperty(String id, String name, String value) {
        return new Profile(id, false, null, null, name, value);
    }
}