# interview questions & answers

> One section per module. Add at least **3 questions** per module before you
> mark it DONE (see the root `README.md` → "DONE status verification").
> Answers are *yours* — write them from memory first, then fix them from the
> module README.

---

## 01 · maven-recap

### Q1. What are the three built-in lifecycles and what does `mvn install` actually run?

**Answer.** Maven has three independent lifecycles: **`clean`**
(`pre-clean → clean → post-clean`), **`default`**
(`validate → compile → test → package → verify → install → deploy`, plus
hidden phases between them), and **`site`**
(`pre-site → site → post-site → site-deploy`). They do not call each other.

`mvn install` runs *only the default lifecycle*, and because a phase implies
every phase before it, it runs: `validate → compile → test → package →
verify → install`. It does **not** run `clean` — that is why `mvn clean install`
is the idiomatic two-lifecycle command. Each phase delegates to plugin goals
bound to it, e.g. `test → maven-surefire-plugin:test`,
`package → maven-jar-plugin:jar`, `install → maven-install-plugin:install`.
`install` copies the artifact into `~/.m2/repository` so other local projects
can consume it; `deploy` would push it to a remote repository.

### Q2. Difference between `compile`, `provided`, `runtime`, `test` and `system` scopes — with an example each.

**Answer.** Scope controls (a) which classpaths the dependency is on —
compile / test / runtime — and (b) whether it propagates to consumers.

| Scope | Compile CP | Test CP | Runtime CP | Transitive | Example |
|---|---|---|---|---|---|
| `compile` (default) | ✅ | ✅ | ✅ | ✅ | `commons-lang3` |
| `provided` | ✅ | ✅ | ❌ | ❌ | `jakarta.servlet-api` — the container supplies it |
| `runtime` | ❌ | ✅ | ✅ | ✅ | `postgresql` JDBC driver |
| `test` | ❌ | ✅ | ❌ | ❌ | `junit-jupiter`, `assertj-core` |
| `system` | ✅ | ✅ | ❌ | ❌ | a local jar you point at with `<systemPath>` (avoid) |

The key subtlety: **only `compile` and `runtime` are transitive**. A
`provided` dependency is needed to *compile* your code but the runtime
environment (servlet container, app server) provides it, so it must not be
shipped. A `test` dependency must never reach a consumer. `system` is like
`provided` but you hard-code a path — it is non-portable and deprecated in
favour of installing the jar into a local repository.

### Q3. `dependencies` vs `dependencyManagement`.

**Answer.** `<dependencies>` **adds** a dependency to the classpath —
without it, the artifact is not used, no matter what `dependencyManagement`
says. `<dependencyManagement>` only **declares a version (and optionally
scope/type/exclusions)** that can be *inherited* — it adds nothing by itself.

Concretely: if the parent lists `assertj-core 3.25.3` in
`<dependencyManagement>` and the child declares `assertj-core` with no
`<version>`, the child gets 3.25.3. If the child declares nothing at all, it
gets nothing. If the child declares an explicit `<version>`, the child's wins
— `dependencyManagement` supplies a default, it does not override.

`dependencyManagement` is also the mechanism behind **BOM imports**
(`<type>pom</type><scope>import</scope>`), which is why Spring Boot ships a
BOM: one import manages versions of hundreds of artifacts so your modules stay
consistent.

### Q4. What does the Maven Wrapper do and why should you commit it?

**Answer.** `mvnw` / `mvnw.cmd` + `.mvn/wrapper/maven-wrapper.properties` pin
the Maven **version used by the repo**. The script downloads that exact Maven
distribution into a cache (`~/.m2/wrapper/dists/` by default) and runs it,
regardless of what `mvn` is on the developer's `PATH`. `distributionUrl`
points at a `-bin.zip` on Maven Central (or an internal mirror).

You commit `mvnw`, `mvnw.cmd` and the properties file (the wrapper JAR too, if
your wrapper uses jar mode) so every contributor and CI job builds with the
same Maven — no more "works on my machine" bugs from tool drift, no need to
install Maven at all. Use `./mvnw clean verify` in CI instead of `mvn clean
verify`.

### Q5. Why does `mvn package` also run my tests, and where does everything in `target/` come from?

**Answer.** Because the `default` lifecycle is a linear chain: asking Maven to
reach `package` means it first reaches every phase before it, including
`test`. In that phase the `maven-surefire-plugin:test` goal — bound to `test`
by default for `jar`/`war` packaging — runs the unit tests in
`src/test/java`. If any fail, the build stops before `package` and no jar is
produced.

Contents of `target/` (jar packaging) come from these plugins, in order:
`maven-resources-plugin:resources` → `target/classes` (from
`src/main/resources`), `maven-compiler-plugin:compile` → `target/classes`
(from `src/main/java`), `maven-resources-plugin:testResources` →
`target/test-classes`, `maven-compiler-plugin:testCompile` →
`target/test-classes`, `maven-surefire-plugin:test` →
`target/surefire-reports/`, `maven-jar-plugin:jar` →
`target/<artifactId>-<version>.jar`. `mvn clean` (the *clean* lifecycle)
deletes the whole `target/` directory first.

---

## 02 · dependency-management

### Q1. `dependencies` vs `dependencyManagement` — and why does it matter for a multi-module build?

**Answer.** They are **not** two ways of doing the same thing; they do
different jobs:

- `<dependencies>` **adds an artifact to the classpath**. If an artifact is
  not declared here (directly or transitively), it is not used — no matter
  what `dependencyManagement` says.
- `<dependencyManagement>` only **declares a version (and optionally
  `scope`, `type`, `exclusions`) that can be inherited**. It adds nothing to
  any classpath by itself.

Concretely, with `assertj-core 3.25.3` in the parent's
`<dependencyManagement>`:

```xml
<!-- child pom.xml -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <!-- no <version> — comes from parent management -->
</dependency>
```

The child gets 3.25.3. Remove the `<dependency>` block from the child
entirely → the child gets nothing. Add an explicit `<version>3.24.2</version>`
→ the child gets 3.24.2, because `dependencyManagement` supplies a *default*,
not a lock.

Why this matters for multi-module: it is the single place where the version
of a shared library is decided. One bump in the parent's
`<dependencyManagement>` propagates to every child that inherits it. Without
it you'd have `commons-lang3` at three different versions across three
modules and no way to see it without grepping every `pom.xml`.

### Q2. What is a BOM, and why is `<type>pom</type><scope>import</scope>` the idiom for importing one?

**Answer.** A **BOM** (Bill of Materials) is a `pom`-packaged artifact whose
only job is to sit in `<dependencyManagement>` and pin versions of a whole
family of artifacts — e.g. `junit-bom` pins every JUnit 5 artifact,
`spring-boot-dependencies` pins Spring Boot + its whole ecosystem. It usually
contains no code and no `<dependencies>`; it is a version manifest.

You consume it like this, inside **your** `<dependencyManagement>`:

```xml
<dependency>
    <groupId>org.junit</groupId>
    <artifactId>junit-bom</artifactId>
    <version>5.10.2</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

The two `<type>`/`<scope>` elements are not decorative — they are what turn
a normal `<dependency>` entry into an *import*. `type=pom` says "this
artifact is a pom, not a jar"; `scope=import` says "merge its
`<dependencyManagement>` section into mine". Without `import`, Maven would
try to add `junit-bom` itself as a compile-time jar dependency and fail.

**Why a BOM is better than extending the library's parent POM**: Maven has
single inheritance, and you may already extend your own parent. A BOM is
composition — you can import multiple BOMs (`junit-bom`, `jackson-bom`,
`spring-boot-dependencies`) into one project. You cannot extend multiple
parents. That is exactly why Spring Boot ships both a parent **and** a BOM;
the BOM is the one you use when you can't (or don't want to) inherit from
Boot's parent.

Your own entries always win over a BOM import: if you declare
`junit-jupiter 5.11.0` in your `<dependencyManagement>` *after* importing
`junit-bom 5.10.2`, you get 5.11.0 — the nearest-declaration rule applies
inside management too.

### Q3. Where does Maven actually get the version of `junit-jupiter` in module 01's `pom.xml`? Trace it.

**Answer.** Walk the chain:

1. `01-maven-recap/pom.xml` declares `<dependency>junit-jupiter</dependency>`
   with **no `<version>`** and no `<dependencyManagement>` of its own.
2. It inherits from the root `pom.xml` via the `<parent>` block.
3. The root `pom.xml` has this in `<dependencyManagement>`:
   ```xml
   <dependency>
       <groupId>org.junit</groupId>
       <artifactId>junit-bom</artifactId>
       <version>5.10.2</version>
       <type>pom</type>
       <scope>import</scope>
   </dependency>
   ```
   That import pulls in JUnit's own managed versions.
4. JUnit's `junit-bom 5.10.2` maps `org.junit.jupiter:junit-jupiter` →
   `5.10.2`.

So the effective version is **5.10.2**, and it is decided in exactly one
place: the root `pom.xml`'s BOM import. Change that number and every module
that declares `junit-jupiter` without a version moves with it.

The diagnostic tool for this is:

```bash
./mvnw -pl 01-maven-recap help:effective-pom -Doutput=effective.xml
```

Grep `effective.xml` for `junit-jupiter` — the `<version>` element will be
present in the *effective* pom even though your source pom has none. That is
the whole point of the exercise: the effective pom is what Maven actually
builds with, and it is the only reliable way to answer "where did this
version come from?".

### Q4. Declare `assertj-core` in the parent, in the child, and on the command line — which version wins?

**Answer.** Precedence, highest first:

1. **CLI `-D` property override** — `./mvnw dependency:tree -Dassertj.version=3.23.1`
   only wins if the version is actually written as `${assertj.version}` *and*
   the property is resolved at the right level. It does **not** override an
   inline `<version>3.25.3</version>`.
2. **Explicit `<version>` in the consuming pom** — hard-coded in the child's
   `<dependency>` beats any management.
3. **Child's own `<dependencyManagement>`** — beats the parent's.
4. **Parent's `<dependencyManagement>`** (including BOM imports).

Two subtleties worth memorising:

- A property override from the CLI wins over a property set in a parent pom
  because Maven user properties (`-D`) are the highest-precedence property
  source. But it only wins *if the pom uses the property*. Hard-coded
  versions ignore it entirely.
- Properties set in the **child's** `<properties>` override the same
  property in the **parent's** `<properties>`, because the child is nearer.
  That's why the standard pattern is: `<assertj.version>` in the parent's
  `<properties>`, `${assertj.version}` in the parent's
  `<dependencyManagement>`, and nothing in the child — everything is
  overridable in exactly one place.

This is precisely what the module 2 lab ("Version precedence" exercise) is
designed to make you *see*, one `dependency:tree` at a time.

### Q5. `pluginManagement` vs `plugins` — and why does removing a plugin from the child still use the pinned version?

**Answer.** Same relationship as `dependencyManagement` vs `dependencies`,
but for `<build>`:

- `<pluginManagement>` **declares config and version for a plugin, but does
  not run it**. It is a template.
- `<plugins>` **actually runs the plugin** (or, more precisely, contributes
  its executions to the lifecycle).

The subtle bit is: **if the default lifecycle already binds the plugin**, you
don't need an explicit `<plugins>` entry to activate it. For `jar`
packaging, `maven-compiler-plugin:compile` is bound to `compile`,
`maven-surefire-plugin:test` to `test`, `maven-jar-plugin:jar` to `package`,
and so on. So:

1. Parent `<pluginManagement>` pins `maven-compiler-plugin 3.16.0` with
   `<release>17</release>`.
2. Child declares the plugin with no version and no configuration → Maven
   uses 3.16.0 + release 17. ✅
3. Child removes the `<plugin>` block entirely → the compiler plugin is
   *still* invoked by the default lifecycle, and Maven still looks up its
   config in `pluginManagement`. The pinned version and `<release>` still
   apply. ✅

This trips people up because it looks like the plugin "disappeared". What
actually disappeared is an *explicit activation*; the default binding is
still there, and `pluginManagement` still supplies the config. To prove it,
read the `maven-compiler-plugin:<version>:compile` line in the build log —
it's the version from `pluginManagement`, not the default.

### Q6. How would you diagnose "where did this plugin/version/config actually come from?" on a real project?

**Answer.** Three commands cover 95% of cases:

1. **`./mvnw help:effective-pom -Doutput=effective.xml`** — the pom Maven
   actually builds with, after parent inheritance, BOM imports, active
   profiles, and property interpolation. This is the ground truth for
   dependency versions, scopes, and plugin configuration. Diff two
   `effective-pom` outputs to see exactly what a parent change did.
2. **`./mvnw help:active-profiles`** — which profiles are active and *why*
   (default, `-P`, property, JDK). Saves you guessing why a "prod" property
   is set on your machine but not on CI.
3. **`./mvnw dependency:tree -Dverbose`** — the *resolved* dependency graph,
   including omitted nodes (`version managed from X`, `conflict with Y`).
   This is the tool that answers "why is version Z on my classpath when I
   asked for W?".

If the question is specifically about a *property*, the effective pom will
show it already interpolated — grep for the final value and, if you want
the origin, run `./mvnw help:evaluate -Dexpression=assertj.version -q -DforceStdout`
in each module to see which value each context resolves to.

---

## 03 · transitive-resolution

### Q1. What is a transitive dependency, and how does Maven decide which version to use when several paths reach the same artifact?

**Answer.** A **transitive dependency** is a dependency of a dependency: if
your project declares `A` and `A` declares `B`, then `B` is on your classpath
even though you never wrote it in your pom. Only `compile` and `runtime` scoped
dependencies propagate this way — `provided`, `test` and `system` do not.

When the same artifact (`groupId:artifactId`) is reachable along several paths,
Maven picks one version using **nearest-wins**:

1. Compute the depth of each path (edges from the root project).
2. The path with the **smallest depth** wins.
3. **Tie** at the same depth → the one **declared first** wins. "First" means
   the order of the direct dependencies in the root pom, then the order of
   that dependency's own `<dependencies>`, and so on.

The winning version goes on the classpath; every other version is *omitted*
from the resolved graph. `mvn dependency:tree -Dverbose` prints those
omissions explicitly:

```
\- com.example:lib-b:jar:1.0.0:compile
\- (com.example:shared:jar:2.0.0:compile - omitted for conflict with 1.0.0)
```

To **override** nearest-wins without touching the graph, declare the wanted
version explicitly at depth 1 (a direct dependency) or in your
`<dependencyManagement>` — an explicit declaration always beats a transitive
one.

### Q2. Explain scope mediation, with an example.

**Answer.** When your project depends on `B` at scope **P**, and `B` depends
on `C` at scope **Q**, the scope of `C` as *you* see it is `mediate(P, Q)`,
not `Q`. This is **scope mediation**.

| your scope on B ↓ / B's scope on C → | compile | provided | runtime | test |
|---|---|---|---|---|
| **compile**  | compile | – | runtime | – |
| **provided** | provided | – | provided | – |
| **runtime**  | runtime | – | runtime | – |
| **test**     | test | – | test | – |

`–` means **dropped**: Maven does not pull `C` into your project at all.

Examples:

- `compile` on `B`, `B` has `C` at `compile` → you get `C` at `compile`. ✅
- `compile` on `B`, `B` has `C` at `runtime` → you get `C` at `runtime`
  (JDBC drivers arrive exactly this way).
- `compile` on `B`, `B` has `C` at `provided` → **dropped**. `B` assumed the
  runtime would supply `C`; you must declare it yourself if you need it.
- `compile` on `B`, `B` has `C` at `test` → **dropped**. Test dependencies
  never travel.
- `test` on `B`, `B` has `C` at `compile` → you get `C` at `test`. `B` is
  test-only for you, and so is everything it brings in.

The key rule: **a `test`, `provided` or `system` child scope is never
propagated**. A `compile` or `runtime` child scope is propagated, but the
resulting scope is at most as broad as the parent's.

### Q3. `<exclusions>` vs `<optional>` — when do you use each?

**Answer.** They look similar but control opposite directions of the graph:

- **`<exclusions>`** sit *inside* a `<dependency>` in *your* pom and describe
  what you refuse to **receive** from that specific dependency. They do not
  affect what *consumers* of your artifact see. Use them when a library
  transitively brings something you do not want — a second logging backend,
  an old JSON parser, a conflicting SLF4J binding.
- **`<optional>true</optional>`** marks one of *your* dependencies as not
  passed on to **consumers** of your artifact. It does not change your own
  classpath. Use it when your library *uses* a dependency internally (an
  optional integration, a plugin, a driver) but the consumer should not
  inherit it unless they explicitly ask.

A one-line way to remember: **exclusion is what you don't want to receive;
optional is what you don't want to give.**

There is also `<scope>provided</scope>`, which looks similar to `<optional>`
for compile-classpath purposes but says something different ("the runtime
container supplies this") and does *not* let the consumer opt in. For "the
consumer might want this", the right tool is `<optional>true</optional>` and
the consumer re-declares the dependency explicitly.

### Q4. You get a `NoSuchMethodError` in production that never appears locally. Walk through how you would diagnose it.

**Answer.** `NoSuchMethodError`, `ClassNotFoundException`, and
`AbstractMethodError` in production almost always mean **a dependency clash**:
the classpath at runtime has an *older* version of a library than the one your
code was compiled against — often because two libraries each pulled a
different version and nearest-wins picked the wrong one.

Diagnostic sequence:

1. **Reproduce on a clean checkout.** `./mvnw clean verify` from scratch — is
   a stale local `~/.m2` shadowing a SNAPSHOT? Run with `-U` to force
   re-resolution.
2. **Print the resolved tree**: `./mvnw dependency:tree -Dverbose`. Look for
   `omitted for conflict with` and `version managed from` lines. The version
   that is *not* on the classpath is often the one your code was compiled
   against.
3. **Confirm the runtime classpath.** `./mvnw dependency:build-classpath
   -Dmdep.outputFile=cp.txt` and diff local vs. CI vs. production.
4. **Find who brings the wrong version**:
   `./mvnw dependency:tree -Dincludes=com.foo:the-lib`. Two versions → you
   have a conflict.
5. **Fix it in one of three ways**, least to most invasive:
    - Pin the correct version in `<dependencyManagement>` (best when the
      version is needed project-wide).
    - Declare the correct version as a direct dependency (nearest-wins then
      picks it).
    - `<exclusion>` the wrong version from the one dependency that pulls it
      (only when you need the rest of that dependency's transitives).
6. **Prevent it next time** with Enforcer `dependencyConvergence` (fails the
   build if any artifact resolves to multiple versions) and by running
   `dependency:analyze` in CI.

The root cause is usually one artifact whose *major* version silently changed.
`-Dverbose` is the tool that makes the omission visible.

### Q5. How would you enforce a consistent dependency tree across a multi-module build?

**Answer.** Three layers, cheapest first:

1. **Centralise versions in the parent's `<dependencyManagement>`.** Every
   child declares `<groupId>:<artifactId>` with no version; one bump in the
   parent moves the whole build. Combine with a BOM import for third-party
   families (`junit-bom`, `jackson-bom`, `spring-boot-dependencies`) so you
   don't hand-maintain dozens of versions.

2. **Fail the build on convergence problems** with
   `maven-enforcer-plugin`:

   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-enforcer-plugin</artifactId>
       <executions>
           <execution>
               <goals><goal>enforce</goal></goals>
               <configuration>
                   <rules>
                       <dependencyConvergence/>
                   </rules>
               </configuration>
           </execution>
       </executions>
   </plugin>
   ```

`dependencyConvergence` fails the build if any artifact resolves to more
than one version *after* mediation. It's the safety net for the
`NoSuchMethodError` class of bugs.

3. **Detect dead and missing declarations** with `./mvnw dependency:analyze`:
    - *Used undeclared* — your code uses an artifact that only arrives
      transitively. It could disappear on the next dependency bump; declare it
      directly.
    - *Unused declared* — you declare something you never use. Remove it.

Add all three to CI (`./mvnw -B verify`) so a bad version bump fails the
build instead of production. In a multi-module build, the parent pom is the
single place to change; the Enforcer plugin and `dependency:analyze` are the
single place to *prove* the change was safe.

---

## 04 · repositories-settings

### Q1. Where does Maven look for an artifact, and in what order?

**Answer.** For every artifact Maven needs, it walks a fixed lookup chain and
stops at the first hit:

1. **Local repository** — `~/.m2/repository` by default (overridable with
   `-Dmaven.repo.local=...` or `<localRepository>` in `settings.xml`). Always
   consulted first. No network involved.
2. **A mirror of the repository** that declares the artifact — if a
   `<mirror>`'s `mirrorOf` pattern matches the repository's id, the mirror's
   URL **replaces** the repository's URL (it is not an "additional" repo).
3. **The repository's own URL** — when no mirror matched.
4. **Fail** with `Could not resolve dependencies` / `Non-resolvable import POM`
   / `ArtifactNotFoundException`, naming the repositories it tried.

The local repository and every remote repository use the **same directory
layout**:

```
<base>/<groupId with dots → slashes>/<artifactId>/<version>/<artifactId>-<version>.<ext>
```

so a Central URL is the local path with an HTTP base in front:
`https://repo.maven.apache.org/maven2/com/vbforge/foo/1.0.0/foo-1.0.0.jar`.

### Q2. What is a mirror, and how does `mirrorOf` work?

**Answer.** A **mirror** replaces one or more repositories with a different
URL. It is configured in `~/.m2/settings.xml` (per-developer / per-CI) — never
in the pom, because "route everything through our Nexus" is not a property of
the project.

`mirrorOf` patterns (all case-insensitive on the id, case-sensitive on the
literal tokens):

| Pattern | Matches |
|---|---|
| `*` | every repository |
| `external:*` | every repo whose URL is not `localhost` or `file://` |
| `central` | exactly the repository with id `central` |
| `central,spring-milestones` | any of the listed ids |
| `*,!my-internal` | everything except `my-internal` (negation wins) |

If two mirrors match the same repository, **the first one in `settings.xml`
wins** — same as Maven itself. Negation (`!id`) is checked across the whole
pattern: if any `!id` names the repo, the whole `mirrorOf` does not match,
regardless of how many positive parts matched.

Three common real uses: a corporate proxy (`mirrorOf=*`), a faster local
mirror of Central, or blocking an unwanted repo (`mirrorOf=bad-repo` mapped to
a known-good URL).

### Q3. What does `SNAPSHOT` mean, and how do SNAPSHOT repositories differ from release repositories?

**Answer.** A `SNAPSHOT` version — e.g. `1.0.0-SNAPSHOT` — is a **mutable,
in-development** version. Every time the producing project deploys, Maven
overwrites the artifact with a timestamped file (`foo-1.0.0-20240101.120000-1.jar`)
and updates `maven-metadata.xml` in the same directory. Consumers re-check
that metadata, which is why `-U` exists: to force Maven to look for a *newer*
SNAPSHOT when the local copy still looks up-to-date.

The suffix is **case-sensitive**: Maven only treats `-SNAPSHOT` (uppercase) as
a snapshot. `1.0.0-snapshot` is an ordinary release version.

A repository can independently enable or disable releases and snapshots:

```xml
<releases><enabled>true</enabled></releases>
<snapshots><enabled>false</enabled></snapshots>
```

Central has snapshots **disabled** — deliberately. Public repos should not
serve mutable artifacts, because reproducibility depends on a given GAV always
resolving to the same bytes. SNAPSHOTs belong on a private repo (Nexus /
Artifactory) reachable only by the teams that need them.

`release` versions on the other hand are treated as immutable. Maven will
*not* re-download a release already present locally, even with `-U` — the
local copy is trusted once fetched (barring checksum mismatches).

### Q4. Difference between `-o` and `-U` — and what happens if you use both?

**Answer.** They are not opposites; they act on different stages of the lookup.

- **`-o` (offline)** — Maven must not touch the network at all. Every
  artifact must already be in `~/.m2/repository`, or the build fails with
  `Cannot access central in offline mode`. Useful on a plane, in an air-gapped
  CI runner after a warm `~/.m2`, or to prove "the build genuinely does not
  need the network".
- **`-U` (force update)** — Maven re-checks **SNAPSHOT metadata and release
  metadata** (e.g. `maven-metadata.xml` for version ranges and `LATEST` /
  `RELEASE`) against remote repositories, ignoring whatever it has cached.
  Useful when a colleague deployed a fresh `-SNAPSHOT` and your build keeps
  using the stale one.

If you pass both, **`-o` wins**: offline means "no network calls at all", so
there is nothing to update. Maven silently ignores `-U` in that case.

Neither flag re-downloads a fully-resolved release artifact: once a
`1.0.0` jar is in the local repository, `-U` will not fetch it again. If you
genuinely need a re-fetch, delete the artifact directory or add
`-Dmaven.repo.local=/tmp/m2-empty` for the run.

### Q5. You see `Could not resolve dependencies` / `Non-resolvable import POM` in CI. Walk through the diagnosis.

**Answer.** The error means Maven exhausted its lookup chain without finding
the artifact. Diagnose in this order:

1. **Read the full error block.** Maven prints the artifact it could not
   resolve, the version, and — importantly — the list of repositories it
   tried. That list is the key: if only `central` appears and you expect
   `corp-nexus`, the settings on the CI runner are wrong.
2. **Check the settings in effect.** `./mvnw help:effective-settings` on the
   CI runner (or `-X` and grep for `settings.xml`) — is the corporate mirror
   actually being loaded? Is `settings.xml` in `~/.m2/settings.xml` on the
   runner, or passed with `-s`? Is the credentials `<server>` id matching the
   `<mirror>` id?
3. **Check the repository is enabled for the version.** A `-SNAPSHOT` in a
   repo with `<snapshots><enabled>false</enabled></snapshots>` — or a release
   in a repo with `<releases><enabled>false</enabled></releases>` — is
   invisible to Maven. This is the single most common cause of "the URL is
   right, the network works, but Maven says it can't find it".
4. **Run with `-X` and grep for the artifact's URL.** `./mvnw -X
   dependency:resolve | grep "the-lib"` — you will see exactly which URL was
   requested and what the server replied (200, 404, 401, timeout). A 401 is
   credentials; a 404 is wrong path or wrong version; a timeout is a firewall.
5. **Test resolution in isolation.** `./mvnw dependency:get -Dartifact=g:a:v
   -DremoteRepositories=id::default::https://...`. This bypasses the pom
   entirely and proves whether the artifact is fetchable from a specific URL.
6. **Beware the "works on my machine" trap.** Your local `~/.m2` might
   contain the artifact from an old download — the CI is a *clean* runner and
   is failing to fetch it. `./mvnw -o -U` on your machine reproduces the CI
   behaviour without touching the network.

The fix is usually one of: add/patch a repository in the pom, fix the
`mirrorOf` pattern, enable snapshots on the snapshot repo, or supply the
missing credentials in the runner's `settings.xml`.

---

## 05 · properties-profiles-versions

### Q1. What is the difference between a property, a profile, and a version, and how do the three fit together?

**Answer.** They are three orthogonal mechanisms that happen to meet in the
pom:

- A **property** (`<properties><foo>bar</foo></properties>`) is a named string
  used anywhere in the pom as `${foo}`. It is a *substitution* mechanism —
  no condition, no scope. Custom properties live in the pom; built-ins like
  `${project.version}` and `${project.basedir}` come from Maven itself.
- A **profile** is a conditional slice of pom — extra properties,
  dependencies, plugins, repositories — that applies only when its activation
  rules fire. It is a *condition* mechanism.
- A **version** is the artifact's `version` coordinate. It is what tells
  consumers which release of your artifact they are depending on.

They fit together in the classic pattern:

```xml
<properties>
    <assertj.version>3.25.3</assertj.version>       <!-- property -->
</properties>

<profiles>
    <profile>
        <id>ci</id>
        <activation><property><name>env.CI</name></property></activation>
        <properties>
            <assertj.version>3.26.0</assertj.version>  <!-- override -->
        </properties>
    </profile>
</profiles>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>${assertj.version}</version>       <!-- property use -->
        </dependency>
    </dependencies>
</dependencyManagement>
```

A version is *where* the value goes; a property is *how* it is written once;
a profile is *when* the property changes. Three orthogonal mechanisms, one
pom.

### Q2. What does resource filtering do, and what is the standard foot-gun?

**Answer.** **Resource filtering** substitutes `${...}` placeholders inside
files under `src/main/resources` (and `src/test/resources`) during the
`process-resources` phase, on the way to `target/classes`. The same
substitution as pom properties, applied to *files* — most commonly used to
bake `${project.version}`, an environment endpoint, or a profile-controlled
flag into `application.properties`.

Enable it per resource directory:

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

The foot-gun is enabling filtering on **the whole** `src/main/resources`
directory when some of it contains binary files (fonts, keystores, images) or
literal `${...}` strings that are *not* Maven placeholders — e.g. Freemarker
templates, `@Value("${some.spring.property}")` snippets, or SQL with `${var}`
variables. Filtering will substitute or corrupt them.

The standard fix is **two resource blocks**: one with `filtering=true` for the
files that should be interpolated, one with `filtering=false` for everything
else, with `<includes>` / `<excludes>` carving out the split. Never use a
single blanket `<filtering>true</filtering>` unless you have audited every
file under `src/main/resources`.

### Q3. How does profile activation work, and what does `activeByDefault` actually mean?

**Answer.** A profile is active if **any** of its activation rules fire, or if
its id is passed with `-P`:

| Source | Rule | Notes |
|---|---|---|
| CLI | `-P <id>` | highest priority; the one you control from a shell |
| `activeByDefault` | `<activeByDefault>true</activeByDefault>` | active only when **no** `-P` is given |
| JDK | `<jdk>17</jdk>` | prefix match against the running JDK version |
| OS | `<os><name>Linux</name></os>` | exact match (case-sensitive) |
| Property | `<property><name>env</name><value>prod</value></property>` | fires when the property is set (and matches, if a value is given) |
| File | `<file><exists>...</exists></file>` | presence/absence of a file |

The important subtlety: **`activeByDefault` is disabled the moment any
profile is activated with `-P` (or by another activation source)**. Maven
assumes "the user has chosen a profile deliberately, so drop the fallback".
This is why the common "dev is default, prod via `-Pprod`" pattern works — and
why mixing `activeByDefault` with auto-activating rules (JDK, OS, property)
can leave you with a `dev` profile that is silent even though the pom says it
should be on.

Diagnose with `./mvnw help:active-profiles` (lists what is active and *why*)
and `./mvnw help:all-profiles` (lists everything, active or not).

### Q4. Compare `SNAPSHOT` and release versions, and explain what "CI-friendly versions" are.

**Answer.** A **SNAPSHOT** version (`1.0.0-SNAPSHOT`) is mutable: every deploy
to the snapshot repository overwrites the same logical GAV, and Maven updates
`maven-metadata.xml` so consumers know a newer build exists. `-U` forces
Maven to re-check that metadata. The suffix is **case-sensitive** — only
`-SNAPSHOT` (uppercase) is a Maven SNAPSHOT; `1.0.0-snapshot` is a normal,
immutable release version.

A **release** version (`1.0.0`) is immutable: Maven will not re-download it
once it is in `~/.m2`, and the release repository is expected to reject an
overwrite. This immutability is why the two live in different repositories:
snapshots go to a mutable repo, releases to a release-only repo (Central,
Nexus "releases", etc.). Reproducibility depends on a release GAV always
resolving to the same bytes.

**CI-friendly versions** move the version out of the coordinates and into a
property, so a single `-D` on the command line sets the version of a whole
multi-module build:

```xml
<version>${revision}</version>

<properties>
    <revision>1.0.0-SNAPSHOT</revision>
    <sha1/>
    <changelist/>
</properties>
```

Then `./mvnw -Drevision=1.2.3 clean deploy` publishes every module at
`1.2.3`, with no pom edits. Only `${revision}`, `${sha1}` and
`${changelist}` are recognised by Maven itself. The default value *must* be
present (or the property must be set on the CLI), otherwise Maven fails before
the build starts — this is a deliberate guard against accidentally building
with a literal `${revision}` on the classpath.

### Q5. You are asked to move a version bump across a multi-module project. Which tooling and which commands?

**Answer.** Three layers, cheapest first:

1. **Centralise the version in the parent's `<dependencyManagement>` (or a
   `<properties>` entry referenced from there).** One edit, all modules follow
   on the next build. This is where the version *should* live for any shared
   library — no tooling needed.

2. **Use `versions:` for the mechanical parts:**
    - `./mvnw versions:display-dependency-updates` — list newer versions of
      every dependency, transitive-safe.
    - `./mvnw versions:display-plugin-updates` — same for plugins.
    - `./mvnw versions:update-properties` — bump every `<properties>` entry
      that is out of date (with `-DincludeProperties=...` to restrict).
    - `./mvnw versions:update-parent` — bump the parent reference.
    - `./mvnw versions:set -DnewVersion=2.0.0 -DprocessAllModules=true` — set
      the project version of *every* module in a multi-module build in one
      shot. Without `-DprocessAllModules=true`, only the top-level pom changes
      and child modules keep the old version, which typically breaks
      inter-module dependencies.

3. **CI-friendly versions (`${revision}`)** — if the project is set up this
   way, a single `./mvnw -Drevision=2.0.0 clean deploy` sets the version of
   every module without any tool needing to rewrite pom files. This is the
   most reliable approach on CI, because nothing in the working tree needs to
   be modified before `deploy`.

For inter-module dependency versions inside a multi-module build, remember
that `<version>${project.version}</version>` propagates automatically as long
as modules are declared in the same reactor — but the moment one module is
built standalone, the property resolves to the module's own version, which is
not necessarily the parent's. Use `${project.parent.version}` when you need
the parent's version specifically.

---

## 06 · multi-module-reactor

### Q1. What is the Maven reactor, and how is build order determined?

**Answer.** The **reactor** is the set of modules Maven is currently building
in one invocation, plus the order it processes them in. It is not a
configuration element — it is a computed result of the aggregator pom's
`<modules>` tree plus each module's internal dependencies.

Build order is a **topological sort** of the inter-module dependency graph. A
module is built only after every module it depends on (transitively). Ties
between modules that are both "ready to build next" are broken by
**declaration order** — the module listed first in `<modules>` wins. This is
why the reactor order is deterministic across runs and machines.

The order is *not* the order the modules appear in `<modules>`. If you list
`app, service, core` but `app → service → core`, the reactor still builds
`core → service → app`. Maven prints the resulting order in the
`Reactor Build Order:` block at the start of every build — read that log line
whenever a multi-module build surprises you.

Cycles are forbidden: if A depends on B and B depends on A, there is no valid
order and Maven fails at validation, before compiling anything.

### Q2. Difference between an aggregator and a parent pom — and can one pom be both?

**Answer.** Two orthogonal roles:

- **Aggregator** — a `pom`-packaged module that declares `<modules>`. Its
  job: "when you build me, build these together". It shares no configuration.
  An aggregator with no children is legal and common: it is just a build
  entry point.
- **Parent** — a pom that other modules reference in their `<parent>` block.
  Its job: share properties, `dependencyManagement`, `pluginManagement`,
  repositories, plugins. A parent has no opinion about what gets built
  together — a module that inherits from it is not automatically in its
  reactor.

Yes, one pom can play both roles, and the root pom of this lab does. That is
convenient but conflates two concerns: a real codebase sometimes splits them
into a "build-all" aggregator and a separate "company-parent" that other
projects (not in the same reactor) also inherit from.

The distinction matters when debugging: an aggregator-only pom is often the
top of a CI job's build, but nothing inherits its configuration — the modules
get their settings from whichever pom is in their `<parent>` block, which may
be a completely different artifact. Aggregator-only is precisely: `packaging =
pom` **and** `<modules>` non-empty **and** no children.

### Q3. What do `-pl`, `-am`, `-amd` and `-rf` do, and when would you use each?

**Answer.** Four flags that select a slice of the reactor:

| Flag | Meaning | Typical use |
|---|---|---|
| `-pl <ids>` | "project list" — build only these modules | run tests for just the module you changed |
| `-am` | "also make" — add every module the target depends on (transitively) | build one module **and its upstream deps**, when the deps are not installed |
| `-amd` | "also make dependents" — add every module that depends on the target | run the *whole downstream* after changing a shared library |
| `-rf <id>` | "resume from" — skip modules until `<id>` is reached, then continue to the end | restart a failed build without re-running everything upstream |

The everyday combinations:

```bash
# iteration on one module while its deps are already installed
./mvnw -pl my-service test

# a clean machine: build the service and everything it needs, no more
./mvnw -pl my-service -am clean verify

# changed a shared library: prove nothing downstream broke
./mvnw -pl my-core -amd clean verify

# a failure in my-service stopped a full build; resume
./mvnw clean verify -rf my-service
```

`-pl` alone frequently fails on a fresh checkout because Maven cannot resolve
the inter-module dependencies — they are not in `~/.m2` yet. `-am` is the
standard fix. `-amd` is the CI-safety command after touching a low-level
module: it builds the target *and* proves every consumer still compiles.

### Q4. How do you keep one version across all modules of a multi-module project?

**Answer.** Two approaches, from most manual to most ergonomic:

1. **Hand-maintained `<version>` in every pom.** Works, but every version
   bump requires editing every module's pom *and* every inter-module
   dependency — high risk of "one module stayed on the old version".

2. **CI-friendly versions via `${revision}`.** The version coordinate becomes
   a property:

   ```xml
   <version>${revision}</version>

   <properties>
       <revision>1.0.0-SNAPSHOT</revision>
       <sha1/>
       <changelist/>
   </properties>
   ```

   A single `./mvnw -Drevision=2.0.0 clean deploy` sets the version of every
   module in the build. Only `${revision}`, `${sha1}` and `${changelist}` are
   recognised by Maven itself; the property **must** have a default (or be
   passed on the CLI), otherwise Maven fails before the build starts — a
   deliberate guard against accidentally resolving a literal `${revision}`.

   Inter-module dependencies use `${project.version}` (which resolves to the
   current module's version — the same for every module by construction) and
   follow automatically.

3. **`versions:set -DnewVersion=X -DprocessAllModules=true`** — the pragmatic
   middle ground for a project that is not yet `${revision}`-ready. Without
   `-DprocessAllModules=true`, only the top-level pom changes and child
   modules keep the old version, typically breaking inter-module dependency
   resolution on the next build.

### Q5. `mvn validate` fails with "The projects in the reactor contain a cyclic reference". What does it mean, and how do you fix it?

**Answer.** The reactor must be a **DAG** (directed acyclic graph). The error
means two or more modules form a cycle — A depends on B, B depends on A (or a
longer chain A → B → C → A) — and there is no valid build order. Maven detects
this at validation, before compiling anything, and prints the cycle path:

```
[ERROR] The projects in the reactor contain a cyclic reference:
        The projects com.x:a, com.x:b, com.x:c constitute a cycle.
```

The error message is the diagnosis: the listed modules are the cycle.

**How to fix it** — in order of preference:

1. **Extract the shared piece into a third module.** If A and B both need the
   same class/config, neither should own it. Extract it into `common` and
   make both A and B depend on `common`. This is almost always the correct
   fix, because a cycle usually indicates two modules that should never have
   been separated.

2. **Invert one edge via an interface.** If A → B → A only because B needs
   to call back into A, define an interface in A, have A depend on B, and let
   B take an instance of the interface — the compile-time dependency then
   points one way only.

3. **Split a module.** If the cycle is really one module that has grown to
   contain two roles, split it so each role lives in its own module and only
   one direction of the dependency remains.

Fixing a cycle by "reorder the `<modules>` list" is impossible — the reactor
order is computed, not declared. Fixing it by moving a class between modules
usually works but tends to create the same cycle again six months later. The
durable fix is a shared module for the shared piece.

---

## 07 · cli-and-ci-in-practice

### Q1. `verify` vs `install` — and which is the right default for CI?

**Answer.** Both run the same default lifecycle up to a different phase:

| Phase the command reaches | Phases run | Writes to `~/.m2`? |
|---|---|---|
| `clean verify` | `validate → compile → test → package → verify` | ❌ |
| `clean install` | `validate → compile → test → package → verify → install` | ✅ |

`install` is `verify` plus one extra step: copy the built artifact into the
local repository so other projects on the same machine can consume it. For a
single-module project, the practical difference is invisible — the build
either succeeds or fails identically. For a multi-module project, it matters:
if you run `install` on the aggregator, every module gets copied to `~/.m2`,
which is what lets a *later* `-pl <module>` build resolve the inter-module
dependencies.

**The right default for CI is `clean verify`.** Reasons:

1. **No side effects.** The local repository is a developer convenience, not
   a build output. A CI job that populates `~/.m2` on a shared runner can
   accidentally feed stale artifacts into the *next* job on the same agent.
2. **Reproducibility.** A build that depends on artifacts previously
   `install`ed into `~/.m2` is not hermetic — you cannot be sure it works on
   a truly clean machine. `verify` builds strictly from the sources and the
   remote repositories.
3. **Speed.** `install` copies jars you do not need, and can trigger
   `_remote.repositories` bookkeeping on every run.

Use `install` locally during iterative development when you want a dependent
project to pick up the new version, or in the *last* step of a genuinely
multi-JVM CI pipeline where downstream jobs on other machines need the
artifact.

### Q2. `-DskipTests` vs `-Dmaven.test.skip=true` — what is the difference, and when would you use each?

**Answer.** They sound identical; they are not.

| Flag | Compiles `src/test/java`? | Runs tests? | Use case |
|---|---|---|---|
| `-DskipTests` | ✅ yes | ❌ no | "I trust the tests pass, just give me the artifact fast" |
| `-Dmaven.test.skip=true` | ❌ no | ❌ no | "Get me past a broken test source file" or a build that has no tests at all |

`-DskipTests` still binds the `test` phase and invokes
`maven-surefire-plugin:test` — the plugin then skips its own execution
based on the property. The compiler plugin runs normally, so **a syntax
error in a test file still fails the build**. That is a feature, not a bug:
you catch compile-time test breakage without paying the wall-clock cost of
running the tests.

`-Dmaven.test.skip=true` also tells `maven-compiler-plugin:testCompile` to
skip its execution. Nothing under `src/test/java` is even parsed, so a
broken test source file is silently ignored.

**Which to use:**

- **Local iteration on non-test code**: `-DskipTests` is usually the right
  choice — you want the compiler to keep checking the tests.
- **A "package only" CI job that definitely runs tests in another job**:
  `-Dmaven.test.skip=true` is acceptable if that job runs on a machine with
  no test infrastructure (no DB, no Docker) and you are intentionally
  producing an artifact to be tested elsewhere.
- **Almost never both.** If you find yourself wanting `-Dmaven.test.skip=true`
  on `main`, you are probably hiding something that should be fixed.

There is also `-DskipITs`, which is the failsafe-only equivalent of
`-DskipTests`: unit tests still run, integration tests are skipped.

### Q3. You want a `ClassNotFoundException` / `NoSuchMethodError` to fail the build instead of showing up at runtime. What do you add to CI?

**Answer.** Two complementary checks, both bound to the `verify` phase so
they run before `install`/`deploy`:

1. **`maven-enforcer-plugin` with `dependencyConvergence`.** This fails the
   build whenever the same artifact resolves to more than one version in the
   reactor. That is exactly the class of problem that produces
   `NoSuchMethodError` at runtime: two libraries pulled different versions,
   nearest-wins picked an older one, and the code compiled against the newer
   API. The rule makes the build fail *now*, on the version-conflict, rather
   than at 3 a.m. in production.

   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-enforcer-plugin</artifactId>
       <version>3.4.1</version>
       <executions>
           <execution>
               <goals><goal>enforce</goal></goals>
               <configuration>
                   <rules>
                       <dependencyConvergence/>
                       <requireMavenVersion><version>[3.9,)</version></requireMavenVersion>
                       <requireJavaVersion><version>[17,)</version></requireJavaVersion>
                   </rules>
               </configuration>
           </execution>
       </executions>
   </plugin>
   ```

2. **`dependency:analyze`** (either as a CI step or bound to `verify`).
   Reports two categories:
    - *Used undeclared* — your code compiles against an artifact that only
      arrives transitively. It works today because a dependency happens to
      pull it in; the day that dependency stops doing so, your build breaks
      (or the *runtime* does). The fix is to declare it explicitly.
    - *Unused declared* — you declare something you never use. Removing it
      slims the graph and reduces the chance of a future conflict.

   ```bash
   ./mvnw -B -ntp dependency:analyze
   ```

   `dependency:analyze` does not fail the build by default; add
   `<failOnWarning>true</failOnWarning>` or check the output in a script.

Together, these two make "the classpath is coherent and honest" a **build
invariant**, not an after-the-fact diagnosis. That is the difference between
a project that only ever has `NoSuchMethodError` bugs on Fridays, and one
that does not have them at all.

### Q4. You have a 12-module reactor, and CI is slow. Walk through the levers, cheapest first.

**Answer.** In order of effort-to-payoff:

1. **Cache `~/.m2/repository`.** In GitHub Actions this is one keyword on
   `actions/setup-java@v4`: `cache: maven`. Key is derived from
   `**/pom.xml`, so any pom change invalidates it cleanly. On a cold runner
   the first build downloads hundreds of megabytes from Central — often
   60–90 % of wall-clock time. This is almost always the single biggest win,
   and it is free.

2. **Parallelise with `-T 1C`.** Maven builds independent reactor modules
   concurrently, respecting inter-module dependencies. On a typical 4-core
   CI runner, a 12-module reactor is 2–4× faster. The reactor log remains
   readable because every line is prefixed with the module id. Caveats:
   plugins that shell out to shared resources (a single DB, a fixed port)
   may not be thread-safe; test on a branch first.

3. **Trim the graph.** `./mvnw dependency:analyze` finds unused declared
   dependencies. Each one removed is one less artifact to download, one
   less conflict to resolve, one less thing that can break.

4. **Split the CI pipeline.** Run the fast stuff (compile, unit tests) on
   every push; run the slow stuff (integration tests, package, deploy) on
   merge to `main`. A `mvn -pl <module> -am test` on a PR is often minutes
   faster than a full `verify`.

5. **Push the slow stuff out of Maven.** If a test spins up Docker
   containers or hits a remote service, it is rarely a `mvn` problem. It is
   an infrastructure problem. Move it to a dedicated job with its own cache
   and its own timeout.

6. **Do not add `-q` to CI just to "make it quiet".** `-q` hides warnings
   that would otherwise tell you what is slow. Use `-B -ntp` for readable
   logs, and only add `-q` if the log volume is genuinely a problem for
   the log-storage backend.

7. **Never add `-X` to CI.** Debug output is hundreds of megabytes per run,
   hides the actual errors, and slows the build. `-X` is a local diagnosis
   tool.

### Q5. How would you configure `surefire` and `failsafe` for a project that has both unit and integration tests?

**Answer.** Two plugins, two phases, two naming conventions. The correct
config is minimal because the defaults are already what you want:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>3.2.5</version>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

What this does:

- **surefire** runs at the `test` phase, picking up `*Test` / `*Tests` /
  `Test*` / `*TestCase`. You do not need to declare it — the default
  lifecycle binds it.
- **failsafe** runs at the `integration-test` phase (which is between
  `package` and `verify`) and picks up `*IT` / `IT*` / `*ITCase`. You **do**
  need to declare it, because the failsafe plugin is not part of the default
  lifecycle. The second goal, `verify`, is what actually **fails the build**
  if an integration test failed — the `integration-test` goal runs the tests
  but does not fail the build (so that post-test cleanup can run).

The naming convention matters because it is what separates the two:

| Class name | Picked up by | Runs at |
|---|---|---|
| `FooTest` | surefire | `test` |
| `FooIT` | failsafe | `integration-test` (between `package` and `verify`) |

This matters for CI because:

- `./mvnw test` runs only unit tests — fast feedback during development.
- `./mvnw verify` runs unit **and** integration tests — the CI default.
- `./mvnw -DskipITs verify` runs unit tests but not integration tests —
  useful when a dependency (e.g. a database) is unavailable but you still
  want the rest of `verify`.

And it is why `verify` — not `test` — is the correct CI phase. `mvn test`
would silently skip every `*IT` class.

---

## 08 · capstone

> Meta-questions — the kind a senior interviewer asks when they want to know
> whether you have ever *owned* a build, not just used one.

### Q1. You inherit a Java project with a Maven build nobody fully understands. Walk me through your first two days with it.

**Answer.** In order, and without changing anything yet:

**Day 1 — understand the current state.**

1. **Clone it fresh and build it.** `git clone`, then run whatever the
   README says. If the README is silent, try `./mvnw clean verify`, then
   `mvn clean verify`. Note every step that is not obvious from the pom.
2. **Read the root pom top-to-bottom.** Note the packaging (`pom` =
   aggregator/parent), the `<modules>`, the `<parent>` block, the
   `<properties>`, `<dependencyManagement>`, `<build>`. Do not fix
   anything — just inventory.
3. **`./mvnw validate`** — this triggers reactor setup without compiling.
   Read the `Reactor Build Order:` block. Understand the dependency chain
   between modules.
4. **`./mvnw dependency:tree -Dverbose`** — save the output. This is the
   ground truth of what is on every classpath. Grep for `omitted for
   conflict with` and `version managed from` — every hit is a decision
   someone made, intentionally or not.
5. **`./mvnw help:effective-pom -Doutput=effective.xml`** — this is what
   Maven actually builds with, after inheritance, BOM imports, and active
   profiles. Diff it against the source poms to see what the ancestors add.
6. **`./mvnw help:active-profiles`** — which profiles fire on a default
   build, and why.
7. **`./mvnw dependency:analyze`** — the two lists (used undeclared, unused
   declared) are your backlog.
8. **Check the git history of the poms.** `git log --follow pom.xml` shows
   who changed what and when. A version bump that came with a comment is
   worth more than one that did not.

**Day 2 — plan the migration.**

9. **Write down the problems you found**, ordered by risk and effort. Not
   by "how hard to fix" but by "how likely this is to break the build when
   we touch it". Move the wrapper to the top of the list (its change is
   invisible and irreversible in the good way — nothing breaks by using
   the wrapper). Move scope tightening to the bottom (highest risk of
   uncovering a hidden dependency).
10. **Do the wrapper first.** Commit it. Push it. Get CI green on the
    unchanged project. That gives you a known-good baseline.
11. **Then move versions to `dependencyManagement`.** One library at a
    time, each in its own commit. Run `verify` after each. If the version
    shifts a transitive dependency, `dependency:tree -Dverbose` tells you
    which one, and you can decide whether it is a bug fix or a regression.
12. **Then tighten scopes.** Again one at a time. A compile-time failure
    here is a *good* outcome — it means the code was relying on something
    that should never have been on the compile classpath.
13. **Then add the checks** — Enforcer `dependencyConvergence`,
    `dependency:analyze` in CI, failsafe for ITs.

**The two rules that make this safe:**

- **One change per commit.** The git history of the poms is the migration
  log. A future engineer bisects it, not the write-up.
- **CI green between every commit.** If the build goes red, revert and
  retry differently. Do not stack fixes.

By the end of day 2 you have a baseline, a prioritised backlog, and the
first few commits. Day 3+ is execution against that backlog.

### Q2. How do you decide which versions to pin, which to leave managed by a BOM, and which to leave to transitive resolution?

**Answer.** Three tiers, in order of preference:

**Tier 1 — managed by an upstream BOM, do not pin.**
If a library family publishes a BOM that is compatible as a set (JUnit,
Jackson, Spring Boot, Netty), import it in the parent's
`<dependencyManagement>` and delete every version you were pinning by hand.
The BOM's whole point is that the versions are known to work together; your
hand-pinned version is a bet that you know better than the maintainers.
That bet is occasionally correct — it usually is not.

**Tier 2 — explicitly pinned in your `dependencyManagement`.**
Use this for artifacts that:
- Have no upstream BOM, or whose BOM you are deliberately not importing
  (usually because it drags in more than you want).
- Are "the boring ones" — logging, commons, a specific JSON library.
- You deliberately hold at an older version because a newer one broke you.
  Document *why* in the pom as a comment on the managed version; a
  "pinned for a reason" version is a debt, and debt needs a note.

**Tier 3 — left to transitive resolution, do not manage.**
Genuinely small utilities that come along for the ride and that you never
use directly — `byte-buddy` via Mockito, `error_prone_annotations` via
Guava. You should not name them in your pom at all. If
`dependency:analyze` reports one as "used undeclared", it graduates to Tier
2. If it is "unused declared", remove it.

**The decision rule in one sentence:** manage any artifact that your code
imports directly, or that a risk assessment says could be a problem at
runtime (logging, codecs, security-sensitive libs). Leave the rest to the
BOM or the transitive graph.

**The failure mode to avoid** is "manage everything" — pinning every
transitive dependency to a specific version. This defeats the purpose of
using a well-maintained library: you no longer get its bug fixes or its
security patches. And it makes `dependency:analyze` noise out of a clean
graph. Pinning is a scalpel, not a hammer.

### Q3. What is a Maven build's "definition of done"? How do you know when a project's build is production-grade?

**Answer.** A production-grade Maven build satisfies these properties, all
of which are observable without reading the pom:

1. **Reproducible.** `git clone` + `./mvnw -B -ntp clean verify` succeeds
   on a machine that has never had Maven installed. Same Java version, same
   Maven version, same result — everywhere.
2. **Hermetic.** The build does not depend on anything in a developer's
   `~/.m2` that is not reproducible from the sources plus Maven Central (or
   the configured mirrors). The classic test: clean a fresh
   `-Dmaven.repo.local=/tmp/m2-test` directory and build again.
3. **Coherent.** `dependency:tree` shows one version of each artifact — no
   `omitted for conflict` unless the omission is intentional and
   documented. Enforcer's `dependencyConvergence` rule passes.
4. **Honest.** `dependency:analyze` reports no "used undeclared" (nothing
   compiles against a transitive) and no "unused declared" (nothing is
   listed but never referenced). Each remaining warning has a one-line
   justification.
5. **Tested.** Unit tests, integration tests, and anything else you
   actually run are all *binding* — a failure breaks the build. `mvn test`
   is fast; `mvn verify` is complete; the difference is intentional and
   documented.
6. **Gate-kept.** A CI workflow runs `./mvnw -B -ntp clean verify` on
   every push and pull request, blocks merges on failure, and caches
   `~/.m2/repository` so the check is fast enough that nobody wants to
   bypass it.
7. **Documented.** The README says, in one line, how to build the project
   and what the standard commands are. Nothing about Maven is tribal
   knowledge on this team.
8. **Reversible.** Any change to the build is a single commit, revertible
   in isolation. When a version bump breaks the build, `git revert` gets
   you back to green without thinking.

**What production-grade does *not* mean:** elaborate custom plugins, a
ten-file Maven configuration, dozens of active profiles, or a hand-rolled
dependency resolver. Every property above is achievable with the standard
plugins, a single parent pom, and the wrapper. If a build feels complex, it
is almost always because someone solved a problem the standard tool
already solved — the fastest fix is usually deleting the custom code and
using the default.

The single best proxy: **would you be comfortable being paged about this
build at 3 a.m.?** If the answer is no — because you do not understand how
it produces its output, or because it depends on a machine you have never
seen — it is not production-grade yet.

---







