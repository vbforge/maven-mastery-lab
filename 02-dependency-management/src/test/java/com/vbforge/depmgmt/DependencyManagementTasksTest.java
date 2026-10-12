package com.vbforge.depmgmt;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DependencyManagementTasksTest — tests for every method in
 * {@link DependencyManagementTasks}.
 *
 * <p><b>Getting started:</b>
 * <ol>
 *   <li>Implement all 5 methods in {@code DependencyManagementTasks.java}.</li>
 *   <li>Remove the {@code @Disabled} annotation below.</li>
 *   <li>Run {@code ./mvnw test -pl 02-dependency-management -Dtest=DependencyManagementTasksTest} ✅</li>
 * </ol>
 */
@Disabled("Remove @Disabled once all 5 methods in DependencyManagementTasks are implemented")
@DisplayName("DependencyManagementTasks — your implementations")
class DependencyManagementTasksTest {

    private static String lines(String... lines) {
        return String.join("\n", lines);
    }

    // ── Task 1: effectiveVersion ──────────────────────────────────────────────

    @Nested
    @DisplayName("Task 1 — effectiveVersion()")
    class EffectiveVersionTest {

        @Test
        @DisplayName("explicit version wins over managed")
        void explicitWins() {
            assertThat(DependencyManagementTasks.effectiveVersion("1.0.0", "2.0.0"))
                    .isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("no explicit version → managed version")
        void managedFallback() {
            assertThat(DependencyManagementTasks.effectiveVersion(null, "2.0.0"))
                    .isEqualTo("2.0.0");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   "})
        @DisplayName("blank explicit version counts as missing")
        void blankExplicit(String blank) {
            assertThat(DependencyManagementTasks.effectiveVersion(blank, "2.0.0"))
                    .isEqualTo("2.0.0");
        }

        @Test
        @DisplayName("explicit version without managed one is used")
        void explicitOnly() {
            assertThat(DependencyManagementTasks.effectiveVersion("1.0.0", null))
                    .isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("both missing → IllegalStateException")
        void bothMissing() {
            assertThatThrownBy(() -> DependencyManagementTasks.effectiveVersion(null, null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("both blank → IllegalStateException")
        void bothBlank() {
            assertThatThrownBy(() -> DependencyManagementTasks.effectiveVersion(" ", ""))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ── Task 2: mergeManagement ───────────────────────────────────────────────

    @Nested
    @DisplayName("Task 2 — mergeManagement()")
    class MergeManagementTest {

        @Test
        @DisplayName("entries from both maps appear; child wins on conflict")
        void childWins() {
            Map<String, String> parent = Map.of("a:x", "1", "a:y", "2");
            Map<String, String> child = Map.of("a:y", "9", "a:z", "3");

            assertThat(DependencyManagementTasks.mergeManagement(parent, child))
                    .containsExactlyInAnyOrderEntriesOf(Map.of("a:x", "1", "a:y", "9", "a:z", "3"));
        }

        @Test
        @DisplayName("empty child → parent entries")
        void emptyChild() {
            Map<String, String> parent = Map.of("a:x", "1");

            assertThat(DependencyManagementTasks.mergeManagement(parent, Map.of()))
                    .containsExactlyInAnyOrderEntriesOf(parent);
        }

        @Test
        @DisplayName("null maps are treated as empty")
        void nullMaps() {
            assertThat(DependencyManagementTasks.mergeManagement(null, null)).isEmpty();
            assertThat(DependencyManagementTasks.mergeManagement(null, Map.of("a:x", "1")))
                    .containsExactlyInAnyOrderEntriesOf(Map.of("a:x", "1"));
            assertThat(DependencyManagementTasks.mergeManagement(Map.of("a:x", "1"), null))
                    .containsExactlyInAnyOrderEntriesOf(Map.of("a:x", "1"));
        }

        @Test
        @DisplayName("the arguments are not modified")
        void argumentsUntouched() {
            Map<String, String> parent = new HashMap<>(Map.of("a:x", "1"));
            Map<String, String> child = new HashMap<>(Map.of("a:x", "9", "a:y", "2"));

            DependencyManagementTasks.mergeManagement(parent, child);

            assertThat(parent).containsExactlyInAnyOrderEntriesOf(Map.of("a:x", "1"));
            assertThat(child).containsExactlyInAnyOrderEntriesOf(Map.of("a:x", "9", "a:y", "2"));
        }
    }

    // ── Task 3: isBomImport ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 3 — isBomImport()")
    class IsBomImportTest {

        @Test
        @DisplayName("pom + import → true")
        void bom() {
            assertThat(DependencyManagementTasks.isBomImport("pom", "import")).isTrue();
        }

        @ParameterizedTest(name = "type={0}, scope={1} → false")
        @CsvSource({
                "jar, import",
                "pom, compile",
                "pom, test",
                "jar, compile",
                "POM, import",
                "pom, IMPORT"
        })
        @DisplayName("anything else → false")
        void notBom(String type, String scope) {
            assertThat(DependencyManagementTasks.isBomImport(type, scope)).isFalse();
        }

        @Test
        @DisplayName("null type or scope → false")
        void nulls() {
            assertThat(DependencyManagementTasks.isBomImport(null, "import")).isFalse();
            assertThat(DependencyManagementTasks.isBomImport("pom", null)).isFalse();
            assertThat(DependencyManagementTasks.isBomImport(null, null)).isFalse();
        }
    }

    // ── Task 4: renderDependency ──────────────────────────────────────────────

    @Nested
    @DisplayName("Task 4 — renderDependency()")
    class RenderDependencyTest {

        @Test
        @DisplayName("full declaration: group, artifact, version, scope")
        void full() {
            assertThat(DependencyManagementTasks.renderDependency(
                    "org.assertj", "assertj-core", "3.25.3", "test"))
                    .isEqualTo(lines(
                            "<dependency>",
                            "    <groupId>org.assertj</groupId>",
                            "    <artifactId>assertj-core</artifactId>",
                            "    <version>3.25.3</version>",
                            "    <scope>test</scope>",
                            "</dependency>"));
        }

        @Test
        @DisplayName("managed version → no <version> line")
        void withoutVersion() {
            assertThat(DependencyManagementTasks.renderDependency(
                    "org.assertj", "assertj-core", null, "test"))
                    .isEqualTo(lines(
                            "<dependency>",
                            "    <groupId>org.assertj</groupId>",
                            "    <artifactId>assertj-core</artifactId>",
                            "    <scope>test</scope>",
                            "</dependency>"));
        }

        @Test
        @DisplayName("no version and no scope → only the coordinates")
        void minimal() {
            assertThat(DependencyManagementTasks.renderDependency(
                    "org.assertj", "assertj-core", null, null))
                    .isEqualTo(lines(
                            "<dependency>",
                            "    <groupId>org.assertj</groupId>",
                            "    <artifactId>assertj-core</artifactId>",
                            "</dependency>"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"compile", "", "  "})
        @DisplayName("default or blank scope is omitted")
        void defaultScopeOmitted(String scope) {
            assertThat(DependencyManagementTasks.renderDependency(
                    "org.slf4j", "slf4j-api", "2.0.9", scope))
                    .isEqualTo(lines(
                            "<dependency>",
                            "    <groupId>org.slf4j</groupId>",
                            "    <artifactId>slf4j-api</artifactId>",
                            "    <version>2.0.9</version>",
                            "</dependency>"));
        }

        @Test
        @DisplayName("blank version is omitted")
        void blankVersion() {
            assertThat(DependencyManagementTasks.renderDependency(
                    "org.slf4j", "slf4j-api", "  ", "runtime"))
                    .isEqualTo(lines(
                            "<dependency>",
                            "    <groupId>org.slf4j</groupId>",
                            "    <artifactId>slf4j-api</artifactId>",
                            "    <scope>runtime</scope>",
                            "</dependency>"));
        }

        @Test
        @DisplayName("null groupId throws IllegalArgumentException")
        void nullGroupId() {
            assertThatThrownBy(() -> DependencyManagementTasks.renderDependency(
                    null, "assertj-core", "3.25.3", "test"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("blank artifactId throws IllegalArgumentException")
        void blankArtifactId() {
            assertThatThrownBy(() -> DependencyManagementTasks.renderDependency(
                    "org.assertj", "  ", "3.25.3", "test"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── Task 5: unmanagedDependencies ─────────────────────────────────────────

    @Nested
    @DisplayName("Task 5 — unmanagedDependencies()")
    class UnmanagedDependenciesTest {

        @Test
        @DisplayName("a dependency with no version and no management is reported")
        void reported() {
            List<String> declared = List.of("org.assertj:assertj-core", "org.slf4j:slf4j-api");
            Map<String, String> managed = Map.of("org.assertj:assertj-core", "3.25.3");

            assertThat(DependencyManagementTasks.unmanagedDependencies(declared, managed))
                    .containsExactly("org.slf4j:slf4j-api");
        }

        @Test
        @DisplayName("an explicit version makes it fine even without management")
        void explicitVersion() {
            assertThat(DependencyManagementTasks.unmanagedDependencies(
                    List.of("org.slf4j:slf4j-api:2.0.9"), Map.of()))
                    .isEmpty();
        }

        @Test
        @DisplayName("everything managed → nothing reported")
        void allManaged() {
            assertThat(DependencyManagementTasks.unmanagedDependencies(
                    List.of("a:x", "a:y"), Map.of("a:x", "1", "a:y", "2")))
                    .isEmpty();
        }

        @Test
        @DisplayName("a blank managed version does not count as managed")
        void blankManaged() {
            assertThat(DependencyManagementTasks.unmanagedDependencies(
                    List.of("a:x"), Map.of("a:x", " ")))
                    .containsExactly("a:x");
        }

        @Test
        @DisplayName("declaration order is preserved")
        void orderPreserved() {
            assertThat(DependencyManagementTasks.unmanagedDependencies(
                    List.of("c:z", "a:x", "b:y"), Map.of("a:x", "1")))
                    .containsExactly("c:z", "b:y");
        }

        @Test
        @DisplayName("null list or map is treated as empty")
        void nulls() {
            assertThat(DependencyManagementTasks.unmanagedDependencies(null, null)).isEmpty();
            assertThat(DependencyManagementTasks.unmanagedDependencies(null, Map.of("a:x", "1"))).isEmpty();
            assertThat(DependencyManagementTasks.unmanagedDependencies(List.of("a:x"), null))
                    .containsExactly("a:x");
        }
    }
}