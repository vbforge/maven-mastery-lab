package com.vbforge.repositories;

/**
 * A Maven mirror, as configured in {@code ~/.m2/settings.xml}.
 *
 * <p>{@code mirrorOf} is the interesting field: it decides which repositories
 * this mirror replaces. The recognised patterns are:
 * <ul>
 *   <li>{@code *} — every repository</li>
 *   <li>{@code external:*} — every repository except {@code localhost} and
 *       {@code file://} ones (treated as {@code *} here for simplicity)</li>
 *   <li>an exact repository id — e.g. {@code central}</li>
 *   <li>a comma-separated list — {@code central,spring-milestones}</li>
 *   <li>a negation with {@code !} — {@code *,!my-internal-repo}</li>
 * </ul>
 *
 * <p>If two mirrors match the same repository, the <b>first</b> one wins —
 * same as Maven.
 */
public record Mirror(String id, String mirrorOf, String url) {

    public Mirror {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url must not be blank");
        }
        mirrorOf = mirrorOf == null ? "" : mirrorOf;
    }
}