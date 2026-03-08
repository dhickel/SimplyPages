package io.mindspice.simplypages.components.forum;

import io.mindspice.simplypages.core.Component;

import java.util.Map;
import java.util.Set;

/**
 * Batch resolver for a single normalized forum tag key.
 */
public interface ForumTagResolver {

    /**
     * @return tag key handled by this resolver
     */
    String key();

    /**
     * Resolves a batch of values for this key.
     *
     * <p>Return map keys must match requested values. Missing values are treated as unresolved.</p>
     */
    Map<String, Component> resolveBatch(Set<String> values);
}
