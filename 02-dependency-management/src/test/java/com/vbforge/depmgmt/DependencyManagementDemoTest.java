package com.vbforge.depmgmt;

import com.vbforge.depmgmt.DependencyManagementDemo.DeclarationBlock;
import com.vbforge.depmgmt.DependencyManagementDemo.VersionSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DependencyManagementDemoTest — verifies every concept shown in
 * {@link DependencyManagementDemo}.
 *
 * <p>These tests are always GREEN. Read them as an executable specification.
 */
@DisplayName("DependencyManagementDemo — API contracts")
class DependencyManagementDemoTest {

    // ── 1. dependencies vs dependencyManagement ───────────────────────────────

    @Nested
    @DisplayName("1. dependencies vs dependencyManagement")
    class DeclarationBlockTest {

        @Test
        @DisplayName("dependencies adds to the classpath")
        void dependenciesAddsToClasspath() {
            assertThat(DeclarationBlock.DEPENDENCIES.addsToClasspath()).isTrue();
        }

        @Test
        @DisplayName("dependencyManagement adds nothing to the classpath")
        void managementAddsNothing() {
            assertThat(DeclarationBlock.DEPENDENCY_MANAGEMENT.addsToClasspath()).isFalse();
        }

        @Test
        @DisplayName("each block knows its XML tag")
        void tags() {
            assertThat(DeclarationBlock.DEPENDENCIES.tag()).isEqualTo("<dependencies>");
            assertThat(DeclarationBlock.DEPENDENCY_MANAGEMENT.tag()).isEqualTo("<dependencyManagement>");
        }
    }

    // ── 2. Parent POM inheritance ─────────────────────────────────────────────

    @Nested
    @DisplayName("2. Parent inheritance")
    class ParentInheritanceTest {

        @Test
        @DisplayName("the parent is a pom-packaged project with a dependencyManagement block")
        void parentShape() {
            assertThat(DependencyManagementDemo.parentPom())
                    .contains("<packaging>pom</packaging>")
                    .contains("<dependencyManagement>");
        }

        @Test
        @DisplayName("the child points at the parent")
        void childHasParent() {
            assertThat(DependencyManagementDemo.childPom())
                    .contains("<parent>")
                    .contains("<artifactId>maven-mastery-lab</artifactId>");
        }

        @Test
        @DisplayName("the child's dependency has neither a version nor a scope")
        void childDependencyIsBare() {
            String child = DependencyManagementDemo.childPom();
            String dependencies = child.substring(child.indexOf("<dependencies>"));

            assertThat(dependencies)
                    .contains("<artifactId>assertj-core</artifactId>")
                    .doesNotContain("<version>")
                    .doesNotContain("<scope>");
        }

        @Test
        @DisplayName("the parent supplies both version and scope")
        void parentSuppliesVersionAndScope() {
            assertThat(DependencyManagementDemo.parentPom())
                    .contains("<version>${assertj.version}</version>")
                    .contains("<scope>test</scope>");
        }
    }

    // ── 3. BOM import ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("3. BOM import")
    class BomImportTest {

        @Test
        @DisplayName("a BOM import uses type pom and scope import")
        void bomImport() {
            assertThat(DependencyManagementDemo.bomImportSnippet())
                    .contains("<type>pom</type>")
                    .contains("<scope>import</scope>");
        }

        @Test
        @DisplayName("there are 4 version sources; explicit is strongest, imported BOM is weakest")
        void precedence() {
            assertThat(DependencyManagementDemo.versionPrecedence())
                    .hasSize(4)
                    .first().isEqualTo(VersionSource.EXPLICIT_IN_DEPENDENCY);
            assertThat(DependencyManagementDemo.versionPrecedence())
                    .last().isEqualTo(VersionSource.IMPORTED_BOM);
        }

        @Test
        @DisplayName("the own dependencyManagement beats the parent's")
        void ownBeatsParent() {
            assertThat(DependencyManagementDemo.versionPrecedence())
                    .containsSubsequence(
                            VersionSource.OWN_DEPENDENCY_MANAGEMENT,
                            VersionSource.PARENT_DEPENDENCY_MANAGEMENT);
        }
    }

    // ── 4. pluginManagement ───────────────────────────────────────────────────

    @Nested
    @DisplayName("4. pluginManagement")
    class PluginManagementTest {

        @Test
        @DisplayName("the parent pins the plugin version inside pluginManagement")
        void parentPins() {
            assertThat(DependencyManagementDemo.pluginManagementParentSnippet())
                    .contains("<pluginManagement>")
                    .contains("<version>3.13.0</version>");
        }

        @Test
        @DisplayName("the child declares the plugin without a version")
        void childIsBare() {
            assertThat(DependencyManagementDemo.pluginManagementChildSnippet())
                    .contains("<artifactId>maven-compiler-plugin</artifactId>")
                    .doesNotContain("<version>")
                    .doesNotContain("<pluginManagement>");
        }
    }

    // ── 5. Version properties ─────────────────────────────────────────────────

    @Nested
    @DisplayName("5. Version properties")
    class VersionPropertiesTest {

        @Test
        @DisplayName("a child can override the parent's version property")
        void override() {
            assertThat(DependencyManagementDemo.versionOverrideSnippet())
                    .contains("<assertj.version>3.24.2</assertj.version>");
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
            DependencyManagementDemo.main(new String[0]);
        } finally {
            System.setOut(original);
        }

        assertThat(buffer.toString(StandardCharsets.UTF_8))
                .contains("=== dependencies vs dependencyManagement ===")
                .contains("=== Parent inheritance ===")
                .contains("=== BOM import ===")
                .contains("=== pluginManagement ===")
                .contains("=== Version properties ===")
                .contains("=== effective-pom ===");
    }
}