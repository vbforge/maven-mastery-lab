package com.vbforge.cli;

import com.vbforge.cli.CliAndCiDemo.SkipFlag;
import com.vbforge.cli.CliAndCiDemo.TestKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CliAndCiDemoTest — verifies every concept shown in {@link CliAndCiDemo}.
 *
 * <p>These tests are always GREEN. Read them as an executable specification.
 */
@DisplayName("CliAndCiDemo — API contracts")
class CliAndCiDemoTest {

    @Nested
    @DisplayName("1. verify vs install")
    class VerifyVsInstallTest {

        @Test
        @DisplayName("the snippet contrasts clean verify and clean install")
        void snippet() {
            assertThat(CliAndCiDemo.verifyVsInstallSnippet())
                    .contains("clean verify")
                    .contains("clean install");
        }
    }

    @Nested
    @DisplayName("2. Skip flags")
    class SkipFlagsTest {

        @Test
        @DisplayName("-DskipTests does not skip compilation")
        void skipTestsDoesNotCompile() {
            assertThat(CliAndCiDemo.skipFlagTable())
                    .filteredOn(r -> r.flag().equals("-DskipTests"))
                    .singleElement()
                    .satisfies(r -> {
                        assertThat(r.skipsCompiling()).isFalse();
                        assertThat(r.skipsRunning()).isTrue();
                    });
        }

        @Test
        @DisplayName("-Dmaven.test.skip=true skips both")
        void mavenTestSkipSkipsBoth() {
            assertThat(CliAndCiDemo.skipFlagTable())
                    .filteredOn(r -> r.flag().equals("-Dmaven.test.skip=true"))
                    .singleElement()
                    .satisfies(r -> {
                        assertThat(r.skipsCompiling()).isTrue();
                        assertThat(r.skipsRunning()).isTrue();
                    });
        }

        @Test
        @DisplayName("the table has four rows and a SkipFlag record shape")
        void table() {
            List<SkipFlag> table = CliAndCiDemo.skipFlagTable();
            assertThat(table).hasSize(4);
            assertThat(table).extracting(SkipFlag::flag)
                    .containsExactly(
                            "-DskipTests",
                            "-Dmaven.test.skip=true",
                            "-DskipITs",
                            "-DskipTests -DskipITs");
        }
    }

    @Nested
    @DisplayName("3. surefire vs failsafe")
    class TestNamingTest {

        @Test
        @DisplayName("TestKind has exactly three values")
        void kinds() {
            assertThat(TestKind.values()).containsExactly(
                    TestKind.SUREFIRE, TestKind.FAILSAFE, TestKind.NONE);
        }

        @Test
        @DisplayName("the naming table has surefire, failsafe, and none examples")
        void examples() {
            assertThat(CliAndCiDemo.testNamingExamples())
                    .extracting(row -> row[1])
                    .contains("surefire", "failsafe", "none");
        }
    }

    @Nested
    @DisplayName("4. CI flags")
    class CiFlagsTest {

        @Test
        @DisplayName("the flag table covers -B, -ntp, -q, -X, -U, -o")
        void flags() {
            assertThat(CliAndCiDemo.ciFlagTable())
                    .extracting(row -> row[0])
                    .containsExactly("-B", "-ntp", "-q", "-X", "-U", "-o");
        }
    }

    @Nested
    @DisplayName("5. Parallel builds")
    class ParallelTest {

        @Test
        @DisplayName("the snippet shows both -T 1C and -T 4")
        void snippet() {
            assertThat(CliAndCiDemo.parallelSnippet())
                    .contains("-T 1C")
                    .contains("-T 4");
        }
    }

    @Nested
    @DisplayName("6. Caching")
    class CachingTest {

        @Test
        @DisplayName("the caching snippet uses actions/setup-java cache=maven")
        void snippet() {
            assertThat(CliAndCiDemo.cachingSnippet())
                    .contains("setup-java")
                    .contains("cache: maven");
        }
    }

    @Test
    @DisplayName("main() runs without exceptions and prints every section")
    void mainPrintsAllSections() {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            CliAndCiDemo.main(new String[0]);
        } finally {
            System.setOut(original);
        }

        assertThat(buffer.toString(StandardCharsets.UTF_8))
                .contains("=== verify vs install ===")
                .contains("=== skip flags ===")
                .contains("=== surefire vs failsafe ===")
                .contains("=== CI flags ===")
                .contains("=== parallel builds ===")
                .contains("=== caching ~/.m2 in CI ===");
    }
}