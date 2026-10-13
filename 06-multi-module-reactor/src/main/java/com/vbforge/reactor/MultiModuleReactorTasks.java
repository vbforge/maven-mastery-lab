package com.vbforge.reactor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * MultiModuleReactorTasks — implement every method below to make
 * {@code MultiModuleReactorTasksTest} go GREEN.
 *
 * <p><b>Workflow:</b>
 * <ol>
 *   <li>Read {@code MultiModuleReactorDemo.java} to understand the concept.</li>
 *   <li>Implement a method — remove the {@code UnsupportedOperationException}.</li>
 *   <li>When all 5 are done, remove the {@code @Disabled} annotation from
 *       {@code MultiModuleReactorTasksTest} and run
 *       {@code ./mvnw test -pl 06-multi-module-reactor -Dtest=MultiModuleReactorTasksTest} ✅</li>
 * </ol>
 *
 * <p>All methods operate on a {@link Module} list — a reactor — where each
 * module's {@code dependencies} are the ids of other modules in the reactor.
 * Do not change method signatures or add external libraries — only the Java
 * standard library is needed.
 */
public class MultiModuleReactorTasks {

    // ── Task 1 ─────────────────────────────────────────────────────────────────

    /**
     * Returns the reactor build order: a topological sort of {@code modules}
     * such that each module appears after every module it depends on.
     *
     * <p>Ties are broken by <b>declaration order</b>: at each step, scan the
     * input list left-to-right and emit any module whose dependencies have all
     * been emitted already. Repeat until nothing more can be emitted.
     *
     * <p>This matches Maven: if two modules could be built next, the one listed
     * first in {@code <modules>} wins.
     *
     * <p>Examples:
     * <pre>{@code
     *   modules = [ core,          // no deps
     *               service,       // deps: core
     *               app ]          // deps: service
     *   buildOrder(modules) → [core, service, app]
     *
     *   modules = [ b,    // deps: a
     *               a ]   // no deps
     *   buildOrder(modules) → [a, b]
     *
     *   modules = [ c,    // deps: a
     *               a,
     *               b ]   // no deps
     *   buildOrder(modules) → [a, b, c]
     * }</pre>
     *
     * <p>If no valid order exists (there is a cycle), throw
     * {@link IllegalStateException}.
     *
     * <p>Hints:
     * <ul>
     *   <li>Keep a {@code Set} of emitted ids and loop while progress is made.</li>
     *   <li>One pass over {@code modules} per iteration is enough.</li>
     *   <li>Emit a module only when every dependency it declares is already
     *       in the emitted set.</li>
     *   <li>At the end, if the emitted set is smaller than the input list, a
     *       cycle exists.</li>
     * </ul>
     *
     * @param modules the reactor
     * @return the module ids in build order
     * @throws IllegalStateException if the reactor contains a cycle
     */
    public static List<String> buildOrder(List<Module> modules) {
        throw new UnsupportedOperationException("TODO: implement buildOrder");
    }

    // ── Task 2 ─────────────────────────────────────────────────────────────────

    /**
     * Detects a cycle in the reactor.
     *
     * <p>If the graph is acyclic, returns an empty list. Otherwise returns the
     * cycle as an ordered list of module ids, in the order they form the
     * cycle — the first element is <b>not</b> repeated at the end.
     *
     * <p>Examples:
     * <pre>{@code
     *   modules = [ a (deps: b), b (deps: c), c (deps: a) ]
     *   detectCycle(modules) → [a, b, c]   // a → b → c → a
     *
     *   modules = [ core, service (deps: core) ]
     *   detectCycle(modules) → []          // no cycle
     *
     *   modules = [ x (deps: y), y (deps: x), z ]
     *   detectCycle(modules) → [x, y]  or  [y, x]
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>Depth-first search with three colours is the classic approach:
     *       <b>white</b> (unvisited), <b>gray</b> (on the current path),
     *       <b>black</b> (done).</li>
     *   <li>A back-edge to a gray node is a cycle.</li>
     *   <li>Keep a stack of the current path to reconstruct the cycle.</li>
     *   <li>{@code null} input or an empty list → empty list.</li>
     * </ul>
     *
     * @param modules the reactor
     * @return the cycle as an ordered list, or an empty list if none
     */
    public static List<String> detectCycle(List<Module> modules) {
        throw new UnsupportedOperationException("TODO: implement detectCycle");
    }

    // ── Task 3 ─────────────────────────────────────────────────────────────────

    /**
     * Returns the set of module ids Maven would build for
     * {@code mvn -pl <target> -am}.
     *
     * <p>{@code -am} ("also make") adds every module the target depends on,
     * transitively. The target itself is <b>included</b> in the result.
     *
     * <p>Examples:
     * <pre>{@code
     *   modules = [ core, service (deps: core), app (deps: service) ]
     *
     *   alsoMake("service", modules) → {core, service}
     *   alsoMake("app",     modules) → {core, service, app}
     *   alsoMake("core",    modules) → {core}
     *   alsoMake("nope",    modules) → {}          (unknown target)
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>Look up the target module by id; if absent, return an empty set.</li>
     *   <li>Do a transitive closure over the {@code dependencies} edges.</li>
     *   <li>{@link java.util.ArrayDeque} works well as a work-list.</li>
     *   <li>A {@link LinkedHashSet} preserves visit order — handy for tests.</li>
     * </ul>
     *
     * @param target  the module id passed to {@code -pl}
     * @param modules the reactor
     * @return the set of ids Maven would build (target included)
     */
    public static Set<String> alsoMake(String target, List<Module> modules) {
        throw new UnsupportedOperationException("TODO: implement alsoMake");
    }

    // ── Task 4 ─────────────────────────────────────────────────────────────────

    /**
     * Returns the set of module ids Maven would build for
     * {@code mvn -pl <target> -amd}.
     *
     * <p>{@code -amd} ("also make dependents") adds every module that depends
     * on the target, transitively. The target itself is <b>included</b>.
     *
     * <p>Examples:
     * <pre>{@code
     *   modules = [ core, service (deps: core), app (deps: service) ]
     *
     *   alsoMakeDependents("core",    modules) → {core, service, app}
     *   alsoMakeDependents("service", modules) → {service, app}
     *   alsoMakeDependents("app",     modules) → {app}
     *   alsoMakeDependents("nope",    modules) → {}
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>You can invert the edge direction: for each module, "who depends
     *       on it" is the set of modules whose {@code dependencies} list
     *       contains its id.</li>
     *   <li>Same work-list strategy as Task 3, but on the inverted graph.</li>
     * </ul>
     *
     * @param target  the module id passed to {@code -pl}
     * @param modules the reactor
     * @return the set of ids Maven would build (target included)
     */
    public static Set<String> alsoMakeDependents(String target, List<Module> modules) {
        throw new UnsupportedOperationException("TODO: implement alsoMakeDependents");
    }

    // ── Task 5 ─────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} iff the given pom is an <b>aggregator only</b>:
     * it declares {@code <modules>} and its {@code <packaging>} is
     * {@code "pom"}, but <b>no</b> module in the reactor declares it as its
     * parent.
     *
     * <p>Examples:
     * <pre>{@code
     *   // pom-packaged, declares modules, has no children → true
     *   isAggregatorOnly(new PomSummary("build-all", "pom",
     *                                   List.of("a", "b"), List.of())) → true
     *
     *   // pom-packaged, but has children → false (it is also a parent)
     *   isAggregatorOnly(new PomSummary("root", "pom",
     *                                   List.of("a", "b"), List.of("a", "b"))) → false
     *
     *   // pom-packaged, no modules → false (not an aggregator at all)
     *   isAggregatorOnly(new PomSummary("parent-only", "pom",
     *                                   List.of(), List.of("a"))) → false
     *
     *   // jar-packaged → false (jar modules cannot aggregate)
     *   isAggregatorOnly(new PomSummary("core", "jar",
     *                                   List.of(), List.of())) → false
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>Three checks, all needed: packaging is {@code "pom"}, modules
     *       non-empty, children empty.</li>
     *   <li>Use {@code "pom".equals(pom.packaging())} — the packaging can be
     *       {@code null}.</li>
     * </ul>
     *
     * @param pom the pom summary
     * @return {@code true} if this pom plays the aggregator role and nothing else
     */
    public static boolean isAggregatorOnly(PomSummary pom) {
        throw new UnsupportedOperationException("TODO: implement isAggregatorOnly");
    }
}