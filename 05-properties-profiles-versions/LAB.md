# LAB — properties-profiles-versions

Work in a scratch folder (`05-properties-profiles-versions/lab/`), not in the
module sources. Everything you build here is throwaway.
Write your observations in the **Notes** under each exercise.

> All commands assume you are inside `05-properties-profiles-versions/lab/`.
> Adjust the wrapper path to `../mvnw` (or whichever relative path matches your
> layout).

## 1. Custom and built-in properties

1. Create `lab/props/pom.xml` (jar packaging) with:
   ```xml
   <properties>
       <app.env>dev</app.env>
       <app.name>my-app</app.name>
   </properties>
   ```
2. Run `../../mvnw help:evaluate -Dexpression=project.version -q -DforceStdout` and
   the same for `project.basedir`, `project.build.directory`, `app.env`.
3. Now override `app.env` from the CLI:
   `../../mvnw help:evaluate -Dexpression=app.env -q -DforceStdout -Dapp.env=prod`.

**Notes:** which properties come from Maven itself, and which from your pom?
Does `-D` override a `<properties>` value defined in the pom? Which wins if you
also declare `<app.env>` in a child pom?

## 2. Resource filtering

1. Add `src/main/resources/application.properties` containing:
   ```
   app.name=${app.name}
   app.version=${project.version}
   app.unknown=${not.a.real.property}
   ```
2. Configure filtering (see `PropertiesProfilesVersionsDemo.filteringSnippet()`).
3. `../../mvnw clean package` and inspect `target/classes/application.properties`.

**Notes:** which placeholders were substituted? What happened to
`${not.a.real.property}` — is it removed, left as-is, or replaced with an
empty string? What happens if you set `<filtering>false</filtering>`?

## 3. dev / prod profiles switching a property

1. In `lab/props/pom.xml` add two profiles:
    - `dev` with `<activeByDefault>true</activeByDefault>` and
      `<env.endpoint>http://localhost:8080</env.endpoint>`
    - `prod` with `<env.endpoint>https://api.example.com</env.endpoint>`
2. `../../mvnw help:active-profiles`
3. `../../mvnw -Pprod help:active-profiles`
4. With resource filtering enabled, add `endpoint=${env.endpoint}` to
   `application.properties` and rebuild with and without `-Pprod`.

**Notes:** in step 3, is `dev` still active? Why? What does this tell you about
`activeByDefault` and `-P` on the same command line? Which `endpoint` ends up
in `target/classes/application.properties` in each case?

## 4. Profile activation by JDK, OS, and property

Add three more profiles to `lab/props/pom.xml`:

- `<id>on17</id>` with `<activation><jdk>17</jdk></activation>`
- `<id>onLinux</id>` with `<activation><os><name>Linux</name></os></activation>`
- `<id>onMetrics</id>` with `<activation><property><name>metrics.enabled</name></property></activation>`

Then:

1. `../../mvnw help:active-profiles`
2. `../../mvnw -Dmetrics.enabled=true help:active-profiles`
3. Try with a JDK activation you know will not match (e.g. `<jdk>1.8</jdk>`).

**Notes:** how many profiles are active by default? Does `activeByDefault` on
`dev` disable `on17`? Why or why not? Are the auto-activated profiles still
active when you pass `-Pprod`? Which activation source is *not* disabled by
`-P`?

## 5. SNAPSHOT vs release in practice

1. Change the version of `lab/props` to `0.1.0-SNAPSHOT`, `../../mvnw install`.
2. `ls ~/.m2/repository/com/vbforge/props/` — do you see a snapshot suffix?
3. Change the version to `0.1.0` (release), `install` again, then change it
   back to `0.1.0-SNAPSHOT` and try `install` a third time.

**Notes:** what does the directory name look like for the SNAPSHOT version?
Where is the timestamped jar? Why does Maven treat releases as immutable (it
refuses to overwrite an existing release unless you ask)? What is the
practical consequence of *not* bumping the version on every release?

## 6. CI-friendly versions with `${revision}`

1. In `lab/props/pom.xml`, replace the hard-coded version with:
   ```xml
   <version>${revision}</version>
   <properties>
       <revision>0.1.0-SNAPSHOT</revision>
   </properties>
   ```
2. `../../mvnw install`
3. `../../mvnw -Drevision=1.0.0 install`
4. Inspect the artifact directories in `~/.m2/repository/com/vbforge/props/`.

**Notes:** what happens if `${revision}` has **no** default value? Why does
Maven require the property to be set (either in the pom or on the CLI) for the
build to even start? What does this buy you in a multi-module build compared
to editing every pom?

## Stretch

1. `../../mvnw versions:display-dependency-updates` — how many artifacts of
   this lab have newer versions available? Pick one and try
   `versions:update-properties`.
2. `../../mvnw help:evaluate -Dexpression=project.properties -q -DforceStdout`
   — what does the raw properties map look like? Where do `java.version` and
   `maven.compiler.release` come from in this lab?
3. `../../mvnw -Pprod,metrics help:active-profiles` — can you activate more
   than one profile at once? Is there a comma syntax, or do you need
   `-P` twice?