package com.vbforge.repositories;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RepositoriesSettingsTasksTest — tests for every method in
 * {@link RepositoriesSettingsTasks}.
 *
 * <p><b>Getting started:</b>
 * <ol>
 *   <li>Implement all 5 methods in {@code RepositoriesSettingsTasks.java}.</li>
 *   <li>Remove the {@code @Disabled} annotation below.</li>
 *   <li>Run {@code ./mvnw test -pl 04-repositories-settings -Dtest=RepositoriesSettingsTasksTest} ✅</li>
 * </ol>
 */
@Disabled("Remove @Disabled once all 5 methods in RepositoriesSettingsTasks are implemented")
@DisplayName("RepositoriesSettingsTasks — your implementations")
class RepositoriesSettingsTasksTest {

    // ── Task 1: localRepoPath ────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 1 — localRepoPath()")
    class LocalRepoPathTest {

        @Test
        @DisplayName("dots in groupId become slashes")
        void dotsToSlashes() {
            assertThat(RepositoriesSettingsTasks.localRepoPath("com.vbforge", "foo", "1.0.0"))
                    .isEqualTo("com/vbforge/foo/1.0.0");
        }

        @Test
        @DisplayName("deep groupId becomes a deep tree")
        void deepGroup() {
            assertThat(RepositoriesSettingsTasks.localRepoPath(
                    "org.junit.jupiter", "junit-jupiter", "5.10.2"))
                    .isEqualTo("org/junit/jupiter/junit-jupiter/5.10.2");
        }

        @Test
        @DisplayName("single-segment groupId stays single")
        void singleSegment() {
            assertThat(RepositoriesSettingsTasks.localRepoPath("a", "b", "1"))
                    .isEqualTo("a/b/1");
        }

        @ParameterizedTest(name = "null {0} → IllegalArgumentException")
        @CsvSource({
                "groupId,  foo,      1.0.0",
                "com.x,    artifactId,1.0.0",
                "com.x,    foo,      version"
        })
        @DisplayName("any null argument throws IllegalArgumentException")
        void nulls(String marker, String artifactId, String version) {
            String groupId = "groupId".equals(marker) ? null : marker;
            String artifact = "artifactId".equals(marker) ? null : artifactId;
            String ver = "version".equals(marker) ? null : version;
            assertThatThrownBy(() -> RepositoriesSettingsTasks.localRepoPath(groupId, artifact, ver))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " "})
        @DisplayName("blank arguments throw IllegalArgumentException")
        void blanks(String blank) {
            assertThatThrownBy(() -> RepositoriesSettingsTasks.localRepoPath(blank, "foo", "1.0.0"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> RepositoriesSettingsTasks.localRepoPath("com.x", blank, "1.0.0"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> RepositoriesSettingsTasks.localRepoPath("com.x", "foo", blank))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── Task 2: remoteUrl ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 2 — remoteUrl()")
    class RemoteUrlTest {

        @Test
        @DisplayName("Central + jar produces the canonical URL")
        void centralJar() {
            assertThat(RepositoriesSettingsTasks.remoteUrl(
                    "https://repo.maven.apache.org/maven2", "com.vbforge", "foo", "1.0.0", "jar"))
                    .isEqualTo("https://repo.maven.apache.org/maven2/com/vbforge/foo/1.0.0/foo-1.0.0.jar");
        }

        @Test
        @DisplayName("pom extension works too")
        void pom() {
            assertThat(RepositoriesSettingsTasks.remoteUrl(
                    "https://repo.maven.apache.org/maven2", "com.vbforge", "foo", "1.0.0", "pom"))
                    .isEqualTo("https://repo.maven.apache.org/maven2/com/vbforge/foo/1.0.0/foo-1.0.0.pom");
        }

        @Test
        @DisplayName("a trailing slash on the base URL is stripped")
        void trailingSlash() {
            assertThat(RepositoriesSettingsTasks.remoteUrl(
                    "https://repo.maven.apache.org/maven2/", "com.vbforge", "foo", "1.0.0", "jar"))
                    .isEqualTo("https://repo.maven.apache.org/maven2/com/vbforge/foo/1.0.0/foo-1.0.0.jar");
        }

        @Test
        @DisplayName("deep groupId becomes a deep path")
        void deepGroup() {
            assertThat(RepositoriesSettingsTasks.remoteUrl(
                    "https://repo1.maven.org/maven2", "org.junit.jupiter",
                    "junit-jupiter", "5.10.2", "jar"))
                    .isEqualTo("https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter/5.10.2/junit-jupiter-5.10.2.jar");
        }

        @Test
        @DisplayName("any null or blank argument throws IllegalArgumentException")
        void nulls() {
            assertThatThrownBy(() -> RepositoriesSettingsTasks.remoteUrl(
                    null, "com.x", "foo", "1.0.0", "jar"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> RepositoriesSettingsTasks.remoteUrl(
                    "https://x", "com.x", "foo", "1.0.0", null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> RepositoriesSettingsTasks.remoteUrl(
                    "  ", "com.x", "foo", "1.0.0", "jar"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── Task 3: selectMirror ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 3 — selectMirror()")
    class SelectMirrorTest {

        private Mirror mirror(String id, String mirrorOf) {
            return new Mirror(id, mirrorOf, "https://" + id + ".example");
        }

        @Test
        @DisplayName("'*' matches any repository")
        void star() {
            Mirror m = mirror("m1", "*");
            assertThat(RepositoriesSettingsTasks.selectMirror("central", List.of(m))).isEqualTo(m);
            assertThat(RepositoriesSettingsTasks.selectMirror("spring", List.of(m))).isEqualTo(m);
        }

        @Test
        @DisplayName("'external:*' matches any repository")
        void external() {
            Mirror m = mirror("m1", "external:*");
            assertThat(RepositoriesSettingsTasks.selectMirror("central", List.of(m))).isEqualTo(m);
        }

        @Test
        @DisplayName("exact id matches only that id")
        void exact() {
            Mirror m = mirror("m1", "central");
            assertThat(RepositoriesSettingsTasks.selectMirror("central", List.of(m))).isEqualTo(m);
            assertThat(RepositoriesSettingsTasks.selectMirror("spring", List.of(m))).isNull();
        }

        @Test
        @DisplayName("comma-separated list matches any of the ids")
        void list() {
            Mirror m = mirror("m1", "central,spring-milestones");
            assertThat(RepositoriesSettingsTasks.selectMirror("central", List.of(m))).isEqualTo(m);
            assertThat(RepositoriesSettingsTasks.selectMirror("spring-milestones", List.of(m))).isEqualTo(m);
            assertThat(RepositoriesSettingsTasks.selectMirror("other", List.of(m))).isNull();
        }

        @Test
        @DisplayName("'!id' excludes that id even when '*' is present")
        void exclusion() {
            Mirror m = mirror("m1", "*,!central");
            assertThat(RepositoriesSettingsTasks.selectMirror("central", List.of(m))).isNull();
            assertThat(RepositoriesSettingsTasks.selectMirror("spring", List.of(m))).isEqualTo(m);
        }

        @Test
        @DisplayName("first matching mirror wins")
        void firstWins() {
            Mirror m1 = mirror("m1", "central");
            Mirror m2 = mirror("m2", "*");
            assertThat(RepositoriesSettingsTasks.selectMirror("central", List.of(m1, m2))).isEqualTo(m1);
            assertThat(RepositoriesSettingsTasks.selectMirror("spring", List.of(m1, m2))).isEqualTo(m2);
        }

        @Test
        @DisplayName("no matching mirror → null")
        void noMatch() {
            Mirror m = mirror("m1", "other");
            assertThat(RepositoriesSettingsTasks.selectMirror("central", List.of(m))).isNull();
        }

        @Test
        @DisplayName("null or blank inputs → null")
        void nulls() {
            Mirror m = mirror("m1", "*");
            assertThat(RepositoriesSettingsTasks.selectMirror(null, List.of(m))).isNull();
            assertThat(RepositoriesSettingsTasks.selectMirror("", List.of(m))).isNull();
            assertThat(RepositoriesSettingsTasks.selectMirror("central", null)).isNull();
            assertThat(RepositoriesSettingsTasks.selectMirror("central", List.of())).isNull();
        }
    }

    // ── Task 4: isSnapshot ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Task 4 — isSnapshot()")
    class IsSnapshotTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "1.0.0-SNAPSHOT",
                "1.0-SNAPSHOT",
                "2.0.0-SNAPSHOT",
                "1.0.0-alpha-SNAPSHOT",
                "-SNAPSHOT"
        })
        @DisplayName("SNAPSHOT suffix → true")
        void snapshots(String version) {
            assertThat(RepositoriesSettingsTasks.isSnapshot(version)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "1.0.0",
                "1.0.0-RELEASE",
                "1.0.0-snapshot",
                "SNAPSHOT",
                "1.0.0-SNAPSHOT-1",
                " 1.0.0-SNAPSHOT "
        })
        @DisplayName("anything else → false")
        void notSnapshots(String version) {
            assertThat(RepositoriesSettingsTasks.isSnapshot(version)).isFalse();
        }

        @Test
        @DisplayName("null → false")
        void nullVersion() {
            assertThat(RepositoriesSettingsTasks.isSnapshot(null)).isFalse();
        }
    }

    // ── Task 5: resolutionOrder ──────────────────────────────────────────────

    @Nested
    @DisplayName("Task 5 — resolutionOrder()")
    class ResolutionOrderTest {

        private Mirror mirror(String id, String mirrorOf) {
            return new Mirror(id, mirrorOf, "https://" + id + ".example");
        }

        private Map<String, String> repos(String... ids) {
            Map<String, String> m = new LinkedHashMap<>();
            for (String id : ids) {
                m.put(id, "https://" + id + ".example/repo");
            }
            return m;
        }

        @Test
        @DisplayName("no repos → only the local step")
        void onlyLocal() {
            assertThat(RepositoriesSettingsTasks.resolutionOrder(Map.of(), List.of()))
                    .containsExactly("local");
        }

        @Test
        @DisplayName("no mirrors → local, then each repo in order")
        void noMirrors() {
            assertThat(RepositoriesSettingsTasks.resolutionOrder(
                    repos("central", "spring"), List.of()))
                    .containsExactly("local", "repo:central", "repo:spring");
        }

        @Test
        @DisplayName("'*' mirror replaces every repo but appears once")
        void starMirrorDeduped() {
            assertThat(RepositoriesSettingsTasks.resolutionOrder(
                    repos("central", "spring"),
                    List.of(mirror("nexus", "*"))))
                    .containsExactly("local", "mirror:nexus");
        }

        @Test
        @DisplayName("mirror on one repo only; the other stays as 'repo:'")
        void partialMirror() {
            assertThat(RepositoriesSettingsTasks.resolutionOrder(
                    repos("central", "internal"),
                    List.of(mirror("nexus", "central"))))
                    .containsExactly("local", "mirror:nexus", "repo:internal");
        }

        @Test
        @DisplayName("null repos treated as empty; null mirrors means no matches")
        void nulls() {
            assertThat(RepositoriesSettingsTasks.resolutionOrder(null, null))
                    .containsExactly("local");
            assertThat(RepositoriesSettingsTasks.resolutionOrder(repos("central"), null))
                    .containsExactly("local", "repo:central");
        }

        @Test
        @DisplayName("mirror order follows repo declaration order")
        void orderFollowsRepos() {
            Map<String, String> r = new LinkedHashMap<>();
            r.put("a", "https://a");
            r.put("b", "https://b");

            List<Mirror> mirrors = List.of(
                    mirror("ma", "a"),
                    mirror("mb", "b"));

            assertThat(RepositoriesSettingsTasks.resolutionOrder(r, mirrors))
                    .containsExactly("local", "mirror:ma", "mirror:mb");
        }
    }
}