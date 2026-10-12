package com.vbforge.resolution;

import java.util.List;

/**
 * TransitiveResolutionDemo — annotated reference for how Maven builds the
 * resolved dependency graph.
 *
 * <p>Read top-to-bottom. Each section covers one mechanism and explains
 * <em>why</em> it exists — not just what the XML looks like.
 *
 * <p>Topics:
 * <ol>
 *   <li>What a transitive dependency is</li>
 *   <li>Nearest-wins + first-declaration tie-break</li>
 *   <li>The scope mediation table</li>
 *   <li>{@code <exclusions>} vs {@code <optional>}</li>
 *   <li>{@code dependency:tree} and how to read it</li>
 *   <li>{@code dependency:analyze} and Enforcer {@code dependencyConvergence}</li>
 * </ol>
 *
 * <p>Run: {@code ./mvnw -q exec:java -pl 03-transitive-resolution
 * -Dexec.mainClass="com.vbforge.resolution.TransitiveResolutionDemo"}
 */
public class TransitiveResolutionDemo {

    public static void main(String[] args) {
        transitiveDependenciesDemo();
        System.out.println("---");
        nearestWinsDemo();
        System.out.println("---");
        scopeMediationDemo();
        System.out.println("---");
        exclusionsDemo();
        System.out.println("---");
        treeDiagnosisDemo();
        System.out.println("---");
        analyzeAndEnforcerDemo();
    }

    // ── 1. What is a transitive dependency? ──────────────────────────────────

    /**
     * A <b>transitive</b> dependency is a dependency of a dependency. If your
     * project declares {@code commons-lang3} and {@code commons-lang3} declares
     * {@code commons-text}, then {@code commons-text} lands on your classpath
     * even though you never wrote it in the pom.
     *
     * <p>WHY this matters: most of the JARs on a real classpath are transitive.
     * "Why is this on my classpath?" and "why is it <em>missing</em> from my
     * classpath?" are the two most common debugging questions, and both are
     * answered by the transitive rules.
     *
     * <p>Only {@code compile} and {@code runtime} dependencies propagate to
     * consumers. {@code provided}, {@code test} and {@code system} do not.
     */
    static DependencyNode transitiveExample() {
        return DependencyNode.of(
                artifact("com.vbforge", "my-app", "1.0.0", "compile"),
                DependencyNode.of(
                        artifact("org.apache.commons", "commons-lang3", "3.14.0", "compile"),
                        DependencyNode.leaf(
                                artifact("org.apache.commons", "commons-text", "1.11.0", "compile"))
                )
        );
    }

    static void transitiveDependenciesDemo() {
        System.out.println("=== transitive dependencies ===");
        printTree(transitiveExample(), 0);
        System.out.println();
        System.out.println("commons-text is not in my-app's pom — it arrives via commons-lang3.");
        System.out.println("Only compile and runtime propagate: provided, test and system do not.");
    }

    // ── 2. Nearest-wins + first-declaration tie-break ────────────────────────

    /**
     * When the same artifact ({@code groupId:artifactId}) is reachable along
     * several paths, Maven picks one version:
     * <ol>
     *   <li><b>Nearest-wins</b> — the version at the smallest depth wins.</li>
     *   <li><b>Tie-break</b> at the same depth — the path declared first wins.</li>
     * </ol>
     * Everything else is <em>omitted</em> from the resolved graph.
     */
    static DependencyNode nearestWinsExample() {
        // my-app → lib-a → shared:1.0.0   (depth 2)
        // my-app → lib-b → shared:2.0.0   (depth 2, tie → first declared wins)
        // my-app → lib-c → lib-x → shared:3.0.0 (depth 3 → loses)
        return DependencyNode.of(
                artifact("com.vbforge", "my-app", "1.0.0", "compile"),
                DependencyNode.of(artifact("com.example", "lib-a", "1.0.0", "compile"),
                        DependencyNode.leaf(artifact("com.example", "shared", "1.0.0", "compile"))),
                DependencyNode.of(artifact("com.example", "lib-b", "1.0.0", "compile"),
                        DependencyNode.leaf(artifact("com.example", "shared", "2.0.0", "compile"))),
                DependencyNode.of(artifact("com.example", "lib-c", "1.0.0", "compile"),
                        DependencyNode.of(artifact("com.example", "lib-x", "1.0.0", "compile"),
                                DependencyNode.leaf(artifact("com.example", "shared", "3.0.0", "compile"))))
        );
    }

    static void nearestWinsDemo() {
        System.out.println("=== nearest-wins ===");
        printTree(nearestWinsExample(), 0);
        System.out.println();
        System.out.println("shared appears at depths 2, 2, 3.");
        System.out.println("Nearest: depth 2 → tie between 1.0.0 and 2.0.0.");
        System.out.println("First declared wins → 1.0.0.");
    }

    // ── 3. Scope mediation ───────────────────────────────────────────────────

    /**
     * A row of Maven's scope mediation table.
     *
     * <p>If your project has {@code B} at scope {@code parentScope}, and
     * {@code B} has {@code C} at scope {@code childScope}, then {@code C} is
     * visible to you at scope {@code effectiveScope}. A {@code null} effective
     * scope means <b>dropped</b> — {@code C} is not pulled in at all.
     */
    public record ScopeMediation(String parentScope, String childScope, String effectiveScope) {}

    /**
     * The full mediation table for the four standard scopes, in the same shape
     * as Maven's official documentation.
     */
    public static List<ScopeMediation> scopeMediationTable() {
        return List.of(
                new ScopeMediation("compile",  "compile",  "compile"),
                new ScopeMediation("compile",  "runtime",  "runtime"),
                new ScopeMediation("compile",  "provided", null),
                new ScopeMediation("compile",  "test",     null),

                new ScopeMediation("provided", "compile",  "provided"),
                new ScopeMediation("provided", "runtime",  "provided"),
                new ScopeMediation("provided", "provided", null),
                new ScopeMediation("provided", "test",     null),

                new ScopeMediation("runtime",  "compile",  "runtime"),
                new ScopeMediation("runtime",  "runtime",  "runtime"),
                new ScopeMediation("runtime",  "provided", null),
                new ScopeMediation("runtime",  "test",     null),

                new ScopeMediation("test",     "compile",  "test"),
                new ScopeMediation("test",     "runtime",  "test"),
                new ScopeMediation("test",     "provided", null),
                new ScopeMediation("test",     "test",     null)
        );
    }

    static void scopeMediationDemo() {
        System.out.println("=== scope mediation ===");
        System.out.printf("%-10s | %-10s | %s%n", "parent", "child", "effective");
        System.out.println("-".repeat(42));
        for (ScopeMediation m : scopeMediationTable()) {
            System.out.printf("%-10s | %-10s | %s%n",
                    m.parentScope(), m.childScope(),
                    m.effectiveScope() == null ? "(dropped)" : m.effectiveScope());
        }
        System.out.println();
        System.out.println("Reading: if YOUR project has B at scope X, and B depends on C at scope Y,");
        System.out.println("then C is visible to your project at scope 'effective'. '(dropped)' means");
        System.out.println("C is not visible to you at all — Maven does not pull it in.");
    }

    // ── 4. <exclusions> vs <optional> ────────────────────────────────────────

    /** Drop one artifact (and its subtree) that arrives via a specific dependency. */
    public static String exclusionSnippet() {
        return """
                <dependency>
                    <groupId>com.example</groupId>
                    <artifactId>lib-a</artifactId>
                    <version>1.0.0</version>
                    <exclusions>
                        <exclusion>
                            <groupId>commons-logging</groupId>
                            <artifactId>commons-logging</artifactId>
                        </exclusion>
                    </exclusions>
                </dependency>
                """;
    }

    /** Do not pass this dependency on to consumers of <em>your</em> artifact. */
    public static String optionalSnippet() {
        return """
                <dependency>
                    <groupId>com.example</groupId>
                    <artifactId>lib-b</artifactId>
                    <version>1.0.0</version>
                    <optional>true</optional>
                </dependency>
                """;
    }

    static void exclusionsDemo() {
        System.out.println("=== exclusions ===");
        System.out.println("To drop an artifact that arrives transitively, add an <exclusion>:");
        System.out.println(exclusionSnippet());
        System.out.println("Exclusion removes that artifact (and its subtree) from this dependency only.");
        System.out.println();
        System.out.println("To prevent YOUR dependency from being passed to consumers, use <optional>:");
        System.out.println(optionalSnippet());
        System.out.println("Difference: exclusion is what you receive; optional is what you give.");
    }

    // ── 5. dependency:tree ───────────────────────────────────────────────────

    static void treeDiagnosisDemo() {
        System.out.println("=== dependency:tree ===");
        System.out.println("print the resolved tree         : ./mvnw dependency:tree");
        System.out.println("show omitted nodes and reasons  : ./mvnw dependency:tree -Dverbose");
        System.out.println("filter to one artifact          : ./mvnw dependency:tree -Dincludes=commons-logging");
        System.out.println("write it to a file              : ./mvnw dependency:tree -DoutputFile=tree.txt");
        System.out.println();
        System.out.println("typical lines you will see:");
        System.out.println("  +- commons-logging:commons-logging:jar:1.2:compile");
        System.out.println("  +- (commons-logging:commons-logging:jar:1.1:compile - omitted for conflict with 1.2)");
        System.out.println();
        System.out.println("the suffix ':jar:1.2:compile' is type:version:scope — read it left to right");
    }

    // ── 6. dependency:analyze + Enforcer ─────────────────────────────────────

    /** Fail the build the moment two versions of the same artifact meet. */
    public static String enforcerSnippet() {
        return """
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-enforcer-plugin</artifactId>
                    <version>3.4.1</version>
                    <executions>
                        <execution>
                            <id>enforce</id>
                            <goals><goal>enforce</goal></goals>
                            <configuration>
                                <rules>
                                    <dependencyConvergence/>
                                </rules>
                            </configuration>
                        </execution>
                    </executions>
                </plugin>
                """;
    }

    static void analyzeAndEnforcerDemo() {
        System.out.println("=== dependency:analyze + Enforcer ===");
        System.out.println("find unused-declared and used-undeclared deps:");
        System.out.println("  ./mvnw dependency:analyze");
        System.out.println();
        System.out.println("fail the build when the same artifact resolves to different versions:");
        System.out.println(enforcerSnippet());
        System.out.println("This is the safety net for 'NoSuchMethodError at runtime'.");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    static Artifact artifact(String groupId, String artifactId, String version, String scope) {
        return new Artifact(groupId, artifactId, version, scope);
    }

    static void printTree(DependencyNode node, int indent) {
        System.out.println("  ".repeat(indent)
                + node.artifact().gav() + "  [" + node.artifact().scope() + "]");
        for (DependencyNode child : node.children()) {
            printTree(child, indent + 1);
        }
    }
}