# LAB — cli-and-ci-in-practice

Work in a scratch folder (`07-cli-and-ci-in-practice/lab/`), not in the module
sources. All exercises target a throwaway project you create here.
Write your observations in the **Notes** under each exercise.

> All commands below assume you are inside `07-cli-and-ci-in-practice/lab/`.
> Adjust the wrapper path (`../../mvnw` or `../../../mvnw`) to match your
> layout.

## 1. `verify` vs `install`

1. Create `lab/cli/pom.xml` (jar packaging) with a trivial `App` class and a
   passing `AppTest`.
2. `../../mvnw clean verify`, then check whether the artifact landed in
   `~/.m2/repository`.
3. `../../mvnw clean install`, then check again.

**Notes:** what is the difference in `~/.m2/repository/<group>/<artifact>/`?
Which is the right choice for CI, and why? What does `./mvnw clean verify`
**not** do that `install` does?

## 2. `-DskipTests` vs `-Dmaven.test.skip=true`

1. In `lab/cli/src/test/java/.../AppTest.java`, replace the file with one
   that has a syntax error (missing brace, or an unused import that references
   a missing class).
2. Run `../../mvnw clean test -DskipTests` — observe what fails.
3. Fix the syntax error, and now add a test that *runs* but *fails* (an
   assertion). Run `../../mvnw clean test -DskipTests` again.
4. Run `../../mvnw clean test -Dmaven.test.skip=true`.

**Notes:** in step 2, did the build fail even though you "skipped tests"?
Why? In step 4, did compilation of `src/test/java` even happen? What is the
exact plugin-log line that proves it? Which flag would you use in a fast
local iteration loop, and which one in a CI "package only" job?

## 3. surefire vs failsafe

1. In `lab/cli/pom.xml`, configure `maven-failsafe-plugin` (copy the standard
   `<executions>` block from the docs).
2. Add `AppIT.java` in `src/test/java` — an "integration" test (it can be a
   trivial assertion).
3. Run `../../mvnw clean test` — does it run `AppIT`?
4. Run `../../mvnw clean verify` — does it now?

**Notes:** which phase runs surefire? Which phase runs failsafe? What does
`mvn package` do with `AppIT`? Why is it useful that failsafe runs *after*
`package` (hint: the built jar exists at that point)? Look at
`target/failsafe-reports/` and `target/surefire-reports/` — do both exist
after `verify`?

## 4. `-B`, `-q`, `-X`

1. `../../mvnw clean test 2>&1 | wc -l` — note the line count.
2. `../../mvnw -B clean test 2>&1 | wc -l` — note the difference.
3. `../../mvnw -q clean test 2>&1 | wc -l`.
4. `../../mvnw -X clean test 2>&1 | wc -l` — you may want `head -500` on
   this one, `-X` is extremely verbose.
5. `../../mvnw -ntp clean test 2>&1 | wc -l`.

**Notes:** which flag removed ANSI escape codes from the output? Which
dramatically reduced the log? Which produced a log you would never want in
CI? Which is the smallest change to the default output? Which of these would
you add to `build.yml`, and which would you only ever run locally?

## 5. Parallel builds

1. If you have the module 06 `sample-app` still available, `cd` there.
2. `time ../../mvnw clean verify` — the full reactor, sequential.
3. `time ../../mvnw -T 1C clean verify` — one thread per core.
4. `time ../../mvnw -T 4 clean verify` — fixed at four.

**Notes:** how big is the difference on your machine? Does the reactor
`Reactor Build Order:` block change under `-T`? Does the per-module summary
at the end of the build change? Which modules ran in parallel and which did
not (hint: `sample-service` must wait for `sample-core`)? Is `-T` worth
enabling by default in CI?

## 6. Review the CI workflow

Open the repo's `.github/workflows/build.yml` (created earlier in this lab).
Read it line by line with the following questions in hand.

**Notes:** which JDK distribution is used, and why does that matter for
reproducibility? Which Maven is used — `mvn` or `./mvnw`? What does
`cache: maven` do, and what does it key on? Which Maven flags are passed,
and which of them would be *wrong* to omit in CI? What does the workflow do
when a job fails (does it upload anything)? Why is `permissions: contents:
read` there, and what is the effect of removing it?

## Stretch

1. Add `-Dmaven.test.skip=true` to `build.yml` and push to a branch — does
   the CI badge still go green? Would you want that on `main`? Why is
   `-DskipTests` sometimes acceptable in CI and `-Dmaven.test.skip=true`
   almost never?
2. Enable `-T 1C` in `build.yml`. Compare the wall-clock time of two CI runs
   (before and after). What is the actual speedup on GitHub's runner?
3. Change `clean verify` to `clean install` in `build.yml` — what does the
   run do differently, and is there any effect you can observe from the
   workflow logs alone? What would you break if you did this on a real
   multi-JVM CI pipeline?
4. Look at the "Cache restored from key" line in the workflow log. What is
   the cache key composed of? What happens to the cache when you bump a
   version in the root `pom.xml`?