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






