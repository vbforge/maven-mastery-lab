# LAB 01-maven-recap

Work in a scratch folder (`01-maven-recap/lab/`), not in the module sources.
Everything you build here is throwaway — the module tests do not see it.
Write your observations in the **Notes** under each exercise.

> All commands below assume you are inside `01-maven-recap/lab/<project>/`
> unless stated otherwise. Use the wrapper (`../../mvnw`) when it exists;
> plain `mvn` is fine for the first exercises, but Exercise 5 will show you why
> you should stop doing that.

## 1. A jar project by hand

1. Create `lab/hello/pom.xml` from scratch — no `mvn archetype:generate`.
   It must have `modelVersion`, `groupId=com.vbforge`, `artifactId=hello`,
   `version=1.0.0`, and **no `<packaging>`** element at all.
2. Create `lab/hello/src/main/java/com/vbforge/hello/App.java` with a `main`
   that prints one line.
3. Run `mvn -f pom.xml clean package`.
4. Inspect what Maven produced: `ls target/`, then
   `jar tf target/hello-1.0.0.jar`.

**Notes:** what packaging did Maven assume when you left it out? Which files
ended up inside the jar — where did the `.class` come from, and where did
`META-INF/MANIFEST.MF` come from? Which plugin created the jar?

## 2. Coordinates, in the log

1. Still in `hello/`, run `mvn help:evaluate -Dexpression=project.groupId -q -DforceStdout`
   then do the same for `project.artifactId`, `project.version`,
   `project.packaging`.
2. Rename the artifact to `Hello` (uppercase) and run `mvn clean package` again.
3. Now run `mvn dependency:get -Dartifact=com.vbforge:hello:1.0.0` from any
   directory (it will fail — that is the point).

**Notes:** which coordinate part did Maven itself reject? Which part was only
rejected by *you* noticing the naming rule in the README? Which command is the
GAV string used in, and what does the error message look like?

## 3. Scopes: JUnit must not leak into the jar

1. Add `junit-jupiter` and `assertj-core` to `hello/pom.xml` as
   `<scope>test</scope>` dependencies (no version — that comes later in module
   02; for now hard-code `5.10.2` and `3.25.3`).
2. Write a trivial `AppTest` under `src/test/java`.
3. `mvn clean package`, then `jar tf target/hello-1.0.0.jar`.
4. Now change the JUnit scope to `compile` and `mvn clean package` again.
   Look at the jar a second time.
5. Finally move JUnit to `provided` and repeat.

**Notes:** in step 3, is JUnit inside the jar? In step 4 — is it now? (Hint:
`package` does not bundle dependencies for `jar` packaging — you are checking
the *classpath*, not the jar contents. Where *would* you see the difference?
Try `mvn dependency:build-classpath -Dmdep.outputFile=cp.txt` in each case and
compare the three files.)

## 4. Lifecycle phases, one at a time

1. From a clean `hello/`, run each of these and after each one write down what
   is now inside `target/`:
    - `mvn validate`
    - `mvn compile`
    - `mvn test`
    - `mvn package`
    - `mvn verify`
    - `mvn install`
2. After `mvn install`, run
   `ls ~/.m2/repository/com/vbforge/hello/1.0.0/`.
3. Grep the log for `--- ` lines: `mvn package | grep -- "--- "`.

**Notes:** which phase first creates `target/classes`? Which phase first runs
surefire? Which phase first creates a jar? Which phase copies the jar into
`~/.m2`? Why did `mvn package` also run your tests even though you never asked
for the `test` phase? List the plugin goals the `--- ` lines showed for
`mvn install` in order.

## 5. Wrapper: pin Maven, forget the PATH

1. From `hello/`, run `mvn wrapper:wrapper -Dmaven=3.9.6`.
2. Inspect the files that appeared: `.mvn/wrapper/maven-wrapper.properties`,
   `mvnw`, `mvnw.cmd`, and (maybe) `maven-wrapper.jar`.
3. Run `./mvnw -v` — which Maven version does it report?
4. Temporarily change `distributionUrl` in `maven-wrapper.properties` to
   `3.8.8`, run `./mvnw -v` again, then change it back.
5. Run `./mvnw clean verify` and compare the log to `mvn clean verify`.

**Notes:** which wrapper files are committed and which are not (check
`.gitignore` in the root repo)? What breaks on a teammate's machine if you
*don't* commit `mvnw`? Where does the wrapper download Maven to, and is that
inside or outside your project?

## 6. Overriding plugin config (breaking the default binding)

1. In `hello/pom.xml`, add the `maven-compiler-plugin` with
   `<release>11</release>` inside `<configuration>`. Recompile — what does
   `javap -v target/classes/.../App.class | grep major` say?
2. Then try `<release>8</release>`. Does it still compile? Why not?
3. Remove the plugin block entirely and recompile — which `release` is used,
   and where does it come from? (Look at the parent `pom.xml` in the repo root.)

**Notes:** which class-file major version corresponds to Java 11 vs 17? Did
declaring the plugin *override* a default binding, or *add* one? What does
this tell you about the difference between `build/plugins` and
`build/pluginManagement` (which you will meet properly in module 02)?

## Stretch

1. Delete `target/` and run `mvn clean package` in *one* command. Which of
   the two lifecycles (`clean`, `default`) runs first, and why does the order
   matter?
2. Run `mvn package -DskipTests` and then `mvn package -Dmaven.test.skip=true`.
   Diff the logs. Which one still compiles the tests? (This is the seed of a
   module-07 task.)
3. `mvn -X package | head -50` — find the line where Maven prints the
   `groupId:artifactId:version:packaging` of *your* project. That is the GAV
   you will use in every `-pl` command later.