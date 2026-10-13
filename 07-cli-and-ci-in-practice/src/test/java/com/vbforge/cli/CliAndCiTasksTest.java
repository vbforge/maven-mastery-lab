package com.vbforge.cli;

import com.vbforge.cli.CliAndCiDemo.TestKind;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CliAndCiTasksTest — tests for every method in {@link CliAndCiTasks}.
 *
 * <p><b>Getting started:</b>
 * <ol>
 *   <li>Implement all 5 methods in {@code CliAndCiTasks.java}.</li>
 *   <li>Remove the {@code @Disabled} annotation below.</li>
 *   <li>Run {@code ./mvnw test -pl 07-cli-and-ci-in-practice -Dtest=CliAndCiTasksTest} ✅</li>
 * </ol>
 */
@Disabled("Remove @Disabled once all 5 methods in CliAndCiTasks are implemented")
@DisplayName("CliAndCiTasks — your implementations")
class CliAndCiTasksTest {

    // ── Task 1: buildCommand ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 1 — buildCommand()")
    class BuildCommandTest {

        @Test
        @DisplayName("no options: just goals")
        void noOptions() {
            assertThat(CliAndCiTasks.buildCommand(List.of("clean", "verify"), List.of()))
                    .isEqualTo("mvn clean verify");
        }

        @Test
        @DisplayName("boolean flags come before goals")
        void booleanFlags() {
            assertThat(CliAndCiTasks.buildCommand(List.of("clean", "verify"),
                            List.of(CliOption.flag("-B"), CliOption.flag("-ntp"))))
                    .isEqualTo("mvn -B -ntp clean verify");
        }

        @Test
        @DisplayName("-D flags with values use the = form")
        void dFlags() {
            assertThat(CliAndCiTasks.buildCommand(List.of("test"),
                            List.of(CliOption.flag("-DskipTests"),
                                    CliOption.of("-Dmaven.test.skip", "true"))))
                    .isEqualTo("mvn -DskipTests -Dmaven.test.skip=true test");
        }

        @Test
        @DisplayName("non -D flags with values use the space form")
        void spacedValues() {
            assertThat(CliAndCiTasks.buildCommand(List.of("verify"),
                            List.of(CliOption.of("-T", "1C"),
                                    CliOption.of("-pl", "module-a"),
                                    CliOption.flag("-am"))))
                    .isEqualTo("mvn -T 1C -pl module-a -am verify");
        }

        @Test
        @DisplayName("null options list is treated as empty")
        void nullOptions() {
            assertThat(CliAndCiTasks.buildCommand(List.of("clean", "verify"), null))
                    .isEqualTo("mvn clean verify");
        }

        @Test
        @DisplayName("single goal")
        void singleGoal() {
            assertThat(CliAndCiTasks.buildCommand(List.of("validate"), List.of()))
                    .isEqualTo("mvn validate");
        }

        @Test
        @DisplayName("null or empty goals throws IllegalArgumentException")
        void invalidGoals() {
            assertThatThrownBy(() -> CliAndCiTasks.buildCommand(null, List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> CliAndCiTasks.buildCommand(List.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("blank goal throws IllegalArgumentException")
        void blankGoal() {
            assertThatThrownBy(() -> CliAndCiTasks.buildCommand(List.of("  "), List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> CliAndCiTasks.buildCommand(List.of("clean", ""), List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null option element throws IllegalArgumentException")
        void nullOptionElement() {
            assertThatThrownBy(() -> CliAndCiTasks.buildCommand(
                    List.of("verify"), java.util.Arrays.asList(CliOption.flag("-B"), null)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── Task 2: phasesRun ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 2 — phasesRun()")
    class PhasesRunTest {

        @Test
        @DisplayName("clean verify → up to verify")
        void cleanVerify() {
            assertThat(CliAndCiTasks.phasesRun("clean verify"))
                    .containsExactly("validate", "compile", "test", "package", "verify");
        }

        @Test
        @DisplayName("leading mvn / ./mvnw token is ignored")
        void leadingToken() {
            assertThat(CliAndCiTasks.phasesRun("mvn clean verify"))
                    .containsExactly("validate", "compile", "test", "package", "verify");
            assertThat(CliAndCiTasks.phasesRun("./mvnw clean install"))
                    .containsExactly("validate", "compile", "test", "package", "verify", "install");
        }

        @Test
        @DisplayName("package stops before verify")
        void packageStops() {
            assertThat(CliAndCiTasks.phasesRun("package"))
                    .containsExactly("validate", "compile", "test", "package");
        }

        @Test
        @DisplayName("validate → only itself")
        void validateOnly() {
            assertThat(CliAndCiTasks.phasesRun("validate"))
                    .containsExactly("validate");
        }

        @Test
        @DisplayName("deploy → full lifecycle")
        void deployFull() {
            assertThat(CliAndCiTasks.phasesRun("clean deploy"))
                    .containsExactly("validate", "compile", "test", "package",
                            "verify", "install", "deploy");
        }

        @Test
        @DisplayName("flags after the phase are ignored")
        void trailingFlags() {
            assertThat(CliAndCiTasks.phasesRun("./mvnw clean install -U -DskipTests"))
                    .containsExactly("validate", "compile", "test", "package",
                            "verify", "install");
        }

        @Test
        @DisplayName("later phase wins when several are listed")
        void laterPhaseWins() {
            assertThat(CliAndCiTasks.phasesRun("test verify"))
                    .containsExactly("validate", "compile", "test", "package", "verify");
        }

        @Test
        @DisplayName("clean alone → empty")
        void cleanOnly() {
            assertThat(CliAndCiTasks.phasesRun("clean")).isEmpty();
        }

        @Test
        @DisplayName("unknown or blank command → empty")
        void unknowns() {
            assertThat(CliAndCiTasks.phasesRun("bogus")).isEmpty();
            assertThat(CliAndCiTasks.phasesRun("")).isEmpty();
            assertThat(CliAndCiTasks.phasesRun("   ")).isEmpty();
            assertThat(CliAndCiTasks.phasesRun(null)).isEmpty();
        }
    }

    // ── Task 3: skipsCompilingTests ──────────────────────────────────────────

    @Nested
    @DisplayName("Task 3 — skipsCompilingTests()")
    class SkipsCompilingTestsTest {

        @Test
        @DisplayName("-Dmaven.test.skip=true → true")
        void fullSkip() {
            assertThat(CliAndCiTasks.skipsCompilingTests(Set.of("-Dmaven.test.skip=true")))
                    .isTrue();
        }

        @Test
        @DisplayName("bare -Dmaven.test.skip → true")
        void bareFlag() {
            assertThat(CliAndCiTasks.skipsCompilingTests(Set.of("-Dmaven.test.skip")))
                    .isTrue();
        }

        @Test
        @DisplayName("full skip alongside other flags")
        void withOthers() {
            assertThat(CliAndCiTasks.skipsCompilingTests(
                    Set.of("-B", "-ntp", "-Dmaven.test.skip=true")))
                    .isTrue();
        }

        @Test
        @DisplayName("-DskipTests → false (still compiles)")
        void skipTestsFalse() {
            assertThat(CliAndCiTasks.skipsCompilingTests(Set.of("-DskipTests")))
                    .isFalse();
        }

        @Test
        @DisplayName("-DskipITs → false")
        void skipITsFalse() {
            assertThat(CliAndCiTasks.skipsCompilingTests(Set.of("-DskipITs")))
                    .isFalse();
        }

        @Test
        @DisplayName("explicit false → false")
        void explicitFalse() {
            assertThat(CliAndCiTasks.skipsCompilingTests(Set.of("-Dmaven.test.skip=false")))
                    .isFalse();
        }

        @Test
        @DisplayName("case-sensitive: -DMAVEN.TEST.SKIP=true → false")
        void caseSensitive() {
            assertThat(CliAndCiTasks.skipsCompilingTests(Set.of("-DMAVEN.TEST.SKIP=true")))
                    .isFalse();
        }

        @Test
        @DisplayName("empty or null → false")
        void emptyOrNull() {
            assertThat(CliAndCiTasks.skipsCompilingTests(Set.of())).isFalse();
            assertThat(CliAndCiTasks.skipsCompilingTests(null)).isFalse();
        }
    }

    // ── Task 4: classifyTest ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 4 — classifyTest()")
    class ClassifyTestTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "GreeterTest",
                "GreeterTests",
                "TestGreeter",
                "GreeterTestCase",
                "com.foo.BarTest",
                "com.foo.bar.BazTests"
        })
        @DisplayName("surefire patterns")
        void surefire(String className) {
            assertThat(CliAndCiTasks.classifyTest(className)).isEqualTo(TestKind.SUREFIRE);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "GreeterIT",
                "ITGreeter",
                "GreeterITCase",
                "com.foo.BarIT",
                "com.foo.ITBar"
        })
        @DisplayName("failsafe patterns")
        void failsafe(String className) {
            assertThat(CliAndCiTasks.classifyTest(className)).isEqualTo(TestKind.FAILSAFE);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "Greeter",
                "GreeterHelper",
                "GreetingService",
                "Item",
                "com.foo.Greeter"
        })
        @DisplayName("neither pattern → NONE")
        void none(String className) {
            assertThat(CliAndCiTasks.classifyTest(className)).isEqualTo(TestKind.NONE);
        }

        @Test
        @DisplayName("TestIT matches surefire first (both plugins would run it in Maven)")
        void bothPatternsSurefireWins() {
            assertThat(CliAndCiTasks.classifyTest("TestIT")).isEqualTo(TestKind.SUREFIRE);
        }

        @Test
        @DisplayName("null or blank → NONE")
        void nulls() {
            assertThat(CliAndCiTasks.classifyTest(null)).isEqualTo(TestKind.NONE);
            assertThat(CliAndCiTasks.classifyTest("")).isEqualTo(TestKind.NONE);
            assertThat(CliAndCiTasks.classifyTest("   ")).isEqualTo(TestKind.NONE);
        }
    }

    // ── Task 5: ciCommand ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 5 — ciCommand()")
    class CiCommandTest {

        @Test
        @DisplayName("clean verify → ./mvnw -B -ntp clean verify")
        void cleanVerify() {
            assertThat(CliAndCiTasks.ciCommand(List.of("clean", "verify")))
                    .isEqualTo("./mvnw -B -ntp clean verify");
        }

        @Test
        @DisplayName("clean install")
        void cleanInstall() {
            assertThat(CliAndCiTasks.ciCommand(List.of("clean", "install")))
                    .isEqualTo("./mvnw -B -ntp clean install");
        }

        @Test
        @DisplayName("extra flags come after the goals")
        void extraFlags() {
            assertThat(CliAndCiTasks.ciCommand(List.of("clean", "verify", "-DskipTests")))
                    .isEqualTo("./mvnw -B -ntp clean verify -DskipTests");
        }

        @Test
        @DisplayName("multi-token flags survive intact")
        void multiTokenFlags() {
            assertThat(CliAndCiTasks.ciCommand(List.of("clean", "verify", "-T", "1C")))
                    .isEqualTo("./mvnw -B -ntp clean verify -T 1C");
        }

        @Test
        @DisplayName("starts with ./mvnw (wrapper), never plain mvn")
        void usesWrapper() {
            assertThat(CliAndCiTasks.ciCommand(List.of("verify")))
                    .startsWith("./mvnw")
                    .doesNotStartWith("mvn ");
        }

        @Test
        @DisplayName("always includes -B and -ntp")
        void requiredFlags() {
            String cmd = CliAndCiTasks.ciCommand(List.of("verify"));
            assertThat(cmd).contains(" -B ").contains(" -ntp ");
        }

        @Test
        @DisplayName("null or empty args throws IllegalArgumentException")
        void nullOrEmpty() {
            assertThatThrownBy(() -> CliAndCiTasks.ciCommand(null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> CliAndCiTasks.ciCommand(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("blank element throws IllegalArgumentException")
        void blankElement() {
            assertThatThrownBy(() -> CliAndCiTasks.ciCommand(List.of("clean", "  ")))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}