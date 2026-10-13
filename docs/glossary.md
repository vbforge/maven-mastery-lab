# glossary

> Every term the lab uses, one paragraph each, cross-referenced to the module
> that introduces it. Terms are alphabetical. If a term has a "see also", it
> is worth reading together.

---

## Aggregator

A `pom`-packaged module that declares `<modules>`. Its job is to bring a set
of modules into one build (one reactor). It does **not** share any
configuration. A pom can be an aggregator and a parent at the same time,
which is the common case for a multi-module root. **Aggregator-only** means:
`<packaging>pom</packaging>`, non-empty `<modules>`, and no children. [06]

*See also:* Parent POM, Reactor.

## Artifact

Any file Maven resolves: a `jar`, a `pom`, a `war`, a sources jar, a
javadoc jar. Identified by coordinates (groupId, artifactId, version,
packaging). Once an artifact is in your local repository, Maven generally
never fetches it again — release artifacts are treated as immutable. [01]

*See also:* Coordinates, Local repository, SNAPSHOT.

## Build order

The order Maven processes modules in a reactor. It is a **topological sort**
of the inter-module dependency graph, with ties broken by declaration order
in `<modules>`. Not the order the modules are listed — the order in which
they *can* be built. Printed at the start of every multi-module build in the
`Reactor Build Order:` block. [06]

*See also:* Reactor, Aggregator.

## BOM (Bill of Materials)

A `pom`-packaged artifact whose only job is to sit in `<dependencyManagement>`
and pin versions of a whole family of artifacts (JUnit, Jackson, Spring
Boot…). Imported with `<type>pom</type><scope>import</scope>`. Your own
entries always win over an imported BOM's. A project has **one parent** but
**many BOMs** — that is the whole point. [02]

*See also:* dependencyManagement, Parent POM.

## Classpath (compile / test / runtime)

Three logical classpaths Maven builds for your project:

- **compile** — what your main sources compile against
- **test** — compile classpath plus `test`-scoped dependencies
- **runtime** — what the packaged artifact ships with at run time

Scopes decide which classpaths an artifact lands on. Getting a scope wrong
either bloats the artifact (test-only code shipped) or breaks the runtime
(something `provided` that nothing provides). [01, 03]

*See also:* Scope, Scope mediation.

## clean lifecycle

The three-phase chain `pre-clean → clean → post-clean`. `mvn clean` deletes
`target/`. It is independent of the default lifecycle — `mvn clean install`
runs both, `mvn install` alone does not clean. [01]

*See also:* default lifecycle, site lifecycle.

## Coordinates

The four fields that uniquely identify an artifact: `groupId`, `artifactId`,
`version`, `packaging`. The colon-joined form `groupId:artifactId:version` is
used everywhere — in `mvn dependency:get -Dartifact=…`, in error messages, in
build logs. [01]

*See also:* Artifact, GAV.

## default lifecycle

Maven's main phase chain: `validate → compile → test → package → verify →
install → deploy` (with hidden phases between). Running a phase runs every
phase before it in the same chain. `mvn package` therefore compiles *and*
tests. [01]

*See also:* clean lifecycle, Phase, Plugin binding.

## dependency

A declaration in `<dependencies>` that an artifact is needed on the
classpath. Adding it is what makes a library available; the version may come
from `<dependencyManagement>` or be written inline. If nothing declares it,
it is not on the classpath — even if the version is managed. [02]

*See also:* dependencyManagement, Scope, Transitive dependency.

## dependency:analyze

A Maven goal that reports two categories: **used undeclared** (your code
compiles against an artifact you do not declare — it arrives transitively)
and **unused declared** (you declare something you never use). Used
undeclared is the brittle one; unused declared is the deadweight one. It
does not fail the build by default. [03, 08]

*See also:* Enforcer, Transitive dependency.

## dependencyManagement

The pom block that pins versions (and optionally scope, type, exclusions)
for dependencies without adding them to any classpath. It is the single
place where a shared library's version is decided. It **supplies defaults,
not locks** — an explicit `<version>` in a `<dependency>` always wins. [02]

*See also:* dependencies, BOM, pluginManagement.

## Effective POM

The pom Maven actually builds with, after merging your pom with its parents,
imported BOMs, active profiles, and the super POM, and after interpolating
all properties. The source pom is a *fragment*; the effective pom is the
truth. `./mvnw help:effective-pom -Doutput=effective.xml`. [02]

*See also:* Parent POM, Property, Profile.

## Enforcer

A Maven plugin (`maven-enforcer-plugin`) that fails the build when a rule is
violated. The three rules worth using on any project:
`dependencyConvergence` (fails if an artifact resolves to multiple versions),
`requireMavenVersion`, `requireJavaVersion`. `dependencyConvergence` is the
safety net for `NoSuchMethodError` in production. [03, 08]

*See also:* dependency:analyze, Transitive dependency.

## Exclusion

An `<exclusion>` block inside a `<dependency>` that stops a specific
artifact (and its subtree) from being pulled in *from that dependency*. It
is what you refuse to **receive**. Compare with `<optional>true</optional>`,
which is what you refuse to **give** to consumers. [03]

*See also:* Optional, Transitive dependency.

## Failsafe

The plugin that runs **integration tests** (`maven-failsafe-plugin`), bound
to the `integration-test` phase (between `package` and `verify`). Default
includes `*IT`, `IT*`, `*ITCase`. Runs *after* `package`, so it can use the
built jar. The `verify` goal is what actually fails the build if an
integration test failed. [07]

*See also:* Surefire, Phase.

## GAV

Shorthand for `groupId:artifactId:version` — the string form of coordinates.
Used in commands (`mvn dependency:get -Dartifact=<GAV>`), in logs, and in
error messages. "The GAV of this artifact is `com.vbforge:foo:1.0.0`." [01]

*See also:* Coordinates.

## GroupId

The first coordinate. Namespaces the artifact — usually the reverse of a
domain you own (`com.vbforge`). Lowercase. Turned into a directory tree in
the local repository (`. → /`). [01]

*See also:* Coordinates, ArtifactId.

## install (phase)

The phase that copies the built artifact into the local repository
(`~/.m2/repository`) so other local projects can consume it. `mvn install`
runs the whole default lifecycle up to and including this phase. CI should
prefer `verify` — `install` writes to `~/.m2`, which is not what a clean
build should do. [01, 07]

*See also:* verify, Local repository.

## Local repository

The directory `~/.m2/repository` (by default) where Maven stores every
artifact it has downloaded or installed. Consulted **before** any remote
lookup. Same layout as a remote repository: `<group-with-slashes>/<artifactId>/<version>/<file>`.
Override with `-Dmaven.repo.local=<path>` or `<localRepository>` in
`settings.xml`. [04]

*See also:* Remote repository, Lookup order.

## Lookup order

The sequence Maven follows for every artifact: **local repository → mirror
(if matched) → the repository's own URL → fail**. A mirror *replaces* the
repo it matches, it is not an extra repo. [04]

*See also:* Mirror, Local repository, Remote repository.

## Metadata

Small XML files (`maven-metadata.xml`) that describe the versions available
for an artifact in a repository — used for version ranges, `LATEST` /
`RELEASE`, and SNAPSHOT freshness checks. `-U` forces Maven to re-fetch
metadata. [04]

*See also:* SNAPSHOT, Remote repository.

## Mirror

A `<mirror>` in `settings.xml` that replaces one or more repositories with a
different URL. Configured per-developer or per-CI, never in the pom.
`mirrorOf` patterns: `*`, `external:*`, exact id, comma-separated list,
`!id` negation. First matching mirror wins. [04]

*See also:* Lookup order, settings.xml.

## Module

A pom that is part of a reactor, listed in some aggregator's `<modules>`.
An "inter-module dependency" is a dependency from one reactor module to
another, usually written as `<version>${project.version}</version>`. [06]

*See also:* Reactor, Aggregator, Parent POM.

## Nearest-wins

The rule that resolves a version conflict in transitive resolution: the
path with the **smallest depth** from the root project wins. If two paths
have the same depth, the one declared **first** wins. Other versions are
*omitted* from the resolved graph. `dependency:tree -Dverbose` shows the
omissions. [03]

*See also:* Transitive dependency, Scope mediation, dependencyManagement.

## Optional

`<optional>true</optional>` on a dependency marks it as not passed on to
**consumers** of your artifact. It does not affect your own classpath. Use
it for optional integrations or plugins that consumers should only inherit
if they ask. [03]

*See also:* Exclusion, Scope.

## Packaging

The type of artifact a module produces: `jar` (default), `war`, `pom`,
`ear`, etc. Declared as `<packaging>`. A `pom`-packaged module produces no
jar — it exists to aggregate modules and/or act as a parent. [01]

*See also:* Aggregator, Parent POM.

## Parent POM

A pom referenced in another pom's `<parent>` block. Shares properties,
`dependencyManagement`, `pluginManagement`, repositories, plugins. **Not**
the same as an aggregator: a parent can have no `<modules>` at all, and a
module that inherits from a parent is not automatically in its reactor.
Maven has single inheritance — one parent per pom. [02, 06]

*See also:* Aggregator, BOM.

## Phase

A step in a lifecycle. Running a phase runs every phase before it in the
same lifecycle. A phase with no bound plugin goal does nothing. [01]

*See also:* Plugin binding, default lifecycle.

## Plugin binding

The association between a phase and a plugin goal. `compile` binds to
`maven-compiler-plugin:compile`, `test` to `maven-surefire-plugin:test`,
`package` to `maven-jar-plugin:jar`, and so on. You can add bindings with an
`<executions>` block. Default bindings still fire even if you do not declare
the plugin in `<plugins>`. [01, 02]

*See also:* pluginManagement, Phase.

## pluginManagement

`pluginManagement` for plugins: pins a plugin's version and configuration
without running it. Children that declare the plugin — or that rely on the
default lifecycle binding — automatically pick up the managed config. [02]

*See also:* dependencyManagement, Plugin binding.

## POM (Project Object Model)

The `pom.xml` file itself: Maven's declarative description of what to build.
`<modelVersion>4.0.0</modelVersion>` is the only value Maven 2/3 accept.
Convention over configuration means most things (source layout, output
directory, default plugins) are inferred. [01]

*See also:* Effective POM, Coordinates.

## Profile

A conditional slice of pom — extra properties, dependencies, plugins, or
repositories — that applies only when its activation rules fire. Activation
sources: `-P`, `activeByDefault`, JDK prefix, OS name, property, file.
Within one profile, activation rules are OR'd. `-P` **disables** any
`activeByDefault` profile. [05]

*See also:* Property, Effective POM.

## Property

A named string used anywhere in the pom as `${name}`. Custom properties
live in `<properties>`; built-ins include `${project.version}`,
`${project.basedir}`, `${project.build.directory}`. Also used for resource
filtering. `-D` on the CLI is the highest-precedence source — but only if
the pom actually uses the property. [05]

*See also:* Resource filtering, Profile.

## Reactor

The set of modules Maven is currently building in one invocation, plus the
order it processes them in. Determined by the aggregator tree plus each
module's inter-module dependencies. A reactor must be a DAG — cycles fail
before compilation. [06]

*See also:* Build order, Aggregator, Module.

## Release version

A version that is **not** a SNAPSHOT: `1.0.0`, not `1.0.0-SNAPSHOT`. Treated
as immutable — Maven will not re-download one already in `~/.m2`, and a
release repository is expected to reject an overwrite. The whole point of
separating snapshot and release repositories. [05]

*See also:* SNAPSHOT, Local repository.

## Remote repository

An HTTP(S) server storing artifacts in the standard Maven layout. Configured
in `<repositories>` (pom) or in `settings.xml` profiles. Central is the
default. The URL is `<base>/<group-with-slashes>/<artifactId>/<version>/<file>`.
A repository can independently enable/disable `<releases>` and
`<snapshots>`. [04]

*See also:* Local repository, Mirror, SNAPSHOT.

## Resource filtering

Substituting `${...}` placeholders **inside files** under
`src/main/resources` (and `src/test/resources`) at build time. Enabled per
resource directory with `<filtering>true</filtering>`. The foot-gun is
filtering binary files or non-Maven `${...}` strings; the fix is two resource
blocks, one filtered, one not. [05]

*See also:* Property, Build order.

## Scope

A per-dependency field that controls (a) which classpaths the artifact is on
(compile / test / runtime) and (b) whether it propagates to consumers. Five
standard scopes: `compile`, `provided`, `runtime`, `test`, `system`. Only
`compile` and `runtime` are transitive. [01]

*See also:* Classpath, Scope mediation.

## Scope mediation

The rule that computes the effective scope of a transitive dependency. If
your project has `B` at scope **P**, and `B` declares `C` at scope **Q**,
then `C` is visible to your project at a scope determined by the P×Q table
— not just `Q`. `provided` and `test` children are always **dropped**;
`provided` / `runtime` / `test` parents propagate the parent scope
unchanged; `compile` + `runtime` child gives `runtime`. [03]

*See also:* Scope, Transitive dependency.

## settings.xml

The user-level Maven configuration file, `~/.m2/settings.xml`. Contains
`<localRepository>`, `<servers>` (credentials), `<mirrors>`,
`<proxies>`, and `<profiles>` that can supply repositories or properties.
Never checked into a project — it is per-developer and per-CI. Override with
`-s <path>`. [04]

*See also:* Mirror, Remote repository.

## site lifecycle

The three-phase chain `pre-site → site → post-site → site-deploy`. Produces
project documentation. Independent of `clean` and `default`. [01]

*See also:* clean lifecycle, default lifecycle.

## SNAPSHOT

A version suffix (`-SNAPSHOT`, uppercase — Maven is case-sensitive) marking
a mutable, in-development version. Every deploy overwrites the GAV, and
Maven updates `maven-metadata.xml` so consumers know a newer build exists.
`-U` forces a re-check. Central disables snapshots; they belong on private
repositories. [04, 05]

*See also:* Release version, Metadata, Repository.

## Surefire

The plugin that runs **unit tests** (`maven-surefire-plugin`), bound to the
`test` phase. Default includes `*Test`, `*Tests`, `Test*`, `*TestCase`.
Runs before `package`, so it cannot use the built jar — that is what
failsafe is for. [07]

*See also:* Failsafe, Phase.

## Transitive dependency

A dependency of a dependency. Arrives on your classpath without you
declaring it. Governed by nearest-wins, scope mediation, `<exclusions>`, and
`<optional>`. `mvn dependency:tree` shows the resolved graph; `-Dverbose`
shows the omitted nodes and why. [03]

*See also:* Nearest-wins, Scope mediation, Exclusion.

## verify (phase)

The phase between `package` and `install`. It is where you bind checks that
run **after packaging** but **before** the artifact is installed: integration
tests (failsafe), Enforcer rules, `dependency:analyze`, spotbugs, custom
verification. **`verify` is the right phase for CI** — it runs everything
except the copy to `~/.m2`. [01, 07]

*See also:* install, Failsafe.

## versions plugin

A Maven plugin with goals that report and update versions:
`display-dependency-updates`, `display-plugin-updates`,
`display-parent-updates`, `update-properties`, `set -DnewVersion=X
-DprocessAllModules=true`. The `set` goal in a multi-module build **without**
`-DprocessAllModules=true` changes only the top-level pom and leaves children
on the old version. [05]

*See also:* CI-friendly versions, Property.

## Wrapper (mvnw / mvnw.cmd)

A pair of scripts checked into the repository that download and use a
specific Maven version, so every contributor and CI job builds with the
same Maven — no install required, no tool drift. Configured by
`.mvn/wrapper/maven-wrapper.properties` (`distributionUrl` points at a
`-bin.zip` on Maven Central). Use `./mvnw` everywhere; never bare `mvn` in
scripts or CI. [01, 07]

*See also:* Local repository, settings.xml.

---

## How to use this glossary

- **Reading top-to-bottom** is not the intended use — terms cross-reference
  each other; follow the *See also* lines when a term is unfamiliar.
- **The bracket numbers** point to the module that introduces the term in
  depth. The cheatsheet is the one-page summary; the module README is the
  source of truth.
- **When a term is not here**, it is almost always a variant of one that is
  (e.g. "transitive closure" → *Transitive dependency*). If a term is
  genuinely missing, it belongs in the module that taught it first — open
  an issue or add it there.