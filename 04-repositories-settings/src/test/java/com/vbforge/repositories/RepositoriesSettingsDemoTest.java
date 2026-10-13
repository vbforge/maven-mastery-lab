package com.vbforge.repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RepositoriesSettingsDemoTest — verifies every concept shown in
 * {@link RepositoriesSettingsDemo}.
 *
 * <p>These tests are always GREEN. Read them as an executable specification.
 */
@DisplayName("RepositoriesSettingsDemo — API contracts")
class RepositoriesSettingsDemoTest {

    @Nested
    @DisplayName("1. Local repository")
    class LocalRepositoryTest {

        @Test
        @DisplayName("Central URL is the well-known Maven Central endpoint")
        void central() {
            assertThat(RepositoriesSettingsDemo.CENTRAL_URL)
                    .isEqualTo("https://repo.maven.apache.org/maven2");
        }

        @Test
        @DisplayName("localRepoPath turns dots into slashes and appends artifact/version")
        void path() {
            assertThat(RepositoriesSettingsDemo.localRepoPath("com.vbforge", "foo", "1.0.0"))
                    .isEqualTo("com/vbforge/foo/1.0.0");
        }

        @Test
        @DisplayName("the layout snippet mentions ~/.m2/repository")
        void layout() {
            assertThat(RepositoriesSettingsDemo.localRepoLayout())
                    .contains("~/.m2/repository")
                    .contains("com/vbforge/foo/1.0.0");
        }
    }

    @Nested
    @DisplayName("2. Remote repositories")
    class RemoteRepositoriesTest {

        @Test
        @DisplayName("the repositories snippet declares a <repository> with id and url")
        void snippet() {
            assertThat(RepositoriesSettingsDemo.sampleRepositoriesSnippet())
                    .contains("<repositories>")
                    .contains("<id>spring-milestones</id>")
                    .contains("<url>https://repo.spring.io/milestone</url>");
        }

        @Test
        @DisplayName("remoteUrl composes base + group + artifact + version + file")
        void url() {
            assertThat(RepositoriesSettingsDemo.remoteUrl(
                    RepositoriesSettingsDemo.CENTRAL_URL, "com.vbforge", "foo", "1.0.0", "jar"))
                    .isEqualTo("https://repo.maven.apache.org/maven2/com/vbforge/foo/1.0.0/foo-1.0.0.jar");
        }

        @Test
        @DisplayName("a trailing slash on the base URL is stripped")
        void trailingSlash() {
            assertThat(RepositoriesSettingsDemo.remoteUrl(
                    RepositoriesSettingsDemo.CENTRAL_URL + "/", "com.vbforge", "foo", "1.0.0", "pom"))
                    .isEqualTo("https://repo.maven.apache.org/maven2/com/vbforge/foo/1.0.0/foo-1.0.0.pom");
        }
    }

    @Nested
    @DisplayName("3. Mirrors")
    class MirrorsTest {

        @Test
        @DisplayName("the mirror snippet declares <mirrorOf>*</mirrorOf>")
        void snippet() {
            assertThat(RepositoriesSettingsDemo.sampleMirrorSnippet())
                    .contains("<mirrors>")
                    .contains("<mirrorOf>*</mirrorOf>")
                    .contains("nexus.corp.example");
        }
    }

    @Nested
    @DisplayName("4. SNAPSHOT repositories")
    class SnapshotTest {

        @Test
        @DisplayName("the snapshot repository snippet enables snapshots, disables releases")
        void snippet() {
            String s = RepositoriesSettingsDemo.snapshotRepositorySnippet();
            assertThat(s)
                    .contains("<snapshots><enabled>true</enabled></snapshots>")
                    .contains("<releases><enabled>false</enabled></releases>");
        }

        @Test
        @DisplayName("isSnapshot is case-sensitive and suffix-only")
        void detect() {
            assertThat(RepositoriesSettingsDemo.isSnapshot("1.0.0-SNAPSHOT")).isTrue();
            assertThat(RepositoriesSettingsDemo.isSnapshot("1.0.0")).isFalse();
            assertThat(RepositoriesSettingsDemo.isSnapshot("1.0.0-snapshot")).isFalse();
            assertThat(RepositoriesSettingsDemo.isSnapshot(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("5. Offline and update flags")
    class OfflineTest {

        @Test
        @DisplayName("both -o and -U appear in the flag snippet")
        void flags() {
            assertThat(RepositoriesSettingsDemo.offlineAndUpdateFlags())
                    .contains("-o clean verify")
                    .contains("-U clean verify");
        }
    }

    @Nested
    @DisplayName("6. Lookup order")
    class LookupOrderTest {

        @Test
        @DisplayName("four steps, local first, failure last")
        void order() {
            assertThat(RepositoriesSettingsDemo.lookupOrder())
                    .hasSize(4)
                    .first().asString().contains("local repository");

            assertThat(RepositoriesSettingsDemo.lookupOrder())
                    .hasSize(4)
                    .last().asString().contains("fail");
        }
    }

    @Test
    @DisplayName("main() runs without exceptions and prints every section")
    void mainPrintsAllSections() {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            RepositoriesSettingsDemo.main(new String[0]);
        } finally {
            System.setOut(original);
        }

        assertThat(buffer.toString(StandardCharsets.UTF_8))
                .contains("=== local repository ===")
                .contains("=== remote repositories ===")
                .contains("=== mirrors ===")
                .contains("=== SNAPSHOT repositories ===")
                .contains("=== offline and forced updates ===")
                .contains("=== lookup order ===");
    }
}