# LAB — repositories-settings

Work in a scratch folder (`04-repositories-settings/lab/`), not in the module
sources. Everything you build here is throwaway.
Write your observations in the **Notes** under each exercise.

> All commands below assume you are inside `04-repositories-settings/lab/`.
> Use `../mvnw` (adjust the relative path to your wrapper). `settings.xml` is
> per-user, so changes here affect *all* your projects — be careful.

## 1. Inspect the local repository

1. Pick an artifact you already have, e.g. `org.junit.jupiter:junit-jupiter:5.10.2`.
2. `ls ~/.m2/repository/org/junit/jupiter/junit-jupiter/5.10.2/`.
3. Open the `.pom` and one of the `.sha1` files (they are plain text hashes).
4. Remove the artifact directory and run `../mvnw -pl ../ test` from the module
   root — watch Maven re-download it.

**Notes:** which extra files does Maven leave next to the jar? What is
`_remote.repositories` for? What does removing the directory simulate on a
teammate's machine? Why is that usually *not* the fix for "why does this build
fail only for me?".

## 2. Force a fresh download with `-U` and inspect with `-X`

1. `../mvnw -U -X dependency:get -Dartifact=org.assertj:assertj-core:3.25.3`
2. Search the (very long) log for lines containing `https://`.
3. Now re-run **without** `-U`.

**Notes:** how many HTTP requests were made with `-U`? Without it? Which URL
did Maven hit first — the local file, or the remote? Which log line proves
Central was consulted? Copy one line that shows the resolved URL.

## 3. Break the network and use offline mode

1. Disconnect the network (or point `distributionUrl`/repos at an unreachable
   host — do not edit your real settings, use a throwaway `settings.xml`).
2. `../mvnw -o dependency:get -Dartifact=org.apache.commons:commons-lang3:3.14.0`
3. Reconnect. Do it again **without** `-o`.

**Notes:** what is the *exact* error message in offline mode when the artifact
is present locally vs. absent? Does offline mode still work for a fully cached
build? What happens if you mix `-o` and `-U` on the same command line?

## 4. Add a custom remote repository (in the pom)

1. Create `lab/repo/pom.xml` (jar packaging) and declare a `<repositories>`
   element pointing at Maven Central explicitly (overriding nothing, just
   spelling it out).
2. Add a second `<repository>` with a bogus URL.
3. `../mvnw -X dependency:resolve -f lab/repo/pom.xml | grep -E "(Trying|Downloading)"`.

**Notes:** does Maven contact the bogus repo? In what order? What if the
artifact is found in the first repo — does Maven still try the second? Where
in the log do you see the "this repository is disabled for X" line if you add
`<snapshots><enabled>false</enabled></snapshots>`?

## 5. A throwaway mirror in `settings.xml`

Create `lab/settings.xml` with a `<mirrors>` block. Then run every command with
`-s lab/settings.xml` — **never edit your real `~/.m2/settings.xml`**.

1. Add a mirror with `<mirrorOf>central</mirrorOf>` pointing at a mirror of
   Central you trust (e.g. `https://repo1.maven.org/maven2`).
2. Force a re-download of a small artifact with `-U`.
3. Change `mirrorOf` to `*`. Re-run.

**Notes:** what URL shows up in `-X` output for the `central` mirror? Now
change `mirrorOf` to `*,!central` — is the mirror still used? What does
`external:*` do differently from `*`? Which pattern would you use for a
corporate proxy and why?

## 6. SNAPSHOT repository toggles

1. In `lab/repo/pom.xml`, add a repository with
   `<snapshots><enabled>false</enabled></snapshots>` and
   `<releases><enabled>true</enabled></releases>`.
2. Try to resolve an artifact with a `-SNAPSHOT` version from it.

**Notes:** what error do you see? Now flip the flags. What changes? Why does
Maven Central deliberately disable snapshots? What is the practical
consequence if a public repo *did* serve them?

## Stretch

1. `../mvnw help:effective-settings` — is that a real goal, or a typo? What
   should you use instead to see which settings.xml Maven picked up?
2. `../mvnw -Dmaven.repo.local=/tmp/m2-alt clean verify` — what is different
   about the first run compared to the second?
3. `../mvnw dependency:get -Dartifact=com.vbforge:does-not-exist:1.0.0` —
   copy the *full* error block. Identify the three parts: which repos were
   tried, in what order, and which one produced the final "could not resolve".