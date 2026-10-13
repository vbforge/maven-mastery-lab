# 05-properties-profiles-versions

> **05-properties-profiles-versions** | Java 17+ | Package: `com.vbforge.profiles`

## What's covered

| Topic | Key points |
|---|---|
| Properties | custom (`<properties>`) and built-in (`${project.version}`, `${project.basedir}`) |
| Resource filtering | substitute `${...}` inside `src/main/resources` at build time |
| Profiles | conditional pom slices; activated by `-P`, `activeByDefault`, JDK, OS, property, file |
| Activation rules | OR semantics across rules within one profile; `-P` disables `activeByDefault` |
| SNAPSHOT vs release | mutable vs immutable; the suffix is case-sensitive |
| Semantic versioning | `major.minor.patch`; numeric compare (`1.10 > 1.9`); `-SNAPSHOT < release` |
| CI-friendly versions | `${revision}` / `${sha1}` / `${changelist}`, set once on the CLI |
| `versions:` plugin | `display-dependency-updates`, `update-properties`, `set` |

## Files

```
src/
├── main/java/com/vbforge/profiles/
│   ├── Profile.java                                  ← profile model (id + activation rules)
│   ├── ActivationEnv.java                            ← the environment to evaluate against
│   ├── PropertiesProfilesVersionsDemo.java           ← theory + annotated examples
│   └── PropertiesProfilesVersionsTasks.java          ← 🎯 your tasks — implement these methods
└── test/java/com/vbforge/profiles/
├── PropertiesProfilesVersionsDemoTest.java           ← tests for the demo (already green)
└── PropertiesProfilesVersionsTasksTest.java          ← 🧪 tests for your tasks (@Disabled until done)
LAB.md                                                    ← hands-on exercises with the real mvn CLI
```

## 🎯 Tasks

Open `PropertiesProfilesVersionsTasks.java` and implement all 5 methods. Each has
a detailed Javadoc.

| # | Method | Topic |
|---|---|---|
| 1 | `interpolate(text, props)` | replace `${key}` placeholders; leave unknowns alone |
| 2 | `compareVersions(a, b)` | numeric-aware ordering, `1.10 > 1.9`; `-SNAPSHOT < release` |
| 3 | `bumpVersion(version, level)` | `MAJOR` / `MINOR` / `PATCH` bump, suffix preserved |
| 4 | `isProfileActive(profile, env)` | `-P`, `activeByDefault`, JDK prefix, OS, property |
| 5 | `releaseVersion(snapshot)` | strip `-SNAPSHOT` (uppercase, exact) |

When all 5 are implemented, remove `@Disabled` from `PropertiesProfilesVersionsTasksTest`:

```bash
./mvnw test -pl 05-properties-profiles-versions -Dtest=PropertiesProfilesVersionsTasksTest
```

All 5 task groups must go **GREEN** ✅

## Key things to remember

```
Built-in properties:
  ${project.version}          ${project.groupId}      ${project.artifactId}
  ${project.basedir}          ${project.build.directory}
  ${maven.compiler.release}   ${java.version}         ← YOU define these

Profiles — activation sources, OR within one profile:
  -P <id>                     CLI (disables activeByDefault)
  <activeByDefault>true       when no -P given
  <jdk>17</jdk>               prefix match against running JDK
  <os><name>Linux</name>      exact match
  <property><name>env</name><value>prod</value>
  <file><exists>...</exists>

Versioning:
  1.10.0 > 1.9.0             numeric segments, not lexical
  1.0.0-SNAPSHOT < 1.0.0     snapshot < release
  1.2.3 → MAJOR → 2.0.0
  1.2.3 → MINOR → 1.3.0
  1.2.3 → PATCH → 1.2.4

CI-friendly:                 <version>${revision}</version>
                             ./mvnw -Drevision=1.2.3 clean verify

Diagnose:
  ./mvnw help:active-profiles
  ./mvnw help:all-profiles
  ./mvnw -Pprod help:effective-pom
  ./mvnw -Drevision=1.2.3 help:evaluate -Dexpression=project.version -q -DforceStdout
  ./mvnw versions:display-dependency-updates
```

## Run

```bash
# tests of this module (demo + tasks)
./mvnw test -pl 05-properties-profiles-versions

# run the demo
./mvnw -q exec:java -pl 05-properties-profiles-versions \
    -Dexec.mainClass="com.vbforge.profiles.PropertiesProfilesVersionsDemo"
```

## Design notes

- **Two new top-level types (`Profile`, `ActivationEnv`)** — same reason as module 04's `Mirror`: shared model between Demo and Tasks without nested class qualification. `Profile` uses static factories (`of`, `defaultActive`, `onJdk`, `onOs`, `onProperty`) so tests read like English rather than positional record constructors.
- **`activeByDefault` semantics are encoded faithfully** — the "disabled by any `-P`" rule is tested explicitly (`defaultDisabledByCli`) because it is *the* profile gotcha interviewers ask about.
- **`compareVersions` distinguishes numeric and qualifier parts.** Tests include the two most consequential cases — `1.10.0 > 1.9.0` and `1.0.0-SNAPSHOT < 1.0.0` — plus a lexical qualifier case. `1.0` vs `1.0.0` is tested to force the "missing segment == 0" rule.
- **`bumpVersion` preserves suffixes.** `1.2.3-SNAPSHOT` + PATCH → `1.2.4-SNAPSHOT` is the pattern a release flow actually wants; the naive "strip and re-add" fails here. `1.0` + MINOR → `1.1.0` forces padding to a proper triple.
- **`interpolate` deliberately leaves unknown placeholders in place**, matching Maven's behaviour under resource filtering. The `null`-value-in-map test forces a null-check on the value that beginners forget.
- **`releaseVersion` is case-sensitive on purpose.** `1.2.0-snapshot` stays as-is — that is a real Maven rule, not a stylistic choice. `-SNAPSHOT` → `""` is included to force suffix-based (not prefix-based) stripping.
- **LAB sequencing** goes from "see one property" → "see it replaced in a file" → "make it profile-controlled" → "make the profile auto-activate" → "see the version coordinate go SNAPSHOT and back" → "make the version itself a property". Each layer adds exactly one concept, and every exercise has a `**Notes**` that pushes toward the *why*.
- **Q4 bundles SNAPSHOT and CI-friendly versions** because they are two halves of the same question ("what does the version coordinate actually mean?").
- **Q5 is the practical one.** `versions:set -DprocessAllModules=true` is the command calling it out explicitly saves a broken multi-module build.
