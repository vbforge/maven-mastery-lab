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

