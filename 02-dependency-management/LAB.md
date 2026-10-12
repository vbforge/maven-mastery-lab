# LAB — dependency-management

Work in a scratch folder (`02-dependency-management/lab/`), not in the module sources.
Create a tiny parent + child: `lab/parent/pom.xml` (packaging `pom`) and `lab/parent/child/pom.xml`.
Write your observations in the **Notes** under each exercise.

## 1. Declared vs managed
1. In the parent, put `assertj-core` **only** in `<dependencyManagement>`. In the child, add nothing.
2. Run `../mvnw dependency:tree` in the child → is assertj there?
3. Add `assertj-core` (no version) to the child's `<dependencies>` and run it again.

**Notes:** what changed, and what does that prove about `dependencyManagement`?

## 2. Inheritance of version AND scope
1. Manage assertj with `<scope>test</scope>` in the parent; declare it in the child with no version and no scope.
2. `../mvnw help:effective-pom -Doutput=effective.xml` and find the assertj block.

**Notes:** where did the child's version and scope come from?

## 3. Version precedence
Declare the same artifact with a different version at each level, one at a time, and run `dependency:tree` after each step:
1. parent `dependencyManagement` → 3.25.3
2. child `dependencyManagement` → 3.24.2
3. child explicit `<version>` → 3.23.1

**Notes:** which version won at each step? Does it match the order in the README?

## 4. BOM import
1. Remove JUnit's versions and import `org.junit:junit-bom:5.10.2` (`type=pom`, `scope=import`) in the parent.
2. Declare `junit-jupiter` in the child without a version → `dependency:tree`.
3. Override one junit artifact's version in your own `dependencyManagement` and check which one wins.

**Notes:** how many artifacts did one import manage? Why is a BOM import better than a custom parent here?

## 5. Version properties
1. Move assertj's version into `<assertj.version>` in the parent.
2. Override the property in the child's `<properties>` → `dependency:tree`.
3. Override from the command line: `../mvnw dependency:tree -Dassertj.version=3.23.1`.

**Notes:** which source of the property won? Order: CLI vs child vs parent.

## 6. pluginManagement
1. In the parent's `<pluginManagement>`, pin `maven-compiler-plugin` 3.13.0 with `<release>17</release>`.
2. In the child, declare the plugin with no version and no configuration.
3. `../mvnw compile` — read the log line `maven-compiler-plugin:<version>:compile`.
4. Remove the plugin from the child's `<plugins>` entirely and run again.

**Notes:** is the pinned version still used in step 4? Why? (hint: the compiler plugin is bound by the default lifecycle)

## Stretch
Run `../mvnw help:effective-pom` on the module `02-dependency-management/pom.xml` and find where
`junit-jupiter` gets its version from. Trace it back to the root `pom.xml`.