package com.vbforge.reactor;

import java.util.List;

/**
 * A minimal description of a single pom, used to classify it as an
 * <em>aggregator</em>, a <em>parent</em>, both, or neither.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code packaging} — {@code "pom"}, {@code "jar"}, etc.</li>
 *   <li>{@code declaresModules} — the ids listed in {@code <modules>} (empty
 *       if there is no aggregator role)</li>
 *   <li>{@code isParentOf} — the ids of modules that declare <em>this</em> pom
 *       in their {@code <parent>} block</li>
 * </ul>
 */
public record PomSummary(String artifactId, String packaging,
                         List<String> declaresModules, List<String> isParentOf) {

    public PomSummary {
        if (artifactId == null || artifactId.isBlank()) {
            throw new IllegalArgumentException("artifactId must not be blank");
        }
        declaresModules = declaresModules == null ? List.of() : List.copyOf(declaresModules);
        isParentOf = isParentOf == null ? List.of() : List.copyOf(isParentOf);
    }

    public static PomSummary jar(String id) {
        return new PomSummary(id, "jar", List.of(), List.of());
    }

    /** An aggregator with no children: the "bundle and build together" role only. */
    public static PomSummary aggregatorOnly(String id, String... declaresModules) {
        return new PomSummary(id, "pom", List.of(declaresModules), List.of());
    }

    /** A parent with no {@code <modules>}: shares configuration only. */
    public static PomSummary parentOnly(String id, String... isParentOf) {
        return new PomSummary(id, "pom", List.of(), List.of(isParentOf));
    }

    /** The common "root" pom: declares modules <em>and</em> is inherited from. */
    public static PomSummary aggregatorAndParent(String id,
                                                 List<String> declaresModules,
                                                 List<String> isParentOf) {
        return new PomSummary(id, "pom", declaresModules, isParentOf);
    }
}