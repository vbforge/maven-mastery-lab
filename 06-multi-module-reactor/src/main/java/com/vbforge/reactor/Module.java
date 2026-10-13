package com.vbforge.reactor;

import java.util.List;

/**
 * A module inside a Maven reactor, modelled only by what matters for reactor
 * ordering: its {@code artifactId} and the <b>reactor-internal</b> dependencies
 * it declares (other modules in the same build, referenced by artifactId).
 *
 * <p>Dependencies on artifacts <em>outside</em> the reactor are ignored here —
 * the reactor order is defined only by the graph formed by the modules
 * themselves. Everything else is resolved from repositories and does not
 * influence ordering.
 */
public record Module(String id, List<String> dependencies) {

    public Module {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
    }

    /** Convenience factory: {@code Module.of("service", "core")}. */
    public static Module of(String id, String... dependencies) {
        return new Module(id, List.of(dependencies));
    }
}