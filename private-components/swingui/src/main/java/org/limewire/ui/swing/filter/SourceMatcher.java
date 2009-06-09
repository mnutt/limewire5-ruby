package org.limewire.ui.swing.filter;

import org.limewire.ui.swing.filter.SourceFilter.SourceType;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Matcher used to filter a search result by its source.
 */
class SourceMatcher<E extends FilterableItem> implements Matcher<E> {
    /** Source type to filter. */
    private final SourceType sourceType;

    /**
     * Constructs a SourceMatcher for the specified source type.
     */
    public SourceMatcher(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * Returns true if the specified search result matches the source type.
     */
    @Override
    public boolean matches(E item) {
        // TODO implement real logic
        return (sourceType == SourceType.P2P);
    }
}
