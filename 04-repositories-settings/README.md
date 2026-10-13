# 04-repositories-settings

> **04-repositories-settings** | Java 17+ | Package: `com.vbforge.repositories`

## What's covered

| Topic | Key points |
|---|---|
| Local repository | `~/.m2/repository`; group dots → slashes; consulted first, no network |
| Remote repositories | `<repositories>` in the pom; standard URL layout; Central is the default |
| Mirrors | `settings.xml` → `<mirrors>`; `mirrorOf` patterns (`*`, `external:*`, exact, list, `!id`) |
| `settings.xml` | `localRepository`, `<servers>`, `<mirrors>`, `<profiles>`; per-developer / per-CI |
| SNAPSHOT vs release repos | `<releases>` / `<snapshots>` toggles; Central has snapshots disabled |
| Offline and updates | `-o` (offline), `-U` (force re-check of SNAPSHOT metadata) |
| Lookup order | local → mirror (if matched) → repo URL → fail |

## Files

```
src/
├── main/java/com/vbforge/repositories/
│   ├── Mirror.java                          ← mirror model (id, mirrorOf, url)
│   ├── RepositoriesSettingsDemo.java        ← theory + annotated examples
│   └── RepositoriesSettingsTasks.java       ← 🎯 your tasks — implement these methods
└── test/java/com/vbforge/repositories/
├── RepositoriesSettingsDemoTest.java        ← tests for the demo (already green)
└── RepositoriesSettingsTasksTest.java       ← 🧪 tests for your tasks (@Disabled until done)
LAB.md                                            ← hands-on exercises with the real mvn CLI
```

## 🎯 Tasks

Open `RepositoriesSettingsTasks.java` and implement all 5 methods. Each has a
detailed Javadoc.

| # | Method | Topic |
|---|---|---|
| 1 | `localRepoPath(g, a, v)` | relative path inside `~/.m2/repository` |
| 2 | `remoteUrl(base, g, a, v, ext)` | full artifact URL in a remote repo |
| 3 | `selectMirror(repoId, mirrors)` | which mirror applies, first match wins, `!` negation |
| 4 | `isSnapshot(version)` | case-sensitive `-SNAPSHOT` suffix detection |
| 5 | `resolutionOrder(repos, mirrors)` | ordered lookup steps: local → mirror/repo → … |

When all 5 are implemented, remove `@Disabled` from `RepositoriesSettingsTasksTest`:

```bash
./mvnw test -pl 04-repositories-settings -Dtest=RepositoriesSettingsTasksTest
```

All 5 task groups must go **GREEN** ✅

## Key things to remember

```
Local repo layout:
  ~/.m2/repository/<group-with-slashes>/<artifactId>/<version>/
      <artifactId>-<version>.jar
      <artifactId>-<version>.pom

Remote URL uses the SAME layout, just with an HTTP base:
  <base>/<group-with-slashes>/<artifactId>/<version>/<artifactId>-<version>.jar

mirrorOf patterns:
  *                every repo
  external:*       every repo except localhost and file://
  central          exactly this id
  a,b              any of these ids
  *,!internal      everything except 'internal'   (negation wins)

Lookup order:
  1. local repo            (~/.m2/repository)
  2. mirror of the repo    (if a mirrorOf pattern matches)
  3. the repo's own URL    (if no mirror matched)
  4. fail

Diagnose:
  ./mvnw -o                    # offline — only ~/.m2
  ./mvnw -U                    # force re-check of SNAPSHOT metadata
  ./mvnw -X dependency:get -Dartifact=g:a:v
  ./mvnw help:effective-settings
```

## Run

```bash
# tests of this module (demo + tasks)
./mvnw test -pl 04-repositories-settings

# run the demo
./mvnw -q exec:java -pl 04-repositories-settings \
    -Dexec.mainClass="com.vbforge.repositories.RepositoriesSettingsDemo"
```

---

## Design notes

- **`Mirror` is top-level**, not nested in the demo — a single shared model type across Demo and Tasks is cleaner than a qualified `RepositoriesSettingsDemo.Mirror` in every method signature. 
Same reasoning as `Artifact`/`DependencyNode` in module 03.
- **Negation (`!id`) is in scope for `selectMirror`**, because it's the *one* `mirrorOf` rule that trips people up in interviews. I kept `external:*` deliberately simplified (treated as `*`) 
and documented that in the Javadoc and the demo — implementing the full localhost detection would distract from the pattern-matching lesson.
- **`isSnapshot` tests include `" 1.0.0-SNAPSHOT "` → false**, which encodes the fact that Maven does **not** trim whitespace. The test set is calibrated so the naive `contains("SNAPSHOT")` 
implementation fails, forcing the candidate to think about the suffix and case.
- **`resolutionOrder` dedupes mirrors via `LinkedHashSet`** — an implementer who forgets this gets `["local", "mirror:nexus", "mirror:nexus"]` and fails the star-mirror test. 
The README and Javadoc both call this out so a careful reader gets it right.
- **LAB exercises are deliberately non-destructive.** The `settings.xml` exercises use `-s lab/settings.xml`, so the learner never edits the real `~/.m2/settings.xml`. 
Exercise 4's "bogus URL" is a template for reproducing a real-world failure that CI reports and laptops hide.
- **Q5 is a long answer** because the `Could not resolve dependencies` error is the single most-reported Maven issue in the wild. The 6-step diagnosis is the closest thing to a real recipe.

