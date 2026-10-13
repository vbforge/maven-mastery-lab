package com.vbforge.profiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PropertiesProfilesVersionsDemoTest — verifies every concept shown in
 * {@link PropertiesProfilesVersionsDemo}.
 *
 * <p>These tests are always GREEN. Read them as an executable specification.
 */
@DisplayName("PropertiesProfilesVersionsDemo — API contracts")
class PropertiesProfilesVersionsDemoTest {

    @Nested
    @DisplayName("1. Properties")
    class PropertiesTest {

        @Test
        @DisplayName("built-ins include the standard Maven properties")
        void builtins() {
            assertThat(PropertiesProfilesVersionsDemo.BUILTIN_PROPERTIES)
                    .containsKeys("project.groupId", "project.artifactId",
                            "project.version", "project.basedir",
                            "project.build.directory");
        }

        @Test
        @DisplayName("interpolate replaces known placeholders")
        void knownPlaceholders() {
            assertThat(PropertiesProfilesVersionsDemo.interpolate(
                    "app ${project.version} (${env})",
                    Map.of("project.version", "1.2.3", "env", "prod")))
                    .isEqualTo("app 1.2.3 (prod)");
        }

        @Test
        @DisplayName("interpolate leaves unknown placeholders untouched")
        void unknownPlaceholder() {
            assertThat(PropertiesProfilesVersionsDemo.interpolate(
                    "${known} ${missing}", Map.of("known", "hi")))
                    .isEqualTo("hi ${missing}");
        }
    }

    @Nested
    @DisplayName("2. Resource filtering")
    class FilteringTest {

        @Test
        @DisplayName("snippet enables filtering on src/main/resources")
        void snippet() {
            assertThat(PropertiesProfilesVersionsDemo.filteringSnippet())
                    .contains("<filtering>true</filtering>")
                    .contains("<directory>src/main/resources</directory>");
        }
    }

    @Nested
    @DisplayName("3. Profiles")
    class ProfilesTest {

        private final List<Profile> profiles = List.of(
                Profile.defaultActive("dev"),
                Profile.of("prod"),
                Profile.onJdk("jdk17", "17"),
                Profile.onProperty("metrics", "metrics.enabled", "true"));

        @Test
        @DisplayName("no -P → activeByDefault dev is active")
        void noCli() {
            assertThat(PropertiesProfilesVersionsDemo.activeProfiles(
                    profiles, ActivationEnv.of("17.0.2", "Linux")))
                    .contains("dev", "jdk17")
                    .doesNotContain("prod");
        }

        @Test
        @DisplayName("-Pprod disables activeByDefault dev but keeps jdk17")
        void cliDisablesDefault() {
            ActivationEnv env = new ActivationEnv(
                    "17.0.2", "Linux", Map.of(), Set.of("prod"));

            assertThat(PropertiesProfilesVersionsDemo.activeProfiles(profiles, env))
                    .contains("prod", "jdk17")
                    .doesNotContain("dev");
        }

        @Test
        @DisplayName("property activation via -D works without -P")
        void propertyActivates() {
            ActivationEnv env = new ActivationEnv(
                    "11.0.2", "Linux", Map.of("metrics.enabled", "true"), Set.of());

            assertThat(PropertiesProfilesVersionsDemo.activeProfiles(profiles, env))
                    .contains("dev", "metrics")
                    .doesNotContain("jdk17", "prod");
        }

        @Test
        @DisplayName("the profiles snippet has dev and prod")
        void snippet() {
            assertThat(PropertiesProfilesVersionsDemo.profilesSnippet())
                    .contains("<id>dev</id>")
                    .contains("<id>prod</id>")
                    .contains("<activeByDefault>true</activeByDefault>");
        }
    }

    @Nested
    @DisplayName("4. Versioning")
    class VersioningTest {

        @Test
        @DisplayName("1.10.0 > 1.9.0 numerically")
        void numericComparison() {
            assertThat(PropertiesProfilesVersionsDemo.compareVersions("1.10.0", "1.9.0")).isPositive();
            assertThat(PropertiesProfilesVersionsDemo.compareVersions("1.9.0", "1.10.0")).isNegative();
        }

        @Test
        @DisplayName("SNAPSHOT is less than the equivalent release")
        void snapshotLess() {
            assertThat(PropertiesProfilesVersionsDemo.compareVersions("1.0.0-SNAPSHOT", "1.0.0"))
                    .isNegative();
            assertThat(PropertiesProfilesVersionsDemo.compareVersions("1.0.0", "1.0.0-SNAPSHOT"))
                    .isPositive();
        }

        @Test
        @DisplayName("bump major / minor / patch")
        void bump() {
            assertThat(PropertiesProfilesVersionsDemo.bumpVersion(
                    "1.2.3", PropertiesProfilesVersionsDemo.BumpLevel.MAJOR)).isEqualTo("2.0.0");
            assertThat(PropertiesProfilesVersionsDemo.bumpVersion(
                    "1.2.3", PropertiesProfilesVersionsDemo.BumpLevel.MINOR)).isEqualTo("1.3.0");
            assertThat(PropertiesProfilesVersionsDemo.bumpVersion(
                    "1.2.3", PropertiesProfilesVersionsDemo.BumpLevel.PATCH)).isEqualTo("1.2.4");
        }

        @Test
        @DisplayName("release strips the SNAPSHOT suffix")
        void release() {
            assertThat(PropertiesProfilesVersionsDemo.releaseVersion("1.2.0-SNAPSHOT"))
                    .isEqualTo("1.2.0");
            assertThat(PropertiesProfilesVersionsDemo.releaseVersion("1.2.0"))
                    .isEqualTo("1.2.0");
        }
    }

    @Nested
    @DisplayName("5. CI-friendly versions")
    class CiFriendlyTest {

        @Test
        @DisplayName("snippet uses ${revision}")
        void snippet() {
            assertThat(PropertiesProfilesVersionsDemo.ciFriendlySnippet())
                    .contains("<version>${revision}</version>")
                    .contains("<revision>");
        }
    }

    @Test
    @DisplayName("main() runs without exceptions and prints every section")
    void mainPrintsAllSections() {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            PropertiesProfilesVersionsDemo.main(new String[0]);
        } finally {
            System.setOut(original);
        }

        assertThat(buffer.toString(StandardCharsets.UTF_8))
                .contains("=== properties ===")
                .contains("=== resource filtering ===")
                .contains("=== profiles ===")
                .contains("=== versioning ===")
                .contains("=== CI-friendly versions ===")
                .contains("=== versions: plugin ===");
    }
}