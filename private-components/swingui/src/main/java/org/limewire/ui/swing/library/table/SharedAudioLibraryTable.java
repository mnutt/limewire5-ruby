package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.NameRenderer;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;

public class SharedAudioLibraryTable<T extends LocalFileItem> extends LibraryTable<T> {
    public SharedAudioLibraryTable(EventList<T> libraryItems, SharedAudioTableFormat<T> audioFormat, SaveLocationExceptionHandler saveLocationExceptionHandler, ShareTableRendererEditorFactory shareTableRendererEditorFactory) {
        super(libraryItems, audioFormat, saveLocationExceptionHandler, shareTableRendererEditorFactory);
    }

    @Override
    protected void setupCellRenderers(LibraryTableFormat<T> format) {
        super.setupCellRenderers(format);

        getColumnModel().getColumn(SharedAudioTableFormat.LENGTH_INDEX).setCellRenderer(new TimeRenderer());
        getColumnModel().getColumn(SharedAudioTableFormat.SIZE_INDEX).setCellRenderer(new FileSizeRenderer());
        getColumnModel().getColumn(SharedAudioTableFormat.TITLE_INDEX).setCellRenderer(new NameRenderer());
    }
}