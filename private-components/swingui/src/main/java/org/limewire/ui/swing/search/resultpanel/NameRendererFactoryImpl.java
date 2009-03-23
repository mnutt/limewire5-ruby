package org.limewire.ui.swing.search.resultpanel;

import javax.swing.table.TableCellRenderer;

import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.ui.swing.table.IconLabelRenderer;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;

/**
 * The default implementation of NameRendererFactory.
 */
public class NameRendererFactoryImpl implements NameRendererFactory {

    private final IconManager iconManager;
    
    private final CategoryIconManager categoryIconManager;
    
    private final DownloadListManager downloadListManager;
    
    private final LibraryManager libraryManager;
    
    /**
     * Constructs a NameRendererFactory that uses the specified services.
     */
    @Inject
    public NameRendererFactoryImpl(IconManager iconManager, 
            CategoryIconManager categoryIconManager, 
            DownloadListManager downloadListManager, 
            LibraryManager libraryManager) {
        this.iconManager = iconManager;
        this.categoryIconManager = categoryIconManager;
        this.downloadListManager = downloadListManager;
        this.libraryManager = libraryManager;
    }
    
    /**
     * Creates a TableCellRenderer for the Name column with the specified
     * indicator to display the audio artist.
     */
    @Override
    public TableCellRenderer createNameRenderer(boolean showAudioArtist) {
        return new IconLabelRenderer(iconManager, categoryIconManager, 
                downloadListManager, libraryManager, showAudioArtist);
    }

}
