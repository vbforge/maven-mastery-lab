package com.vbforge.profiles;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PropertiesProfilesVersionsTasks — implement every method below to make
 * {@code PropertiesProfilesVersionsTasksTest} go GREEN.
 *
 * <p><b>Workflow:</b>
 * <ol>
 *   <li>Read {@code PropertiesProfilesVersionsDemo.java} to understand the concept.</li>
 *   <li>Implement a method — remove the {@code UnsupportedOperationException}.</li>
 *   <li>When all 5 are done, remove the {@code @Disabled} annotation from
 *       {@code PropertiesProfilesVersionsTasksTest} and run
 *       {@code ./mvnw test -pl 05-properties-profiles-versions -Dtest=PropertiesProfilesVersionsTasksTest} ✅</li>
 * </ol>
 *
 * <p>Do not change method signatures or add external libraries — only the Java
 * standard library is needed.
 */
public class PropertiesProfilesVersionsTasks {

    // ── Task 1 ─────────────────────────────────────────────────────────────────

    /**
     * Substitutes {@code ${key}} placeholders in {@code text} with values from
     * {@code props}. Single-pass, no recursion.
     *
     * <p>Examples:
     * <pre>{@code
     *   interpolate("Hello ${name}!", {name=World})          → "Hello World!"
     *   interpolate("${a}-${b}", {a=1, b=2})                 → "1-2"
     *   interpolate("${missing}", {})                        → "${missing}"  (unchanged)
     *   interpolate("${known} ${missing}", {known=hi})       → "hi ${missing}"
     *   interpolate(null, {a=b})                             → null
     *   interpolate("no placeholders", null)                 → "no placeholders"
     *   interpolate("no placeholders", {})                   → "no placeholders"
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>{@link String#replace(CharSequence, CharSequence)} replaces <em>all</em>
     *       occurrences — that is what you want here.</li>
     *   <li>Skip properties whose key or value is {@code null}.</li>
     *   <li>Unknown placeholders are left as-is, not removed.</li>
     * </ul>
     *
     * @param text  the text containing {@code ${key}} placeholders (may be null)
     * @param props the substitutions (may be null or empty)
     * @return the interpolated text
     */
    public static String interpolate(String text, Map<String, String> props) {
        throw new UnsupportedOperationException("TODO: implement interpolate");
    }

    // ── Task 2 ─────────────────────────────────────────────────────────────────

    /**
     * Compares two Maven versions.
     *
     * <p>Rules:
     * <ul>
     *   <li>Split each version into a numeric part (before the first {@code -})
     *       and an optional qualifier (the rest).</li>
     *   <li>Numeric parts split on {@code .}; each segment is compared as an
     *       <b>integer</b>. A missing trailing segment is treated as {@code 0}.</li>
     *   <li>If numeric parts are equal:
     *       <ul>
     *         <li>a version <b>with</b> a qualifier is <b>less than</b> the same
     *             version without one (so {@code 1.0.0-SNAPSHOT < 1.0.0})</li>
     *         <li>two qualifiers compare lexically</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * <p>Examples:
     * <pre>{@code
     *   compareVersions("1.10.0", "1.9.0")            < 0 is FALSE: 1.10.0 > 1.9.0
     *   compareVersions("1.0.0", "1.0.0")             == 0
     *   compareVersions("2.0.0", "1.9.9")             > 0
     *   compareVersions("1.0.0-SNAPSHOT", "1.0.0")    < 0
     *   compareVersions("1.0.0", "1.0.0-SNAPSHOT")    > 0
     *   compareVersions("1.0.0-alpha", "1.0.0-beta")  < 0
     *   compareVersions("1.0", "1.0.0")               == 0  (missing segment == 0)
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>{@code "1.0".split("\\.")} gives {@code ["1", "0"]}; iterate over
     *       the longer array and use {@code 0} for the shorter side.</li>
     *   <li>Return {@link Integer#compare(int, int)} or a manual {@code -1/0/1}.</li>
     *   <li>Any {@code null} argument throws {@link IllegalArgumentException}.</li>
     * </ul>
     *
     * @param a first version
     * @param b second version
     * @return negative if {@code a < b}, zero if equal, positive if {@code a > b}
     * @throws IllegalArgumentException if either argument is null
     */
    public static int compareVersions(String a, String b) {
        throw new UnsupportedOperationException("TODO: implement compareVersions");
    }

    // ── Task 3 ─────────────────────────────────────────────────────────────────

    /**
     * Bumps a semantic version by one level:
     * <ul>
     *   <li>{@code MAJOR} — {@code X.Y.Z → (X+1).0.0}</li>
     *   <li>{@code MINOR} — {@code X.Y.Z → X.(Y+1).0}</li>
     *   <li>{@code PATCH} — {@code X.Y.Z → X.Y.(Z+1)}</li>
     * </ul>
     *
     * <p>Any {@code -qualifier} suffix is preserved: {@code 1.2.3-SNAPSHOT}
     * bumped at PATCH is {@code 1.2.4-SNAPSHOT}.
     *
     * <p>Examples:
     * <pre>{@code
     *   bumpVersion("1.2.3", "major")            → "2.0.0"
     *   bumpVersion("1.2.3", "minor")            → "1.3.0"
     *   bumpVersion("1.2.3", "patch")            → "1.2.4"
     *   bumpVersion("1.2.3-SNAPSHOT", "patch")   → "1.2.4-SNAPSHOT"
     *   bumpVersion("1.2.3-SNAPSHOT", "major")   → "2.0.0-SNAPSHOT"
     *   bumpVersion("1.0", "minor")              → "1.1.0"     (missing patch == 0)
     *   bumpVersion(null, "patch")               → IllegalArgumentException
     *   bumpVersion("", "patch")                 → IllegalArgumentException
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>Split on {@code "-"} with limit 2 to isolate the qualifier.</li>
     *   <li>Split the numeric part on {@code "\\."} — it may have 1, 2 or 3
     *       segments; use {@code 0} for missing ones.</li>
     *   <li>Rebuild as {@code major + "." + minor + "." + patch + suffix}.</li>
     *   <li>{@code level} is the enum {@link PropertiesProfilesVersionsDemo.BumpLevel};
     *       compare with {@code ==} or use a {@code switch}.</li>
     * </ul>
     *
     * @param version the version to bump (non-null, non-blank)
     * @param level   which segment to bump
     * @return the bumped version
     * @throws IllegalArgumentException if {@code version} is null or blank,
     *                                  or if {@code level} is null
     */
    public static String bumpVersion(String version,
                                     PropertiesProfilesVersionsDemo.BumpLevel level) {
        throw new UnsupportedOperationException("TODO: implement bumpVersion");
    }

    // ── Task 4 ─────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the given profile would be active in the given
     * environment.
     *
     * <p>The rules, evaluated in order — <b>any</b> match wins:
     * <ol>
     *   <li>{@code profile.id()} is in {@code env.cliActive()} (the {@code -P} set) → active.</li>
     *   <li>{@code profile.activeByDefault()} AND {@code env.cliActive()} is
     *       empty → active.</li>
     *   <li>{@code profile.jdk()} matches {@code env.jdk()} by <b>prefix</b>
     *       (so {@code "17"} matches {@code "17.0.2"}).</li>
     *   <li>{@code profile.osName()} equals {@code env.osName()}
     *       (case-sensitive).</li>
     *   <li>{@code profile.propertyName()} is set in {@code env.properties()}
     *       AND ({@code profile.propertyValue()} is {@code null} OR matches
     *       the value exactly).</li>
     * </ol>
     * Any {@code null} input returns {@code false} (except that
     * {@code cliActive} being {@code null} is treated as empty).
     *
     * <p>Examples:
     * <pre>{@code
     *   env = ActivationEnv.of("17.0.2", "Linux")
     *   isProfileActive(Profile.of("ci"), env)                                → false
     *   isProfileActive(Profile.defaultActive("dev"), env)                    → true
     *   isProfileActive(Profile.onJdk("jdk17", "17"), env)                    → true
     *   isProfileActive(Profile.onJdk("jdk8",  "1.8"), env)                   → false
     *   isProfileActive(Profile.onOs("linux", "Linux"), env)                  → true
     *
     *   env2 = new ActivationEnv("11", "Windows", {metrics=true}, Set.of("prod"))
     *   isProfileActive(Profile.of("prod"), env2)                             → true
     *   isProfileActive(Profile.defaultActive("dev"), env2)                   → false  (-P disables defaults)
     *   isProfileActive(Profile.onProperty("metrics","metrics","true"), env2) → true
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>Guard against {@code null} {@code profile} or {@code env} first.</li>
     *   <li>{@code String#startsWith} for the JDK prefix.</li>
     *   <li>For the property rule, use {@code env.properties().get(name)} and
     *       remember a {@code null} value means "not set" — do not confuse it
     *       with a value of {@code ""}.</li>
     * </ul>
     *
     * @param profile the profile to evaluate (may be null)
     * @param env     the environment (may be null)
     * @return {@code true} if the profile would be active
     */
    public static boolean isProfileActive(Profile profile, ActivationEnv env) {
        throw new UnsupportedOperationException("TODO: implement isProfileActive");
    }

    // ── Task 5 ─────────────────────────────────────────────────────────────────

    /**
     * Turns a SNAPSHOT version into its release equivalent by stripping the
     * {@code -SNAPSHOT} suffix.
     *
     * <p>Rules:
     * <ul>
     *   <li>If the input ends in {@code "-SNAPSHOT"} (uppercase, exact), remove
     *       just that suffix.</li>
     *   <li>Otherwise return the input unchanged.</li>
     *   <li>{@code null} → {@code null}.</li>
     *   <li>Empty string → empty string.</li>
     *   <li>Lowercase {@code "-snapshot"} is <b>not</b> a Maven SNAPSHOT — leave
     *       it as-is.</li>
     * </ul>
     *
     * <p>Examples:
     * <pre>{@code
     *   releaseVersion("1.2.0-SNAPSHOT")   → "1.2.0"
     *   releaseVersion("1.0.0-SNAPSHOT")   → "1.0.0"
     *   releaseVersion("1.2.0")            → "1.2.0"      (unchanged)
     *   releaseVersion("1.2.0-snapshot")   → "1.2.0-snapshot"  (unchanged)
     *   releaseVersion("SNAPSHOT")         → "SNAPSHOT"   (no leading version)
     *   releaseVersion("-SNAPSHOT")        → ""           (suffix removed)
     *   releaseVersion("")                 → ""
     *   releaseVersion(null)               → null
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>{@link String#endsWith} + {@link String#substring(int)} is enough.</li>
     *   <li>Do not trim or transform anything else.</li>
     * </ul>
     *
     * @param version the version string (may be null)
     * @return the release version
     */
    public static String releaseVersion(String version) {
        throw new UnsupportedOperationException("TODO: implement releaseVersion");
    }
}