package org.limewire.ui.swing.search;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.ui.swing.search.model.BasicSearchResultsModel;
import org.limewire.ui.swing.search.model.GroupingListEventListener;
import org.limewire.ui.swing.search.model.SearchResultsModel;
import org.limewire.ui.swing.search.model.SimilarResultsDetector;
import org.limewire.ui.swing.search.model.SimilarResultsDetectorFactory;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.PropertiableHeadings;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SearchResultsModelFactory {
    private final SpamManager spamManager;

    private final SimilarResultsDetectorFactory similarResultsDetectorFactory;

    private final LibraryManager libraryManager;

    private final DownloadListManager downloadListManager;

    private final PropertiableHeadings propertiableHeadings;

    @Inject
    public SearchResultsModelFactory(SimilarResultsDetectorFactory similarResultsDetectorFactory,
            SpamManager spamManager, LibraryManager libraryManager,
            DownloadListManager downloadListManager, PropertiableHeadings propertiableHeadings) {
        this.similarResultsDetectorFactory = similarResultsDetectorFactory;
        this.spamManager = spamManager;
        this.libraryManager = libraryManager;
        this.downloadListManager = downloadListManager;
        this.propertiableHeadings = propertiableHeadings;
    }

    public SearchResultsModel createSearchResultsModel() {
        SearchResultsModel searchResultsModel = new BasicSearchResultsModel(propertiableHeadings);

        EventList<VisualSearchResult> visualSearchResults = searchResultsModel
                .getGroupedSearchResults();

        SimilarResultsDetector similarResultsDetector = similarResultsDetectorFactory
                .newSimilarResultsDetector();

        // AlreadyDownloaded listener needs to be added to the list
        // before the grouping listener because the grouping listener uses
        // values set by the AlreadyDownloaded listener
        AlreadyDownloadedListEventListener alreadyDownloadedListEventListener = new AlreadyDownloadedListEventListener(
                libraryManager, downloadListManager);
        visualSearchResults.addListEventListener(alreadyDownloadedListEventListener);

        if (SwingUiSettings.GROUP_SIMILAR_RESULTS_ENABLED.getValue()) {
            GroupingListEventListener groupingListEventListener = new GroupingListEventListener(
                    similarResultsDetector);
            visualSearchResults.addListEventListener(groupingListEventListener);
        }

        SpamListEventListener spamListEventListener = new SpamListEventListener(spamManager,
                similarResultsDetector);
        visualSearchResults.addListEventListener(spamListEventListener);

        return searchResultsModel;
    }

}
