package com.vbforge.profiles;

import com.vbforge.profiles.PropertiesProfilesVersionsDemo.BumpLevel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PropertiesProfilesVersionsTasksTest — tests for every method in
 * {@link PropertiesProfilesVersionsTasks}.
 *
 * <p><b>Getting started:</b>
 * <ol>
 *   <li>Implement all 5 methods in {@code PropertiesProfilesVersionsTasks.java}.</li>
 *   <li>Remove the {@code @Disabled} annotation below.</li>
 *   <li>Run {@code ./mvnw test -pl 05-properties-profiles-versions -Dtest=PropertiesProfilesVersionsTasksTest} ✅</li>
 * </ol>
 */
@Disabled("Remove @Disabled once all 5 methods in PropertiesProfilesVersionsTasks are implemented")
@DisplayName("PropertiesProfilesVersionsTasks — your implementations")
class PropertiesProfilesVersionsTasksTest {

    // ── Task 1: interpolate ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 1 — interpolate()")
    class InterpolateTest {

        @Test
        @DisplayName("single placeholder is replaced")
        void single() {
            assertThat(PropertiesProfilesVersionsTasks.interpolate(
                    "Hello ${name}!", Map.of("name", "World")))
                    .isEqualTo("Hello World!");
        }

        @Test
        @DisplayName("multiple placeholders")
        void multiple() {
            assertThat(PropertiesProfilesVersionsTasks.interpolate(
                    "${a}-${b}", Map.of("a", "1", "b", "2")))
                    .isEqualTo("1-2");
        }

        @Test
        @DisplayName("same placeholder twice → both replaced")
        void repeated() {
            assertThat(PropertiesProfilesVersionsTasks.interpolate(
                    "${x} and ${x}", Map.of("x", "hi")))
                    .isEqualTo("hi and hi");
        }

        @Test
        @DisplayName("unknown placeholder is left unchanged")
        void unknown() {
            assertThat(PropertiesProfilesVersionsTasks.interpolate(
                    "${known} ${missing}", Map.of("known", "hi")))
                    .isEqualTo("hi ${missing}");
        }

        @Test
        @DisplayName("no placeholders → unchanged")
        void noPlaceholders() {
            assertThat(PropertiesProfilesVersionsTasks.interpolate(
                    "plain text", Map.of("x", "y")))
                    .isEqualTo("plain text");
        }

        @Test
        @DisplayName("null text → null")
        void nullText() {
            assertThat(PropertiesProfilesVersionsTasks.interpolate(null, Map.of("a", "b")))
                    .isNull();
        }

        @Test
        @DisplayName("null or empty props → text returned unchanged")
        void nullProps() {
            assertThat(PropertiesProfilesVersionsTasks.interpolate("x${a}", null))
                    .isEqualTo("x${a}");
            assertThat(PropertiesProfilesVersionsTasks.interpolate("x${a}", Map.of()))
                    .isEqualTo("x${a}");
        }

        @Test
        @DisplayName("null key or value is skipped")
        void nullEntriesSkipped() {
            Map<String, String> props = new java.util.HashMap<>();
            props.put("good", "yes");
            props.put("bad", null);
            assertThat(PropertiesProfilesVersionsTasks.interpolate("${good} ${bad}", props))
                    .isEqualTo("yes ${bad}");
        }
    }

    // ── Task 2: compareVersions ──────────────────────────────────────────────

    @Nested
    @DisplayName("Task 2 — compareVersions()")
    class CompareVersionsTest {

        @Test
        @DisplayName("equal versions → 0")
        void equal() {
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.0.0", "1.0.0")).isZero();
        }

        @Test
        @DisplayName("numeric comparison: 1.10.0 > 1.9.0")
        void numericNotLexical() {
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.10.0", "1.9.0")).isPositive();
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.9.0", "1.10.0")).isNegative();
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.2.0", "1.10.0")).isNegative();
        }

        @Test
        @DisplayName("major / minor / patch ordering")
        void normalOrdering() {
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("2.0.0", "1.9.9")).isPositive();
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.1.0", "1.0.9")).isPositive();
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.0.1", "1.0.0")).isPositive();
        }

        @Test
        @DisplayName("missing trailing segment counts as 0")
        void missingSegment() {
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.0", "1.0.0")).isZero();
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1", "1.0.0")).isZero();
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.0", "1.0.1")).isNegative();
        }

        @Test
        @DisplayName("SNAPSHOT is less than the release")
        void snapshotLess() {
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.0.0-SNAPSHOT", "1.0.0"))
                    .isNegative();
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.0.0", "1.0.0-SNAPSHOT"))
                    .isPositive();
        }

        @Test
        @DisplayName("numeric part wins before qualifier comparison")
        void numericBeatsQualifier() {
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.0.1-SNAPSHOT", "1.0.0"))
                    .isPositive();
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.0.0", "1.0.1-SNAPSHOT"))
                    .isNegative();
        }

        @Test
        @DisplayName("qualifiers compare lexically when numeric parts are equal")
        void lexicalQualifiers() {
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.0.0-alpha", "1.0.0-beta"))
                    .isNegative();
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.0.0-beta", "1.0.0-alpha"))
                    .isPositive();
            assertThat(PropertiesProfilesVersionsTasks.compareVersions("1.0.0-SNAPSHOT", "1.0.0-rc1"))
                    .isNegative();
        }

        @Test
        @DisplayName("null argument → IllegalArgumentException")
        void nulls() {
            assertThatThrownBy(() -> PropertiesProfilesVersionsTasks.compareVersions(null, "1.0.0"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> PropertiesProfilesVersionsTasks.compareVersions("1.0.0", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── Task 3: bumpVersion ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 3 — bumpVersion()")
    class BumpVersionTest {

        @ParameterizedTest(name = "bump({0}, {1}) → {2}")
        @CsvSource({
                "1.2.3, MAJOR, 2.0.0",
                "1.2.3, MINOR, 1.3.0",
                "1.2.3, PATCH, 1.2.4",
                "0.0.0, MAJOR, 1.0.0",
                "0.0.0, MINOR, 0.1.0",
                "0.0.0, PATCH, 0.0.1",
                "9.9.9, MAJOR, 10.0.0",
                "9.9.9, MINOR, 9.10.0",
                "9.9.9, PATCH, 9.9.10"
        })
        @DisplayName("plain numeric versions")
        void numeric(String version, BumpLevel level, String expected) {
            assertThat(PropertiesProfilesVersionsTasks.bumpVersion(version, level))
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("SNAPSHOT suffix is preserved")
        void snapshotPreserved() {
            assertThat(PropertiesProfilesVersionsTasks.bumpVersion("1.2.3-SNAPSHOT", BumpLevel.MAJOR))
                    .isEqualTo("2.0.0-SNAPSHOT");
            assertThat(PropertiesProfilesVersionsTasks.bumpVersion("1.2.3-SNAPSHOT", BumpLevel.MINOR))
                    .isEqualTo("1.3.0-SNAPSHOT");
            assertThat(PropertiesProfilesVersionsTasks.bumpVersion("1.2.3-SNAPSHOT", BumpLevel.PATCH))
                    .isEqualTo("1.2.4-SNAPSHOT");
        }

        @Test
        @DisplayName("short version gets padded to major.minor.patch")
        void shortVersions() {
            assertThat(PropertiesProfilesVersionsTasks.bumpVersion("1.0", BumpLevel.MINOR))
                    .isEqualTo("1.1.0");
            assertThat(PropertiesProfilesVersionsTasks.bumpVersion("1", BumpLevel.PATCH))
                    .isEqualTo("1.0.1");
        }

        @Test
        @DisplayName("null, blank version or null level → IllegalArgumentException")
        void invalidInputs() {
            assertThatThrownBy(() -> PropertiesProfilesVersionsTasks.bumpVersion(null, BumpLevel.PATCH))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> PropertiesProfilesVersionsTasks.bumpVersion("", BumpLevel.PATCH))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> PropertiesProfilesVersionsTasks.bumpVersion("   ", BumpLevel.PATCH))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> PropertiesProfilesVersionsTasks.bumpVersion("1.0.0", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── Task 4: isProfileActive ──────────────────────────────────────────────

    @Nested
    @DisplayName("Task 4 — isProfileActive()")
    class IsProfileActiveTest {

        @Test
        @DisplayName("id in cliActive → active")
        void cli() {
            ActivationEnv env = new ActivationEnv(
                    "17.0.2", "Linux", Map.of(), Set.of("prod"));
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(Profile.of("prod"), env)).isTrue();
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(Profile.of("dev"), env)).isFalse();
        }

        @Test
        @DisplayName("activeByDefault is active when no -P is given")
        void defaultActiveNoCli() {
            ActivationEnv env = ActivationEnv.of("17.0.2", "Linux");
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(
                    Profile.defaultActive("dev"), env)).isTrue();
        }

        @Test
        @DisplayName("activeByDefault is disabled when -P is given")
        void defaultDisabledByCli() {
            ActivationEnv env = new ActivationEnv(
                    "17.0.2", "Linux", Map.of(), Set.of("prod"));
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(
                    Profile.defaultActive("dev"), env)).isFalse();
        }

        @Test
        @DisplayName("jdk prefix match")
        void jdk() {
            ActivationEnv env = ActivationEnv.of("17.0.2", "Linux");
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(
                    Profile.onJdk("jdk17", "17"), env)).isTrue();
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(
                    Profile.onJdk("jdk1_8", "1.8"), env)).isFalse();
        }

        @Test
        @DisplayName("os exact match")
        void os() {
            ActivationEnv env = ActivationEnv.of("17.0.2", "Linux");
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(
                    Profile.onOs("linux", "Linux"), env)).isTrue();
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(
                    Profile.onOs("win", "Windows"), env)).isFalse();
        }

        @Test
        @DisplayName("property set with matching value")
        void propertyMatch() {
            ActivationEnv env = new ActivationEnv(
                    "17", "Linux", Map.of("metrics.enabled", "true"), Set.of());
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(
                    Profile.onProperty("metrics", "metrics.enabled", "true"), env)).isTrue();
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(
                    Profile.onProperty("metrics", "metrics.enabled", "false"), env)).isFalse();
        }

        @Test
        @DisplayName("property set, value not specified → any value matches")
        void propertyAnyValue() {
            ActivationEnv env = new ActivationEnv(
                    "17", "Linux", Map.of("debug", "whatever"), Set.of());
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(
                    Profile.onProperty("debug", "debug", null), env)).isTrue();
        }

        @Test
        @DisplayName("property not set → inactive")
        void propertyMissing() {
            ActivationEnv env = ActivationEnv.of("17", "Linux");
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(
                    Profile.onProperty("x", "x", null), env)).isFalse();
        }

        @Test
        @DisplayName("no rules and no -P → inactive")
        void plainInactive() {
            ActivationEnv env = ActivationEnv.of("17", "Linux");
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(
                    Profile.of("plain"), env)).isFalse();
        }

        @Test
        @DisplayName("null profile or env → false")
        void nulls() {
            ActivationEnv env = ActivationEnv.of("17", "Linux");
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(null, env)).isFalse();
            assertThat(PropertiesProfilesVersionsTasks.isProfileActive(Profile.of("x"), null)).isFalse();
        }
    }

    // ── Task 5: releaseVersion ───────────────────────────────────────────────

    @Nested
    @DisplayName("Task 5 — releaseVersion()")
    class ReleaseVersionTest {

        @ParameterizedTest(name = "releaseVersion({0}) → {1}")
        @CsvSource({
                "1.2.0-SNAPSHOT, 1.2.0",
                "1.0.0-SNAPSHOT, 1.0.0",
                "2.0.0-SNAPSHOT, 2.0.0",
                "-SNAPSHOT, ''",
                "1.2.0, 1.2.0",
                "1.2.0-snapshot, 1.2.0-snapshot",
                "1.2.0-SNAPSHOT-1, 1.2.0-SNAPSHOT-1",
                "SNAPSHOT, SNAPSHOT",
                "1.2.0-rc1, 1.2.0-rc1"
        })
        @DisplayName("strip only the exact uppercase suffix")
        void strip(String input, String expected) {
            assertThat(PropertiesProfilesVersionsTasks.releaseVersion(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("null → null")
        void nullVersion() {
            assertThat(PropertiesProfilesVersionsTasks.releaseVersion(null)).isNull();
        }

        @Test
        @DisplayName("empty → empty")
        void emptyVersion() {
            assertThat(PropertiesProfilesVersionsTasks.releaseVersion("")).isEmpty();
        }
    }
}