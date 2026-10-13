## sample-app-parent

**real nested multi-module project** (`sample-app-parent/`)
to experiment on. It is *not* part of the root reactor — it is a fully
independent mini build that lives inside module 06's folder purely as a lab subject. Invoke it explicitly:

```bash
# build the nested sample-app-parent (separate reactor!)
cd 06-multi-module-reactor/sample-app-parent
../../mvnw clean verify

```

**Note:**
Nothing at the repo root will pick it up (module 06 is `jar`-packaged,
so Maven never scans its subfolders for other poms).

---

## The `sample-app-parent/` mini build

Three modules, three roles:

| Module | Role | Depends on |
|---|---|---|
| `sample-core` | pure library with a `Greeter` class + a test | — |
| `sample-service` | uses `Greeter`, has a test that proves it | `sample-core` |
| `sample-app` | a `main()` that exercises `GreetingService` | `sample-service` |

It is the subject of module 06's `LAB.md`. The three modules form the simplest
possible reactor with a meaningful dependency chain, so every reactor concept
(build order, `-pl`/`-am`/`-amd`/`-rf`, cycles) can be demonstrated on real
Maven output instead of a toy in-code graph.

---

## Reactor build order

```
sample-app-parent  [pom]
sample-core        [jar]
sample-service     [jar]
sample-app         [jar]
```

`sample-app-parent` is `pom`-packaged, so it has no artifact to build — but it
still appears in the reactor log, because it is the aggregator.

## Useful commands

```bash
# full reactor
../../mvnw clean verify

# only one module's code, if its deps are already installed
../../mvnw -pl sample-service verify

# one module plus its upstream dependencies
../../mvnw -pl sample-service -am verify

# one module plus everything that depends on it
../../mvnw -pl sample-core -amd verify

# resume from a module after a failure
../../mvnw verify -rf sample-service

# see the tree of the service module
../../mvnw -pl sample-service dependency:tree
```

---

## Design notes / why I made these choices

- **`sample-app-parent/` is a nested, self-contained reactor.** It is *not* in the root `<modules>`, so the root build never touches it. This preserves the "one module = one folder" convention from 01–05, and makes the sample-app a safe scratchpad: if a learner breaks it (e.g. introduces a cycle in the LAB), the root `./mvnw verify` is unaffected.
- **`<relativePath>../../pom.xml</relativePath>`** — verified against Maven's semantics: the path is relative to the pom.xml's *directory*, and the parent lives two levels up (`sample-app/` → `06-multi-module-reactor/` → repo root). Inheriting from the root reuses the pinned Java 17 and JUnit/AssertJ versions, so the sample-app doesn't duplicate config.
- **LAB exercise 2 deliberately fails** (`-pl sample-service` alone). This is the single most common multi-module gotcha in practice and worth seeing once, in a controlled setting.
- **LAB exercise 6's cycle is introduced on purpose**, and the fix prompt ("extract the shared piece into a third module") is spelled out because that is the lesson, not just the workaround.
- **Q5 ends with the durable fix.** `Reordering <modules>` never works (the order is computed, not declared), and moving a single class usually just delays the problem. The `common`-module extraction is the answer that survives code review six months later.

---


