package com.vbforge.profiles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PropertiesProfilesVersionsDemo — annotated reference for properties,
 * profiles, and versioning in Maven.
 *
 * <p>Read top-to-bottom. Each section covers one mechanism and explains
 * <em>why</em> it exists.
 *
 * <p>Topics:
 * <ol>
 *   <li>Custom and built-in properties ({@code ${project.version}}, …)</li>
 *   <li>Resource filtering</li>
 *   <li>Profiles and activation ({@code -P}, {@code activeByDefault}, JDK, OS, property)</li>
 *   <li>SNAPSHOT vs release and semantic versioning</li>
 *   <li>CI-friendly versions ({@code ${revision}})</li>
 *   <li>{@code versions:} plugin</li>
 * </ol>
 *
 * <p>Run: {@code ./mvnw -q exec:java -pl 05-properties-profiles-versions
 * -Dexec.mainClass="com.vbforge.profiles.PropertiesProfilesVersionsDemo"}
 */
public class PropertiesProfilesVersionsDemo {

    public static void main(String[] args) {
        propertiesDemo();
        System.out.println("---");
        resourceFilteringDemo();
        System.out.println("---");
        profilesDemo();
        System.out.println("---");
        versioningDemo();
        System.out.println("---");
        ciFriendlyVersionsDemo();
        System.out.println("---");
        versionsPluginDemo();
    }

    // ── 1. Properties ────────────────────────────────────────────────────────

    /**
     * A <b>property</b> is a named string used anywhere in the pom via
     * {@code ${name}}. Custom properties live in {@code <properties>}; built-in
     * ones are provided by Maven itself.
     *
     * <p>WHY this matters: a version written as {@code ${assertj.version}}
     * lives in exactly one place, and a value like {@code ${project.version}}
     * is never duplicated. Changing the number in one spot moves the whole
     * build — the opposite of copy-paste pom maintenance.
     */
    public static final Map<String, String> BUILTIN_PROPERTIES = builtinProperties();

    private static Map<String, String> builtinProperties() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("project.groupId",    "the project's groupId");
        m.put("project.artifactId", "the project's artifactId");
        m.put("project.version",    "the project's version");
        m.put("project.basedir",    "absolute path to the module's directory");
        m.put("project.build.directory", "target/");
        m.put("basedir",            "legacy alias of project.basedir");
        m.put("java.version",       "value the user (or parent) set — Maven does not provide it");
        m.put("maven.compiler.release", "value the user (or parent) set — Maven does not provide it");
        return m;
    }

    /** Simple, single-pass placeholder substitution, exactly like resource filtering. */
    public static String interpolate(String text, Map<String, String> props) {
        if (text == null) {
            return null;
        }
        if (props == null || props.isEmpty()) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, String> e : props.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                continue;
            }
            result = result.replace("${" + e.getKey() + "}", e.getValue());
        }
        return result;
    }

    static void propertiesDemo() {
        System.out.println("=== properties ===");
        System.out.println("built-in properties Maven provides:");
        BUILTIN_PROPERTIES.forEach((k, v) ->
                System.out.printf("  ${%-25s} → %s%n", k, v));
        System.out.println();
        System.out.println("interpolate(\"app ${project.version} (${env})\", " +
                "{project.version=1.2.3, env=prod}) = "
                + interpolate("app ${project.version} (${env})",
                        Map.of("project.version", "1.2.3", "env", "prod")));
        System.out.println("unknown placeholders are left untouched, exactly like Maven.");
    }

    // ── 2. Resource filtering ────────────────────────────────────────────────

    /**
     * <b>Resource filtering</b> lets Maven substitute {@code ${...}}
     * placeholders <em>inside files</em> under {@code src/main/resources} (and
     * {@code src/test/resources}) — the same substitution as
     * {@link #interpolate}, applied to every resource file Maven copies into
     * {@code target/classes}.
     *
     * <p>WHY this matters: configuration files like
     * {@code application.properties} need the version, environment, or
     * profile-specific endpoint baked in at build time. Filtering is the
     * standard mechanism; enabling it on binary files (or forgetting to
     * re-disable it for some resource sets) is the classic foot-gun.
     */
    public static String filteringSnippet() {
        return """
                <build>
                    <resources>
                        <resource>
                            <directory>src/main/resources</directory>
                            <filtering>true</filtering>
                        </resource>
                    </resources>
                </build>
                """;
    }

    static void resourceFilteringDemo() {
        System.out.println("=== resource filtering ===");
        System.out.println(filteringSnippet());
        System.out.println("placeholders in src/main/resources are substituted at build time.");
        System.out.println("disable filtering for a subfolder holding binaries (fonts, images, keystores).");
    }

    // ── 3. Profiles ──────────────────────────────────────────────────────────

    /**
     * A <b>profile</b> is a conditional slice of pom: extra dependencies,
     * properties, plugins or repositories that apply only when the profile is
     * active.
     *
     * <p>Activation sources:
     * <ul>
     *   <li>{@code -P <id>} on the command line — the one you actually control</li>
     *   <li>{@code <activeByDefault>true</activeByDefault>} — active when no
     *       {@code -P} is given</li>
     *   <li>{@code <jdk>17</jdk>} — prefix match against the running JDK version</li>
     *   <li>{@code <os><name>Linux</name></os>} — matches the running OS</li>
     *   <li>{@code <property><name>env</name><value>prod</value></property>} —
     *       matches a {@code -D} or project property</li>
     *   <li>{@code <file><exists>...</exists></file>} — presence of a file</li>
     * </ul>
     */
    public static String profilesSnippet() {
        return """
                <profiles>
                    <profile>
                        <id>dev</id>
                        <activation><activeByDefault>true</activeByDefault></activation>
                        <properties>
                            <env.endpoint>http://localhost:8080</env.endpoint>
                        </properties>
                    </profile>
                    <profile>
                        <id>prod</id>
                        <properties>
                            <env.endpoint>https://api.example.com</env.endpoint>
                        </properties>
                    </profile>
                </profiles>
                """;
    }

    /**
     * Returns the ids of all profiles in {@code profiles} that would be active
     * in {@code env}, in declaration order.
     */
    public static List<String> activeProfiles(List<Profile> profiles, ActivationEnv env) {
        return profiles.stream()
                .filter(p -> isActive(p, env))
                .map(Profile::id)
                .toList();
    }

    static boolean isActive(Profile p, ActivationEnv env) {
        if (env.cliActive().contains(p.id())) {
            return true;
        }
        if (p.activeByDefault() && env.cliActive().isEmpty()) {
            return true;
        }
        if (p.jdk() != null && env.jdk() != null && env.jdk().startsWith(p.jdk())) {
            return true;
        }
        if (p.osName() != null && p.osName().equals(env.osName())) {
            return true;
        }
        if (p.propertyName() != null) {
            String value = env.properties().get(p.propertyName());
            if (value != null && (p.propertyValue() == null || p.propertyValue().equals(value))) {
                return true;
            }
        }
        return false;
    }

    static void profilesDemo() {
        System.out.println("=== profiles ===");
        System.out.println(profilesSnippet());

        List<Profile> profiles = List.of(
                Profile.defaultActive("dev"),
                Profile.of("prod"),
                Profile.onJdk("jdk17", "17"),
                Profile.onProperty("metrics", "metrics.enabled", "true")
        );

        System.out.println("active on JDK 17.0.2, no -P, no properties:");
        System.out.println("  " + activeProfiles(profiles, ActivationEnv.of("17.0.2", "Linux")));

        System.out.println("active on JDK 17.0.2 with -Pprod:");
        System.out.println("  " + activeProfiles(profiles,
                new ActivationEnv("17.0.2", "Linux", Map.of(), java.util.Set.of("prod"))));

        System.out.println("active with -Dmetrics.enabled=true:");
        System.out.println("  " + activeProfiles(profiles,
                new ActivationEnv("11.0.2", "Linux",
                        Map.of("metrics.enabled", "true"), java.util.Set.of())));
    }

    // ── 4. Versioning ────────────────────────────────────────────────────────

    /**
     * Maven versions are opaque strings, but two conventions matter:
     * <ul>
     *   <li><b>semantic versioning</b> — {@code major.minor.patch}, communicated
     *       to consumers as "safe to upgrade" rules</li>
     *   <li><b>SNAPSHOT</b> — a {@code -SNAPSHOT} suffix marks a mutable,
     *       in-development version; the equivalent release version is greater.</li>
     * </ul>
     */
    public enum BumpLevel {
        MAJOR, MINOR, PATCH;

        String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Compares two Maven versions.
     * <ul>
     *   <li>Numeric segments compare numerically: {@code 1.10.0 > 1.9.0}.</li>
     *   <li>A pre-release qualifier (e.g. {@code -SNAPSHOT}, {@code -alpha},
     *       {@code -rc1}) makes the version <b>less than</b> the same version
     *       without the qualifier: {@code 1.0.0-SNAPSHOT < 1.0.0}.</li>
     *   <li>Two qualifiers are compared lexically.</li>
     * </ul>
     *
     * @return negative if {@code a < b}, zero if equal, positive if {@code a > b}
     */
    public static int compareVersions(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("versions must not be null");
        }
        String[] ap = a.split("-", 2);
        String[] bp = b.split("-", 2);

        int numeric = compareNumeric(ap[0], bp[0]);
        if (numeric != 0) {
            return numeric;
        }
        boolean aHasQ = ap.length > 1;
        boolean bHasQ = bp.length > 1;
        if (aHasQ && !bHasQ) return -1;
        if (!aHasQ && bHasQ) return  1;
        if (!aHasQ)          return  0;
        return ap[1].compareTo(bp[1]);
    }

    private static int compareNumeric(String a, String b) {
        String[] as = a.split("\\.");
        String[] bs = b.split("\\.");
        int n = Math.max(as.length, bs.length);
        for (int i = 0; i < n; i++) {
            int ai = i < as.length ? parse(as[i]) : 0;
            int bi = i < bs.length ? parse(bs[i]) : 0;
            if (ai != bi) return Integer.compare(ai, bi);
        }
        return 0;
    }

    private static int parse(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Bumps the given segment; higher segments are kept, lower ones reset to 0. */
    public static String bumpVersion(String version, BumpLevel level) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank");
        }
        String[] parts = version.split("-", 2);
        String numeric = parts[0];
        String suffix = parts.length > 1 ? "-" + parts[1] : "";

        String[] segs = numeric.split("\\.");
        int major = segs.length > 0 ? parse(segs[0]) : 0;
        int minor = segs.length > 1 ? parse(segs[1]) : 0;
        int patch = segs.length > 2 ? parse(segs[2]) : 0;

        switch (level) {
            case MAJOR -> { major++; minor = 0; patch = 0; }
            case MINOR -> { minor++; patch = 0; }
            case PATCH -> { patch++; }
        }
        return major + "." + minor + "." + patch + suffix;
    }

    /** Strips the {@code -SNAPSHOT} suffix, producing the release version. */
    public static String releaseVersion(String snapshot) {
        if (snapshot == null) {
            return null;
        }
        return snapshot.endsWith("-SNAPSHOT")
                ? snapshot.substring(0, snapshot.length() - "-SNAPSHOT".length())
                : snapshot;
    }

    static void versioningDemo() {
        System.out.println("=== versioning ===");
        System.out.println("compareVersions(\"1.10.0\", \"1.9.0\")     = "
                + compareVersions("1.10.0", "1.9.0")   + "  (numeric, not lexical)");
        System.out.println("compareVersions(\"1.0.0-SNAPSHOT\", \"1.0.0\") = "
                + compareVersions("1.0.0-SNAPSHOT", "1.0.0") + "  (snapshot < release)");
        System.out.println();
        System.out.println("bumpVersion(\"1.2.3\", MAJOR) = " + bumpVersion("1.2.3", BumpLevel.MAJOR));
        System.out.println("bumpVersion(\"1.2.3\", MINOR) = " + bumpVersion("1.2.3", BumpLevel.MINOR));
        System.out.println("bumpVersion(\"1.2.3\", PATCH) = " + bumpVersion("1.2.3", BumpLevel.PATCH));
        System.out.println("bumpVersion(\"1.2.3-SNAPSHOT\", PATCH) = "
                + bumpVersion("1.2.3-SNAPSHOT", BumpLevel.PATCH) + "  (suffix preserved)");
        System.out.println();
        System.out.println("releaseVersion(\"1.2.0-SNAPSHOT\") = " + releaseVersion("1.2.0-SNAPSHOT"));
    }

    // ── 5. CI-friendly versions ──────────────────────────────────────────────

    /**
     * The <b>CI-friendly version</b> pattern puts the project version into a
     * property called {@code revision} (and optional {@code sha1} and
     * {@code changelist}), so a single {@code -Drevision=...} on the command
     * line sets the version of every module in a multi-module build.
     */
    public static String ciFriendlySnippet() {
        return """
                <groupId>com.vbforge</groupId>
                <artifactId>my-app</artifactId>
                <version>${revision}</version>

                <properties>
                    <revision>1.0.0-SNAPSHOT</revision>
                    <sha1/>
                    <changelist/>
                </properties>
                """;
    }

    static void ciFriendlyVersionsDemo() {
        System.out.println("=== CI-friendly versions ===");
        System.out.println(ciFriendlySnippet());
        System.out.println("then: ./mvnw -Drevision=1.2.3 clean verify");
        System.out.println("only ${revision} (and optional ${sha1}, ${changelist}) are supported by Maven itself.");
        System.out.println("the property MUST be set explicitly (default) for the build to be reproducible.");
    }

    // ── 6. versions: plugin ──────────────────────────────────────────────────

    static void versionsPluginDemo() {
        System.out.println("=== versions: plugin ===");
        System.out.println("list newer versions on the classpath  : ./mvnw versions:display-dependency-updates");
        System.out.println("list newer versions of your plugins   : ./mvnw versions:display-plugin-updates");
        System.out.println("list newer parent versions            : ./mvnw versions:display-parent-updates");
        System.out.println("bump a property to the latest         : ./mvnw versions:update-properties");
        System.out.println("set the project version               : ./mvnw versions:set -DnewVersion=2.0.0");
        System.out.println("use versions:set -DprocessAllModules=true in a multi-module build.");
    }
}