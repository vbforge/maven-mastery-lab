package com.vbforge.reactor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * MultiModuleReactorDemo — annotated reference for the Maven reactor.
 *
 * <p>Read top-to-bottom. Each section covers one mechanism and explains
 * <em>why</em> it exists — not just what the XML looks like.
 *
 * <p>Topics:
 * <ol>
 *   <li>What a reactor is</li>
 *   <li>Aggregator vs parent</li>
 *   <li>Reactor build order (topological)</li>
 *   <li>{@code -pl}, {@code -am}, {@code -amd}, {@code -rf}</li>
 *   <li>One version across many modules ({@code ${revision}})</li>
 *   <li>Cycles</li>
 * </ol>
 *
 * <p>Run: {@code ./mvnw -q exec:java -pl 06-multi-module-reactor
 * -Dexec.mainClass="com.vbforge.reactor.MultiModuleReactorDemo"}
 */
public class MultiModuleReactorDemo {

    public static void main(String[] args) {
        reactorDemo();
        System.out.println("---");
        aggregatorVsParentDemo();
        System.out.println("---");
        buildOrderDemo();
        System.out.println("---");
        selectionFlagsDemo();
        System.out.println("---");
        revisionDemo();
        System.out.println("---");
        cyclesDemo();
    }

    // ── 1. What is a reactor? ────────────────────────────────────────────────

    /**
     * The <b>reactor</b> is the set of modules Maven is currently building in
     * one invocation, plus the order it processes them in.
     *
     * <p>WHY this matters: the reactor is what makes multi-module builds work.
     * Maven reads every module's pom, works out which ones depend on which
     * other ones, and builds them in topological order — no {@code mvn install}
     * loop in a bash script required. The reactor log line
     * {@code Reactor Build Order} is the first thing to read when a
     * multi-module build behaves oddly.
     *
     * <p>A module is in the reactor if it is reachable from the aggregator pom
     * through {@code <modules>}.
     */
    static void reactorDemo() {
        System.out.println("=== the reactor ===");
        System.out.println("typical log line:");
        System.out.println("  [INFO] Reactor Build Order:");
        System.out.println("  [INFO]   sample-core     [jar]");
        System.out.println("  [INFO]   sample-service  [jar]");
        System.out.println("  [INFO]   sample-app      [jar]");
        System.out.println();
        System.out.println("The order is computed from the graph of inter-module dependencies,");
        System.out.println("NOT from the order modules are written in <modules>.");
    }

    // ── 2. Aggregator vs parent ──────────────────────────────────────────────

    /**
     * Two roles that are often confused because the same pom frequently plays
     * both:
     * <ul>
     *   <li><b>Aggregator</b> — declares {@code <modules>} and has
     *       {@code <packaging>pom</packaging>}. Its job: "build these together
     *       when you build me". It does <em>not</em> share any configuration.</li>
     *   <li><b>Parent</b> — is referenced in another pom's {@code <parent>}
     *       block. Its job: share dependency versions, plugin config,
     *       properties. It has <em>no</em> opinion about what gets built
     *       together.</li>
     * </ul>
     *
     * <p>WHY this matters: an aggregator with no children is legal and common
     * (it's just a build entry point). A parent with no {@code <modules>} is
     * the classic shape of a shared "framework pom" that is <em>not</em> a
     * build root. Mixing the two roles into one file is convenient but
     * conflates two concerns; a real codebase often uses separate poms.
     */
    public static PomSummary aggregatorOnlyPom() {
        return PomSummary.aggregatorOnly("build-all",
                "01-maven-recap", "02-dependency-management", "03-transitive-resolution");
    }

    public static PomSummary parentOnlyPom() {
        return PomSummary.parentOnly("company-parent",
                "my-service", "my-web", "my-batch");
    }

    public static PomSummary aggregatorAndParentPom() {
        return PomSummary.aggregatorAndParent("maven-mastery-lab",
                List.of("01-maven-recap", "02-dependency-management",
                        "03-transitive-resolution", "04-repositories-settings",
                        "05-properties-profiles-versions", "06-multi-module-reactor"),
                List.of("01-maven-recap", "02-dependency-management",
                        "03-transitive-resolution", "04-repositories-settings",
                        "05-properties-profiles-versions", "06-multi-module-reactor"));
    }

    /**
     * True iff the pom declares {@code <modules>} <em>and</em> is not the parent
     * of any reactor module. In other words: an aggregator that is
     * <b>only</b> an aggregator.
     */
    public static boolean isAggregatorOnly(PomSummary pom) {
        return "pom".equals(pom.packaging())
                && !pom.declaresModules().isEmpty()
                && pom.isParentOf().isEmpty();
    }

    static void aggregatorVsParentDemo() {
        System.out.println("=== aggregator vs parent ===");
        PomSummary agg = aggregatorOnlyPom();
        PomSummary par = parentOnlyPom();
        PomSummary both = aggregatorAndParentPom();

        System.out.printf("aggregatorOnly(%s)          = %s%n", agg.artifactId(), isAggregatorOnly(agg));
        System.out.printf("aggregatorOnly(%s)          = %s%n", par.artifactId(), isAggregatorOnly(par));
        System.out.printf("aggregatorOnly(%s)   = %s%n", both.artifactId(), isAggregatorOnly(both));
        System.out.println();
        System.out.println("aggregator + parent (the common case) is NOT 'aggregator only'.");
    }

    // ── 3. Build order ───────────────────────────────────────────────────────

    /**
     * The reactor's build order is a <b>topological sort</b> of the module
     * graph: a module is built only after every module it depends on.
     *
     * <p>Maven breaks ties by <em>declaration order</em> — if two modules could
     * be built next, the one listed first in {@code <modules>} wins. That makes
     * the order deterministic across runs.
     */
    public static List<Module> sampleReactor() {
        return List.of(
                Module.of("sample-core"),
                Module.of("sample-service", "sample-core"),
                Module.of("sample-app",     "sample-service")
        );
    }

    /** Simple "repeatedly scan, emit ready modules" topological sort. */
    public static List<String> buildOrder(List<Module> modules) {
        List<String> emitted = new ArrayList<>();
        Set<String> emittedSet = new LinkedHashSet<>();
        boolean progress = true;

        while (progress && emitted.size() < modules.size()) {
            progress = false;
            for (Module m : modules) {
                if (emittedSet.contains(m.id())) {
                    continue;
                }
                if (emittedSet.containsAll(m.dependencies())) {
                    emitted.add(m.id());
                    emittedSet.add(m.id());
                    progress = true;
                }
            }
        }

        if (emitted.size() < modules.size()) {
            throw new IllegalStateException("dependency cycle in reactor: " + modules);
        }
        return emitted;
    }

    static void buildOrderDemo() {
        System.out.println("=== build order ===");
        sampleReactor().forEach(m ->
                System.out.printf("  %-16s depends on %s%n", m.id(),
                        m.dependencies().isEmpty() ? "(nothing)" : m.dependencies()));
        System.out.println("=> " + buildOrder(sampleReactor()));
    }

    // ── 4. Selection flags ───────────────────────────────────────────────────

    /**
     * Four flags let you build a slice of the reactor:
     * <ul>
     *   <li>{@code -pl <ids>} — "project list": build only these modules.</li>
     *   <li>{@code -am}      — "also make": include the modules the target
     *       depends on (transitively).</li>
     *   <li>{@code -amd}     — "also make dependents": include the modules
     *       that depend on the target (transitively).</li>
     *   <li>{@code -rf <id>} — "resume from": skip modules until this one is
     *       reached, then continue to the end.</li>
     * </ul>
     *
     * <p>WHY this matters: on CI or a developer laptop you rarely want to build
     * the whole reactor. {@code -pl service -am} is the everyday command for
     * "the service and everything it needs".
     */
    static void selectionFlagsDemo() {
        System.out.println("=== selection flags ===");
        System.out.println("full reactor order  : " + buildOrder(sampleReactor()));
        System.out.println("-pl sample-service           → only 'sample-service' (deps must be installed already)");
        System.out.println("-pl sample-service -am       → ['sample-core', 'sample-service'] (adds upstream)");
        System.out.println("-pl sample-core -amd         → ['sample-core', 'sample-service', 'sample-app'] (adds downstream)");
        System.out.println("-rf sample-service           → ['sample-service', 'sample-app'] (skip until target, then to the end)");
    }

    // ── 5. ${revision} ───────────────────────────────────────────────────────

    /**
     * Every module in a multi-module build usually shares one version. The
     * CI-friendly pattern expresses that as a property:
     * <pre>{@code
     *   <groupId>com.vbforge</groupId>
     *   <artifactId>my-app</artifactId>
     *   <version>${revision}</version>
     *
     *   <properties>
     *       <revision>1.0.0-SNAPSHOT</revision>
     *   </properties>
     * }</pre>
     *
     * <p>WHY this matters: a version bump in a multi-module project without
     * {@code ${revision}} requires editing every child pom <em>and</em> every
     * inter-module dependency. With {@code ${revision}}, one number lives in
     * one place and the entire build follows.
     */
    public static String revisionSnippet() {
        return """
                <project>
                    <groupId>com.vbforge</groupId>
                    <artifactId>my-multi-module</artifactId>
                    <version>${revision}</version>
                    <packaging>pom</packaging>

                    <properties>
                        <revision>1.0.0-SNAPSHOT</revision>
                        <sha1/>
                        <changelist/>
                    </properties>

                    <modules>
                        <module>core</module>
                        <module>service</module>
                        <module>web</module>
                    </modules>
                </project>
                """;
    }

    static void revisionDemo() {
        System.out.println("=== ${revision} ===");
        System.out.println(revisionSnippet());
        System.out.println("then: ./mvnw -Drevision=2.0.0 clean verify");
        System.out.println("only ${revision}, ${sha1} and ${changelist} are recognised by Maven itself.");
    }

    // ── 6. Cycles ────────────────────────────────────────────────────────────

    /**
     * A reactor cannot contain a dependency cycle. If module A depends on B
     * and B depends on A, there is no valid build order — Maven fails
     * immediately, before compiling anything.
     *
     * <p>WHY this matters: cycles are almost always a design smell. The fix is
     * rarely "reorder the modules"; it is to extract the shared piece both
     * modules need into a third module that both can depend on.
     *
     * <p>Maven's error message lists the cycle path explicitly, which is the
     * best hint you get for free.
     */
    public static List<Module> cyclicReactor() {
        return List.of(
                Module.of("a", "b"),
                Module.of("b", "c"),
                Module.of("c", "a")
        );
    }

    static void cyclesDemo() {
        System.out.println("=== cycles ===");
        System.out.println("cyclic reactor: " + cyclicReactor());
        System.out.println();
        System.out.println("Maven's error:");
        System.out.println("  [ERROR] The projects in the reactor contain a cyclic reference:");
        System.out.println("          The projects a, b, c constitute a cycle.");
        System.out.println();
        System.out.println("fix: extract the shared piece both modules need into a new module.");
        System.out.println("the reactor is a DAG, not a general graph.");
    }
}