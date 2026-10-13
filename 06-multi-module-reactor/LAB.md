# LAB — multi-module-reactor

The exercises below all target the **nested** `sample-app/` mini build. It is
a self-contained multi-module reactor with three modules; nothing else in the
repo is touched.

```bash
cd 06-multi-module-reactor/sample-app-parent
```

From that directory the wrapper lives at `../../mvnw`.

Write your observations in the **Notes** under each exercise.

## 1. Read the reactor build order

1. `../../mvnw validate` — this is the cheapest phase that still triggers
   reactor setup. No compilation, no tests.
2. Find the lines that start with `Reactor Build Order`.
3. Repeat with `../../mvnw clean verify` and watch the per-module summary at
   the end.

**Notes:** what is the order, and what determined it? Is the order you see in
`<modules>` (in `sample-app/pom.xml`) the same as the build order? Which
module built first, and why? Copy the summary block at the end of `verify` —
what does the `SUCCESS [  0.xyz s]` column tell you that a plain `mvn compile`
would not?

## 2. Build only one module — and watch it fail

1. `../../mvnw -pl sample-service clean verify`
2. Look at the error.

**Notes:** why does this fail even though `sample-service` compiles fine when
built as part of the whole reactor? Which artifact could Maven not resolve?
What would you have to do once (`../../mvnw install`) before `-pl
sample-service` alone would work?

## 3. Add upstream with `-am`

1. `../../mvnw -pl sample-service -am clean verify`
2. Read the reactor build order this time.

**Notes:** which modules built? Which did not, and why? Compare the reactor
order of `-pl sample-service -am` with the full-reactor order from exercise 1.
Now try `-pl sample-app -am` — how many modules built, and why is that
different?

## 4. Add downstream with `-amd`

1. `../../mvnw -pl sample-core -amd clean verify`
2. Then `../../mvnw -pl sample-service -amd clean verify`.

**Notes:** in the first case, which modules built? In the second? Why does
`-amd` on `sample-core` include *everything* while `-amd` on `sample-app`
includes only `sample-app`? Compare the output to `-am` on the same targets —
do you see the symmetry?

## 5. `-rf` — resume from a module

1. First, deliberately break a test in `sample-service` (add a failing
   assertion).
2. `../../mvnw clean verify` — watch the build stop at `sample-service`.
3. Fix the test.
4. `../../mvnw verify -rf sample-service` — resume from the failed module.

**Notes:** what does `-rf` do with the modules *before* the resume point? Are
they skipped or rebuilt? Does `-rf` require `-am` to work, or does it assume
the skipped modules are already installed? Which modules did Maven re-run?

## 6. Introduce a cycle on purpose

1. Edit `sample-core/pom.xml` to declare a dependency on `sample-service`:
   ```xml
   <dependency>
       <groupId>com.vbforge</groupId>
       <artifactId>sample-service</artifactId>
       <version>${project.version}</version>
   </dependency>
   ```
2. `../../mvnw validate`

**Notes:** copy the exact error text. Does Maven fail at parse, at validation,
or later? Does it name the modules in the cycle? What is the *correct* fix
(hint: extract the shared piece into a third module that both depend on)?

## Stretch

1. `../../mvnw -pl sample-service -am -amd clean verify` — what does the union
   of both selection flags produce? Is it ever useful in practice?
2. `../../mvnw help:effective-pom -pl sample-core | head -80` — how does the
   effective pom of a child differ from the parent's? Where do JUnit and
   AssertJ get their versions from?
3. `../../mvnw dependency:tree -pl sample-service` — how is the inter-module
   dependency `sample-core` shown? What scope does it have?
4. Delete `sample-app/sample-service/target/` and rerun `../../mvnw verify`.
   Does Maven rebuild only what is missing, or every module? Why?

---

### LAB task structure:

- `sample-app-parent/sample-core/pom.xml`
- `sample-app-parent/sample-service/pom.xml`
- `sample-app-parent/sample-app/pom.xml`

- `sample-app-parent/sample-core/src/main/java/com/vbforge/sample/core/Greeter.java`
- `sample-app-parent/sample-core/src/test/java/com/vbforge/sample/core/GreeterTest.java`
- `sample-app-parent/sample-service/src/main/java/com/vbforge/sample/service/GreetingService.java`
- `sample-app-parent/sample-service/src/test/java/com/vbforge/sample/service/GreetingServiceTest.java`
- `sample-app-parent/sample-app/src/main/java/com/vbforge/sample/app/Main.java`













