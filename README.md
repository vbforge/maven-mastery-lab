# maven-mastery-lab

[![build](https://github.com/vbforge/maven-mastery-lab/actions/workflows/build.yml/badge.svg)](https://github.com/vbforge/maven-mastery-lab/actions/workflows/build.yml)
[![Java](https://img.shields.io/badge/Java-17+-blue?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Maven Wrapper](https://img.shields.io/badge/Maven%20Wrapper-3.9.6-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/wrapper/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> A hands-on lab that takes you from "I can run `mvn install`" to "I can design, debug and explain a real multi-module Maven build".
> **Java 17+** | **Maven 3.9+ (via wrapper)** | Package root: `com.vbforge`

---

## Goal

Be able to answer, **and demonstrate**, every Maven question a Junior → Middle Java developer gets in an interview or on a real project:

- What does this `../../pom.xml` do, and why?
- Why is this dependency on my classpath (or missing from it)?
- Why did my build pick *that* version?
- How do I structure a multi-module project, a BOM, profiles, a CI build?

Every topic is learned **three ways**: read it (`Demo`), implement it (`Tasks`), prove it (`Test`) — then touch the real tool in a small **lab**.

---

## How each module works

Every module has the same four classes, so you always know where to look:

| File | Role | State |
|---|---|---|
| `*Demo.java` | Teaching class with `main()`. Annotated, self-contained, **no tasks**. Run it and read top-to-bottom. | complete |
| `*DemoTest.java` | Tests that lock down what the demo teaches. | always 🟢 |
| `*Tasks.java` | Stubs (`throw new UnsupportedOperationException`). **You** implement them — standard library only. | 🔴 → 🟢 |
| `*TasksTest.java` | Ready-made tests for the tasks. Class is `@Disabled` until you finish all tasks. | ⏸ → 🟢 |

Plus per module:

- `../../README.md` — the source of truth: what's covered, tasks table, key things to remember, how to run.
- `LAB.md` — 3–6 hands-on exercises with the real `mvn` CLI (things a unit test cannot show: `dependency:tree`, `help:effective-pom`, reactor logs…).

### The loop (per module)

1. Read the module `../../README.md`.
2. Run the Demo, read `*Demo.java` top-to-bottom.
3. Implement tasks one by one in `*Tasks.java`.
4. Remove `@Disabled` from `*TasksTest.java` → all green.
5. Do the `LAB.md` exercises and write your observations in the lab file.

---

## Repository structure (the full potential playing field)

```
maven-mastery-lab/
├── README.md                          ← this file: the whole story
├── pom.xml                            ← root parent + aggregator (packaging pom)
├── mvnw, mvnw.cmd                     ← Maven Wrapper (pinned Maven)
├── .mvn/
│   └── wrapper/maven-wrapper.properties
├── .gitignore
├── .github/workflows/build.yml        ← CI: ./mvnw -B verify (added in Phase 3)
│
├── docs/
│   ├── cheatsheet.md                  ← commands, lifecycle, scopes on one page
│   ├── interview-questions.md         ← questions collected per module + your answers
│   └── glossary.md                    ← GAV, reactor, BOM, mediation, SNAPSHOT…
│
├── 01-maven-recap/                    
│   ├── pom.xml
│   ├── README.md
│   ├── LAB.md
│   └── src/
│       ├── main/java/com/vbforge/maven_recap/
│       │   ├── MavenDemo.java
│       │   └── MavenTasks.java
│       └── test/java/com/vbforge/maven_recap/
│           ├── MavenDemoTest.java
│           └── MavenTasksTest.java
│
├── 02-dependency-management/          
│   ├── pom.xml  README.md  LAB.md
│   └── src/{main,test}/java/com/vbforge/depmgmt/
│       └── DependencyManagementDemo / Tasks (+ tests)
│
├── 03-transitive-resolution/
│   ├── pom.xml  README.md  LAB.md
│   └── src/{main,test}/java/com/vbforge/resolution/
│
├── 04-repositories-settings/
│   ├── pom.xml  README.md  LAB.md
│   └── src/{main,test}/java/com/vbforge/repositories/
│
├── 05-properties-profiles-versions/
│   ├── pom.xml  README.md  LAB.md
│   └── src/{main,test}/java/com/vbforge/profiles/
│
├── 06-multi-module-reactor/
│   ├── pom.xml  README.md  LAB.md
│   ├── src/{main,test}/java/com/vbforge/reactor/
│   └── sample-app/                    ← a real mini multi-module build to experiment on
│       ├── pom.xml                    (parent + aggregator)
│       ├── sample-core/
│       ├── sample-service/
│       └── sample-app/
│
├── 07-cli-and-ci-in-practice/
│   ├── pom.xml  README.md  LAB.md
│   └── src/{main,test}/java/com/vbforge/cli/
│
└── 08-capstone/                       
    ├── README.md                      ← checklist + before/after notes
    └── (links to the real project migrate)
```

> Directory names are numbered for order.

### Root `../../pom.xml` (example for this repo)

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.vbforge</groupId>
    <artifactId>maven-mastery-lab</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>          <!-- aggregator + parent -->

    <properties>
        <java.version>17</java.version>
        <maven.compiler.release>${java.version}</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <modules>
        <module>01-maven-recap</module>
        <!-- uncomment one at a time (WIP = 1) -->
        <!-- <module>02-dependency-management</module> -->
        <!-- <module>03-transitive-resolution</module> -->
        <!-- <module>04-repositories-settings</module> -->
        <!-- <module>05-properties-profiles-versions</module> -->
        <!-- <module>06-multi-module-reactor</module> -->
        <!-- <module>07-cli-and-ci-in-practice</module> -->
    </modules>

    <dependencyManagement>              <!-- versions live HERE, once -->
        <dependencies>
            <dependency>
                <groupId>org.junit</groupId>
                <artifactId>junit-bom</artifactId>
                <version>5.10.2</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.assertj</groupId>
                <artifactId>assertj-core</artifactId>
                <version>3.25.3</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

---

## Roadmap

| Module                                       | Topic                                                         |
|----------------------------------------------|---------------------------------------------------------------|
| [`01-maven-recap`](01-maven-recap/README.md) | pom, coordinates, scopes, lifecycle, plugins, wrapper         |
| `02-dependency-management`                   | `dependencyManagement`, parent POM, BOM, `pluginManagement`   |
| `03-transitive-resolution`                   | nearest-wins, exclusions, scope mediation, conflict diagnosis |
| `04-repositories-settings`                   | `~/.m2`, Central, mirrors, `settings.xml`, SNAPSHOT repos     |
| `05-properties-profiles-versions`            | properties, profiles, SNAPSHOT vs release, versioning         |
| `06-multi-module-reactor`                    | aggregator vs parent, reactor order, `-pl`/`-am`              |
| `07-cli-and-ci-in-practice`                  | day-to-day commands, surefire vs failsafe, CI                 |
| `08-capstone`                                | migrate one of your own projects to a clean Maven build       |

---

## Modules descriptions

### 01 · `maven-recap`

**Covers**

| Topic | Key points |
|---|---|
| `../../pom.xml` | Project Object Model; `modelVersion` always `4.0.0`; convention over configuration |
| Coordinates | `groupId` / `artifactId` / `version` / `packaging`; `groupId:artifactId:version` format |
| Dependency scopes | `compile`, `provided`, `runtime`, `test`, `system`; only `compile` and `runtime` are transitive |
| Lifecycle | `validate → compile → test → package → verify → install → deploy`; each phase runs all upstream phases |
| Plugins | goals bound to phases; default bindings for compiler, surefire, jar, install, deploy |
| Wrapper | `mvnw` / `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`; pinned Maven per repo |

**Tasks** (`MavenTasks.java`)

| # | Method | Topic |
|---|---|---|
| 1 | `toCoordinateString(groupId, artifactId, version)` | GAV string with validation |
| 2 | `isValidArtifactId(id)` | naming rules — lowercase, starts with a letter |
| 3 | `isTransitiveScope(scope)` | which scopes propagate to consumers |
| 4 | `nextPhase(current)` | lifecycle progression, `null` at the end |
| 5 | `defaultLifecycle()` | the 7 phases in order |
| 6 | `distributionUrl(mavenVersion)` | wrapper URL for a given Maven version |

**Lab:** create a jar project by hand · add JUnit as `test` and prove it's absent from the jar · run each lifecycle phase separately and read the plugin goals in the log · add the wrapper and build with `./mvnw`.

**Done when:** you can explain, without notes, why `mvn package` also ran your tests and where `target/` content comes from.

---

### 02 · `dependency-management`

**Covers:** `dependencies` vs `dependencyManagement` · version inheritance from a parent POM · importing a BOM (`<type>pom</type><scope>import</scope>`) · `pluginManagement` · version properties (`${junit.version}`) · `mvn help:effective-pom`.

**Tasks**

| # | Method | Topic |
|---|---|---|
| 1 | `effectiveVersion(declared, managed)` | explicit version wins, else managed, else error |
| 2 | `mergeManagement(parent, child)` | child entries override parent entries |
| 3 | `isBomImport(type, scope)` | recognise an imported BOM |
| 4 | `renderDependency(g, a, v, scope)` | produce a correct `<dependency>` snippet (omit defaults) |
| 5 | `unmanagedDependencies(declared, managed)` | which declarations have no version anywhere |

**Lab:** build a parent with a BOM import · child modules declare deps without versions · compare `effective-pom` before/after.

### 03 · `transitive-resolution`

**Covers:** transitive dependencies · **nearest-wins** + first-declaration tie-break · scope mediation table · `<exclusions>` · `mvn dependency:tree [-Dverbose]` · `dependency:analyze` · Enforcer `dependencyConvergence`.

**Tasks** (on a small in-code dependency graph model)

| # | Method | Topic |
|---|---|---|
| 1 | `depthOf(graph, artifact)` | distance from the root project |
| 2 | `resolveVersion(graph, artifact)` | nearest-wins, tie → first declared |
| 3 | `mediateScope(parentScope, childScope)` | resulting scope of a transitive dependency |
| 4 | `applyExclusions(graph, exclusions)` | prune excluded subtrees |
| 5 | `findConflicts(graph)` | artifacts requested with more than one version |

**Lab:** reproduce a version conflict with two libraries · diagnose with `dependency:tree -Dverbose` · fix it three ways (declare directly, `dependencyManagement`, exclusion) and note the trade-offs.

### 04 · `repositories-settings`

**Covers:** local repo `~/.m2/repository` · Maven Central · remote repositories · mirrors · `settings.xml` (servers, mirrors, profiles) · SNAPSHOT vs release repos · offline mode (`-o`) · forcing updates (`-U`).

**Tasks**

| # | Method | Topic |
|---|---|---|
| 1 | `localRepoPath(g, a, v)` | path of an artifact inside `~/.m2` |
| 2 | `remoteUrl(base, g, a, v, ext)` | URL of an artifact in a remote repo |
| 3 | `selectMirror(repoId, mirrors)` | which mirror applies (`*`, exact id, `external:*`) |
| 4 | `isSnapshot(version)` | SNAPSHOT detection |
| 5 | `resolutionOrder(...)` | local → mirror/remote lookup order |

**Lab:** inspect the folder layout of a downloaded artifact · `mvn dependency:get` · break the network and use `-o` · `install` a jar and consume it from a second project.

### 05 · `properties-profiles-versions`

**Covers:** custom and built-in properties (`${project.version}`) · resource filtering · profiles and activation (`-P`, `activeByDefault`, property, JDK, OS) · SNAPSHOT vs release · semantic versioning · CI-friendly versions (`${revision}`) · `versions:` plugin.

**Tasks**

| # | Method | Topic |
|---|---|---|
| 1 | `interpolate(text, props)` | replace `${key}` placeholders |
| 2 | `compareVersions(a, b)` | numeric-aware version ordering (`1.10 > 1.9`) |
| 3 | `bumpVersion(version, level)` | major / minor / patch bump |
| 4 | `isProfileActive(profile, env)` | activation rules |
| 5 | `releaseVersion(snapshot)` | `1.2.0-SNAPSHOT` → `1.2.0` |

**Lab:** a `dev` / `prod` profile switching a property · filter it into `application.properties` · `mvn -Pprod help:active-profiles` · `versions:display-dependency-updates`.

### 06 · `multi-module-reactor`

**Covers:** aggregator vs parent (not the same thing!) · `<modules>` · the **reactor** and its build order · inter-module dependencies · `-pl`, `-am`, `-amd`, `-rf` · `${revision}` for one version across modules · avoiding cycles.

**Tasks**

| # | Method | Topic |
|---|---|---|
| 1 | `buildOrder(modules)` | topological sort of modules by dependencies |
| 2 | `detectCycle(modules)` | reject circular module dependencies |
| 3 | `alsoMake(target, modules)` | what `-am` pulls in |
| 4 | `alsoMakeDependents(target, modules)` | what `-amd` pulls in |
| 5 | `isAggregatorOnly(pom)` | aggregator vs parent classification |

**Lab:** use `sample-app/` · read the "Reactor Build Order" in the log · build only one module with `-pl sample-service -am` · introduce a cycle on purpose and read the error.

### 07 · `cli-and-ci-in-practice`

**Covers:** `verify` vs `install` · `-DskipTests` vs `-Dmaven.test.skip=true` · `-T` parallel builds · `-q`, `-X`, `-B` · surefire (unit) vs failsafe (integration) · `spring-boot-maven-plugin` repackage · running Maven in Docker and CI (GitHub Actions / Jenkins) · caching `~/.m2`.

**Tasks**

| # | Method | Topic |
|---|---|---|
| 1 | `buildCommand(goals, options)` | assemble a valid `mvn` command line |
| 2 | `phasesRun(command)` | which phases a command executes |
| 3 | `skipsCompilingTests(flags)` | difference between the two skip flags |
| 4 | `classifyTest(className)` | `*Test` → surefire, `*IT` → failsafe |
| 5 | `ciCommand(options)` | non-interactive CI-safe command (`-B`, `-ntp`, wrapper) |

**Lab:** split unit and integration tests with failsafe · add `.github/workflows/build.yml` running `./mvnw -B verify` · cache `~/.m2` and compare build times.

---

### 08 · `capstone`

Pick **one real project of your own** and give it a production-grade Maven build. Record before/after in `08-capstone/README.md`.

- [ ] Parent POM with `dependencyManagement` and `pluginManagement`
- [ ] BOM imported instead of hand-pinned versions
- [ ] Correct scopes everywhere (nothing test-only on the compile classpath)
- [ ] `dependency:analyze` clean (no unused-declared / used-undeclared)
- [ ] `dev` / `prod` profiles where they genuinely help
- [ ] Wrapper committed, Maven version pinned
- [ ] Unit tests on surefire, integration tests on failsafe
- [ ] CI runs `./mvnw -B verify` on every push
- [ ] Short write-up: what was wrong, what you changed, what you learned

--- 

## DONE status verification:

**Definition of done (per module):** demo read · all tasks implemented · all tests green · lab written up · 
3+ interview questions added to `docs/interview-questions.md`.

---

## Quick start

```bash
git clone https://github.com/vbforge/maven-mastery-lab.git
cd maven-mastery-lab

# build everything that is enabled in the root pom
./mvnw clean verify

# one module only
./mvnw test -pl 01-maven-recap

# run a module's demo
./mvnw -q exec:java -pl 01-maven-recap \
    -Dexec.mainClass="com.vbforge.maven_recap.MavenDemo"

# only the tasks of a module (after removing @Disabled)
./mvnw test -pl 01-maven-recap -Dtest=MavenTasksTest
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

---

## Interview checklist

- [ ] Explain the three built-in lifecycles and what `mvn install` actually runs
- [ ] Difference between `compile`, `provided`, `runtime`, `test` scopes — with an example each
- [ ] `dependencies` vs `dependencyManagement`
- [ ] How Maven resolves a version conflict (nearest-wins) and how you override it
- [ ] What a BOM is and why Spring Boot uses one
- [ ] Aggregator vs parent POM
- [ ] What `SNAPSHOT` means and how it differs from a release
- [ ] `verify` vs `install`; `-DskipTests` vs `-Dmaven.test.skip`
- [ ] Why use the Maven Wrapper
- [ ] How you would diagnose "ClassNotFoundException / NoSuchMethodError" caused by a dependency clash

---

## License

`@vbforge`

MIT — learn from it, fork it, build your own lab.

---

