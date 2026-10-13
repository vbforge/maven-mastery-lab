# 07-cli-and-ci-in-practice

> **07-cli-and-ci-in-practice** | Java 17+ | Package: `com.vbforge.cli`

## What's covered

| Topic | Key points |
|---|---|
| `verify` vs `install` | `verify` runs the lifecycle up to `verify` without writing to `~/.m2`; `install` adds one more phase |
| `-DskipTests` vs `-Dmaven.test.skip=true` | first still compiles tests; second skips both |
| surefire vs failsafe | `*Test` / `*Tests` / `Test*` / `*TestCase` → surefire; `*IT` / `IT*` / `*ITCase` → failsafe |
| CI flags | `-B` (batch), `-ntp` (no transfer progress), `-q` (quiet), `-X` (debug), `-U` / `-o` |
| Parallel builds | `-T 1C` (one thread per core), `-T 4` (fixed) |
| Caching `~/.m2` | `actions/setup-java` `cache: maven` — the single biggest CI speed win |
| GitHub Actions | `.github/workflows/build.yml` runs `./mvnw -B -ntp clean verify` |

## Files

```
07-cli-and-ci-in-practice/
├── pom.xml
├── README.md
├── LAB.md
└── src/
    ├── main/java/com/vbforge/cli/
    │   ├── CliOption.java
    │   ├── CliAndCiDemo.java
    │   └── CliAndCiTasks.java
    └── test/java/com/vbforge/cli/
        ├── CliAndCiDemoTest.java
        └── CliAndCiTasksTest.java
```


## 🎯 Tasks

Open `CliAndCiTasks.java` and implement all 5 methods. Each has a detailed
Javadoc.

| # | Method | Topic |
|---|---|---|
| 1 | `buildCommand(goals, options)` | assemble a valid `mvn` command line from goals + flags |
| 2 | `phasesRun(command)` | which phases of the default lifecycle a command runs |
| 3 | `skipsCompilingTests(flags)` | `-DskipTests` vs `-Dmaven.test.skip=true` |
| 4 | `classifyTest(className)` | `*Test` → surefire, `*IT` → failsafe, else `NONE` |
| 5 | `ciCommand(args)` | non-interactive CI command (`./mvnw -B -ntp <args>`) |

When all 5 are implemented, remove `@Disabled` from `CliAndCiTasksTest`:

```bash
./mvnw test -pl 07-cli-and-ci-in-practice -Dtest=CliAndCiTasksTest
```

All 5 task groups must go **GREEN** ✅

## Key things to remember

```
verify vs install:
  ./mvnw clean verify     ← CI default; no writes to ~/.m2
  ./mvnw clean install    ← verify + copy to ~/.m2

Skip flags:
  -DskipTests                compiles tests, does NOT run them
  -Dmaven.test.skip=true     skips compilation AND execution

Test naming (default includes):
  surefire (test phase)      *Test   *Tests   Test*   *TestCase
  failsafe (verify phase)    *IT     IT*      *ITCase

CI flags:
  -B       batch mode (no ANSI, no prompts)
  -ntp     no transfer progress
  -q       quiet
  -X       debug
  -U       update snapshots
  -o       offline

Canonical CI command:
  ./mvnw -B -ntp clean verify

Cache ~/.m2 in GitHub Actions:
  - uses: actions/setup-java@v4
    with:
      distribution: temurin
      java-version: '17'
      cache: maven

Parallel builds:
  ./mvnw -T 1C clean verify
```

## Run

```bash
# tests of this module (demo + tasks)
./mvnw test -pl 07-cli-and-ci-in-practice

# run the demo
./mvnw -q exec:java -pl 07-cli-and-ci-in-practice \
    -Dexec.mainClass="com.vbforge.cli.CliAndCiDemo"
```

---

## Design notes / why made these choices

- **`CliOption` is top-level.** Same pattern as `Mirror`, `Module`, `Profile`, `Artifact`. Every module from 03 onwards has had one shared model type; this keeps the Tasks' signatures clean and consistent.
- **`TestKind` is nested in `CliAndCiDemo`** — matches `BumpLevel` in module 05's Demo. It is a small enum that only exists to classify test names, and it is referenced from both the Demo and the Tasks.
- **`buildCommand` takes two lists, not one map.** Options have natural ordering (`-B` before `-ntp`; `-T 1C` before `-pl module-a`) and a map cannot express "boolean flag" cleanly. Two lists keep the API honest about both.
- **`phasesRun` uses max-index, not first-token.** Maven's real semantics: `mvn test verify` runs `test` then `verify` — the *end state* is the phases up to `verify`. Returning up to `verify` matches that. The `laterPhaseWins` test locks this in.
- **`skipsCompilingTests` is deliberately narrow.** It only returns `true` for the full-skip flag; `-DskipTests` is the trap, and the test explicitly asserts `false`. The `-Dmaven.test.skip=false` test forces the implementer to be exact rather than "contains anything with `maven.test.skip`".
- **`classifyTest` checks surefire first.** This is a real Maven gotcha — a class named `TestIT` matches surefire's `Test*` *and* failsafe's `*IT`, and *both* plugins will actually run it in a real build. Documented in the Javadoc and asserted in a test.
- **`ciCommand` prepends `./mvnw -B -ntp`.** The whole point of the task is that CI *must* use the wrapper and *must* run in batch mode. There is no way to disable either — a caller who wants a plain `mvn` build is not building for CI, and can use `buildCommand` instead.
- **LAB exercise 2 uses a syntactically broken test file.** This is the *only* way to make the `-DskipTests` vs `-Dmaven.test.skip=true` difference observable in a real Maven run — a test that merely *fails* at runtime is caught by both flags. The exercise sets up the trap on purpose.
- **LAB exercise 6 reviews the actual `build.yml`** written in a previous session. This makes the module's teaching loop back into the CI setup that already exists, rather than describing CI in the abstract.
- **Q4 is a "walk through the levers" answer** because the interviewer's real question is "do you know which optimizations matter and which are decoration?" — the ranking (cache > `-T` > trim > split) is what they want to hear, not an unordered list.
- **Q5's "why `verify`, not `test`"** is the practical punchline: a CI pipeline that runs `mvn test` silently skips every `*IT` class. That single sentence has saved more junior engineers than any other piece of advice in this lab.




