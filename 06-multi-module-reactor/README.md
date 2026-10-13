# 06-multi-module-reactor

> **06-multi-module-reactor** | Java 17+ | Package: `com.vbforge.reactor`

## What's covered

| Topic | Key points |
|---|---|
| The reactor | the set of modules being built + the topological order Maven processes them in |
| Aggregator vs parent | aggregator declares `<modules>`; parent is inherited from `<parent>`; they are different roles |
| Build order | topological sort of module dependencies; ties broken by declaration order |
| `-pl` / `-am` / `-amd` / `-rf` | build a slice of the reactor; upstream, downstream, or resume from |
| `${revision}` | one version across many modules, overridable on the CLI |
| Cycles | a reactor is a DAG; cycles fail before compilation |

## Files

```
src/
├── main/java/com/vbforge/reactor/
│   ├── Module.java                       ← reactor module model (id + reactor deps)
│   ├── PomSummary.java                   ← single-pom classifier for aggregator vs parent
│   ├── MultiModuleReactorDemo.java       ← theory + annotated examples
│   └── MultiModuleReactorTasks.java      ← 🎯 your tasks — implement these methods
└── test/java/com/vbforge/reactor/
├── MultiModuleReactorDemoTest.java       ← tests for the demo (already green)
└── MultiModuleReactorTasksTest.java      ← 🧪 tests for your tasks (@Disabled until done)

sample-app-parent/                          ← a real mini multi-module build to experiment on
├── README.md                               ← short description doc (sample-app-parent)
├── pom.xml                                 ← aggregator + parent (sample-app-parent)
├── sample-core/                            ← library, no reactor deps
├── sample-service/                         ← depends on sample-core
└── sample-app/                             ← depends on sample-service, has a main()
LAB.md
```

> **The `sample-app-parent/` project is NOT part of the root reactor.** It is a
> self-contained multi-module build nested inside this module's folder, built
> only when you invoke it explicitly. Nothing at the repo root will try to
> build it.

## 🎯 Tasks

Open `MultiModuleReactorTasks.java` and implement all 5 methods. Each has a
detailed Javadoc.

| # | Method | Topic |
|---|---|---|
| 1 | `buildOrder(modules)` | topological sort; ties by declaration order; throws on cycle |
| 2 | `detectCycle(modules)` | DFS; returns the cycle as a list, or empty |
| 3 | `alsoMake(target, modules)` | `-am`: target + transitive upstream |
| 4 | `alsoMakeDependents(target, modules)` | `-amd`: target + transitive downstream |
| 5 | `isAggregatorOnly(pom)` | aggregator-only vs parent-only vs both |

When all 5 are implemented, remove `@Disabled` from `MultiModuleReactorTasksTest`:

```bash
./mvnw test -pl 06-multi-module-reactor -Dtest=MultiModuleReactorTasksTest
```

All 5 task groups must go **GREEN** ✅

## Key things to remember

```
Aggregator vs parent:
  aggregator    declares <modules>, packaging=pom; "build these together"
  parent        is <parent> of other modules; "share this config"
  aggregator + parent  is common — the root pom in this lab plays both roles
  aggregator-only      pom + modules + NO children

Reactor build order = topological sort of module dependencies.
Ties broken by declaration order in <modules>.

Selection flags:
  -pl <ids>    build only these modules (deps must be installed already)
  -am          also make upstream  (target + everything it depends on)
  -amd         also make dependents (target + everything that depends on it)
  -rf <id>     resume from <id>, continue to the end of the reactor

${revision} — one version across all modules:
  <version>${revision}</version>
  <properties><revision>1.0.0-SNAPSHOT</revision></properties>
  ./mvnw -Drevision=2.0.0 clean verify

Diagnose:
  ./mvnw help:effective-pom -pl module
  ./mvnw validate          # shows "Reactor Build Order:" without building
  ./mvnw -pl <id> -am -X   # see exactly which modules are selected and why
  ./mvnw -pl <id> -am -amd # rarely useful — the union of both directions
```

## Run

```bash
# tests of this module (demo + tasks)
./mvnw test -pl 06-multi-module-reactor

# run the demo
./mvnw -q exec:java -pl 06-multi-module-reactor \
    -Dexec.mainClass="com.vbforge.reactor.MultiModuleReactorDemo"

# build the nested sample-app (separate reactor!)
cd 06-multi-module-reactor/sample-app-parent
../../mvnw clean verify
```

## The `sample-app-parent/` mini build

Three modules, three roles:

| Module | Role | Depends on |
|---|---|---|
| `sample-core` | pure library with a `Greeter` class + a test | — |
| `sample-service` | uses `Greeter`, has a test that proves it | `sample-core` |
| `sample-app` | a `main()` that exercises `GreetingService` | `sample-service` |

Build order is `core → service → app`. Try `-pl`, `-am`, `-amd` on it — the
LAB walks you through it.

## Design notes / why I made these choices

- **`sample-app-parent/` is a nested, self-contained reactor.** It is *not* in the root `<modules>`, so the root build never touches it. This preserves the "one module = one folder" convention from 01–05, and makes the sample-app a safe scratchpad: if a learner breaks it (e.g. introduces a cycle in the LAB), the root `./mvnw verify` is unaffected.
- **`<relativePath>../../pom.xml</relativePath>`** — verified against Maven's semantics: the path is relative to the pom.xml's *directory*, and the parent lives two levels up (`sample-app-parent/` → `06-multi-module-reactor/` → repo root). Inheriting from the root reuses the pinned Java 17 and JUnit/AssertJ versions, so the sample-app doesn't duplicate config.
- **`Module` vs `PomSummary`.** The reactor graph (Task 1–4) only needs id + deps; classification (Task 5) needs packaging + modules + children. Keeping them separate means each type is exactly the shape its method needs — no dummy fields, no ambiguity in the tests.
- **`alsoMake` includes the target.** The README language "what `-am` pulls in" is ambiguous, but the *practical* question is "what would Maven build?", and that set always includes the target. Documented in the Javadoc and tested explicitly so there is no confusion.
- **`detectCycle` returns the cycle path, not a boolean.** A boolean teaches "does a cycle exist"; the path teaches *which* modules are involved, which is what the user actually needs to fix the problem. The test checks the set of modules in the cycle, not the direction, because DFS can find the cycle starting from any node in it.
- **`buildOrder` throws `IllegalStateException` on cycle.** Same reasoning: it forces the implementer to handle the cycle case rather than return a silently-wrong list. The `selfLoop` test proves the case where a module depends on itself — easy to miss if you only look for length ≥ 2 cycles.
- **The diamond test** exercises the genuinely tricky case: two branches that both depend on a shared root and re-merge at a common child. Naive implementations that don't track "already emitted" can emit a node twice; ones that don't scan the input list in order can produce a non-deterministic result.
- **LAB exercise 2 deliberately fails** (`-pl sample-service` alone). This is the single most common multi-module gotcha in practice and worth seeing once, in a controlled setting.
- **LAB exercise 6's cycle is introduced on purpose**, and the fix prompt ("extract the shared piece into a third module") is spelled out because that is the lesson, not just the workaround.
- **Q5 ends with the durable fix.** `Reordering <modules>` never works (the order is computed, not declared), and moving a single class usually just delays the problem. The `common`-module extraction is the answer that survives code review six months later.
