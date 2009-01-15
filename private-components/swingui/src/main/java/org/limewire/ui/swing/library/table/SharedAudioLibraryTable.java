package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.NameRenderer;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;

public class SharedAudioLibraryTable<T extends LocalFileItem> extends LibraryTable<T> {
    private final PlayRendererEditor playEditor;
    private final PlayRendererEditor playRenderer;

    public SharedAudioLibraryTable(EventList<T> libraryItems, SharedAudioTableFormat<T> audioFormat, AudioPlayer player, SaveLocationExceptionHandler saveLocationExceptionHandler, ShareTableRendererEditorFactory shareTableRendererEditorFactory) {
        super(libraryItems, audioFormat, saveLocationExceptionHandler, shareTableRendererEditorFactory);

        playEditor = new PlayRendererEditor(this, player);
        getColumnModel().getColumn(SharedAudioTableFormat.PLAY_INDEX).setCellEditor(playEditor);
        playRenderer = new PlayRendererEditor(this, player);
        getColumnModel().getColumn(SharedAudioTableFormat.PLAY_INDEX).setCellRenderer(playRenderer);
        getColumnModel().getColumn(SharedAudioTableFormat.PLAY_INDEX).setMaxWidth(14);
        getColumnModel().getColumn(SharedAudioTableFormat.PLAY_INDEX).setMinWidth(14);
    }


    @Override
    public void dispose() {
        super.dispose();
        playEditor.dispose();
        playRenderer.dispose();
    }
    @Override
    protected void setupCellRenderers(LibraryTableFormat<T> format) {
        super.setupCellRenderers(format);

        getColumnModel().getColumn(SharedAudioTableFormat.LENGTH_INDEX).setCellRenderer(new TimeRenderer());
        getColumnModel().getColumn(SharedAudioTableFormat.SIZE_INDEX).setCellRenderer(new FileSizeRenderer());
        getColumnModel().getColumn(SharedAudioTableFormat.TITLE_INDEX).setCellRenderer(new NameRenderer());
    }
}