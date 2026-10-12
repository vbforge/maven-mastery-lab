package com.vbforge.depmgmt;

import java.util.List;
import java.util.Map;

/**
 * DependencyManagementTasks — implement every method below to make
 * {@code DependencyManagementTasksTest} go GREEN.
 *
 * <p><b>Workflow:</b>
 * <ol>
 *   <li>Read {@code DependencyManagementDemo.java} to understand the concept.</li>
 *   <li>Implement a method — remove the {@code UnsupportedOperationException}.</li>
 *   <li>When all 5 are done, remove the {@code @Disabled} annotation from
 *       {@code DependencyManagementTasksTest} and run
 *       {@code ./mvnw test -pl 02-dependency-management -Dtest=DependencyManagementTasksTest} ✅</li>
 * </ol>
 *
 * <p>Maps in this class use the key format {@code groupId:artifactId}
 * (for example {@code "org.assertj:assertj-core"}) and map to a version string.
 * Do not change method signatures or add external libraries — only the Java
 * standard library is needed.
 */
public class DependencyManagementTasks {

    // ── Task 1 ─────────────────────────────────────────────────────────────────

    /**
     * Decides which version a dependency ends up with: the version written in
     * the dependency itself wins; otherwise the version from
     * {@code dependencyManagement}; otherwise Maven fails the build.
     *
     * <p>Examples:
     * <pre>{@code
     *   effectiveVersion("1.0.0", "2.0.0") → "1.0.0"   (explicit wins)
     *   effectiveVersion(null,    "2.0.0") → "2.0.0"   (managed)
     *   effectiveVersion("",      "2.0.0") → "2.0.0"   (blank counts as missing)
     *   effectiveVersion(null,    null)    → IllegalStateException
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>"Missing" means {@code null}, empty, or whitespace-only.</li>
     *   <li>Return the chosen string as-is.</li>
     * </ul>
     *
     * @param declared the version written in the dependency (may be missing)
     * @param managed  the version from dependencyManagement (may be missing)
     * @return the version Maven would use
     * @throws IllegalStateException if both are missing
     */
    public static String effectiveVersion(String declared, String managed) {
        throw new UnsupportedOperationException("TODO: implement effectiveVersion");
    }

    // ── Task 2 ─────────────────────────────────────────────────────────────────

    /**
     * Merges a parent's and a child's {@code dependencyManagement}. Both maps
     * use {@code groupId:artifactId} keys. Every entry from both appears in the
     * result; when both define the same key, the <b>child</b> wins.
     *
     * <p>Examples:
     * <pre>{@code
     *   parent = {a:x → 1, a:y → 2}
     *   child  = {a:y → 9, a:z → 3}
     *   result = {a:x → 1, a:y → 9, a:z → 3}
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>A {@code null} map is treated as empty.</li>
     *   <li>Return a <b>new</b> map; never modify the arguments.</li>
     *   <li>Copy the parent first, then {@code putAll} the child.</li>
     * </ul>
     *
     * @param parent the parent's managed versions
     * @param child  the child's managed versions
     * @return the merged, new map
     */
    public static Map<String, String> mergeManagement(Map<String, String> parent,
                                                      Map<String, String> child) {
        throw new UnsupportedOperationException("TODO: implement mergeManagement");
    }

    // ── Task 3 ─────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if a dependency declaration is a <b>BOM import</b>:
     * its {@code type} is exactly {@code "pom"} and its {@code scope} is exactly
     * {@code "import"}.
     *
     * <p>Examples:
     * <pre>{@code
     *   isBomImport("pom", "import")     → true
     *   isBomImport("jar", "import")     → false
     *   isBomImport("pom", "compile")    → false
     *   isBomImport("pom", null)         → false
     *   isBomImport(null,  null)         → false
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>Comparison is case-sensitive, as in Maven.</li>
     *   <li>Beware of {@code NullPointerException}: put the literal first,
     *       e.g. {@code "pom".equals(type)}.</li>
     * </ul>
     *
     * @param type  the dependency type (e.g. {@code "pom"}, {@code "jar"})
     * @param scope the dependency scope (e.g. {@code "import"}, {@code "test"})
     * @return {@code true} if this declaration imports a BOM
     */
    public static boolean isBomImport(String type, String scope) {
        throw new UnsupportedOperationException("TODO: implement isBomImport");
    }

    // ── Task 4 ─────────────────────────────────────────────────────────────────

    /**
     * Renders a {@code <dependency>} XML snippet. Elements appear in the order
     * {@code groupId, artifactId, version, scope}; indentation is 4 spaces per
     * level; lines are joined with {@code "\n"} and there is <b>no trailing
     * newline</b>.
     *
     * <p>Leave out what Maven does not need:
     * <ul>
     *   <li>{@code version} is omitted if {@code null} or blank (it comes from management)</li>
     *   <li>{@code scope} is omitted if {@code null}, blank, or {@code "compile"} (the default)</li>
     * </ul>
     *
     * <p>Examples:
     * <pre>{@code
     *   renderDependency("org.assertj", "assertj-core", "3.25.3", "test")
     *
     *   <dependency>
     *       <groupId>org.assertj</groupId>
     *       <artifactId>assertj-core</artifactId>
     *       <version>3.25.3</version>
     *       <scope>test</scope>
     *   </dependency>
     *
     *   renderDependency("org.assertj", "assertj-core", null, null)
     *
     *   <dependency>
     *       <groupId>org.assertj</groupId>
     *       <artifactId>assertj-core</artifactId>
     *   </dependency>
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>Build a {@code List<String>} of lines, then {@code String.join("\n", lines)}.</li>
     *   <li>{@code groupId} and {@code artifactId} are mandatory.</li>
     * </ul>
     *
     * @param groupId    the group coordinate
     * @param artifactId the artifact coordinate
     * @param version    the version, or {@code null} when managed elsewhere
     * @param scope      the scope, or {@code null} for the default
     * @return the XML snippet
     * @throws IllegalArgumentException if groupId or artifactId is null or blank
     */
    public static String renderDependency(String groupId, String artifactId,
                                          String version, String scope) {
        throw new UnsupportedOperationException("TODO: implement renderDependency");
    }

    // ── Task 5 ─────────────────────────────────────────────────────────────────

    /**
     * Finds declarations that Maven could not build: dependencies with no
     * explicit version <b>and</b> no managed version.
     *
     * <p>Each declared entry is either {@code "groupId:artifactId"} (no version)
     * or {@code "groupId:artifactId:version"} (explicit version). {@code managed}
     * maps {@code groupId:artifactId} to a version.
     *
     * <p>Return the {@code groupId:artifactId} of every problem declaration, in
     * the order they were declared.
     *
     * <p>Examples:
     * <pre>{@code
     *   declared = ["org.assertj:assertj-core", "org.slf4j:slf4j-api"]
     *   managed  = {"org.assertj:assertj-core" → "3.25.3"}
     *   result   = ["org.slf4j:slf4j-api"]
     *
     *   declared = ["org.slf4j:slf4j-api:2.0.9"]     (explicit version)
     *   managed  = {}
     *   result   = []
     * }</pre>
     *
     * <p>Hints:
     * <ul>
     *   <li>{@code split(":")} gives 2 parts (no version) or 3 parts (explicit).</li>
     *   <li>A managed entry whose value is {@code null} or blank does not count as managed.</li>
     *   <li>A {@code null} list or map is treated as empty.</li>
     * </ul>
     *
     * @param declared the declared dependencies
     * @param managed  the managed versions
     * @return the declarations without any version, in declaration order
     */
    public static List<String> unmanagedDependencies(List<String> declared,
                                                     Map<String, String> managed) {
        throw new UnsupportedOperationException("TODO: implement unmanagedDependencies");
    }
}