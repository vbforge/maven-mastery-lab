# cheatsheet

> One-page reference for the whole lab. If it isn't here, it's in a module README.
> Module numbers in brackets point to where a topic is taught.

---

## Coordinates — `groupId:artifactId:version`

```
com.vbforge:maven-mastery-lab:1.0.0-SNAPSHOT
 └───┬────┘ └──────┬───────┘  └──────┬─────┘
  groupId      artifactId         version
```

- `groupId` — reverse domain, lowercase
- `artifactId` — lowercase, kebab-case, starts with a letter
- `version` — `major.minor.patch`, `-SNAPSHOT` for dev
- `packaging` — `jar` (default), `war`, `pom`, … [01]

---

## Scopes

| Scope | Compile CP | Test CP | Runtime CP | Transitive | Example |
|---|---|---|---|---|---|
| `compile` (default) | ✅ | ✅ | ✅ | ✅ | `commons-lang3` |
| `provided` | ✅ | ✅ | ❌ | ❌ | `jakarta.servlet-api` |
| `runtime` | ❌ | ✅ | ✅ | ✅ | `postgresql` driver |
| `test` | ❌ | ✅ | ❌ | ❌ | `junit-jupiter` |
| `system` | ✅ | ✅ | ❌ | ❌ | local jar path (avoid) |

**Only `compile` and `runtime` propagate to consumers.** [01, 03]

---

## Lifecycles

**Three independent chains.** Running a phase runs every phase before it in *that* chain.

```
clean    : pre-clean → clean → post-clean
default  : validate → compile → test → package → verify → install → deploy
site     : pre-site → site → post-site → site-deploy
```

| Command | Runs |
|---|---|
| `mvn validate` | `validate` |
| `mvn compile` | `validate → compile` |
| `mvn test` | `validate → compile → test` |
| `mvn package` | `validate → compile → test → package` |
| `mvn verify` | `validate → compile → test → package → verify` |
| `mvn install` | `… → verify → install` |
| `mvn deploy` | `… → install → deploy` |

`mvn clean install` = run `clean` lifecycle, then default up to `install`. [01]

---

## Default plugin bindings (jar packaging)

| Phase | Plugin goal |
|---|---|
| `process-resources` | `maven-resources-plugin:resources` |
| `compile` | `maven-compiler-plugin:compile` |
| `process-test-resources` | `maven-resources-plugin:testResources` |
| `test-compile` | `maven-compiler-plugin:testCompile` |
| `test` | `maven-surefire-plugin:test` |
| `package` | `maven-jar-plugin:jar` |
| `verify` | (no default) |
| `install` | `maven-install-plugin:install` |
| `deploy` | `maven-deploy-plugin:deploy` |

---

## dependencies vs dependencyManagement

```xml
<!-- parent: declares the version ONCE -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>3.25.3</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- child: adds to classpath, inherits version + scope -->
<dependencies>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
    </dependency>
</dependencies>
```

- `dependencies` **adds to classpath**
- `dependencyManagement` **only pins** — adds nothing by itself
- A `null` child declares → nothing lands. Explicit `<version>` in child wins. [02]

---

## BOM import

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.junit</groupId>
            <artifactId>junit-bom</artifactId>
            <version>5.10.2</version>
            <type>pom</type>       <!-- this artifact is a pom -->
            <scope>import</scope>  <!-- merge its dependencyManagement -->
        </dependency>
    </dependencies>
</dependencyManagement>
```

- One parent per project, **many BOMs**.
- Own entries win over the imported BOM.
- Own `dependencyManagement` beats parent's.
- Parent's beats imported BOM's. [02]

---

## Version precedence (highest first)

1. Explicit `<version>` in the dependency
2. The consuming pom's own `dependencyManagement`
3. The parent's `dependencyManagement` (inherited)
4. Imported BOMs (first import wins)
5. Transitive resolution (nearest-wins, tie → first declared) [02, 03]

---

## pluginManagement vs plugins

- `pluginManagement` **declares** version + config, **runs nothing**
- `plugins` **activates** the plugin (or adds executions)

**A plugin bound to the lifecycle by default still runs even if you don't declare it in `<plugins>`** — the default binding invokes it, and `pluginManagement` supplies the config. That's why `maven-compiler-plugin` "disappears" from a child and still compiles with the parent's pinned version. [02]

---

## Transitive resolution

**Nearest-wins**, tie → first declared.

```
root
├── a
│   └── shared:1.0.0        ← depth 2
└── b
    └── shared:2.0.0        ← depth 2, tie → FIRST wins (1.0.0)
```

**Only `compile` and `runtime` propagate.** `provided` and `test` never reach consumers.

### Scope mediation

If your project has `B` at scope **P**, and `B` has `C` at scope **Q**, then `C` is visible to you at:

| P ↓ / Q → | `compile` | `provided` | `runtime` | `test` |
|---|---|---|---|---|
| **`compile`** | compile | — | runtime | — |
| **`provided`** | provided | — | provided | — |
| **`runtime`** | runtime | — | runtime | — |
| **`test`** | test | — | test | — |

`—` means **dropped**: the artifact is not pulled into your classpath at all. [03]

### Exclusions vs optional

- `<exclusions>` — what you **refuse to receive** from a dependency (per-dependency)
- `<optional>true</optional>` — what you **refuse to give** to consumers [03]

---

## Repositories

**Lookup order for every artifact:**

1. Local: `~/.m2/repository`
2. Mirror (if a `mirrorOf` pattern matches the repo id)
3. The repository's own URL
4. Fail

**Layout** (same local and remote):

```
<base>/<groupId-with-slashes>/<artifactId>/<version>/<artifactId>-<version>.<ext>
```

Central: `https://repo.maven.apache.org/maven2` [04]

### `mirrorOf` patterns

| Pattern | Matches |
|---|---|
| `*` | every repo |
| `external:*` | every repo except `localhost` / `file://` |
| `central` | exactly the repo id `central` |
| `a,b,c` | any of these ids |
| `*,!internal` | everything except `internal` (negation wins) |

First matching mirror wins. [04]

### SNAPSHOT

- Version suffix is **case-sensitive**: `-SNAPSHOT` (uppercase).
- Mutable; every deploy overwrites the GAV.
- Central has snapshots **disabled**; snapshot repos are private.
- Force a re-check with `-U`; force offline with `-o` (`-o` wins if both are given). [04]

---

## Properties

```xml
<properties>
    <assertj.version>3.25.3</assertj.version>
    <maven.compiler.release>17</maven.compiler.release>
</properties>
```

**Built-ins Maven provides:** `${project.version}`, `${project.groupId}`, `${project.artifactId}`, `${project.basedir}`, `${project.build.directory}`. [05]

**Resource filtering** substitutes `${...}` inside `src/main/resources`:

```xml
<build>
    <resources>
        <resource>
            <directory>src/main/resources</directory>
            <filtering>true</filtering>
        </resource>
    </resources>
</build>
```

**Foot-gun:** filtering is text-only. Disable it for subdirectories with binaries (fonts, keystores) or literal `${...}` strings (Freemarker, `@Value("${...}")`). [05]

---

## Profiles

Activation sources, **OR within one profile**:

| Source | Effect |
|---|---|
| `-P <id>` | highest priority |
| `<activeByDefault>true</activeByDefault>` | active only when **no** `-P` given |
| `<jdk>17</jdk>` | prefix match on running JDK |
| `<os><name>Linux</name></os>` | exact OS match |
| `<property><name>env</name><value>prod</value></property>` | property present (and matching) |

Diagnose: `./mvnw help:active-profiles`, `./mvnw help:all-profiles`. [05]

---

## Versions

| Comparison | Result |
|---|---|
| `1.10.0` vs `1.9.0` | `1.10.0 > 1.9.0` (numeric, not lexical) |
| `1.0.0-SNAPSHOT` vs `1.0.0` | snapshot < release |
| `1.0` vs `1.0.0` | equal (missing segment = 0) |
| `1.0.0-alpha` vs `1.0.0-beta` | lexical on qualifier |

**Bump:** `MAJOR → (X+1).0.0`, `MINOR → X.(Y+1).0`, `PATCH → X.Y.(Z+1)`.

**Release:** strip `-SNAPSHOT` (uppercase, exact).

**CI-friendly:**

```xml
<version>${revision}</version>
<properties>
    <revision>1.0.0-SNAPSHOT</revision>
</properties>
```

Then `./mvnw -Drevision=2.0.0 clean verify`. Only `${revision}`, `${sha1}`, `${changelist}` are recognised by Maven. [05]

---

## Multi-module reactor

**Aggregator** declares `<modules>`. **Parent** is inherited from. Same pom can be both.

**Build order** = topological sort; ties broken by declaration order in `<modules>`.

| Command | Effect |
|---|---|
| `-pl <id>` | build only this module (deps must be installed) |
| `-pl <id> -am` | build target **+ transitive upstream** |
| `-pl <id> -amd` | build target **+ transitive downstream** |
| `-rf <id>` | resume from `<id>`, continue to the end |

Cycles fail at validation, before compiling. Fix by extracting the shared piece into a third module. [06]

**Inter-module dependency:**

```xml
<dependency>
    <groupId>com.vbforge</groupId>
    <artifactId>sample-core</artifactId>
    <version>${project.version}</version>
</dependency>
```

---

## CLI flags

| Flag | Effect |
|---|---|
| `-B` | batch mode (no ANSI, no prompts) — CI default |
| `-ntp` | no transfer progress — CI default |
| `-q` | quiet (warnings/errors only) |
| `-X` | debug (verbose; **never** in CI) |
| `-U` | force re-check of SNAPSHOT metadata |
| `-o` | offline; only `~/.m2` (fails if anything missing) |
| `-T 1C` | one thread per CPU core |
| `-T 4` | fixed 4 threads |
| `-pl`, `-am`, `-amd`, `-rf` | module selection (see above) |

**Canonical CI command:** `./mvnw -B -ntp clean verify` [07]

---

## Skip flags

| Flag | Compiles tests? | Runs tests? |
|---|---|---|
| `-DskipTests` | ✅ | ❌ |
| `-Dmaven.test.skip=true` | ❌ | ❌ |
| `-DskipITs` | ✅ | ❌ (failsafe only) |

`-DskipTests` still **compiles** `src/test/java`, so a broken test source file *still fails the build*. [07]

---

## surefire vs failsafe

| Class name | Plugin | Phase |
|---|---|---|
| `*Test`, `*Tests`, `Test*`, `*TestCase` | surefire | `test` |
| `*IT`, `IT*`, `*ITCase` | failsafe | `integration-test` (then `verify`) |

`mvn test` runs only unit tests. `mvn verify` runs **both**. [07]

---

## Diagnose

| Question | Command |
|---|---|
| What is actually being built? | `./mvnw validate` (prints `Reactor Build Order:`) |
| What version is on the classpath? | `./mvnw dependency:tree` |
| Why did a version get omitted? | `./mvnw dependency:tree -Dverbose` |
| Where does this version come from? | `./mvnw help:effective-pom -Doutput=effective.xml` |
| Which profiles are active, and why? | `./mvnw help:active-profiles` |
| Which deps are unused/undeclared? | `./mvnw dependency:analyze` |
| Which artifacts is the build using? | `./mvnw dependency:build-classpath -Dmdep.outputFile=cp.txt` |
| Which Maven is used? | `./mvnw -v` |
| Where is my local repo? | `./mvnw help:evaluate -Dexpression=settings.localRepository -q -DforceStdout` |
| Newer versions available? | `./mvnw versions:display-dependency-updates` |

---

## Bare minimum production build

```
git clone <repo> && cd <repo>
./mvnw -B -ntp clean verify
```

Green, on a fresh machine, with only JDK 17 installed. Everything else
follows from the nine items in the capstone checklist. [08]