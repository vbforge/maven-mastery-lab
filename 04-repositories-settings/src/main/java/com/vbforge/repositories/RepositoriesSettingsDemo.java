package com.vbforge.repositories;

import java.util.List;

/**
 * RepositoriesSettingsDemo — annotated reference for how Maven finds artifacts.
 *
 * <p>Read top-to-bottom. Each section covers one mechanism and explains
 * <em>why</em> it exists — not just what the XML looks like.
 *
 * <p>Topics:
 * <ol>
 *   <li>The local repository ({@code ~/.m2/repository}) and its layout</li>
 *   <li>Remote repositories and Maven Central</li>
 *   <li>Mirrors and {@code mirrorOf} patterns</li>
 *   <li>SNAPSHOT vs release repositories</li>
 *   <li>Offline mode ({@code -o}) and forced updates ({@code -U})</li>
 *   <li>The full lookup order — local, mirrors, remotes</li>
 * </ol>
 *
 * <p>Run: {@code ./mvnw -q exec:java -pl 04-repositories-settings
 * -Dexec.mainClass="com.vbforge.repositories.RepositoriesSettingsDemo"}
 */
public class RepositoriesSettingsDemo {

    public static void main(String[] args) {
        localRepositoryDemo();
        System.out.println("---");
        remoteRepositoriesDemo();
        System.out.println("---");
        mirrorsDemo();
        System.out.println("---");
        snapshotRepositoriesDemo();
        System.out.println("---");
        offlineAndUpdatesDemo();
        System.out.println("---");
        lookupOrderDemo();
    }

    // ── 1. Local repository ──────────────────────────────────────────────────

    /** Maven Central, the default remote repository. */
    public static final String CENTRAL_URL = "https://repo.maven.apache.org/maven2";

    /**
     * Maven keeps everything it has ever downloaded under a single directory:
     * the <b>local repository</b>, by default {@code ~/.m2/repository}.
     *
     * <p>WHY this matters: every build consults it first, before any network
     * call. Deleting it forces a re-download (usually unnecessary). A corrupted
     * jar in it is the single most common cause of "why does this build fail
     * only on my machine?".
     *
     * <p>Layout — the group id becomes a directory tree, one directory per
     * dot, and the artifact's own directory contains the version:
     * <pre>{@code
     *   ~/.m2/repository/com/vbforge/foo/1.0.0/
     *       foo-1.0.0.jar
     *       foo-1.0.0.pom
     *       foo-1.0.0.jar.sha1
     *       _remote.repositories
     * }</pre>
     *
     * <p>The relative path inside the repository is the artifact's "local
     * repo path". The remote URL uses the same layout, just with an HTTP base.
     */
    public static String localRepoLayout() {
        return """
                ~/.m2/repository/
                └── com/vbforge/foo/1.0.0/
                    ├── foo-1.0.0.jar
                    ├── foo-1.0.0.pom
                    ├── foo-1.0.0.jar.sha1
                    └── _remote.repositories
                """;
    }

    /** The relative path of an artifact inside the local repository. */
    public static String localRepoPath(String groupId, String artifactId, String version) {
        return groupId.replace('.', '/') + "/" + artifactId + "/" + version;
    }

    static void localRepositoryDemo() {
        System.out.println("=== local repository ===");
        System.out.print(localRepoLayout());
        System.out.println();
        System.out.println("localRepoPath(\"com.vbforge\", \"foo\", \"1.0.0\") = "
                + localRepoPath("com.vbforge", "foo", "1.0.0"));
        System.out.println("override with -Dmaven.repo.local=/some/path, or in settings.xml <localRepository>");
    }

    // ── 2. Remote repositories ───────────────────────────────────────────────

    /**
     * A <b>remote repository</b> is any HTTP(S) server that stores artifacts in
     * the standard layout. Maven Central is the default; extra repositories are
     * declared in the pom (or in {@code settings.xml} profiles).
     *
     * <p>WHY this matters: adding a repository is a commitment — Maven will
     * contact it for anything it can't find elsewhere. Forgetting to add one
     * (or adding one behind an unreachable URL) is the classic "artifact not
     * found" cause.
     *
     * <p>The URL uses the same path layout as the local repository: {@code
     * <base>/<groupId-with-slashes>/<artifactId>/<version>/<file>}.
     */
    public static String sampleRepositoriesSnippet() {
        return """
                <repositories>
                    <repository>
                        <id>spring-milestones</id>
                        <url>https://repo.spring.io/milestone</url>
                        <releases><enabled>true</enabled></releases>
                        <snapshots><enabled>false</enabled></snapshots>
                    </repository>
                </repositories>
                """;
    }

    /** The full URL of an artifact in a remote repository. */
    public static String remoteUrl(String baseUrl, String groupId, String artifactId,
                                   String version, String extension) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/"
                + groupId.replace('.', '/') + "/"
                + artifactId + "/" + version + "/"
                + artifactId + "-" + version + "." + extension;
    }

    static void remoteRepositoriesDemo() {
        System.out.println("=== remote repositories ===");
        System.out.println(sampleRepositoriesSnippet());
        System.out.println("Maven Central URL: " + CENTRAL_URL);
        System.out.println("remoteUrl(\"" + CENTRAL_URL + "\", \"com.vbforge\", \"foo\", \"1.0.0\", \"jar\")");
        System.out.println("  = " + remoteUrl(CENTRAL_URL, "com.vbforge", "foo", "1.0.0", "jar"));
    }

    // ── 3. Mirrors ───────────────────────────────────────────────────────────

    /**
     * A <b>mirror</b> replaces one or more repositories with a different URL.
     * Defined in {@code ~/.m2/settings.xml} (never in the pom — mirrors are a
     * per-developer / per-CI concern, not a project concern).
     *
     * <p>WHY this matters: three real-world uses —
     * <ul>
     *   <li>Corporate proxy — mirror {@code *} onto an internal Nexus/Artifactory</li>
     *   <li>Faster cache — mirror Central to a local mirror</li>
     *   <li>Block a bad repo — mirror {@code bad-repo} to a known-good one</li>
     * </ul>
     *
     * <p>{@code mirrorOf} patterns:
     * <pre>
     *   *                      every repository
     *   external:*             every repo except localhost and file://
     *   central                exactly the repo with id 'central'
     *   central,spring-miles   any of these ids
     *   *,!my-internal         everything except 'my-internal'
     * </pre>
     */
    public static String sampleMirrorSnippet() {
        return """
                <settings>
                    <mirrors>
                        <mirror>
                            <id>corp-nexus</id>
                            <mirrorOf>*</mirrorOf>
                            <url>https://nexus.corp.example/repository/maven-public/</url>
                        </mirror>
                    </mirrors>
                </settings>
                """;
    }

    static void mirrorsDemo() {
        System.out.println("=== mirrors ===");
        System.out.println("typical ~/.m2/settings.xml:");
        System.out.println(sampleMirrorSnippet());
        System.out.println("If two mirrors match, the FIRST one wins.");
        System.out.println("'external:*' is the safe default for a corporate proxy:");
        System.out.println("  it leaves file:// and localhost repos untouched.");
    }

    // ── 4. SNAPSHOT repositories ─────────────────────────────────────────────

    /**
     * A repository can separately enable or disable <b>release</b> and
     * <b>SNAPSHOT</b> artifacts. Maven only looks for a SNAPSHOT in a repo
     * that has {@code <snapshots><enabled>true</enabled></snapshots>} — and
     * by default, Central has snapshots <em>disabled</em>.
     *
     * <p>WHY this matters: "why can't Maven find my {@code 1.0.0-SNAPSHOT}?"
     * is almost always because the repository that hosts it is not configured
     * for snapshots. And no public repo should ever serve snapshots — they are
     * mutable and would break everyone's reproducibility.
     */
    public static String snapshotRepositorySnippet() {
        return """
                <repository>
                    <id>my-snapshots</id>
                    <url>https://nexus.corp.example/repository/maven-snapshots/</url>
                    <releases><enabled>false</enabled></releases>
                    <snapshots><enabled>true</enabled></snapshots>
                </repository>
                """;
    }

    /** Detects a SNAPSHOT version (Maven uses the uppercase suffix only). */
    public static boolean isSnapshot(String version) {
        return version != null && version.endsWith("-SNAPSHOT");
    }

    static void snapshotRepositoriesDemo() {
        System.out.println("=== SNAPSHOT repositories ===");
        System.out.println(snapshotRepositorySnippet());
        System.out.println("isSnapshot(\"1.0.0-SNAPSHOT\") = " + isSnapshot("1.0.0-SNAPSHOT"));
        System.out.println("isSnapshot(\"1.0.0\")          = " + isSnapshot("1.0.0"));
        System.out.println("isSnapshot(\"1.0.0-snapshot\") = " + isSnapshot("1.0.0-snapshot")
                + "  (case-sensitive — Maven requires uppercase SNAPSHOT)");
    }

    // ── 5. Offline mode and forced updates ───────────────────────────────────

    /**
     * Two flags that change how aggressively Maven talks to the network:
     * <ul>
     *   <li>{@code -o} (offline) — use only the local repository; fail if an
     *       artifact is missing. Good on a plane; fatal in CI.</li>
     *   <li>{@code -U} (update snapshots) — re-check remote repositories for
     *       newer SNAPSHOT / release metadata. Needed when a colleague pushed
     *       a new {@code -SNAPSHOT} and you still see the old one.</li>
     * </ul>
     *
     * <p>WHY this matters: a stale cached SNAPSHOT in {@code ~/.m2} is the
     * single most common "but I built it correctly!" confusion. {@code -U}
     * is the fix; {@code -o} is the diagnosis.
     */
    public static String offlineAndUpdateFlags() {
        return """
                ./mvnw -o clean verify      # offline: only ~/.m2, no network
                ./mvnw -U clean verify      # force re-check of SNAPSHOT metadata
                ./mvnw -U -pl my-module test
                """;
    }

    static void offlineAndUpdatesDemo() {
        System.out.println("=== offline and forced updates ===");
        System.out.print(offlineAndUpdateFlags());
        System.out.println();
        System.out.println("-o wins over -U if both are given: offline means 'no network at all'.");
    }

    // ── 6. Lookup order ──────────────────────────────────────────────────────

    /**
     * For every artifact, Maven tries the sources in this order:
     * <ol>
     *   <li><b>Local repository</b> — {@code ~/.m2/repository}. Always first,
     *       no network involved.</li>
     *   <li><b>Mirror of the repository that declares the artifact</b> — if a
     *       mirror's {@code mirrorOf} matches the repo id, the mirror's URL
     *       replaces the repo's URL (not added to it).</li>
     *   <li><b>The repository's own URL</b> — when no mirror matched.</li>
     * </ol>
     * Only after all of these fail does the build report "could not resolve".
     *
     * <p>Enumerating the steps for a small set of repositories and mirrors is
     * exactly what {@code -X} shows in the log, one line per attempt.
     */
    public static List<String> lookupOrder() {
        return List.of(
                "1. local repository (~/.m2/repository)",
                "2. mirror of the repository that declares the artifact (if any)",
                "3. the repository's own URL (if no mirror matched)",
                "4. fail: 'Could not resolve dependencies' / 'Non-resolvable ...'"
        );
    }

    static void lookupOrderDemo() {
        System.out.println("=== lookup order ===");
        lookupOrder().forEach(System.out::println);
        System.out.println();
        System.out.println("See the actual attempts with: ./mvnw -X dependency:get -Dartifact=com.vbforge:foo:1.0.0");
    }
}