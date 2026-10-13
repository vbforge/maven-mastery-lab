# 08 — capstone

> **The goal of the whole lab, in one module.** Pick a real project of your
> own, give it a production-grade Maven build, and write down what you
> changed and why.

---

## Why this module exists

Modules 01–07 taught you one mechanism at a time on a controlled toy. Module
08 is where you find out whether you actually internalised them: whether you
can look at a *messy, real* build — the kind you inherited from a colleague,
or wrote yourself two years ago — and turn it into something a reviewer would
approve without comments.

The distinction that matters: **you are not being asked to write new code.**
You are being asked to take working code and give it a build worth keeping.
This is the same task you will be handed on your first week at a real job.

You are done when a colleague can clone your project, run one command, and
get a green build with all the guarantees a modern Maven project should
provide.

---

## 1. Pick a project

The right capstone is **real** — not a fresh scaffold you created for the
exercise. Fresh scaffolds hide all the mess that makes Maven hard.

**Good candidates, in order of preference:**

1. **A project of yours that "works but is scary."** A pom you have been
   avoiding. A multi-module build that only one person on the team can
   build. A project with hand-copied jars in `lib/`. This is the ideal
   capstone: the pain is real, and the win is measurable.

2. **A well-known OSS project you use and understand.** Fork it, migrate
   its build, keep the code as-is. Good if you do not have a private
   project you can share; the *diff of the pom* becomes the deliverable.

3. **A small project you built once and never cleaned up.** Even a
   `Hello World` with a proper build beats a fresh `mvn archetype:generate`
   — it has history, and history is where the mess lives.

**Avoid:**

- A project you cannot publish or screenshot. You need to be able to show
  the write-up to someone.
- A project so large that migration takes weeks. A 3-module build is
  enough to exercise every mechanism.
- A project with a *different build tool*. Porting Gradle → Maven is a
  different (larger) exercise; the capstone is about *fixing* a Maven
  build, not converting one.

**Where to put it:** not inside this repo. Create a separate repository,
or use a branch of an existing one, and link to it from your write-up. The
lab repo is the *lesson*; the capstone is the *product*.

---

## 2. The checklist

Nine items. Each maps to one or more modules. Do them in the order below —
each one makes the next one easier.

### ☐ 1. Parent POM with `dependencyManagement` and `pluginManagement`

**Maps to:** module 02.

If the project is multi-module, the parent pom is where versions and plugin
config live, *once*. Children declare `<groupId>:<artifactId>` with no
`<version>` and no configuration. If the project is single-module, this is
still worth doing: extract the version strings into properties and pin the
plugin versions, so a future split into modules is a five-minute change.

**Smell test:** grep every pom for `<version>` inside a `<dependency>` or
`<plugin>`. Every match that is not the project's own version is a candidate
to move to the parent.

### ☐ 2. A BOM imported instead of hand-pinned versions

**Maps to:** module 02.

If the project uses a large family of artifacts with a known-compatible
version set (JUnit, Jackson, Spring Boot, Netty, anything with a published
BOM), import it via `<type>pom</type><scope>import</scope>` in the parent's
`<dependencyManagement>`. Delete every version you no longer need to pin
yourself.

**Smell test:** count how many `<version>` elements you can *remove* after
adding one `<dependency>` import. If the answer is more than 3, it was
worth doing.

### ☐ 3. Correct scopes everywhere

**Maps to:** modules 01, 03.

Every dependency has the *narrowest* scope that lets the code compile and
the tests run. Nothing test-only on the compile classpath. Nothing shipped
that the runtime does not need.

**Smell test:** run `./mvnw dependency:tree`. Grep for `junit`, `assertj`,
`mockito`, `testcontainers` — none of them should appear on the compile
classpath of the main artifact. Run `./mvnw dependency:analyze` and check
the "Used undeclared" section — every entry is a missing explicit
declaration.

### ☐ 4. `dependency:analyze` is clean

**Maps to:** module 03.

No *used undeclared* (a dependency that arrives transitively but that your
code compiles against — brittle the moment the transitive path changes).
No *unused declared* (a dependency you list but never import — dead weight).

**Smell test:** `./mvnw dependency:analyze` prints two sections. Both
should be empty, or every remaining entry should be documented in the
write-up with a one-sentence justification.

### ☐ 5. `dev` / `prod` profiles where they genuinely help

**Maps to:** module 05.

Add profiles **only where they solve a real problem**: a different endpoint
in `application.properties`, a different JDK toolchain, an optional
integration-test suite. A profile that merely duplicates the default build
is worse than no profile — it hides bugs behind a flag.

**Smell test:** `./mvnw help:active-profiles`. Every active profile should
have a reason in one sentence. If you cannot articulate the reason, delete
the profile.

### ☐ 6. Wrapper committed, Maven version pinned

**Maps to:** modules 01, 07.

`mvnw`, `mvnw.cmd`, and `.mvn/wrapper/maven-wrapper.properties` are in the
repository. `distributionUrl` pins a specific Maven version. Every script
and CI config uses `./mvnw`, never plain `mvn`.

**Smell test:** clone the repo into a fresh directory on a machine that
has *never* had Maven installed. `./mvnw clean verify` should work. If it
does not, the wrapper is not doing its job.

### ☐ 7. Unit tests on surefire, integration tests on failsafe

**Maps to:** module 07.

Unit tests are `*Test` and run at the `test` phase via surefire.
Integration tests are `*IT` and run at the `integration-test` phase via
failsafe. The failsafe plugin is declared with the standard two-goal
execution block (`integration-test` and `verify`). No test class is
ambiguous about which plugin owns it.

**Smell test:** `./mvnw test` should pass in under a minute on a
well-designed project — it runs only unit tests. `./mvnw verify` runs
everything, and may take longer. If `mvn test` is slow, some `*IT` tests
are running in the wrong place.

### ☐ 8. CI runs `./mvnw -B verify` on every push

**Maps to:** module 07.

A single CI workflow (GitHub Actions, Jenkins, GitLab CI — whatever the
project already uses) that runs `./mvnw -B -ntp clean verify` on every
push and every pull request. The `~/.m2/repository` is cached. A failure
in `verify` blocks the merge.

**Smell test:** click the CI badge. It should be green, and it should have
run on the last commit. If the badge is stale or missing, the CI story is
aspirational, not real.

### ☐ 9. Short write-up

**Maps to:** every module.

Fill in `write-up.md` in this folder. What was wrong, what you changed,
what you learned, what you would do differently next time. This is the
deliverable you actually show to a reviewer or an interviewer.

**Smell test:** read it a week later. Can you still explain each change?
Could a colleague repeat the migration on a different project using your
notes?

---

## 3. The write-up (`write-up.md`)

The template is already in `08-capstone/write-up.md`. It has four sections:

1. **Before** — what the build looked like when you started. Include the
   real pom (or a link to the commit before your changes). List the
   specific problems: hand-pinned versions, mixed scopes, no wrapper, no
   CI, unclear module boundaries, whatever you actually found.

2. **After** — what it looks like now. Include the new poms (or a link to
   the final commit). List the specific improvements, one per checklist
   item, with the one-line *why* for each.

3. **Measured impact** — numbers, where you have them. CI time before and
   after. Number of `<version>` elements removed. Lines of pom XML
   removed. `dependency:analyze` output before and after. Anything you can
   quantify.

4. **What I learned** — free-form. What surprised you. What you would tell
   a colleague starting the same migration. What you would do differently
   if you started today.

**Length:** two to four pages is right. Long enough to be specific, short
enough that a reviewer will read it. If you can say something in a sentence
and a diff, do not write a paragraph.

---

## 4. Pitfalls (and how to recover)

Every migration hits at least one of these. They are not failure — they
are the interesting part.

| Pitfall | Symptom | Recovery |
|---|---|---|
| **Scope reduction breaks compilation** | After narrowing a scope from `compile` to `test`, a main-source file no longer compiles | The dependency *was* on the wrong scope; either the code using it belongs in test sources, or the dependency is genuinely `compile` and something else was making it *look* test-only. |
| **Moving a version to `dependencyManagement` changes resolution** | A transitive dependency shifts version, or two libraries stop seeing eye to eye | You replaced a hand-pinned version with a managed one that a BOM or parent now supplies differently. Read the `dependency:tree -Dverbose` line for the artifact; the `version managed from` annotation is your clue. |
| **Enabling the wrapper breaks a script** | A shell script that assumed `mvn` on the PATH now fails on a machine where it is not installed | Update the script to `./mvnw`. This is the *point* of the wrapper — the failure is a sign it is working. |
| **CI takes longer, not shorter** | You added a cache, but the build time went *up* | The cache key is wrong (hashes too much), or the cache is empty on every run (does not hash what you think). Check the "Cache restored from key" line. |
| **`dependency:analyze` reports false positives** | It says a dependency is "unused" but you know it is used reflectively | Add `<ignoredDependencies>` for that artifact. Document *why* in the pom with a comment — a false positive is worth a one-line note. |
| **Profiles multiply and confuse** | `help:active-profiles` shows five profiles active on a default build | Delete every profile that does not have a one-sentence reason. Profiles are not free — each one is another axis of behaviour to test. |

**Rule of thumb:** if a change feels like it is getting larger than the
project, stop and do it in a branch. The migration is a sequence of small
reversible steps, not a weekend rewrite.

---

## 5. Definition of done

You are finished when **all** of the following are true:

- [ ] Every item in the checklist in §2 is done, and the write-up in
  `write-up.md` justifies it.
- [ ] `./mvnw -B -ntp clean verify` passes on a **fresh clone** on a
  machine that has never had Maven installed.
- [ ] CI is green on the latest commit, and the badge is on the README.
- [ ] `./mvnw dependency:analyze` produces no unexplained warnings.
- [ ] `write-up.md` is at least two pages and at most four, and includes
  at least one **number** (build time, pom size, count of versions
  removed — anything measurable).
- [ ] You have added the three meta-questions in
  `docs/interview-questions.md` under `## 08 · capstone` and answered
  them in your own words.

---

## 6. What this unlocks

Once you have finished this module, you can honestly say — to yourself, to
a colleague, to an interviewer — the sentence from the root README:

> *I can design, debug, and explain a real multi-module Maven build.*

That is a materially different claim from "I have used Maven". Most Java
developers never get there, because most projects never have a reason to
force them. You just did.

---

## Links

- **Write-up template:** [`write-up.md`](./write-up.md)
- **Lab interview questions:** [`../docs/interview-questions.md`](../docs/interview-questions.md) → `## 08 · capstone`
- **All modules:** [`../README.md`](../README.md) → Roadmap

---

## Design notes / why made these choices

- **Two files, not one.** `README.md` is the mission; `write-up.md` is the workspace. Splitting them means a learner can read the instructions without scrolling past their own draft, and the write-up is a real, self-contained document they can share.
- **The checklist is numbered 1–9 and *ordered*.** Each item makes the next easier (wrapper first, versions second, scopes third, checks last). Presenting them unordered would leave every learner doing them in a random order and hitting avoidable pitfalls.
- **Every checklist item has a "smell test"** — a concrete command or observation that tells you whether it is done. "Add a BOM" is aspirational; "grep every pom for `<version>` inside `<dependency>`" is actionable.
- **The pitfalls table is not decoration.** Every row is a real failure mode that shows up in actual migrations, with a specific recovery. The point is to make the learner *not* panic when something goes wrong on day 1.
- **The "measured impact" table in the write-up template has empty cells for numbers the learner might not have.** Blank cells with a footnote are honest; fake numbers are not. This is intentional modelling of good engineering communication.
- **The DoD requires a *number*.** "At least one measurable outcome" forces the learner to produce something a reviewer can verify. A write-up that is all prose is a story; a write-up with numbers is evidence.
- **`What is still wrong` is a required section.** Every real migration leaves something behind. Requiring it normalises honesty, and it distinguishes learners who understand "done" vs "finished".
- **Q1 is a "first two days" answer**, not a "first month" answer — it fits the scale of the capstone, and it demonstrates the *sequencing* that is the actual skill.
- **Q2 names three tiers explicitly.** Interviewers love categories. "Tier 1 / Tier 2 / Tier 3" is memorable and defensible; "it depends" is not.
- **Q3 lists eight observable properties**, not a narrative. This is the format that reads well in an interview and in a performance review. The closing heuristic ("would you be comfortable being paged at 3 a.m.?") is the memorable one-liner.
- **No code, no tests, no lab exercises.** Everything else in this repo has those; the capstone's teaching is that *the migration itself is the exercise*. Adding a toy demo would defeat the point.
