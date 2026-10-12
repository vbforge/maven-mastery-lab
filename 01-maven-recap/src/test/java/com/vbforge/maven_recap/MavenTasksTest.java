package com.vbforge.maven_recap;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MavenTasksTest — tests for every method in {@link MavenTasks}.
 *
 * <p><b>Getting started:</b>
 * <ol>
 *   <li>Implement all 6 methods in {@code MavenTasks.java}.</li>
 *   <li>Remove the {@code @Disabled} annotation below.</li>
 *   <li>Run {@code mvn test -Dtest=MavenTasksTest} — everything should go GREEN ✅</li>
 * </ol>
 */
@Disabled("Remove @Disabled once all 6 methods in MavenTasks are implemented")
@DisplayName("MavenTasks — your implementations")
class MavenTasksTest {

    // ── Task 1: toCoordinateString ────────────────────────────────────────────

    @Nested
    @DisplayName("Task 1 — toCoordinateString()")
    class ToCoordinateStringTest {

        @Test
        @DisplayName("formats com.vbforge:java-core:1.0.0")
        void javaCore() {
            assertThat(MavenTasks.toCoordinateString("com.vbforge", "java-core", "1.0.0"))
                    .isEqualTo("com.vbforge:java-core:1.0.0");
        }

        @Test
        @DisplayName("formats junit-jupiter coordinate")
        void junit() {
            assertThat(MavenTasks.toCoordinateString("org.junit.jupiter", "junit-jupiter", "5.10.2"))
                    .isEqualTo("org.junit.jupiter:junit-jupiter:5.10.2");
        }

        @ParameterizedTest(name = "{0}:{1}:{2}")
        @CsvSource({
                "com.vbforge, maven-recap, 1.0.0",
                "org.assertj, assertj-core, 3.25.3",
                "org.apache.commons, commons-lang3, 3.14.0"
        })
        @DisplayName("multiple valid coordinates")
        void validCoordinates(String g, String a, String v) {
            assertThat(MavenTasks.toCoordinateString(g, a, v)).isEqualTo(g + ":" + a + ":" + v);
        }

        @Test
        @DisplayName("null groupId throws IllegalArgumentException")
        void nullGroupId() {
            assertThatThrownBy(() -> MavenTasks.toCoordinateString(null, "java-core", "1.0.0"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null artifactId throws IllegalArgumentException")
        void nullArtifactId() {
            assertThatThrownBy(() -> MavenTasks.toCoordinateString("com.vbforge", null, "1.0.0"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null version throws IllegalArgumentException")
        void nullVersion() {
            assertThatThrownBy(() -> MavenTasks.toCoordinateString("com.vbforge", "java-core", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("empty groupId throws IllegalArgumentException")
        void emptyGroupId() {
            assertThatThrownBy(() -> MavenTasks.toCoordinateString("", "java-core", "1.0.0"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("blank artifactId throws IllegalArgumentException")
        void blankArtifactId() {
            assertThatThrownBy(() -> MavenTasks.toCoordinateString("com.vbforge", "  ", "1.0.0"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("blank version throws IllegalArgumentException")
        void blankVersion() {
            assertThatThrownBy(() -> MavenTasks.toCoordinateString("com.vbforge", "java-core", " "))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── Task 2: isValidArtifactId ─────────────────────────────────────────────

    @Nested
    @DisplayName("Task 2 — isValidArtifactId()")
    class IsValidArtifactIdTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "java-core", "maven-recap", "maven_recap", "commons-lang3",
                "a", "a1", "spring-boot-starter-web", "my.lib", "java_core2"
        })
        @DisplayName("valid kebab-case / snake_case artifactIds")
        void valid(String id) {
            assertThat(MavenTasks.isValidArtifactId(id)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Java-Core", "JAVA", "javaCore", "java core", "java-core!", "java/core",
                "1-java", "-java", "", " ", "_java", ".java"
        })
        @DisplayName("invalid artifactIds")
        void invalid(String id) {
            assertThat(MavenTasks.isValidArtifactId(id)).isFalse();
        }

        @Test
        @DisplayName("null is invalid")
        void nullIsInvalid() {
            assertThat(MavenTasks.isValidArtifactId(null)).isFalse();
        }
    }

    // ── Task 3: isTransitiveScope ─────────────────────────────────────────────

    @Nested
    @DisplayName("Task 3 — isTransitiveScope()")
    class IsTransitiveScopeTest {

        @ParameterizedTest
        @ValueSource(strings = {"compile", "runtime"})
        @DisplayName("transitive scopes")
        void transitive(String scope) {
            assertThat(MavenTasks.isTransitiveScope(scope)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"provided", "test", "system"})
        @DisplayName("non-transitive scopes")
        void nonTransitive(String scope) {
            assertThat(MavenTasks.isTransitiveScope(scope)).isFalse();
        }

        @Test
        @DisplayName("unknown scope → false")
        void unknownScope() {
            assertThat(MavenTasks.isTransitiveScope("bogus")).isFalse();
        }

        @Test
        @DisplayName("null → false")
        void nullScope() {
            assertThat(MavenTasks.isTransitiveScope(null)).isFalse();
        }

        @Test
        @DisplayName("blank → false")
        void blankScope() {
            assertThat(MavenTasks.isTransitiveScope("")).isFalse();
            assertThat(MavenTasks.isTransitiveScope("  ")).isFalse();
        }
    }

    // ── Task 4: nextPhase ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 4 — nextPhase()")
    class NextPhaseTest {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({
                "validate, compile",
                "compile,  test",
                "test,     package",
                "package,  verify",
                "verify,   install",
                "install,  deploy"
        })
        @DisplayName("each phase advances to the next")
        void happyPath(String current, String next) {
            assertThat(MavenTasks.nextPhase(current)).isEqualTo(next);
        }

        @Test
        @DisplayName("deploy is the last phase → null")
        void deployIsLast() {
            assertThat(MavenTasks.nextPhase("deploy")).isNull();
        }

        @Test
        @DisplayName("unknown phase → null")
        void unknownPhase() {
            assertThat(MavenTasks.nextPhase("bogus")).isNull();
        }

        @Test
        @DisplayName("empty string → null")
        void emptyPhase() {
            assertThat(MavenTasks.nextPhase("")).isNull();
        }

        @Test
        @DisplayName("null → null")
        void nullPhase() {
            assertThat(MavenTasks.nextPhase(null)).isNull();
        }
    }

    // ── Task 5: defaultLifecycle ──────────────────────────────────────────────

    @Nested
    @DisplayName("Task 5 — defaultLifecycle()")
    class DefaultLifecycleTest {

        @Test
        @DisplayName("returns 7 phases in exact order")
        void phases() {
            List<String> expected = List.of(
                    "validate", "compile", "test", "package", "verify", "install", "deploy");
            assertThat(MavenTasks.defaultLifecycle()).containsExactlyElementsOf(expected);
        }

        @Test
        @DisplayName("first phase is validate")
        void firstPhase() {
            assertThat(MavenTasks.defaultLifecycle().get(0)).isEqualTo("validate");
        }

        @Test
        @DisplayName("last phase is deploy")
        void lastPhase() {
            List<String> phases = MavenTasks.defaultLifecycle();
            assertThat(phases.get(phases.size() - 1)).isEqualTo("deploy");
        }

        @Test
        @DisplayName("install appears before deploy")
        void installBeforeDeploy() {
            List<String> phases = MavenTasks.defaultLifecycle();
            assertThat(phases.indexOf("install")).isLessThan(phases.indexOf("deploy"));
        }
    }

    // ── Task 6: distributionUrl ───────────────────────────────────────────────

    @Nested
    @DisplayName("Task 6 — distributionUrl()")
    class DistributionUrlTest {

        @Test
        @DisplayName("3.9.6 produces the expected URL")
        void threeNineSix() {
            assertThat(MavenTasks.distributionUrl("3.9.6"))
                    .isEqualTo("https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip");
        }

        @Test
        @DisplayName("3.8.8 produces the expected URL")
        void threeEightEight() {
            assertThat(MavenTasks.distributionUrl("3.8.8"))
                    .isEqualTo("https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.8/apache-maven-3.8.8-bin.zip");
        }

        @Test
        @DisplayName("URL starts with https and ends with -bin.zip")
        void shape() {
            String url = MavenTasks.distributionUrl("3.9.6");
            assertThat(url).startsWith("https://");
            assertThat(url).endsWith("-bin.zip");
        }

        @Test
        @DisplayName("null version → null")
        void nullVersion() {
            assertThat(MavenTasks.distributionUrl(null)).isNull();
        }

        @Test
        @DisplayName("blank version → null")
        void blankVersion() {
            assertThat(MavenTasks.distributionUrl("")).isNull();
            assertThat(MavenTasks.distributionUrl("   ")).isNull();
        }
    }
}