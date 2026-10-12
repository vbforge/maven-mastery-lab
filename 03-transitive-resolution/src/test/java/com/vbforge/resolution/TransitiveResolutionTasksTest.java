package com.vbforge.resolution;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TransitiveResolutionTasksTest — tests for every method in
 * {@link TransitiveResolutionTasks}.
 *
 * <p><b>Getting started:</b>
 * <ol>
 *   <li>Implement all 5 methods in {@code TransitiveResolutionTasks.java}.</li>
 *   <li>Remove the {@code @Disabled} annotation below.</li>
 *   <li>Run {@code ./mvnw test -pl 03-transitive-resolution -Dtest=TransitiveResolutionTasksTest} ✅</li>
 * </ol>
 */
@Disabled("Remove @Disabled once all 5 methods in TransitiveResolutionTasks are implemented")
@DisplayName("TransitiveResolutionTasks — your implementations")
class TransitiveResolutionTasksTest {

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Artifact art(String ga, String version) {
        return art(ga, version, "compile");
    }

    private static Artifact art(String ga, String version, String scope) {
        String[] parts = ga.split(":");
        return new Artifact(parts[0], parts[1], version, scope);
    }

    private static DependencyNode node(String ga, String version, DependencyNode... children) {
        return DependencyNode.of(art(ga, version), children);
    }

    private static DependencyNode leaf(String ga, String version) {
        return DependencyNode.leaf(art(ga, version));
    }

    // ── Task 1: depthOf ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 1 — depthOf()")
    class DepthOfTest {

        private final DependencyNode root = node("com.vbforge:root", "1.0.0",
                node("com.vbforge:a", "1.0.0",
                        leaf("com.vbforge:a-child", "1.0.0")),
                node("com.vbforge:b", "1.0.0",
                        node("com.vbforge:b-child", "1.0.0",
                                leaf("com.vbforge:deep", "1.0.0"))));

        @Test
        @DisplayName("root itself is depth 0")
        void rootIsZero() {
            assertThat(TransitiveResolutionTasks.depthOf(root, "com.vbforge:root")).isZero();
        }

        @Test
        @DisplayName("direct dependencies are depth 1")
        void directIsOne() {
            assertThat(TransitiveResolutionTasks.depthOf(root, "com.vbforge:a")).isEqualTo(1);
            assertThat(TransitiveResolutionTasks.depthOf(root, "com.vbforge:b")).isEqualTo(1);
        }

        @Test
        @DisplayName("grandchildren are depth 2")
        void grandchildIsTwo() {
            assertThat(TransitiveResolutionTasks.depthOf(root, "com.vbforge:a-child")).isEqualTo(2);
            assertThat(TransitiveResolutionTasks.depthOf(root, "com.vbforge:b-child")).isEqualTo(2);
        }

        @Test
        @DisplayName("deeper nodes have bigger depths")
        void deepIsThree() {
            assertThat(TransitiveResolutionTasks.depthOf(root, "com.vbforge:deep")).isEqualTo(3);
        }

        @Test
        @DisplayName("version is ignored — matches on groupId:artifactId")
        void ignoresVersion() {
            assertThat(TransitiveResolutionTasks.depthOf(root, "com.vbforge:a")).isEqualTo(1);
        }

        @Test
        @DisplayName("unknown artifact → -1")
        void unknown() {
            assertThat(TransitiveResolutionTasks.depthOf(root, "com.vbforge:nope")).isEqualTo(-1);
        }

        @Test
        @DisplayName("null or blank inputs → -1")
        void nulls() {
            assertThat(TransitiveResolutionTasks.depthOf(null, "a:b")).isEqualTo(-1);
            assertThat(TransitiveResolutionTasks.depthOf(root, null)).isEqualTo(-1);
            assertThat(TransitiveResolutionTasks.depthOf(root, "")).isEqualTo(-1);
            assertThat(TransitiveResolutionTasks.depthOf(root, "  ")).isEqualTo(-1);
        }

        @Test
        @DisplayName("nearest path wins — reports the shallowest depth")
        void nearestWins() {
            // root -> x -> shared  (depth 2)
            // root -> shared       (depth 1)
            DependencyNode graph = node("com.vbforge:root", "1.0.0",
                    node("com.vbforge:x", "1.0.0", leaf("com.vbforge:shared", "1.0.0")),
                    leaf("com.vbforge:shared", "2.0.0"));
            assertThat(TransitiveResolutionTasks.depthOf(graph, "com.vbforge:shared")).isEqualTo(1);
        }
    }

    // ── Task 2: resolveVersion ───────────────────────────────────────────────

    @Nested
    @DisplayName("Task 2 — resolveVersion()")
    class ResolveVersionTest {

        @Test
        @DisplayName("the only occurrence's version is returned")
        void onlyOccurrence() {
            DependencyNode graph = node("com.vbforge:root", "1.0.0",
                    leaf("com.vbforge:a", "1.2.3"));
            assertThat(TransitiveResolutionTasks.resolveVersion(graph, "com.vbforge:a"))
                    .isEqualTo("1.2.3");
        }

        @Test
        @DisplayName("nearest wins — depth 1 beats depth 3")
        void nearestWins() {
            DependencyNode graph = node("com.vbforge:root", "1.0.0",
                    node("com.vbforge:x", "1.0.0",
                            node("com.vbforge:y", "1.0.0",
                                    leaf("com.vbforge:shared", "3.0.0"))),
                    leaf("com.vbforge:shared", "1.0.0"));
            assertThat(TransitiveResolutionTasks.resolveVersion(graph, "com.vbforge:shared"))
                    .isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("tie at the same depth — first declared wins")
        void tieFirstWins() {
            DependencyNode graph = node("com.vbforge:root", "1.0.0",
                    node("com.vbforge:a", "1.0.0", leaf("com.vbforge:shared", "1.0.0")),
                    node("com.vbforge:b", "1.0.0", leaf("com.vbforge:shared", "2.0.0")));
            assertThat(TransitiveResolutionTasks.resolveVersion(graph, "com.vbforge:shared"))
                    .isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("unknown artifact → null")
        void unknown() {
            DependencyNode graph = node("com.vbforge:root", "1.0.0");
            assertThat(TransitiveResolutionTasks.resolveVersion(graph, "com.vbforge:nope")).isNull();
        }

        @Test
        @DisplayName("null or blank input → null")
        void nulls() {
            DependencyNode graph = node("com.vbforge:root", "1.0.0", leaf("a:a", "1"));
            assertThat(TransitiveResolutionTasks.resolveVersion(null, "a:a")).isNull();
            assertThat(TransitiveResolutionTasks.resolveVersion(graph, null)).isNull();
            assertThat(TransitiveResolutionTasks.resolveVersion(graph, "")).isNull();
            assertThat(TransitiveResolutionTasks.resolveVersion(graph, "  ")).isNull();
        }
    }

    // ── Task 3: mediateScope ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 3 — mediateScope()")
    class MediateScopeTest {

        @ParameterizedTest(name = "{0} + {1} → {2}")
        @CsvSource({
                "compile,  compile,  compile",
                "compile,  runtime,  runtime",
                "provided, compile,  provided",
                "provided, runtime,  provided",
                "runtime,  compile,  runtime",
                "runtime,  runtime,  runtime",
                "test,     compile,  test",
                "test,     runtime,  test"
        })
        @DisplayName("effective scope for propagating pairs")
        void effective(String parent, String child, String expected) {
            assertThat(TransitiveResolutionTasks.mediateScope(parent, child)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0} + {1} → dropped")
        @CsvSource({
                "compile,  provided",
                "compile,  test",
                "provided, provided",
                "provided, test",
                "runtime,  provided",
                "runtime,  test",
                "test,     provided",
                "test,     test"
        })
        @DisplayName("non-propagating pairs are dropped")
        void dropped(String parent, String child) {
            assertThat(TransitiveResolutionTasks.mediateScope(parent, child)).isNull();
        }

        @Test
        @DisplayName("null or blank input → null")
        void nulls() {
            assertThat(TransitiveResolutionTasks.mediateScope(null, "compile")).isNull();
            assertThat(TransitiveResolutionTasks.mediateScope("compile", null)).isNull();
            assertThat(TransitiveResolutionTasks.mediateScope("", "compile")).isNull();
            assertThat(TransitiveResolutionTasks.mediateScope("compile", " ")).isNull();
        }

        @Test
        @DisplayName("unknown scopes → null")
        void unknown() {
            assertThat(TransitiveResolutionTasks.mediateScope("bogus", "compile")).isNull();
            assertThat(TransitiveResolutionTasks.mediateScope("compile", "bogus")).isNull();
        }
    }

    // ── Task 4: applyExclusions ──────────────────────────────────────────────

    @Nested
    @DisplayName("Task 4 — applyExclusions()")
    class ApplyExclusionsTest {

        @Test
        @DisplayName("an excluded leaf is removed")
        void removesLeaf() {
            DependencyNode input = node("root:root", "1.0.0",
                    leaf("a:keep", "1.0.0"),
                    leaf("b:drop", "1.0.0"));

            DependencyNode expected = node("root:root", "1.0.0",
                    leaf("a:keep", "1.0.0"));

            assertThat(TransitiveResolutionTasks.applyExclusions(input, Set.of("b:drop")))
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("excluding an intermediate node removes its whole subtree")
        void removesSubtree() {
            DependencyNode input = node("root:root", "1.0.0",
                    node("a:keep", "1.0.0", leaf("a:keep:child", "1.0.0")),
                    node("b:drop", "1.0.0",
                            leaf("b:drop:child", "1.0.0"),
                            leaf("b:drop:other", "1.0.0")));

            DependencyNode expected = node("root:root", "1.0.0",
                    node("a:keep", "1.0.0", leaf("a:keep:child", "1.0.0")));

            assertThat(TransitiveResolutionTasks.applyExclusions(input, Set.of("b:drop")))
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("exclusions apply at every depth")
        void appliesAtAnyDepth() {
            DependencyNode input = node("root:root", "1.0.0",
                    node("a:a", "1.0.0",
                            node("b:b", "1.0.0",
                                    leaf("deep:bad", "1.0.0"))));

            DependencyNode expected = node("root:root", "1.0.0",
                    node("a:a", "1.0.0",
                            node("b:b", "1.0.0")));

            assertThat(TransitiveResolutionTasks.applyExclusions(input, Set.of("deep:bad")))
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("empty or null exclusion set leaves the tree unchanged")
        void emptySet() {
            DependencyNode input = node("root:root", "1.0.0", leaf("a:a", "1.0.0"));
            assertThat(TransitiveResolutionTasks.applyExclusions(input, Set.of())).isEqualTo(input);
            assertThat(TransitiveResolutionTasks.applyExclusions(input, null)).isEqualTo(input);
        }

        @Test
        @DisplayName("the root can never be excluded")
        void rootSurvives() {
            DependencyNode input = node("root:root", "1.0.0", leaf("a:a", "1.0.0"));
            assertThat(TransitiveResolutionTasks.applyExclusions(input, Set.of("root:root")))
                    .isEqualTo(input);
        }

        @Test
        @DisplayName("the input tree is not modified")
        void inputUnchanged() {
            DependencyNode input = node("root:root", "1.0.0", leaf("a:a", "1.0.0"));
            TransitiveResolutionTasks.applyExclusions(input, Set.of("a:a"));
            assertThat(input.children()).hasSize(1);
        }

        @Test
        @DisplayName("null root → null")
        void nullRoot() {
            assertThat(TransitiveResolutionTasks.applyExclusions(null, Set.of("a:a"))).isNull();
        }
    }

    // ── Task 5: findConflicts ────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 5 — findConflicts()")
    class FindConflictsTest {

        @Test
        @DisplayName("no conflicts → empty map")
        void noConflicts() {
            DependencyNode graph = node("root:root", "1.0.0",
                    leaf("a:a", "1.0.0"),
                    node("b:b", "1.0.0", leaf("c:c", "1.0.0")));
            assertThat(TransitiveResolutionTasks.findConflicts(graph)).isEmpty();
        }

        @Test
        @DisplayName("two versions of the same artifact → one entry")
        void oneConflict() {
            DependencyNode graph = node("root:root", "1.0.0",
                    node("a:a", "1.0.0", leaf("shared:shared", "1.0.0")),
                    node("b:b", "1.0.0", leaf("shared:shared", "2.0.0")));

            Map<String, Set<String>> conflicts = TransitiveResolutionTasks.findConflicts(graph);
            assertThat(conflicts).containsOnlyKeys("shared:shared");
            assertThat(conflicts.get("shared:shared"))
                    .containsExactlyInAnyOrder("1.0.0", "2.0.0");
        }

        @Test
        @DisplayName("multiple conflicts across different artifacts")
        void multiple() {
            DependencyNode graph = node("root:root", "1.0.0",
                    node("a:a", "1.0.0",
                            leaf("s1:s1", "1.0.0"),
                            leaf("s2:s2", "1.0.0")),
                    node("b:b", "1.0.0",
                            leaf("s1:s1", "2.0.0"),
                            leaf("s2:s2", "2.0.0")));

            Map<String, Set<String>> conflicts = TransitiveResolutionTasks.findConflicts(graph);
            assertThat(conflicts).containsOnlyKeys("s1:s1", "s2:s2");
        }

        @Test
        @DisplayName("same version in multiple places → no conflict")
        void sameVersionNoConflict() {
            DependencyNode graph = node("root:root", "1.0.0",
                    node("a:a", "1.0.0", leaf("shared:shared", "1.0.0")),
                    node("b:b", "1.0.0", leaf("shared:shared", "1.0.0")));
            assertThat(TransitiveResolutionTasks.findConflicts(graph)).isEmpty();
        }

        @Test
        @DisplayName("three versions of the same artifact → one entry with all three")
        void threeVersions() {
            DependencyNode graph = node("root:root", "1.0.0",
                    node("a:a", "1.0.0", leaf("shared:shared", "1.0.0")),
                    node("b:b", "1.0.0", leaf("shared:shared", "2.0.0")),
                    node("c:c", "1.0.0", leaf("shared:shared", "3.0.0")));

            Map<String, Set<String>> conflicts = TransitiveResolutionTasks.findConflicts(graph);
            assertThat(conflicts.get("shared:shared"))
                    .containsExactlyInAnyOrder("1.0.0", "2.0.0", "3.0.0");
        }

        @Test
        @DisplayName("null root → empty map")
        void nullRoot() {
            assertThat(TransitiveResolutionTasks.findConflicts(null)).isEmpty();
        }
    }
}