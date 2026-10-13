# Capstone write-up — <your project name here>

> **Template.** Copy this file, rename it (e.g. `write-up-my-service.md`), and
> fill it in as you migrate your project. Keep it in this folder or in your
> own repo — the point is that the write-up exists, not where it lives.

---

## Metadata

| Field | Value |
|---|---|
| Project | `<project name / link>` |
| Repository | `<URL, or "private — available on request">` |
| Migration started | `<date>` |
| Migration finished | `<date>` |
| Time spent | `<hours, roughly — a number, not "a while">` |
| Modules | `<count>` |
| Artifacts | `<count, if multi-module>` |
| Lines of pom XML (before) | `<count>` |
| Lines of pom XML (after) | `<count>` |

---

## Before

Describe the build you started with. Be specific about what was *wrong*
— not "it was messy" but "every child pom re-declared the JUnit version".

Include:

- A link to the commit / tag *before* your changes (or paste the original
  parent pom in a `<details>` block).
- A bullet list of concrete problems. One per line. Number them — the
  write-up's *After* section refers back to these numbers.
- Anything you noticed that you *did not* fix, and why (out of scope,
  needs a code change, needs buy-in from another team).

Example problems:

1. `<groupId>` was a placeholder from the original scaffold — never
   changed to the company's actual domain.
2. Every module hard-coded `junit-jupiter:5.9.2` in its own `<dependencies>`.
3. `src/main/java` used `org.junit.jupiter.api.Assertions` — JUnit was
   `compile`-scoped, so it shipped in the production jar.
4. No wrapper — the build required Maven 3.6+ on the PATH.
5. Integration tests were named `*Test` — they ran in the `test` phase
   and made `mvn test` take 8 minutes.
6. No CI. The only thing that verified the build was the one developer
   who could build it locally.

---

## After

For each item above, describe the change and the *why*. Cross-reference
the module that taught it.

| # | Change | Module | Why |
|---|---|---|---|
| 1 | `groupId` set to `com.<company>` in the parent | 01 | Reverse-domain convention; the placeholder was breaking repository deploys. |
| 2 | JUnit version moved to the root `<dependencyManagement>` | 02 | One edit; every module follows. |
| 3 | JUnit scope narrowed to `test` | 01 | Production jar was 12 MB; it is now 3 MB. |
| 4 | Wrapper committed with `distributionUrl` pinned to 3.9.6 | 01, 07 | Fresh clone on any machine builds the same way. |
| 5 | IT tests renamed to `*IT`; failsafe declared | 07 | `mvn test` dropped from 8 min to 40 s; `mvn verify` runs the ITs. |
| 6 | GitHub Actions workflow runs `./mvnw -B -ntp clean verify` | 07 | Merge is blocked on a red build. |

Include the *final* parent pom (or a link to the final commit) in a
`<details>` block. Include a `git diff` of a child pom if the change is
illustrative.

---

## Measured impact

Numbers, where you have them. Even rough numbers beat prose.

| Metric | Before | After | Change |
|---|---|---|---|
| Lines of pom XML | `<n>` | `<n>` | `-<%>` |
| Distinct JUnit versions across modules | `<n>` | `1` | `-<n>` |
| `<version>` elements in child poms | `<n>` | `0` | `-<n>` |
| `mvn test` duration (local) | `<n> s` | `<n> s` | `-<%>` |
| `mvn verify` duration (CI, cold cache) | `<n> s` | `<n> s` | `-<%>` |
| `mvn verify` duration (CI, warm cache) | n/a | `<n> s` | — |
| `dependency:analyze` warnings | `<n>` | `<n>` | `-<n>` |
| Production jar size | `<n> MB` | `<n> MB` | `-<%>` |

If you do not have a number for a row, put `—` and say why in a footnote.
A blank cell is better than a fake number.

## What I learned

Free-form. Three to six short paragraphs. Answer at least:

1. **What surprised me.** The thing you expected to be easy and was not,
   or vice versa. The most interesting part of a migration is almost
   always a surprise.

2. **What I would tell a colleague starting the same migration tomorrow.**
   Two or three concrete pieces of advice. "Do the wrapper first, because
   everything else is easier once you control the Maven version" is a good
   example.

3. **What I would do differently.** Not "I would plan more" — that is a
   non-answer. Something specific: "I moved scopes before I moved versions,
   and had to redo the scopes after the BOM import changed two transitive
   versions. Next time I would import the BOM first."

4. **What is still wrong.** Honest inventory. Every project has something
   you did not fix. Listing it is a feature, not a confession — it shows
   you know the difference between "done" and "finished".

---

## Reviewer checklist

For the person reading this write-up (possibly you, a week later):

- [ ] Every item in the *Before* list has a corresponding row in the
  *After* table.
- [ ] Every *After* row cites a module.
- [ ] Every *Measured impact* row has a number or an explanation for why
  it does not.
- [ ] The *What I learned* section names at least one **specific**
  technical detail, not just a general sentiment.
- [ ] The *What is still wrong* section is not empty. (If it is empty,
  look harder — every real migration leaves something behind.)