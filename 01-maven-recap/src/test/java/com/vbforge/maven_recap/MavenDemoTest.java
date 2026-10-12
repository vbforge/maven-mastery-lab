package com.vbforge.maven_recap;

import com.vbforge.maven_recap.MavenDemo.Coordinates;
import com.vbforge.maven_recap.MavenDemo.Phase;
import com.vbforge.maven_recap.MavenDemo.Scope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MavenDemoTest — verifies every concept shown in {@link MavenDemo}.
 *
 * <p>These tests are always GREEN. Read them as an executable specification
 * of Maven's core concepts.
 */
@DisplayName("MavenDemo — API contracts")
class MavenDemoTest {

    // ── 1. pom.xml ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("1. pom.xml model")
    class PomXmlTest {

        @Test
        @DisplayName("modelVersion is always 4.0.0")
        void modelVersion() {
            assertThat(MavenDemo.MODEL_VERSION).isEqualTo("4.0.0");
            assertThat(MavenDemo.samplePom())
                    .contains("<modelVersion>4.0.0</modelVersion>");
        }

        @Test
        @DisplayName("sample pom declares all four coordinates")
        void coordinatesPresent() {
            assertThat(MavenDemo.samplePom())
                    .contains("<groupId>com.vbforge</groupId>")
                    .contains("<artifactId>java-core</artifactId>")
                    .contains("<version>1.0.0</version>")
                    .contains("<packaging>pom</packaging>");
        }
    }

    // ── 2. Coordinates ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("2. Coordinates")
    class CoordinatesTest {

        @Test
        @DisplayName("groupId:artifactId:version is split into its parts")
        void parse() {
            Coordinates c = Coordinates.parse("com.vbforge:java-core:1.0.0");

            assertThat(c.groupId()).isEqualTo("com.vbforge");
            assertThat(c.artifactId()).isEqualTo("java-core");
            assertThat(c.version()).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("packaging defaults to jar")
        void defaultPackaging() {
            assertThat(Coordinates.parse("a:b:1").packaging()).isEqualTo("jar");
        }

        @Test
        @DisplayName("a string without exactly three parts is rejected")
        void malformed() {
            assertThatThrownBy(() -> Coordinates.parse("com.vbforge:java-core"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Coordinates.parse("a:b:c:d"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null is rejected")
        void nullInput() {
            assertThatThrownBy(() -> Coordinates.parse(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── 3. Dependency scopes ──────────────────────────────────────────────────

    @Nested
    @DisplayName("3. Dependency scopes")
    class DependencyScopesTest {

        @Test
        @DisplayName("there are exactly 5 standard scopes")
        void fiveScopes() {
            assertThat(Arrays.stream(Scope.values()).map(Scope::id))
                    .containsExactly("compile", "provided", "runtime", "test", "system");
        }

        @Test
        @DisplayName("only compile and runtime are transitive")
        void transitiveScopes() {
            assertThat(Arrays.stream(Scope.values()).filter(Scope::transitive).toList())
                    .containsExactly(Scope.COMPILE, Scope.RUNTIME);
        }

        @Test
        @DisplayName("test scope is on the test classpath only")
        void testScope() {
            assertThat(Scope.TEST.onTestClasspath()).isTrue();
            assertThat(Scope.TEST.onCompileClasspath()).isFalse();
            assertThat(Scope.TEST.onRuntimeClasspath()).isFalse();
        }

        @Test
        @DisplayName("provided is needed to compile but not shipped at runtime")
        void providedScope() {
            assertThat(Scope.PROVIDED.onCompileClasspath()).isTrue();
            assertThat(Scope.PROVIDED.onRuntimeClasspath()).isFalse();
        }

        @Test
        @DisplayName("runtime is not on the compile classpath")
        void runtimeScope() {
            assertThat(Scope.RUNTIME.onCompileClasspath()).isFalse();
            assertThat(Scope.RUNTIME.onRuntimeClasspath()).isTrue();
        }

        @Test
        @DisplayName("compile (default) is available everywhere")
        void compileScope() {
            assertThat(Scope.COMPILE.onCompileClasspath()).isTrue();
            assertThat(Scope.COMPILE.onTestClasspath()).isTrue();
            assertThat(Scope.COMPILE.onRuntimeClasspath()).isTrue();
        }
    }

    // ── 4. Lifecycle ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("4. Lifecycle")
    class LifecycleTest {

        @Test
        @DisplayName("default lifecycle has 7 main phases in order")
        void phaseOrder() {
            assertThat(Arrays.stream(Phase.values()).map(Phase::id))
                    .containsExactly("validate", "compile", "test", "package",
                            "verify", "install", "deploy");
        }

        @Test
        @DisplayName("mvn install also runs everything upstream")
        void installRunsUpstream() {
            assertThat(MavenDemo.phasesRunBy(Phase.INSTALL))
                    .containsExactly(Phase.VALIDATE, Phase.COMPILE, Phase.TEST,
                            Phase.PACKAGE, Phase.VERIFY, Phase.INSTALL);
        }

        @Test
        @DisplayName("mvn package does NOT run install")
        void packageStopsEarly() {
            List<Phase> run = MavenDemo.phasesRunBy(Phase.PACKAGE);
            assertThat(run).endsWith(Phase.PACKAGE).doesNotContain(Phase.INSTALL);
        }

        @Test
        @DisplayName("the first phase runs only itself; the last runs all seven")
        void edges() {
            assertThat(MavenDemo.phasesRunBy(Phase.VALIDATE)).containsExactly(Phase.VALIDATE);
            assertThat(MavenDemo.phasesRunBy(Phase.DEPLOY)).hasSize(7);
        }
    }

    // ── 5. Plugins ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("5. Plugins")
    class PluginsTest {

        private final Map<String, String> bindings = MavenDemo.defaultBindings();

        @Test
        @DisplayName("test phase binds to maven-surefire-plugin:test")
        void surefireBindsToTest() {
            assertThat(bindings).containsEntry("test", "maven-surefire-plugin:test");
        }

        @Test
        @DisplayName("package phase binds to maven-jar-plugin:jar")
        void jarBindsToPackage() {
            assertThat(bindings).containsEntry("package", "maven-jar-plugin:jar");
        }

        @Test
        @DisplayName("bindings are listed in lifecycle order")
        void order() {
            assertThat(bindings.keySet())
                    .containsExactly("compile", "test", "package", "install", "deploy");
        }
    }

    // ── 6. Wrapper ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("6. Wrapper")
    class WrapperTest {

        @Test
        @DisplayName("distributionUrl points to Maven Central and a -bin.zip")
        void distributionUrl() {
            assertThat(MavenDemo.WRAPPER_PROPERTIES)
                    .startsWith("distributionUrl=https://repo.maven.apache.org/")
                    .contains("-bin.zip");
        }

        @Test
        @DisplayName("the version appears twice in the URL")
        void versionAppearsTwice() {
            String props = MavenDemo.WRAPPER_PROPERTIES;
            assertThat(props.split("3\\.9\\.6", -1)).hasSize(3);
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
            MavenDemo.main(new String[0]);
        } finally {
            System.setOut(original);
        }

        assertThat(buffer.toString(StandardCharsets.UTF_8))
                .contains("=== pom.xml ===")
                .contains("=== Coordinates ===")
                .contains("=== Dependency scopes ===")
                .contains("=== Lifecycle ===")
                .contains("=== Plugins ===")
                .contains("=== Wrapper ===");
    }
}