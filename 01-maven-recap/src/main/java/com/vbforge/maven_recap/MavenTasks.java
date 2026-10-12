package com.vbforge.maven_recap;

import java.util.List;

/**
 * MavenTasks — implement every method below to make {@code MavenTasksTest} go GREEN.
 *
 * <p><b>Workflow:</b>
 * <ol>
 *   <li>Read {@code MavenDemo.java} to understand the relevant concept.</li>
 *   <li>Implement a method — remove the {@code UnsupportedOperationException}.</li>
 *   <li>When all 6 are done, remove the {@code @Disabled} annotation from
 *       {@code MavenTasksTest} and run {@code mvn test -Dtest=MavenTasksTest} ✅</li>
 * </ol>
 *
 * <p>Each method has a Javadoc describing exactly what to implement. Do not change
 * method signatures or add external libraries — only the Java standard library is needed.
 */
public class MavenTasks {

    // ── Task 1 ─────────────────────────────────────────────────────────────────

    /**
     * Formats three coordinate parts into the standard Maven coordinate string
     * {@code groupId:artifactId:version}, the format used in commands like
     * {@code mvn dependency:get -Dartifact=...}.
     *
     * <p>Examples:
     * <pre>
     *   toCoordinateString("com.vbforge", "java-core", "1.0.0")
     *       → "com.vbforge:java-core:1.0.0"
     *
     *   toCoordinateString("org.junit.jupiter", "junit-jupiter", "5.10.2")
     *       → "org.junit.jupiter:junit-jupiter:5.10.2"
     * </pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>Join the three parts with a colon {@code ":"}.</li>
     *   <li>Any {@code null} argument must throw {@link IllegalArgumentException}.</li>
     *   <li>Any blank (empty or whitespace-only) argument must throw
     *       {@link IllegalArgumentException}.</li>
     * </ul>
     *
     * @param groupId    the group coordinate (e.g. {@code com.vbforge})
     * @param artifactId the artifact coordinate (e.g. {@code java-core})
     * @param version    the version coordinate (e.g. {@code 1.0.0})
     * @return the coordinate string {@code groupId:artifactId:version}
     * @throws IllegalArgumentException if any argument is null or blank
     */
    public static String toCoordinateString(String groupId, String artifactId, String version) {
        throw new UnsupportedOperationException("TODO: implement toCoordinateString");
    }

    // ── Task 2 ─────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the given string is a valid Maven {@code artifactId}.
     *
     * <p>Rules for a valid artifactId:
     * <ul>
     *   <li>Non-null and non-empty</li>
     *   <li>Must start with a lowercase letter (not a digit or punctuation)</li>
     *   <li>Remaining characters: lowercase letters, digits, hyphen ({@code -}),
     *       underscore ({@code _}) or dot ({@code .})</li>
     *   <li>No spaces, no uppercase letters, no other punctuation</li>
     * </ul>
     *
     * <p>Examples:
     * <pre>
     *   isValidArtifactId("java-core")        → true
     *   isValidArtifactId("maven-recap")      → true
     *   isValidArtifactId("maven_recap")      → true
     *   isValidArtifactId("commons-lang3")    → true
     *   isValidArtifactId("Java-Core")        → false  (uppercase)
     *   isValidArtifactId("java core")        → false  (space)
     *   isValidArtifactId("java-core!")       → false  (exclamation)
     *   isValidArtifactId("1-java")           → false  (starts with digit)
     *   isValidArtifactId("")                 → false
     *   isValidArtifactId(null)               → false
     * </pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>Check {@code null} / empty first.</li>
     *   <li>Check the first character separately from the rest.</li>
     *   <li>Loop over the remaining characters and allow only the characters listed above.</li>
     * </ul>
     *
     * @param artifactId the artifact name to validate
     * @return {@code true} if valid per the rules above
     */
    public static boolean isValidArtifactId(String artifactId) {
        throw new UnsupportedOperationException("TODO: implement isValidArtifactId");
    }

    // ── Task 3 ─────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if a Maven dependency with the given scope is
     * <em>transitive</em> — i.e. it is visible to projects that depend on you.
     *
     * <p>Scopes and their transitivity:
     * <pre>
     *   compile   → true   (default; visible everywhere, propagated)
     *   runtime   → true   (not at compile time, but propagated to consumers)
     *   provided  → false  (expected from the runtime container, e.g. servlet-api)
     *   test      → false  (test-only, never shipped)
     *   system    → false  (you supply the path; like provided)
     * </pre>
     *
     * <p>Examples:
     * <pre>
     *   isTransitiveScope("compile")   → true
     *   isTransitiveScope("runtime")   → true
     *   isTransitiveScope("provided")  → false
     *   isTransitiveScope("test")      → false
     *   isTransitiveScope("system")    → false
     *   isTransitiveScope("unknown")   → false  (anything unrecognised → false)
     *   isTransitiveScope(null)        → false
     * </pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>A {@code switch} or {@code if/else} over the known scopes is enough.</li>
     *   <li>Unrecognised input, {@code null} and blank all return {@code false}.</li>
     * </ul>
     *
     * @param scope the dependency scope (e.g. {@code "compile"})
     * @return {@code true} if the scope is transitive
     */
    public static boolean isTransitiveScope(String scope) {
        throw new UnsupportedOperationException("TODO: implement isTransitiveScope");
    }

    // ── Task 4 ─────────────────────────────────────────────────────────────────

    /**
     * Returns the next phase after the given one in the Maven <b>default</b>
     * lifecycle. Returns {@code null} if the given phase is the last one
     * ({@code deploy}) or is not a recognised phase.
     *
     * <p>The seven main phases, in order:
     * <pre>
     *   validate → compile → test → package → verify → install → deploy
     * </pre>
     *
     * <p>Examples:
     * <pre>
     *   nextPhase("validate") → "compile"
     *   nextPhase("compile")  → "test"
     *   nextPhase("test")     → "package"
     *   nextPhase("install")  → "deploy"
     *   nextPhase("deploy")   → null
     *   nextPhase("bogus")    → null
     *   nextPhase(null)       → null
     * </pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>You may reuse {@link #defaultLifecycle()} (Task 5) to get the ordered list.</li>
     *   <li>Find the index of {@code current}; return the next element, or
     *       {@code null} if {@code current} is not found or is the last element.</li>
     *   <li>{@code List#indexOf} returns {@code -1} when the element is absent.</li>
     * </ul>
     *
     * @param current the current phase
     * @return the next phase, or {@code null}
     */
    public static String nextPhase(String current) {
        throw new UnsupportedOperationException("TODO: implement nextPhase");
    }

    // ── Task 5 ─────────────────────────────────────────────────────────────────

    /**
     * Returns the ordered list of the seven main phases of the Maven
     * <b>default</b> lifecycle.
     *
     * <p>Expected result (in this exact order):
     * <pre>
     *   ["validate", "compile", "test", "package", "verify", "install", "deploy"]
     * </pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>{@code List.of(...)} returns an immutable list — a good fit for a constant.</li>
     *   <li>The order matters — {@code validate} first, {@code deploy} last.</li>
     * </ul>
     *
     * @return the seven default-lifecycle phases in order
     */
    public static List<String> defaultLifecycle() {
        throw new UnsupportedOperationException("TODO: implement defaultLifecycle");
    }

    // ── Task 6 ─────────────────────────────────────────────────────────────────

    /**
     * Returns the {@code distributionUrl} value that belongs in
     * {@code .mvn/wrapper/maven-wrapper.properties} for the given Maven version.
     *
     * <p>The URL points to the binary zip of that Maven version on Maven Central.
     * Format:
     * <pre>
     *   https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/{VERSION}/apache-maven-{VERSION}-bin.zip
     * </pre>
     *
     * <p>Examples:
     * <pre>
     *   distributionUrl("3.9.6") → "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip"
     *   distributionUrl("3.8.8") → "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.8/apache-maven-3.8.8-bin.zip"
     * </pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>The version appears twice in the URL.</li>
     *   <li>Return {@code null} if {@code mavenVersion} is {@code null} or blank.</li>
     *   <li>{@code String.format} or simple concatenation both work.</li>
     * </ul>
     *
     * @param mavenVersion the Maven version (e.g. {@code "3.9.6"})
     * @return the wrapper distribution URL, or {@code null} if version is null/blank
     */
    public static String distributionUrl(String mavenVersion) {
        throw new UnsupportedOperationException("TODO: implement distributionUrl");
    }
}