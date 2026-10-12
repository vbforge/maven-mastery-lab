package com.vbforge.depmgmt;

import java.util.List;

/**
 * DependencyManagementDemo — annotated reference for managing versions in Maven.
 *
 * <p>Read top-to-bottom. Each section covers one building block and explains
 * <em>why</em> it exists — not just what the XML looks like.
 * This class is self-contained: it has no dependency on the exercises.
 *
 * <p>Topics:
 * <ol>
 *   <li>{@code dependencies} vs {@code dependencyManagement}</li>
 *   <li>Parent POM inheritance</li>
 *   <li>BOM import</li>
 *   <li>{@code pluginManagement}</li>
 *   <li>Version properties</li>
 *   <li>{@code help:effective-pom}</li>
 * </ol>
 *
 * <p>Run: {@code ./mvnw -q exec:java -pl 02-dependency-management
 * -Dexec.mainClass="com.vbforge.maven_mastery.depmgmt.DependencyManagementDemo"}
 */
public class DependencyManagementDemo {

    public static void main(String[] args) {
        dependenciesVsManagementDemo();
        System.out.println("---");
        parentInheritanceDemo();
        System.out.println("---");
        bomImportDemo();
        System.out.println("---");
        pluginManagementDemo();
        System.out.println("---");
        versionPropertiesDemo();
        System.out.println("---");
        effectivePomDemo();
    }

    // ── 1. dependencies vs dependencyManagement ───────────────────────────────

    /**
     * Two blocks look almost identical in the pom, but do very different things.
     *
     * <p>WHY this matters: in a multi-module project you want every module to use
     * the <em>same</em> version of a library. {@code dependencyManagement} lets you
     * state that version once; modules then say "I need this library" without
     * repeating (and drifting from) the version.
     *
     * <p>The most common mistake: putting a library only into
     * {@code dependencyManagement} and wondering why it is not on the classpath.
     */
    enum DeclarationBlock {
        DEPENDENCIES(
                "<dependencies>", true,
                "adds the artifact to the classpath"),
        DEPENDENCY_MANAGEMENT(
                "<dependencyManagement>", false,
                "only pins version / scope / exclusions for artifacts declared elsewhere");

        private final String tag;
        private final boolean addsToClasspath;
        private final String effect;

        DeclarationBlock(String tag, boolean addsToClasspath, String effect) {
            this.tag = tag;
            this.addsToClasspath = addsToClasspath;
            this.effect = effect;
        }

        String tag()                { return tag; }
        boolean addsToClasspath()   { return addsToClasspath; }
        String effect()             { return effect; }
    }

    static void dependenciesVsManagementDemo() {
        System.out.println("=== dependencies vs dependencyManagement ===");

        for (DeclarationBlock block : DeclarationBlock.values()) {
            System.out.printf("%-24s addsToClasspath=%-5s %s%n",
                    block.tag(), block.addsToClasspath(), block.effect());
        }
        System.out.println();
        System.out.println("Think of dependencyManagement as a price list, and dependencies as the shopping cart.");
    }

    // ── 2. Parent POM inheritance ─────────────────────────────────────────────

    /**
     * A child pom inherits everything from its {@code <parent>}: properties,
     * {@code dependencyManagement}, {@code pluginManagement}, and so on.
     * A parent used this way has {@code <packaging>pom</packaging>}.
     *
     * <p>WHY this matters: the child declares {@code groupId:artifactId} only.
     * <b>Both the version and the scope</b> come from the parent's management
     * block, so upgrading a library is a one-line change in one file.
     *
     * <p>Note: a <em>parent</em> (inheritance) and an <em>aggregator</em>
     * ({@code <modules>}) are different things; the root pom of this repo is both.
     */
    static String parentPom() {
        return """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>

                    <groupId>com.vbforge</groupId>
                    <artifactId>maven-mastery-lab</artifactId>
                    <version>1.0.0-SNAPSHOT</version>
                    <packaging>pom</packaging>

                    <properties>
                        <assertj.version>3.25.3</assertj.version>
                    </properties>

                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.assertj</groupId>
                                <artifactId>assertj-core</artifactId>
                                <version>${assertj.version}</version>
                                <scope>test</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """;
    }

    static String childPom() {
        return """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>

                    <parent>
                        <groupId>com.vbforge</groupId>
                        <artifactId>maven-mastery-lab</artifactId>
                        <version>1.0.0-SNAPSHOT</version>
                    </parent>

                    <artifactId>dependency-management</artifactId>

                    <dependencies>
                        <dependency>
                            <groupId>org.assertj</groupId>
                            <artifactId>assertj-core</artifactId>
                            <!-- no version, no scope: both come from the parent -->
                        </dependency>
                    </dependencies>
                </project>
                """;
    }

    static void parentInheritanceDemo() {
        System.out.println("=== Parent inheritance ===");
        System.out.println("parent pom:");
        System.out.println(parentPom());
        System.out.println("child pom:");
        System.out.println(childPom());
        System.out.println("The child gets assertj-core 3.25.3 with scope 'test' without stating either.");
    }

    // ── 3. BOM import ─────────────────────────────────────────────────────────

    /**
     * A <b>BOM</b> (Bill of Materials) is a pom that contains only a
     * {@code dependencyManagement} section: a ready-made list of compatible
     * versions. You <em>import</em> it with {@code <type>pom</type>} and
     * {@code <scope>import</scope>}.
     *
     * <p>WHY this matters: a project can have only <em>one</em> parent, but it
     * can import <em>many</em> BOMs. That is why Spring Boot, JUnit and cloud
     * vendors publish BOMs — you get tested, compatible versions in one block.
     *
     * <p>If the same artifact is managed in several places, the first match wins
     * in this order (verify it yourself with {@code help:effective-pom}):
     * <ol>
     *   <li>explicit {@code <version>} in the dependency</li>
     *   <li>the pom's own {@code dependencyManagement}</li>
     *   <li>the parent's {@code dependencyManagement} (inherited)</li>
     *   <li>imported BOMs — if two BOMs manage the same artifact, the first import wins</li>
     * </ol>
     */
    enum VersionSource {
        EXPLICIT_IN_DEPENDENCY("a <version> written in the dependency itself"),
        OWN_DEPENDENCY_MANAGEMENT("this pom's own <dependencyManagement>"),
        PARENT_DEPENDENCY_MANAGEMENT("<dependencyManagement> inherited from the parent"),
        IMPORTED_BOM("an imported BOM (scope import)");

        private final String description;

        VersionSource(String description) { this.description = description; }

        String description() { return description; }
    }

    static List<VersionSource> versionPrecedence() {
        return List.of(VersionSource.values());
    }

    static String bomImportSnippet() {
        return """
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.junit</groupId>
                            <artifactId>junit-bom</artifactId>
                            <version>5.10.2</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                """;
    }

    static void bomImportDemo() {
        System.out.println("=== BOM import ===");
        System.out.println(bomImportSnippet());
        System.out.println("version precedence (highest first):");
        int rank = 1;
        for (VersionSource source : versionPrecedence()) {
            System.out.printf("  %d. %s%n", rank++, source.description());
        }
    }

    // ── 4. pluginManagement ───────────────────────────────────────────────────

    /**
     * {@code pluginManagement} is {@code dependencyManagement} for plugins: it
     * pins a plugin's version and configuration without running the plugin.
     *
     * <p>WHY this matters: builds stay reproducible only when plugin versions are
     * pinned. Plugins bound by the default lifecycle (compiler, surefire, jar)
     * pick up managed versions and configuration automatically, even if a child
     * never mentions them.
     */
    static String pluginManagementParentSnippet() {
        return """
                <build>
                    <pluginManagement>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>3.13.0</version>
                                <configuration>
                                    <release>${java.version}</release>
                                </configuration>
                            </plugin>
                        </plugins>
                    </pluginManagement>
                </build>
                """;
    }

    static String pluginManagementChildSnippet() {
        return """
                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <!-- version and configuration inherited from the parent -->
                        </plugin>
                    </plugins>
                </build>
                """;
    }

    static void pluginManagementDemo() {
        System.out.println("=== pluginManagement ===");
        System.out.println("parent:");
        System.out.println(pluginManagementParentSnippet());
        System.out.println("child:");
        System.out.println(pluginManagementChildSnippet());
    }

    // ── 5. Version properties ─────────────────────────────────────────────────

    /**
     * A version written as {@code ${assertj.version}} refers to a property.
     * The property is defined once and can be overridden by a child pom or from
     * the command line ({@code -Dassertj.version=...}, which wins).
     *
     * <p>WHY this matters: one number controls many places (for example, several
     * artifacts of one library family), and you can try another version without
     * editing the pom.
     */
    static String versionOverrideSnippet() {
        return """
                <properties>
                    <assertj.version>3.24.2</assertj.version>
                </properties>
                """;
    }

    static void versionPropertiesDemo() {
        System.out.println("=== Version properties ===");
        System.out.println("parent defines <assertj.version>3.25.3</assertj.version>; a child overrides it:");
        System.out.println(versionOverrideSnippet());
        System.out.println("or from the CLI (wins over every pom):");
        System.out.println("  ./mvnw dependency:tree -Dassertj.version=3.23.1");
    }

    // ── 6. effective-pom ──────────────────────────────────────────────────────

    /**
     * The pom you write is only a <em>fragment</em>. Maven merges it with its
     * parents, imported BOMs, active profiles and the super POM into the
     * <b>effective pom</b>.
     *
     * <p>WHY this matters: when "where does this version come from?" is the
     * question, the effective pom is the single source of truth.
     */
    static void effectivePomDemo() {
        System.out.println("=== effective-pom ===");
        System.out.println("see the fully merged pom      : ./mvnw help:effective-pom");
        System.out.println("write it to a file            : ./mvnw help:effective-pom -Doutput=effective-pom.xml");
        System.out.println("see the resulting classpath   : ./mvnw dependency:tree");
        System.out.println("filter the tree for a library : ./mvnw dependency:tree -Dincludes=org.assertj");
    }
}