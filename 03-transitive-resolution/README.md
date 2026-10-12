# 03-transitive-resolution

> **03-transitive-resolution** | Java 17+ | Package: `com.vbforge.resolution`

## What's covered

| Topic | Key points |
|---|---|
| Transitive dependencies | a dependency of a dependency; only `compile` and `runtime` propagate |
| Nearest-wins | smallest depth wins; ties broken by declaration order |
| Scope mediation | how a parent and a child scope combine — and when the artifact is dropped |
| `<exclusions>` | remove one artifact (and its subtree) from a specific dependency |
| `dependency:tree [-Dverbose]` | see the resolved graph and the omitted nodes |
| `dependency:analyze` | find used-undeclared and unused-declared dependencies |
| Enforcer `dependencyConvergence` | fail the build if one artifact resolves to multiple versions |

## Files

```
src/
├── main/java/com/vbforge/resolution/
│ ├── Artifact.java ← coordinates + scope (shared model)
│ ├── DependencyNode.java ← tree node (shared model)
│ ├── TransitiveResolutionDemo.java ← theory + annotated examples
│ └── TransitiveResolutionTasks.java ← 🎯 your tasks — implement these methods
└── test/java/com/vbforge/resolution/
├── TransitiveResolutionDemoTest.java ← tests for the demo (already green)
└── TransitiveResolutionTasksTest.java ← 🧪 tests for your tasks (@Disabled until done)
LAB.md ← hands-on exercises with the real mvn CLI
```

## 🎯 Tasks

Open `TransitiveResolutionTasks.java` and implement all 5 methods. Each has a
detailed Javadoc. All methods operate on a {@code DependencyNode} tree and
match artifacts by their `groupId:artifactId`.

| # | Method | Topic |
|---|---|---|
| 1 | `depthOf(root, ga)` | distance from the root, BFS |
| 2 | `resolveVersion(root, ga)` | nearest-wins, tie → first declared |
| 3 | `mediateScope(parentScope, childScope)` | effective scope of a transitive, or `null` if dropped |
| 4 | `applyExclusions(root, excludedGas)` | prune excluded subtrees |
| 5 | `findConflicts(root)` | artifacts resolving to more than one version |

When all 5 are implemented, remove `@Disabled` from `TransitiveResolutionTasksTest`:

```bash
./mvnw test -pl 03-transitive-resolution -Dtest=TransitiveResolutionTasksTest
```

All 5 task groups must go GREEN ✅

## Key things to remember

 - Nearest-wins — the smallest depth from the root wins.
 - Tie at the same depth — the one declared first in the pom wins.

 - Scope mediation (your scope on B → B's scope on C):
   - compile  + compile  → compile
   - compile  + runtime  → runtime
   - provided + compile  → provided
   - provided + runtime  → provided
   - runtime  + compile  → runtime
   - runtime  + runtime  → runtime
   - test     + compile  → test
   - test     + runtime  → test
   - provided / test child  → DROPPED  (non-transitive)

```
<exclusion>  → what you refuse to receive (per dependency)
<optional>   → what you refuse to give (to consumers)
```

### Diagnose:
```bash
Diagnose:
./mvnw dependency:tree
./mvnw dependency:tree -Dverbose
./mvnw dependency:tree -Dincludes=com.example:the-lib
./mvnw dependency:analyze
maven-enforcer-plugin / dependencyConvergence
```

## Run
```bash
# tests of this module (demo + tasks)
./mvnw test -pl 03-transitive-resolution

# run the demo
./mvnw -q exec:java -pl 03-transitive-resolution \
    -Dexec.mainClass="com.vbforge.resolution.TransitiveResolutionDemo"
```

## Design notes / why these choices

- **`Artifact` and `DependencyNode` as top-level types.** Unlike modules 01/02, the tasks here operate on data, not on strings. If the model were nested inside the demo, every task signature would carry a qualified name. Two tiny top-level records keep the API clean and let the tests construct fixtures easily.
- **The tree, not a general graph.** A DAG would be more faithful, but Maven's resolution *is* effectively a tree traversal (paths from the root). The tree is enough to teach nearest-wins, tie-break, mediation, exclusion and conflict detection.
- **`scope` on the `Artifact`, not on the edge.** In real Maven it's an edge property. Flattening it into the node is a deliberate simplification: what a reader of `dependency:tree` sees is exactly one scope per line, so the flattened model matches the mental model.
- **`mediateScope` returns `null` for "dropped".** This mirrors `Coordinates.parse`-style APIs from module 01 (`nextPhase` returns `null` at the end) — consistent within the lab.
- **`applyExclusions` returns a new tree.** No mutation, matches the functional style of the previous modules and makes the "input unchanged" test trivial.
- **Lab exercises are sequenced to build up.** Each exercise layers on the previous one: start by watching transitivity, then reproduce a conflict, then diagnose it, then fix it three ways, then watch scope, then defend with Enforcer + analyze. The `**Notes:**` prompts push toward the *why*, not just the output.
