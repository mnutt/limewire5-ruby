package org.limewire.ui.swing.search.resultpanel;

import javax.swing.table.TableCellRenderer;

/**
 * Defines a factory to create a cell renderer for the Name column.
 */
public interface NameRendererFactory {

    /**
     * Creates a TableCellRenderer for the Name column with the specified
     * indicator to display the audio artist.
     */
    TableCellRenderer createNameRenderer(boolean showAudioArtist);
    
}
