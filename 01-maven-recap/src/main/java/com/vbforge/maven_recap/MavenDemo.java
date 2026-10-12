package com.vbforge.maven_recap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MavenDemo — annotated reference for the Maven build tool.
 *
 * <p>Read top-to-bottom. Each section covers one building block of Maven and
 * explains <em>why</em> it exists — not just what the XML looks like.
 * This class is self-contained: it has no dependency on the exercises.
 *
 * <p>Topics:
 * <ol>
 *   <li>pom.xml — the Project Object Model</li>
 *   <li>Coordinates — groupId / artifactId / version / packaging</li>
 *   <li>Dependency scopes — compile, provided, runtime, test, system</li>
 *   <li>Lifecycle — validate → compile → test → package → verify → install → deploy</li>
 *   <li>Plugins — goals bound to phases</li>
 *   <li>Wrapper — {@code mvnw} and {@code .mvn/wrapper/maven-wrapper.properties}</li>
 * </ol>
 *
 * <p>Run: {@code mvn exec:java -Dexec.mainClass="com.vbforge.maven_recap.MavenDemo"}
 */
public class MavenDemo {

    public static void main(String[] args) {
        pomXmlDemo();
        System.out.println("---");
        coordinatesDemo();
        System.out.println("---");
        dependencyScopesDemo();
        System.out.println("---");
        lifecycleDemo();
        System.out.println("---");
        pluginsDemo();
        System.out.println("---");
        wrapperDemo();
    }

    // ── 1. pom.xml ────────────────────────────────────────────────────────────

    /** The only {@code modelVersion} Maven 2 and Maven 3 understand. */
    static final String MODEL_VERSION = "4.0.0";

    /**
     * The {@code pom.xml} is Maven's Project Object Model — a declarative
     * description of what to build, from what sources, with what dependencies,
     * producing what artifact, using which plugins.
     *
     * <p>WHY this matters: Maven is <em>convention over configuration</em>.
     * Given only coordinates and a dependencies block, Maven knows where your
     * sources live ({@code src/main/java}), where tests live
     * ({@code src/test/java}), what the output looks like ({@code target/}),
     * and what the default build does. You only describe the exceptions.
     *
     * <p>Three things every pom has:
     * <ul>
     *   <li>{@code modelVersion} — always {@code 4.0.0}</li>
     *   <li>Coordinates — identity of the artifact</li>
     *   <li>{@code dependencies} / {@code build} — what to pull, how to build</li>
     * </ul>
     */
    static String samplePom() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.vbforge</groupId>
                    <artifactId>java-core</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                </project>
                """;
    }

    static void pomXmlDemo() {
        System.out.println("=== pom.xml ===");
        System.out.println(samplePom());
        System.out.println("modelVersion is always " + MODEL_VERSION);
        System.out.println("packaging 'pom' means: aggregator/parent, no jar produced");
    }

    // ── 2. Coordinates ────────────────────────────────────────────────────────

    /**
     * Every Maven artifact is identified by four coordinates:
     * {@code groupId}, {@code artifactId}, {@code version}, {@code packaging}.
     * Together they uniquely identify any artifact in any repository.
     *
     * <p>WHY this matters: the string {@code groupId:artifactId:version} is used
     * everywhere — in {@code mvn dependency:get -Dartifact=...}, in error
     * messages and in build logs.
     *
     * <p>Rules:
     * <ul>
     *   <li>{@code groupId} — reverse domain, lowercase (e.g. {@code com.vbforge})</li>
     *   <li>{@code artifactId} — kebab-case, lowercase, no spaces (e.g. {@code java-core})</li>
     *   <li>{@code version} — arbitrary string; {@code 1.0.0} typical, {@code -SNAPSHOT} suffix for dev</li>
     *   <li>{@code packaging} — {@code jar}, {@code war}, {@code pom}, … (default {@code jar})</li>
     * </ul>
     */
    record Coordinates(String groupId, String artifactId, String version, String packaging) {

        /** Splits a {@code groupId:artifactId:version} string; packaging defaults to {@code jar}. */
        static Coordinates parse(String gav) {
            if (gav == null) {
                throw new IllegalArgumentException("coordinate must not be null");
            }
            String[] parts = gav.split(":");
            if (parts.length != 3) {
                throw new IllegalArgumentException(
                        "expected groupId:artifactId:version but got: " + gav);
            }
            return new Coordinates(parts[0], parts[1], parts[2], "jar");
        }
    }

    static void coordinatesDemo() {
        System.out.println("=== Coordinates ===");

        String gav = "com.vbforge:java-core:1.0.0";
        Coordinates c = Coordinates.parse(gav);

        System.out.println("coordinate      : " + gav);
        System.out.println("groupId         : " + c.groupId());
        System.out.println("artifactId      : " + c.artifactId());
        System.out.println("version         : " + c.version());
        System.out.println("packaging       : " + c.packaging() + " (default)");
        System.out.println("used in command : mvn dependency:get -Dartifact=" + gav);
    }

    // ── 3. Dependency scopes ──────────────────────────────────────────────────

    /**
     * A dependency <b>scope</b> controls two things: which classpaths the
     * dependency is placed on (compile / test / runtime), and whether it is
     * <em>transitive</em> — i.e. whether projects that depend on you also get it.
     *
     * <p>WHY this matters: JUnit belongs on the test classpath only — you do
     * not want to ship it inside your jar. A servlet API is {@code provided} —
     * the container supplies it at runtime. Getting scopes wrong bloats your
     * artifact or breaks downstream consumers.
     *
     * <pre>
     *   scope     compile  test  runtime  transitive
     *   compile     yes     yes    yes       yes   (default)
     *   provided    yes     yes    no        no    (e.g. servlet-api)
     *   runtime     no      yes    yes       yes   (e.g. JDBC driver)
     *   test        no      yes    no        no    (e.g. JUnit, AssertJ, Mockito)
     *   system      yes     yes    no        no    (like provided, you give the path)
     * </pre>
     */
    enum Scope {
        COMPILE (true,  true, true,  true),
        PROVIDED(true,  true, false, false),
        RUNTIME (false, true, true,  true),
        TEST    (false, true, false, false),
        SYSTEM  (true,  true, false, false);

        private final boolean onCompileClasspath;
        private final boolean onTestClasspath;
        private final boolean onRuntimeClasspath;
        private final boolean transitive;

        Scope(boolean onCompileClasspath, boolean onTestClasspath,
              boolean onRuntimeClasspath, boolean transitive) {
            this.onCompileClasspath = onCompileClasspath;
            this.onTestClasspath = onTestClasspath;
            this.onRuntimeClasspath = onRuntimeClasspath;
            this.transitive = transitive;
        }

        boolean onCompileClasspath() { return onCompileClasspath; }
        boolean onTestClasspath()    { return onTestClasspath; }
        boolean onRuntimeClasspath() { return onRuntimeClasspath; }
        boolean transitive()         { return transitive; }

        /** The value as written in the pom, e.g. {@code "compile"}. */
        String id() { return name().toLowerCase(Locale.ROOT); }
    }

    static void dependencyScopesDemo() {
        System.out.println("=== Dependency scopes ===");

        System.out.printf("%-10s %-8s %-6s %-8s %s%n",
                "scope", "compile", "test", "runtime", "transitive");
        for (Scope s : Scope.values()) {
            System.out.printf("%-10s %-8s %-6s %-8s %s%n",
                    s.id(), s.onCompileClasspath(), s.onTestClasspath(),
                    s.onRuntimeClasspath(), s.transitive());
        }

        System.out.println();
        System.out.println("JUnit 5 is test-scope; the version comes from the parent's dependencyManagement:");
        System.out.println("""
                    <dependency>
                        <groupId>org.junit.jupiter</groupId>
                        <artifactId>junit-jupiter</artifactId>
                        <scope>test</scope>
                    </dependency>
                """);
    }

    // ── 4. Lifecycle ──────────────────────────────────────────────────────────

    /**
     * Maven's <b>default lifecycle</b> is a fixed, ordered list of <em>phases</em>.
     * Running a phase runs every phase before it. This is why
     * {@code mvn install} also compiles, tests, and packages.
     *
     * <p>WHY this matters: you never call {@code mvn compile} before
     * {@code mvn install} — {@code install} implies everything upstream.
     * This explains "why did my tests run when I only asked for a jar?".
     *
     * <p>Two other lifecycles exist: {@code clean} (removes {@code target/})
     * and {@code site} (documentation). They are independent of the default one.
     */
    enum Phase {
        VALIDATE, COMPILE, TEST, PACKAGE, VERIFY, INSTALL, DEPLOY;

        String id() { return name().toLowerCase(Locale.ROOT); }
    }

    /** Every phase Maven runs when you ask for {@code target} (inclusive). */
    static List<Phase> phasesRunBy(Phase target) {
        return List.of(Phase.values()).subList(0, target.ordinal() + 1);
    }

    static void lifecycleDemo() {
        System.out.println("=== Lifecycle ===");

        System.out.println("default lifecycle: " + List.of(Phase.values()));
        System.out.println();
        System.out.println("mvn package runs: " + phasesRunBy(Phase.PACKAGE));
        System.out.println("mvn install runs: " + phasesRunBy(Phase.INSTALL));
        System.out.println();
        System.out.println("mvn clean install = 'clean' lifecycle first, then the default one up to install");
    }

    // ── 5. Plugins ────────────────────────────────────────────────────────────

    /**
     * Maven is, at its core, a plugin execution engine. Every phase in the
     * lifecycle delegates to one or more <b>plugin goals</b> bound to it.
     *
     * <p>WHY this matters: when you run {@code mvn package}, Maven is really
     * running {@code resources:resources → compiler:compile → surefire:test →
     * jar:jar}. Knowing the binding lets you skip or override a step
     * (e.g. {@code -DskipTests}) without guessing which plugin is involved.
     *
     * @return default bindings for jar packaging, in lifecycle order
     */
    static Map<String, String> defaultBindings() {
        Map<String, String> bindings = new LinkedHashMap<>();
        bindings.put("compile", "maven-compiler-plugin:compile");
        bindings.put("test",    "maven-surefire-plugin:test");
        bindings.put("package", "maven-jar-plugin:jar");
        bindings.put("install", "maven-install-plugin:install");
        bindings.put("deploy",  "maven-deploy-plugin:deploy");
        return bindings;
    }

    static void pluginsDemo() {
        System.out.println("=== Plugins ===");

        defaultBindings().forEach((phase, goal) ->
                System.out.printf("  %-10s → %s%n", phase, goal));

        System.out.println();
        System.out.println("Override compiler version or source level by declaring the plugin:");
        System.out.println("""
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.13.0</version>
                        <configuration>
                            <release>17</release>
                        </configuration>
                    </plugin>
                """);
    }

    // ── 6. Wrapper ────────────────────────────────────────────────────────────

    /**
     * The Maven Wrapper ({@code mvnw} on Linux/macOS, {@code mvnw.cmd} on
     * Windows) is a tiny script checked into the repository that downloads and
     * uses a specific Maven version — without requiring a local Maven install.
     *
     * <p>WHY this matters: every contributor and CI job gets the same Maven
     * version, which removes "works on my machine" bugs caused by tool drift.
     *
     * <p>Files created by {@code mvn wrapper:wrapper}:
     * <ul>
     *   <li>{@code mvnw}, {@code mvnw.cmd} — the scripts (committed)</li>
     *   <li>{@code .mvn/wrapper/maven-wrapper.properties} — the config (committed)</li>
     *   <li>{@code .mvn/wrapper/maven-wrapper.jar} — only in older/"jar" mode; newer
     *       script-only wrapper (3.3+) does not need it</li>
     * </ul>
     *
     * <p>Usage: replace {@code mvn} with {@code ./mvnw} — everything else is the same.
     */
    static final String WRAPPER_PROPERTIES = """
            distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip
            """;

    static void wrapperDemo() {
        System.out.println("=== Wrapper ===");

        System.out.println("typical .mvn/wrapper/maven-wrapper.properties:");
        System.out.print(WRAPPER_PROPERTIES);
        System.out.println();
        System.out.println("on Linux/macOS : ./mvnw clean install");
        System.out.println("on Windows     : mvnw.cmd clean install");
    }
}