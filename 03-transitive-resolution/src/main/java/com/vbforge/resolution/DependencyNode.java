package com.vbforge.resolution;

import java.util.List;

/**
 * A node in a resolved dependency graph — the artifact plus its direct children.
 *
 * <p>The graph is modelled as a tree from the perspective of the project being
 * resolved. It is deliberately small: enough to reason about nearest-wins,
 * scope mediation and exclusions without pulling in Maven itself.
 */
public record DependencyNode(Artifact artifact, List<DependencyNode> children) {

    public DependencyNode {
        if (artifact == null) {
            throw new IllegalArgumentException("artifact must not be null");
        }
        children = children == null ? List.of() : List.copyOf(children);
    }

    /** A node with no children. */
    public static DependencyNode leaf(Artifact artifact) {
        return new DependencyNode(artifact, List.of());
    }

    /** Convenience factory: {@code of(artifact, child1, child2, ...)}. */
    public static DependencyNode of(Artifact artifact, DependencyNode... children) {
        return new DependencyNode(artifact, List.of(children));
    }
}