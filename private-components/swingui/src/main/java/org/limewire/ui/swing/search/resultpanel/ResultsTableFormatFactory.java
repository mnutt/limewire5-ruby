package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.classic.AllTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.AudioTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.DocumentTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.ImageTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.OtherTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.ProgramTableFormat;
import org.limewire.ui.swing.search.resultpanel.classic.VideoTableFormat;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Implements a factory for creating a TableFormat to display search results.
 */
public class ResultsTableFormatFactory {

    private final Provider<IconManager> iconManager;
    
    /**
     * Constructs a ResultsTableFormatFactory with the specified icon manager.
     */
    @Inject
    public ResultsTableFormatFactory(Provider<IconManager> iconManager) {
        this.iconManager = iconManager;
    }

    /**
     * Creates a ResultsTableFormat for the specified search category.
     */
    public ResultsTableFormat<VisualSearchResult> createTableFormat(SearchCategory searchCategory) {
        switch (searchCategory) {
        case ALL:
            return new AllTableFormat(iconManager);
        case AUDIO:
            return new AudioTableFormat();
        case VIDEO:
            return new VideoTableFormat();
        case DOCUMENT:
            return new DocumentTableFormat(iconManager);
        case IMAGE:
            return new ImageTableFormat();
        case PROGRAM:
            return new ProgramTableFormat();
        case OTHER:
            return new OtherTableFormat();
        default:
            throw new IllegalArgumentException("Invalid search category " + searchCategory);
        }
    }
}
