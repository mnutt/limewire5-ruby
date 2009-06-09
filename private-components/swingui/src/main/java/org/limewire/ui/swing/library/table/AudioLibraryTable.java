package org.limewire.ui.swing.library.table;

import javax.swing.table.DefaultTableCellRenderer;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.NameRenderer;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import com.google.inject.Provider;

import ca.odell.glazedlists.EventList;

public class AudioLibraryTable<T extends LocalFileItem> extends LibraryTable<T> {
    private final PlayRendererEditor playEditor;
    private final PlayRendererEditor playRenderer;
    
    public AudioLibraryTable(EventList<T> libraryItems, AudioPlayer player, Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler, ShareTableRendererEditorFactory shareTableRendererEditorFactory,
            Provider<TimeRenderer> timeRenderer, Provider<FileSizeRenderer> fileSizeRenderer, Provider<NameRenderer> nameRenderer, DefaultTableCellRenderer defaultTableCellRenderer) {
        super(libraryItems, new AudioTableFormat<T>(), saveLocationExceptionHandler, shareTableRendererEditorFactory, defaultTableCellRenderer);

        playEditor = new PlayRendererEditor(this, player);
        getColumnModel().getColumn(AudioTableFormat.PLAY_INDEX).setCellEditor(playEditor);
        playRenderer = new PlayRendererEditor(this, player);
        getColumnModel().getColumn(AudioTableFormat.PLAY_INDEX).setCellRenderer(playRenderer);
        getColumnModel().getColumn(AudioTableFormat.PLAY_INDEX).setMaxWidth(14);
        getColumnModel().getColumn(AudioTableFormat.PLAY_INDEX).setMinWidth(14);
        getColumnModel().getColumn(AudioTableFormat.LENGTH_INDEX).setCellRenderer(timeRenderer.get());
        getColumnModel().getColumn(AudioTableFormat.SIZE_INDEX).setCellRenderer(fileSizeRenderer.get());
        getColumnModel().getColumn(AudioTableFormat.TITLE_INDEX).setCellRenderer(nameRenderer.get());
    }

    @Override
    public void dispose() {
        super.dispose();
        playEditor.dispose();
        playRenderer.dispose();
    }
}
