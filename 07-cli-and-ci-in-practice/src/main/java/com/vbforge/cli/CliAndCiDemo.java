package com.vbforge.cli;

import java.util.List;

/**
 * CliAndCiDemo — annotated reference for the everyday Maven CLI and its CI
 * usage.
 *
 * <p>Read top-to-bottom. Each section covers one mechanism and explains
 * <em>why</em> it exists — not just what the flag looks like.
 *
 * <p>Topics:
 * <ol>
 *   <li>{@code verify} vs {@code install}</li>
 *   <li>{@code -DskipTests} vs {@code -Dmaven.test.skip=true}</li>
 *   <li>surefire (unit) vs failsafe (integration) — naming convention</li>
 *   <li>CI flags: {@code -B}, {@code -ntp}, {@code -q}, {@code -X}</li>
 *   <li>Parallel builds: {@code -T}</li>
 *   <li>Caching {@code ~/.m2} in CI</li>
 * </ol>
 *
 * <p>Run: {@code ./mvnw -q exec:java -pl 07-cli-and-ci-in-practice
 * -Dexec.mainClass="com.vbforge.cli.CliAndCiDemo"}
 */
public class CliAndCiDemo {

    public static void main(String[] args) {
        verifyVsInstallDemo();
        System.out.println("---");
        skipFlagsDemo();
        System.out.println("---");
        testNamingDemo();
        System.out.println("---");
        ciFlagsDemo();
        System.out.println("---");
        parallelBuildsDemo();
        System.out.println("---");
        cachingDemo();
    }

    // ── 1. verify vs install ─────────────────────────────────────────────────

    /**
     * The two phases CI most often chooses between, and they do different
     * things:
     * <ul>
     *   <li>{@code verify} — runs the full default lifecycle up to
     *       <b>verify</b>: tests, package, and any checks bound to
     *       {@code verify} (integration tests, spotbugs, dependency:analyze,
     *       Enforcer…). It <b>does not</b> write anything into your local
     *       {@code ~/.m2/repository}.</li>
     *   <li>{@code install} — everything {@code verify} does, plus a copy of
     *       the artifact into {@code ~/.m2/repository}. The local repository
     *       is a developer convenience, not a build output; polluting it with
     *       CI builds is a classic source of "works on my machine" bugs.</li>
     * </ul>
     *
     * <p>Default for CI: <b>{@code clean verify}</b>. Use {@code install} only
     * on the last step of a multi-JVM CI pipeline that genuinely needs the
     * artifact in a shared local repo, or in a developer's local iterative
     * loop.
     */
    public static String verifyVsInstallSnippet() {
        return """
                ./mvnw clean verify     # CI default: no writes to ~/.m2
                ./mvnw clean install    # everything verify does + copy to ~/.m2
                """;
    }

    static void verifyVsInstallDemo() {
        System.out.println("=== verify vs install ===");
        System.out.print(verifyVsInstallSnippet());
        System.out.println();
        System.out.println("verify  runs: validate → compile → test → package → verify");
        System.out.println("install runs: validate → compile → test → package → verify → install");
        System.out.println("install is verify + a copy of the jar into ~/.m2/repository.");
    }

    // ── 2. Skip flags ────────────────────────────────────────────────────────

    /**
     * One row of the "which skip flag does what" table.
     */
    public record SkipFlag(String flag, boolean skipsCompiling, boolean skipsRunning, String note) {
    }

    /**
     * Two flags that sound identical but are not:
     * <ul>
     *   <li>{@code -DskipTests} — <b>compiles</b> the test sources, but does
     *       not <b>run</b> them. The {@code test} phase still executes; the
     *       surefire plugin is told to skip. Useful when you want the
     *       compiler to catch obvious test breakage but save the wall-clock
     *       time of running them.</li>
     *   <li>{@code -Dmaven.test.skip=true} — skips <b>both</b> compilation
     *       and execution. The compiler plugin never compiles
     *       {@code src/test/java}, and surefire never runs. Fastest, but
     *       compilation errors in tests go unnoticed.</li>
     * </ul>
     */
    public static List<SkipFlag> skipFlagTable() {
        return List.of(
                new SkipFlag("-DskipTests", false, true,
                        "compiles tests, does not run them"),
                new SkipFlag("-Dmaven.test.skip=true", true, true,
                        "skips both compilation and execution"),
                new SkipFlag("-DskipITs", false, true,
                        "surefire runs; failsafe is skipped"),
                new SkipFlag("-DskipTests -DskipITs", false, true,
                        "skip unit AND integration tests, still compile both")
        );
    }

    static void skipFlagsDemo() {
        System.out.println("=== skip flags ===");
        System.out.printf("%-32s %-16s %-16s %s%n",
                "flag", "skips compiling?", "skips running?", "notes");
        for (SkipFlag row : skipFlagTable()) {
            System.out.printf("%-32s %-16s %-16s %s%n",
                    row.flag(), row.skipsCompiling(), row.skipsRunning(), row.note());
        }
        System.out.println();
        System.out.println("the classic trap: -DskipTests still compiles src/test/java,");
        System.out.println("so a broken test file FAILS the build even though tests are 'skipped'.");
    }

    // ── 3. surefire vs failsafe ──────────────────────────────────────────────

    /**
     * Two plugins run tests. They are configured by default to pick up
     * <b>different</b> filenames, so the choice of test class name decides
     * which plugin runs it:
     * <ul>
     *   <li>{@code maven-surefire-plugin} — unit tests, bound to the
     *       {@code test} phase. Default includes {@code *Test},
     *       {@code *Tests}, {@code Test*}, {@code *TestCase}.</li>
     *   <li>{@code maven-failsafe-plugin} — integration tests, bound to
     *       {@code integration-test} and {@code verify}. Default includes
     *       {@code *IT}, {@code IT*}, {@code *ITCase}. Failsafe runs after
     *       {@code package}, so it can use the built jar and manage the
     *       lifecycle of external resources (containers, servers).</li>
     * </ul>
     *
     * <p>The names are conventions, not requirements — but every Java project
     * you meet in the wild follows them, and CI scripts assume
     * {@code mvn verify} runs both.
     */
    public enum TestKind {
        SUREFIRE, FAILSAFE, NONE
    }

    /**
     * Example class names and the plugin each one is picked up by.
     */
    public static List<String[]> testNamingExamples() {
        return List.of(
                new String[]{"GreeterTest", "surefire"},
                new String[]{"GreeterTests", "surefire"},
                new String[]{"TestGreeter", "surefire"},
                new String[]{"GreeterTestCase", "surefire"},
                new String[]{"GreeterIT", "failsafe"},
                new String[]{"ITGreeter", "failsafe"},
                new String[]{"GreeterITCase", "failsafe"},
                new String[]{"GreeterHelper", "none"}
        );
    }

    static void testNamingDemo() {
        System.out.println("=== surefire vs failsafe ===");
        System.out.printf("%-25s → %s%n", "class name", "plugin");
        for (String[] row : testNamingExamples()) {
            System.out.printf("%-25s → %s%n", row[0], row[1]);
        }
        System.out.println();
        System.out.println("surefire  runs at phase 'test'      → ./mvnw test");
        System.out.println("failsafe  runs at phase 'verify'    → ./mvnw verify");
        System.out.println("mvn test does NOT run integration tests (*IT).");
    }

    // ── 4. CI flags ──────────────────────────────────────────────────────────

    /**
     * The flags every CI build should (or should not) use.
     */
    public static List<String[]> ciFlagTable() {
        return List.of(
                new String[]{"-B", "batch mode: no ANSI, no interactive prompts; makes logs parsable"},
                new String[]{"-ntp", "no transfer progress: hides download noise, keeps logs short"},
                new String[]{"-q", "quiet: only warnings and errors — great for CI, painful locally"},
                new String[]{"-X", "debug: verbose plugin/goal/URL logs — use for diagnosis, never in CI"},
                new String[]{"-U", "force snapshot update: re-check remote SNAPSHOT metadata"},
                new String[]{"-o", "offline: no network — only ~/.m2, fails if anything is missing"}
        );
    }

    static void ciFlagsDemo() {
        System.out.println("=== CI flags ===");
        for (String[] row : ciFlagTable()) {
            System.out.printf("  %-6s %s%n", row[0], row[1]);
        }
        System.out.println();
        System.out.println("canonical CI command:");
        System.out.println("  ./mvnw -B -ntp clean verify");
        System.out.println("-B and -ntp are the minimum. Everything else is situational.");
    }

    // ── 5. Parallel builds ───────────────────────────────────────────────────

    /**
     * {@code -T <n>} builds independent reactor modules in parallel, respecting
     * inter-module dependencies. Two forms:
     * <ul>
     *   <li>{@code -T 4} — at most four threads.</li>
     *   <li>{@code -T 1C} — one thread per CPU core (the "C" suffix).</li>
     * </ul>
     *
     * <p>WHY this matters: a 12-module reactor on a 4-core CI box can be 2–4×
     * faster with {@code -T 1C}, and the log is still readable because Maven
     * prefixes each line with the module id.
     *
     * <p>Caveats: parallel builds multiply peak memory; plugins that shell out
     * to shared resources (integration tests hitting the same DB, docker
     * containers with fixed ports) may not be thread-safe. Test with
     * {@code -T 1C} on a branch before enabling it on {@code main}.
     */
    public static String parallelSnippet() {
        return """
                ./mvnw -T 1C clean verify      # one thread per CPU core
                ./mvnw -T 4  clean verify      # at most 4 threads
                """;
    }

    static void parallelBuildsDemo() {
        System.out.println("=== parallel builds ===");
        System.out.print(parallelSnippet());
        System.out.println();
        System.out.println("uses the reactor graph — modules are still built in dependency order;");
        System.out.println("only modules with no pending dependency between them run concurrently.");
    }

    // ── 6. Caching ~/.m2 in CI ───────────────────────────────────────────────

    /**
     * Every CI run starts with an empty {@code ~/.m2/repository} unless you
     * cache it. On a fresh runner, the first {@code ./mvnw verify} will
     * download several hundred megabytes from Central — often 60–90 % of the
     * total wall-clock time.
     *
     * <p>The cache key must include a hash of {@code ** /pom.xml}; when a pom
     * changes, the cache is invalidated and a fresh one is populated. This is
     * safe because poms are the only thing that determines what gets
     * downloaded.
     */
    public static String cachingSnippet() {
        return """
                - uses: actions/setup-java@v4
                  with:
                    distribution: temurin
                    java-version: '17'
                    cache: maven          # caches ~/.m2/repository, keyed on **/pom.xml
                """;
    }

    static void cachingDemo() {
        System.out.println("=== caching ~/.m2 in CI ===");
        System.out.println("with GitHub Actions, caching is one keyword:");
        System.out.print(cachingSnippet());
        System.out.println();
        System.out.println("measure the difference: run the workflow twice — the second run's");
        System.out.println("'Set up JDK' step restores the cache, and the build step finishes faster.");
    }

}