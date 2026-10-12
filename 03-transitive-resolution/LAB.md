# LAB — transitive-resolution

Work in a scratch folder (`03-transitive-resolution/lab/`), not in the module sources.
Create a tiny project `lab/tree/pom.xml` (jar packaging) and experiment with it.
Write your observations in the **Notes** under each exercise.

> All commands assume you are inside `03-transitive-resolution/lab/tree/`.
> Use the wrapper: `../../mvnw` if you are inside `lab/tree/`, `../mvnw` if you
> are inside `lab/`. Pick whatever relative path works and stay consistent.

## 1. Watch transitivity happen

1. Create `lab/tree/pom.xml` with **one** dependency: `org.springframework:spring-context:6.1.6`.
2. Run `../../../mvnw dependency:tree` (adjust path to your wrapper).
3. Now add a second dependency: `org.springframework:spring-web:6.1.6`.

**Notes:** how many artifacts did one dependency pull in? Which scope did
`micrometer-observation` appear with? Why does `jakarta.annotation-api` show
up at all — you did not declare it. Did anything change when you added
`spring-web`?

## 2. Reproduce a version conflict

1. In `lab/tree/pom.xml`, declare **both** of these as direct dependencies:
   - `org.apache.commons:commons-text:1.10.0` → depends on `commons-lang3:3.12.0`
   - `org.apache.commons:commons-text:1.11.0` → depends on `commons-lang3:3.13.0`
2. Run `dependency:tree`.

Maven will silently pick one. To see which one and why, move on to exercise 3.

**Notes:** which `commons-lang3` version ended up on the classpath? If you
cannot tell from the tree, what flag do you need? (Hint: `-Dverbose`.)

## 3. Diagnose with `-Dverbose`

Run `dependency:tree -Dverbose`.

**Notes:** copy the exact "omitted for conflict" line you see. What is the
depth of the winner? Of the loser? Does this match the nearest-wins rule? Now
try reordering the two direct dependencies in the pom and run again — did the
winner change? Why or why not?

## 4. Fix it three ways

Reset the pom to the state from exercise 2, then apply the three fixes one at
a time and re-run `dependency:tree` after each:

1. **Declare the winner directly.** Add a direct dependency on the version
   you want (e.g. `commons-lang3:3.13.0`). Depth becomes 1 → nearest-wins
   picks it.
2. **Pin it in `dependencyManagement`.** Remove the direct declaration and
   instead put `commons-lang3` (with the version you want) in
   `<dependencyManagement>`.
3. **Exclude the loser.** Remove the management entry and add an
   `<exclusion>` on the dependency that pulls the unwanted version.

**Notes:** which fix would you choose on a real project, and why? Which one
is portable to a multi-module build? Which one leaves a stray "unused"
declaration that `dependency:analyze` will later flag?

## 5. Watch scope mediation

1. In `lab/tree/pom.xml`, declare `org.postgresql:postgresql:42.7.3` with
   `<scope>runtime</scope>`.
2. Run `dependency:tree`.

**Notes:** what scope does PostgreSQL show up with? Now change the scope to
`test` and run again. Where did it go? What happens if you put the whole
dependency inside `<dependencyManagement>` only — is it on the classpath at
all?

## 6. Sanity checks: `dependency:analyze` + Enforcer

1. Run `dependency:analyze`. Note the "Used undeclared" and "Unused declared"
   sections.
2. Add the Enforcer plugin (see `TransitiveResolutionDemo.enforcerSnippet()`)
   to `lab/tree/pom.xml`.
3. Re-run `dependency:tree` — then deliberately re-introduce the conflict
   from exercise 2 and run `mvn verify`.

**Notes:** which of your declarations did `dependency:analyze` flag as
"unused"? Does that surprise you? What does the Enforcer error message
actually list — the conflicting artifact, the two versions, or the path to
each? Does the build fail at `validate` or later?

## Stretch

1. Compare `<exclusions>` with `<optional>true</optional>` on the *same*
   dependency. Which one changes your own classpath, and which one changes
   the classpath of a *consumer*?
2. `dependency:tree -DoutputFile=tree.txt -DoutputType=text` then
   `dot -Tpng` — build a visual graph. Is the winner obvious from the picture?
3. `dependency:tree -Dincludes=commons-lang3` — what does the output look
   like with `-Dincludes` compared to the unfiltered tree?