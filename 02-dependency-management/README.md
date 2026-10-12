# 02-dependency-management

> **02-dependency-management** | Java 17+ | Package: `com.vbforge.maven_mastery.depmgmt`

## What's covered

| Topic | Key points |
|---|---|
| `dependencies` vs `dependencyManagement` | `dependencies` adds to the classpath; `dependencyManagement` only pins version/scope/exclusions |
| Parent POM inheritance | a child declares `groupId:artifactId` only — version and scope come from the parent |
| BOM import | `<type>pom</type>` + `<scope>import</scope>` pulls in a whole set of managed versions |
| Version precedence | explicit > own management > parent management > imported BOM |
| `pluginManagement` | same idea for plugins: pin version and configuration once |
| Version properties | `${assertj.version}` defined once, overridable in a child or with `-D` |
| `help:effective-pom` | the real, fully merged POM Maven actually uses |

## Files

```
src/
├── main/java/com/vbforge/depmgmt/
│   ├── DependencyManagementDemo.java    ← theory + annotated examples
│   └── DependencyManagementTasks.java   ← 🎯 your tasks — implement these methods
└── test/java/com/vbforge/maven_mastery/depmgmt/
    ├── DependencyManagementDemoTest.java    ← tests for the demo (already green)
    └── DependencyManagementTasksTest.java   ← 🧪 tests for your tasks (@Disabled until done)
LAB.md                                       ← hands-on exercises with the real mvn CLI
```

## 🎯 Tasks

Open `DependencyManagementTasks.java` and implement all 5 methods. Each has a detailed Javadoc.
Maps use the key format `groupId:artifactId`.

| # | Method | Topic |
|---|---|---|
| 1 | `effectiveVersion(declared, managed)` | explicit version wins, else managed, else error |
| 2 | `mergeManagement(parent, child)` | child entries override parent entries |
| 3 | `isBomImport(type, scope)` | recognise an imported BOM |
| 4 | `renderDependency(g, a, v, scope)` | produce a `<dependency>` snippet, omitting what is not needed |
| 5 | `unmanagedDependencies(declared, managed)` | declarations that have no version anywhere |

When all 5 are implemented, remove `@Disabled` from `DependencyManagementTasksTest`:

```bash
./mvnw test -pl 02-dependency-management -Dtest=DependencyManagementTasksTest
```

All 5 task groups must go **GREEN** ✅

## Key things to remember

```xml
<!-- parent: manage once -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <version>${assertj.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- child: use many times — no version, no scope -->
<dependencies>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
    </dependency>
</dependencies>
```

```
dependencyManagement alone  → NOTHING lands on the classpath
dependencies                → the artifact lands on the classpath

Version precedence (highest first):
  1. explicit <version> in the dependency
  2. the pom's own dependencyManagement
  3. the parent's dependencyManagement (inherited)
  4. imported BOMs (first import wins)

BOM import = <type>pom</type> + <scope>import</scope>, only inside dependencyManagement
A project has ONE parent but can import MANY BOMs
```

## Run

```bash
# tests of this module (demo + tasks)
./mvnw test -pl 02-dependency-management

# run the demo
./mvnw -q exec:java -pl 02-dependency-management \
    -Dexec.mainClass="com.vbforge.maven_mastery.depmgmt.DependencyManagementDemo"
```