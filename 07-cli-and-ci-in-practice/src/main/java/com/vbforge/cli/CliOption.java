package com.vbforge.cli;

/**
 * A single command-line flag for Maven, e.g. {@code -B}, {@code -DskipTests},
 * {@code -Dmaven.test.skip=true}, {@code -T 1C} or {@code -pl module-a}.
 *
 * <p>{@code name} always includes the leading dash, exactly as written on the
 * command line. {@code value} is {@code null} for boolean flags that stand
 * alone; it is non-null for flags that take an argument or for {@code -D}
 * properties with a value.
 *
 * <p>Rendering:
 * <ul>
 *   <li>{@code (name, null)}   → {@code name}</li>
 *   <li>{@code (-D*, value)}   → {@code name=value}   (Maven's {@code -D} form)</li>
 *   <li>{@code (other, value)} → {@code name value}   (two tokens)</li>
 * </ul>
 */
public record CliOption(String name, String value) {

    public CliOption {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    /** A boolean flag with no value, e.g. {@code -B}, {@code -ntp}. */
    public static CliOption flag(String name) {
        return new CliOption(name, null);
    }

    /** A flag with a value, e.g. {@code -T 1C}, {@code -Dmaven.test.skip=true}. */
    public static CliOption of(String name, String value) {
        return new CliOption(name, value);
    }

    /** Renders this option the way it would appear on a command line. */
    public String render() {
        if (value == null) {
            return name;
        }
        if (name.startsWith("-D")) {
            return name + "=" + value;
        }
        return name + " " + value;
    }
}