package com.vbforge.resolution;

import com.vbforge.resolution.TransitiveResolutionDemo.ScopeMediation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TransitiveResolutionDemoTest — verifies every concept shown in
 * {@link TransitiveResolutionDemo}.
 *
 * <p>These tests are always GREEN. Read them as an executable specification
 * of Maven's transitive-resolution model.
 */
@DisplayName("TransitiveResolutionDemo — API contracts")
class TransitiveResolutionDemoTest {

    // ── 1. transitive example ─────────────────────────────────────────────────

    @Nested
    @DisplayName("1. transitive dependencies")
    class TransitiveTest {

        @Test
        @DisplayName("root is my-app, direct child is commons-lang3, grandchild is commons-text")
        void treeShape() {
            DependencyNode root = TransitiveResolutionDemo.transitiveExample();
            assertThat(root.artifact().ga()).isEqualTo("com.vbforge:my-app");
            assertThat(root.children()).hasSize(1);

            DependencyNode lang = root.children().get(0);
            assertThat(lang.artifact().ga()).isEqualTo("org.apache.commons:commons-lang3");
            assertThat(lang.children()).hasSize(1);
            assertThat(lang.children().get(0).artifact().ga())
                    .isEqualTo("org.apache.commons:commons-text");
        }
    }

    // ── 2. nearest-wins example ───────────────────────────────────────────────

    @Nested
    @DisplayName("2. nearest-wins")
    class NearestWinsTest {

        @Test
        @DisplayName("shared appears at three depths, in three different versions")
        void threePaths() {
            DependencyNode root = TransitiveResolutionDemo.nearestWinsExample();
            assertThat(root.children()).hasSize(3);

            List<String> versions = root.children().stream()
                    .flatMap(n -> n.children().stream())
                    .filter(n -> n.artifact().ga().equals("com.example:shared"))
                    .map(n -> n.artifact().version())
                    .toList();
            assertThat(versions).containsExactlyInAnyOrder("1.0.0", "2.0.0");

            // the third path is one level deeper
            DependencyNode libC = root.children().get(2);
            assertThat(libC.children().get(0).children().get(0).artifact().ga())
                    .isEqualTo("com.example:shared");
        }
    }

    // ── 3. scope mediation ────────────────────────────────────────────────────

    @Nested
    @DisplayName("3. scope mediation")
    class ScopeMediationTest {

        @Test
        @DisplayName("the table has 16 rows for the four standard scopes")
        void sixteenRows() {
            assertThat(TransitiveResolutionDemo.scopeMediationTable()).hasSize(16);
        }

        @Test
        @DisplayName("compile + compile → compile")
        void compileCompile() {
            assertThat(TransitiveResolutionDemo.scopeMediationTable())
                    .contains(new ScopeMediation("compile", "compile", "compile"));
        }

        @Test
        @DisplayName("compile + runtime → runtime")
        void compileRuntime() {
            assertThat(TransitiveResolutionDemo.scopeMediationTable())
                    .contains(new ScopeMediation("compile", "runtime", "runtime"));
        }

        @Test
        @DisplayName("compile + test → dropped (null)")
        void compileTest() {
            assertThat(TransitiveResolutionDemo.scopeMediationTable())
                    .contains(new ScopeMediation("compile", "test", null));
        }

        @Test
        @DisplayName("provided + compile → provided")
        void providedCompile() {
            assertThat(TransitiveResolutionDemo.scopeMediationTable())
                    .contains(new ScopeMediation("provided", "compile", "provided"));
        }

        @Test
        @DisplayName("runtime + compile → runtime")
        void runtimeCompile() {
            assertThat(TransitiveResolutionDemo.scopeMediationTable())
                    .contains(new ScopeMediation("runtime", "compile", "runtime"));
        }

        @Test
        @DisplayName("test + compile → test")
        void testCompile() {
            assertThat(TransitiveResolutionDemo.scopeMediationTable())
                    .contains(new ScopeMediation("test", "compile", "test"));
        }

        @Test
        @DisplayName("a test or provided child is always dropped")
        void childrenNeverRescued() {
            assertThat(TransitiveResolutionDemo.scopeMediationTable())
                    .filteredOn(m -> m.childScope().equals("test")
                                  || m.childScope().equals("provided"))
                    .allSatisfy(m -> assertThat(m.effectiveScope()).isNull());
        }
    }

    // ── 4. snippets ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("4. exclusions and optional")
    class SnippetsTest {

        @Test
        @DisplayName("exclusion snippet uses <exclusions> and <exclusion>")
        void exclusion() {
            assertThat(TransitiveResolutionDemo.exclusionSnippet())
                    .contains("<exclusions>")
                    .contains("<exclusion>")
                    .contains("<artifactId>commons-logging</artifactId>");
        }

        @Test
        @DisplayName("optional snippet uses <optional>true</optional>")
        void optional() {
            assertThat(TransitiveResolutionDemo.optionalSnippet())
                    .contains("<optional>true</optional>");
        }

        @Test
        @DisplayName("Enforcer snippet uses <dependencyConvergence/>")
        void enforcer() {
            assertThat(TransitiveResolutionDemo.enforcerSnippet())
                    .contains("maven-enforcer-plugin")
                    .contains("<dependencyConvergence/>");
        }
    }

    // ── main() ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("main() runs without exceptions and prints every section")
    void mainPrintsAllSections() {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            TransitiveResolutionDemo.main(new String[0]);
        } finally {
            System.setOut(original);
        }

        assertThat(buffer.toString(StandardCharsets.UTF_8))
                .contains("=== transitive dependencies ===")
                .contains("=== nearest-wins ===")
                .contains("=== scope mediation ===")
                .contains("=== exclusions ===")
                .contains("=== dependency:tree ===")
                .contains("=== dependency:analyze + Enforcer ===");
    }
}