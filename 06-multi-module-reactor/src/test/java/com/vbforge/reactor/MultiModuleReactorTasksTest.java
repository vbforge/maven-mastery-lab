package com.vbforge.reactor;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MultiModuleReactorTasksTest — tests for every method in
 * {@link MultiModuleReactorTasks}.
 *
 * <p><b>Getting started:</b>
 * <ol>
 *   <li>Implement all 5 methods in {@code MultiModuleReactorTasks.java}.</li>
 *   <li>Remove the {@code @Disabled} annotation below.</li>
 *   <li>Run {@code ./mvnw test -pl 06-multi-module-reactor -Dtest=MultiModuleReactorTasksTest} ✅</li>
 * </ol>
 */
@Disabled("Remove @Disabled once all 5 methods in MultiModuleReactorTasks are implemented")
@DisplayName("MultiModuleReactorTasks — your implementations")
class MultiModuleReactorTasksTest {

    // ── fixtures ──────────────────────────────────────────────────────────────

    private static final List<Module> CHAIN = List.of(
            Module.of("core"),
            Module.of("service", "core"),
            Module.of("app", "service"));

    // ── Task 1: buildOrder ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 1 — buildOrder()")
    class BuildOrderTest {

        @Test
        @DisplayName("chain in natural order")
        void chain() {
            assertThat(MultiModuleReactorTasks.buildOrder(CHAIN))
                    .containsExactly("core", "service", "app");
        }

        @Test
        @DisplayName("chain declared in reverse order")
        void reversedChain() {
            List<Module> reversed = List.of(
                    Module.of("app", "service"),
                    Module.of("service", "core"),
                    Module.of("core"));
            assertThat(MultiModuleReactorTasks.buildOrder(reversed))
                    .containsExactly("core", "service", "app");
        }

        @Test
        @DisplayName("multiple roots are emitted in declaration order")
        void multiRoot() {
            List<Module> reactor = List.of(
                    Module.of("a"),
                    Module.of("b"),
                    Module.of("c", "a"));
            assertThat(MultiModuleReactorTasks.buildOrder(reactor))
                    .containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("module declared first still waits for its dependencies")
        void outOfOrder() {
            List<Module> reactor = List.of(
                    Module.of("c", "a"),
                    Module.of("a"),
                    Module.of("b"));
            assertThat(MultiModuleReactorTasks.buildOrder(reactor))
                    .containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("diamond dependency")
        void diamond() {
            //    root
            //   /    \
            //  left  right
            //   \    /
            //    top
            List<Module> reactor = List.of(
                    Module.of("root"),
                    Module.of("left", "root"),
                    Module.of("right", "root"),
                    Module.of("top", "left", "right"));
            List<String> order = MultiModuleReactorTasks.buildOrder(reactor);

            assertThat(order).containsExactly("root", "left", "right", "top");
            assertThat(order.indexOf("root")).isLessThan(order.indexOf("left"));
            assertThat(order.indexOf("root")).isLessThan(order.indexOf("right"));
            assertThat(order.indexOf("left")).isLessThan(order.indexOf("top"));
            assertThat(order.indexOf("right")).isLessThan(order.indexOf("top"));
        }

        @Test
        @DisplayName("simple cycle throws IllegalStateException")
        void cycle() {
            List<Module> cycle = List.of(
                    Module.of("a", "b"),
                    Module.of("b", "c"),
                    Module.of("c", "a"));
            assertThatThrownBy(() -> MultiModuleReactorTasks.buildOrder(cycle))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("self-loop throws IllegalStateException")
        void selfLoop() {
            assertThatThrownBy(() -> MultiModuleReactorTasks.buildOrder(
                    List.of(Module.of("a", "a"))))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("single module with no dependencies")
        void single() {
            assertThat(MultiModuleReactorTasks.buildOrder(List.of(Module.of("only")))
                    ).containsExactly("only");
        }
    }

    // ── Task 2: detectCycle ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 2 — detectCycle()")
    class DetectCycleTest {

        @Test
        @DisplayName("acyclic reactor → empty list")
        void acyclic() {
            assertThat(MultiModuleReactorTasks.detectCycle(CHAIN)).isEmpty();
        }

        @Test
        @DisplayName("empty reactor → empty list")
        void empty() {
            assertThat(MultiModuleReactorTasks.detectCycle(List.of())).isEmpty();
        }

        @Test
        @DisplayName("null reactor → empty list")
        void nullInput() {
            assertThat(MultiModuleReactorTasks.detectCycle(null)).isEmpty();
        }

        @Test
        @DisplayName("three-node cycle a → b → c → a")
        void threeNode() {
            List<Module> cycle = List.of(
                    Module.of("a", "b"),
                    Module.of("b", "c"),
                    Module.of("c", "a"));

            List<String> detected = MultiModuleReactorTasks.detectCycle(cycle);
            assertThat(detected).containsExactlyInAnyOrder("a", "b", "c");
        }

        @Test
        @DisplayName("two-node cycle x → y → x")
        void twoNode() {
            List<Module> cycle = List.of(
                    Module.of("x", "y"),
                    Module.of("y", "x"));

            assertThat(MultiModuleReactorTasks.detectCycle(cycle))
                    .containsExactlyInAnyOrder("x", "y");
        }

        @Test
        @DisplayName("cycle reported even when other modules are fine")
        void cycleAmidstFineModules() {
            List<Module> reactor = List.of(
                    Module.of("core"),
                    Module.of("service", "core"),
                    Module.of("a", "b"),
                    Module.of("b", "a"));
            assertThat(MultiModuleReactorTasks.detectCycle(reactor))
                    .containsExactlyInAnyOrder("a", "b");
        }

        @Test
        @DisplayName("self-loop detected")
        void selfLoop() {
            assertThat(MultiModuleReactorTasks.detectCycle(List.of(Module.of("a", "a"))))
                    .containsExactly("a");
        }
    }

    // ── Task 3: alsoMake ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 3 — alsoMake()")
    class AlsoMakeTest {

        @Test
        @DisplayName("middle of the chain: service + core")
        void middle() {
            assertThat(MultiModuleReactorTasks.alsoMake("service", CHAIN))
                    .containsExactlyInAnyOrder("core", "service");
        }

        @Test
        @DisplayName("top of the chain: everything")
        void top() {
            assertThat(MultiModuleReactorTasks.alsoMake("app", CHAIN))
                    .containsExactlyInAnyOrder("core", "service", "app");
        }

        @Test
        @DisplayName("root has no upstream, only itself")
        void root() {
            assertThat(MultiModuleReactorTasks.alsoMake("core", CHAIN))
                    .containsExactly("core");
        }

        @Test
        @DisplayName("unknown target → empty set")
        void unknown() {
            assertThat(MultiModuleReactorTasks.alsoMake("nope", CHAIN)).isEmpty();
        }

        @Test
        @DisplayName("diamond: also-make includes both branches")
        void diamond() {
            List<Module> reactor = List.of(
                    Module.of("root"),
                    Module.of("left", "root"),
                    Module.of("right", "root"),
                    Module.of("top", "left", "right"));

            assertThat(MultiModuleReactorTasks.alsoMake("top", reactor))
                    .containsExactlyInAnyOrder("root", "left", "right", "top");
            assertThat(MultiModuleReactorTasks.alsoMake("left", reactor))
                    .containsExactlyInAnyOrder("root", "left");
        }

        @Test
        @DisplayName("null reactor or blank target → empty set")
        void nulls() {
            assertThat(MultiModuleReactorTasks.alsoMake("app", null)).isEmpty();
            assertThat(MultiModuleReactorTasks.alsoMake(null, CHAIN)).isEmpty();
            assertThat(MultiModuleReactorTasks.alsoMake("  ", CHAIN)).isEmpty();
        }
    }

    // ── Task 4: alsoMakeDependents ───────────────────────────────────────────

    @Nested
    @DisplayName("Task 4 — alsoMakeDependents()")
    class AlsoMakeDependentsTest {

        @Test
        @DisplayName("bottom of the chain: everything depends on it")
        void bottom() {
            assertThat(MultiModuleReactorTasks.alsoMakeDependents("core", CHAIN))
                    .containsExactlyInAnyOrder("core", "service", "app");
        }

        @Test
        @DisplayName("middle: service + app")
        void middle() {
            assertThat(MultiModuleReactorTasks.alsoMakeDependents("service", CHAIN))
                    .containsExactlyInAnyOrder("service", "app");
        }

        @Test
        @DisplayName("top has no dependents, only itself")
        void top() {
            assertThat(MultiModuleReactorTasks.alsoMakeDependents("app", CHAIN))
                    .containsExactly("app");
        }

        @Test
        @DisplayName("unknown target → empty set")
        void unknown() {
            assertThat(MultiModuleReactorTasks.alsoMakeDependents("nope", CHAIN)).isEmpty();
        }

        @Test
        @DisplayName("diamond: dependents of root include the whole diamond")
        void diamond() {
            List<Module> reactor = List.of(
                    Module.of("root"),
                    Module.of("left", "root"),
                    Module.of("right", "root"),
                    Module.of("top", "left", "right"));

            assertThat(MultiModuleReactorTasks.alsoMakeDependents("root", reactor))
                    .containsExactlyInAnyOrder("root", "left", "right", "top");
            assertThat(MultiModuleReactorTasks.alsoMakeDependents("left", reactor))
                    .containsExactlyInAnyOrder("left", "top");
        }

        @Test
        @DisplayName("null reactor or blank target → empty set")
        void nulls() {
            assertThat(MultiModuleReactorTasks.alsoMakeDependents("app", null)).isEmpty();
            assertThat(MultiModuleReactorTasks.alsoMakeDependents(null, CHAIN)).isEmpty();
        }
    }

    // ── Task 5: isAggregatorOnly ─────────────────────────────────────────────

    @Nested
    @DisplayName("Task 5 — isAggregatorOnly()")
    class IsAggregatorOnlyTest {

        @Test
        @DisplayName("pom + modules, no children → true")
        void pureAggregator() {
            assertThat(MultiModuleReactorTasks.isAggregatorOnly(
                    PomSummary.aggregatorOnly("build-all", "a", "b", "c")))
                    .isTrue();
        }

        @Test
        @DisplayName("pom + modules + children → false (it is also a parent)")
        void aggregatorAndParent() {
            assertThat(MultiModuleReactorTasks.isAggregatorOnly(
                    PomSummary.aggregatorAndParent("root",
                            List.of("a", "b"), List.of("a", "b"))))
                    .isFalse();
        }

        @Test
        @DisplayName("pom + children, no modules → false (parent-only)")
        void parentOnly() {
            assertThat(MultiModuleReactorTasks.isAggregatorOnly(
                    PomSummary.parentOnly("company-parent", "x", "y")))
                    .isFalse();
        }

        @Test
        @DisplayName("jar packaging → false")
        void jar() {
            assertThat(MultiModuleReactorTasks.isAggregatorOnly(PomSummary.jar("core")))
                    .isFalse();
        }

        @Test
        @DisplayName("pom packaging with no modules and no children → false")
        void emptyPom() {
            assertThat(MultiModuleReactorTasks.isAggregatorOnly(
                    new PomSummary("empty", "pom", List.of(), List.of())))
                    .isFalse();
        }
    }
}