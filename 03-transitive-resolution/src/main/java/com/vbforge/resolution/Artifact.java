package com.vbforge.resolution;

/**
 * A single node's identity in a dependency graph: coordinates plus the scope
 * under which the artifact is visible.
 *
 * <p>Note: in Maven, a dependency's <em>scope</em> is a property of the edge
 * that leads to it (the {@code <dependency>} declaration), not of the artifact
 * itself. This record flattens that: each node carries the effective scope it
 * has in the project being resolved — which is what a reader of
 * {@code dependency:tree} actually sees.
 */
public record Artifact(String groupId, String artifactId, String version, String scope) {

    /** {@code groupId:artifactId} — how Maven identifies an artifact family. */
    public String ga() {
        return groupId + ":" + artifactId;
    }

    /** {@code groupId:artifactId:version} — a fully qualified coordinate. */
    public String gav() {
        return groupId + ":" + artifactId + ":" + version;
    }
}