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








