package com.vbforge.cli;

import com.vbforge.cli.CliAndCiDemo.TestKind;

import java.util.List;
import java.util.Set;

/**
 * CliAndCiTasks — implement every method below to make
 * {@code CliAndCiTasksTest} go GREEN.
 *
 * <p><b>Workflow:</b>
 * <ol>
 *   <li>Read {@code CliAndCiDemo.java} to understand the concept.</li>
 *   <li>Implement a method — remove the {@code UnsupportedOperationException}.</li>
 *   <li>When all 5 are done, remove the {@code @Disabled} annotation from
 *       {@code CliAndCiTasksTest} and run
 *       {@code ./mvnw test -pl 07-cli-and-ci-in-practice -Dtest=CliAndCiTasksTest} ✅</li>
 * </ol>
 *
 * <p>Do not change method signatures or add external libraries — only the Java
 * standard library is needed.
 */
public class CliAndCiTasks {

    /**
     * The seven main phases of the default lifecycle, in order. Duplicated
     * here (rather than referencing the Demo) so the tasks remain readable
     * without jumping around.
     */
    private static final List<String> DEFAULT_LIFECYCLE = List.of(
            "validate", "compile", "test", "package", "verify", "install", "deploy");

    // ── Task 1 ─────────────────────────────────────────────────────────────────

    /**
     * Assembles a Maven command line from a list of goals and a list of options.
     *
     * <p>The rendered string always starts with {@code "mvn"}. Options are
     * rendered <b>before</b> goals (matching Maven's own convention), each one
     * separated by a single space. Each option renders per
     * {@link CliOption#render()}:
     * <ul>
     *   <li>{@code -B} (no value) — one token</li>
     *   <li>{@code -DskipTests=true} — one token, because the name starts with {@code -D}</li>
     *   <li>{@code -T 1C} — two tokens, because the name does not start with {@code -D}</li>
     * </ul>
     *
     * <p>Examples:
     * <pre>{@code
     *   buildCommand(["clean", "verify"], [])
     *       → "mvn clean verify"
     *
     *   buildCommand(["clean", "verify"], [flag("-B"), flag("-ntp")])
     *       → "mvn -B -ntp clean verify"
     *
     *   buildCommand(["test"], [flag("-DskipTests"), of("-Dmaven.test.skip", "true")])
     *       → "mvn -DskipTests -Dmaven.test.skip=true test"
     *
     *   buildCommand(["verify"], [of("-T", "1C"), of("-pl", "module-a"), flag("-am")])
     *       → "mvn -T 1C -pl module-a -am verify"
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>A {@link StringBuilder} is the right tool.</li>
     *   <li>Options preserve the input order — do not sort or deduplicate.</li>
     *   <li>{@code null} options list is treated as empty; a {@code null}
     *       element inside a non-null list throws.</li>
     * </ul>
     *
     * @param goals   the Maven goals/phases (must be non-null and non-empty)
     * @param options the CLI flags, in the order they should appear
     * @return the assembled command string
     * @throws IllegalArgumentException if {@code goals} is null/empty, or if any
     *                                  goal is null/blank, or if any option is null
     */
    public static String buildCommand(List<String> goals, List<CliOption> options) {
        throw new UnsupportedOperationException("TODO: implement buildCommand");
    }

    // ── Task 2 ─────────────────────────────────────────────────────────────────

    /**
     * Returns the phases of the Maven <b>default</b> lifecycle that a command
     * line would run.
     *
     * <p>The command may or may not start with {@code mvn} / {@code ./mvnw};
     * either way the leading token is simply ignored because it is not a phase
     * name. Flags are ignored as well. Any token that matches one of the seven
     * default-lifecycle phases is considered.
     *
     * <p>If several phases appear (rare — e.g. {@code mvn test verify}), the
     * <b>latest</b> phase in the lifecycle wins; that is the one Maven
     * reaches after re-running the earlier ones. If no phase appears (e.g.
     * {@code mvn clean} alone), the result is empty.
     *
     * <p>Examples:
     * <pre>{@code
     *   phasesRun("clean verify")               → [validate, compile, test, package, verify]
     *   phasesRun("mvn clean verify")           → [validate, compile, test, package, verify]
     *   phasesRun("package")                    → [validate, compile, test, package]
     *   phasesRun("./mvnw clean install -U")    → [validate, compile, test, package, verify, install]
     *   phasesRun("clean deploy")               → [validate, compile, test, package, verify, install, deploy]
     *   phasesRun("clean")                      → []          (no default-lifecycle phase)
     *   phasesRun("bogus")                      → []
     *   phasesRun(null)                         → []
     *   phasesRun("")                           → []
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>{@code command.split("\\s+")} gives you the tokens.</li>
     *   <li>{@link List#indexOf(Object)} returns {@code -1} for non-phases.</li>
     *   <li>Track the maximum index; then return a copy of the sublist from 0
     *       up to and including it.</li>
     *   <li>Note that {@code -DskipTests} does <b>not</b> remove {@code test}
     *       from the result — the phase still runs, surefire just skips its
     *       own execution.</li>
     * </ul>
     *
     * @param command the Maven command line (may be null or blank)
     * @return the phases the command runs, in order; empty if none
     */
    public static List<String> phasesRun(String command) {
        throw new UnsupportedOperationException("TODO: implement phasesRun");
    }

    // ── Task 3 ─────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} iff the given flags include
     * {@code -Dmaven.test.skip=true}, which is the only one of the two
     * "skip" flags that actually skips <b>compiling</b> test sources.
     *
     * <p>Recognised:
     * <ul>
     *   <li>{@code -Dmaven.test.skip=true}  → {@code true}</li>
     *   <li>{@code -Dmaven.test.skip}       → {@code true}  (bare form, Maven treats empty as truthy)</li>
     * </ul>
     *
     * <p>Not recognised (returns {@code false}):
     * <ul>
     *   <li>{@code -DskipTests} — skips running, but <b>still compiles</b> tests</li>
     *   <li>{@code -DskipITs} — failsafe only</li>
     *   <li>{@code -Dmaven.test.skip=false} — explicitly disabled</li>
     *   <li>anything else</li>
     *   <li>{@code null} or empty set</li>
     * </ul>
     *
     * <p>Hints:
     * <ul>
     *   <li>Two {@link Set#contains} checks — order does not matter.</li>
     *   <li>Comparison is exact; {@code -DMAVEN.TEST.SKIP=true} is not the
     *       same property. Maven is case-sensitive about property names.</li>
     * </ul>
     *
     * @param flags the set of CLI flags (may be null)
     * @return {@code true} if test compilation is skipped
     */
    public static boolean skipsCompilingTests(Set<String> flags) {
        throw new UnsupportedOperationException("TODO: implement skipsCompilingTests");
    }

    // ── Task 4 ─────────────────────────────────────────────────────────────────

    /**
     * Classifies a test class name as picked up by <b>surefire</b>, by
     * <b>failsafe</b>, or by <b>neither</b>, according to the default include
     * patterns of each plugin.
     *
     * <p>Surefire default includes (unit tests):
     * <pre>
     *   ** /Test*.java        → starts with "Test"
     *   ** /*Test.java        → ends with "Test"
     *   ** /*Tests.java       → ends with "Tests"
     *   ** /*TestCase.java    → ends with "TestCase"
     * </pre>
     *
     * <p>Failsafe default includes (integration tests):
     * <pre>
     *   ** /IT*.java          → starts with "IT"
     *   ** /*IT.java          → ends with "IT"
     *   ** /*ITCase.java      → ends with "ITCase"
     * </pre>
     *
     * <p>Surefire is checked <b>first</b>: a class that matches both patterns
     * (e.g. {@code TestIT}) is classified as {@code SUREFIRE}. In a real build
     * <em>both</em> plugins would try to run it — which is why you should
     * avoid such names.
     *
     * <p>Input may include a package prefix ({@code com.foo.BarTest}); the
     * prefix is ignored — only the simple class name is classified.
     *
     * <p>Examples:
     * <pre>{@code
     *   classifyTest("GreeterTest")             → SUREFIRE
     *   classifyTest("GreeterTests")            → SUREFIRE
     *   classifyTest("TestGreeter")             → SUREFIRE
     *   classifyTest("GreeterTestCase")         → SUREFIRE
     *   classifyTest("com.foo.BarTest")         → SUREFIRE
     *   classifyTest("GreeterIT")               → FAILSAFE
     *   classifyTest("ITGreeter")               → FAILSAFE
     *   classifyTest("GreeterITCase")           → FAILSAFE
     *   classifyTest("GreeterHelper")           → NONE
     *   classifyTest(null)                      → NONE
     *   classifyTest("")                        → NONE
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>Trim the package prefix with {@code className.substring(className.lastIndexOf('.') + 1)}.</li>
     *   <li>Check surefire patterns first, then failsafe, then return {@code NONE}.</li>
     *   <li>{@code startsWith}/{@code endsWith} are case-sensitive — that matches
     *       Maven's behaviour.</li>
     * </ul>
     *
     * @param className the class name (may include a package; may be null)
     * @return {@link TestKind#SUREFIRE}, {@link TestKind#FAILSAFE}, or {@link TestKind#NONE}
     */
    public static TestKind classifyTest(String className) {
        throw new UnsupportedOperationException("TODO: implement classifyTest");
    }

    // ── Task 5 ─────────────────────────────────────────────────────────────────

    /**
     * Returns a CI-safe Maven command line: {@code ./mvnw -B -ntp <args...>}.
     *
     * <p>Every CI build should use the wrapper ({@code ./mvnw}), batch mode
     * ({@code -B}) and no-transfer-progress ({@code -ntp}). This method
     * guarantees all three are present, regardless of what the caller passes.
     *
     * <p>The {@code args} are joined with single spaces and appended in the
     * order given. They may mix goals and flags; the method does not
     * distinguish — it trusts the caller.
     *
     * <p>Examples:
     * <pre>{@code
     *   ciCommand(["clean", "verify"])                → "./mvnw -B -ntp clean verify"
     *   ciCommand(["clean", "install"])               → "./mvnw -B -ntp clean install"
     *   ciCommand(["clean", "verify", "-DskipTests"]) → "./mvnw -B -ntp clean verify -DskipTests"
     *   ciCommand(["clean", "verify", "-T", "1C"])    → "./mvnw -B -ntp clean verify -T 1C"
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>{@link String#join(CharSequence, Iterable)} joins a list with a
     *       separator.</li>
     *   <li>Reject {@code null} or empty {@code args}: a CI command with no
     *       goals would do nothing.</li>
     * </ul>
     *
     * @param args the goals and flags (must be non-null and non-empty)
     * @return the CI-safe command line
     * @throws IllegalArgumentException if {@code args} is null/empty, or if
     *                                  any element is null/blank
     */
    public static String ciCommand(List<String> args) {
        throw new UnsupportedOperationException("TODO: implement ciCommand");
    }
}