package org.limewire.ui.swing.library.playlist;

import java.awt.Component;
import java.io.File;
import java.util.Comparator;

import javax.swing.JLabel;
import javax.swing.JTable;

import org.jdesktop.swingx.decorator.SortController;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.playlist.Playlist;
import org.limewire.core.api.playlist.PlaylistListener;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.library.table.DefaultLibraryRenderer;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFormat;
import org.limewire.ui.swing.library.table.PlayRendererEditor;
import org.limewire.ui.swing.library.table.ShareTableRendererEditorFactory;
import org.limewire.ui.swing.table.FileSizeRenderer;
import org.limewire.ui.swing.table.NameRenderer;
import org.limewire.ui.swing.table.QualityRenderer;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.table.TimeRenderer;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

/**
 * Defines a library table used by playlists.
 */
public class PlaylistLibraryTable<T extends LocalFileItem> extends LibraryTable<T> {

    private final PlayRendererEditor playEditor;
    private final PlayRendererEditor playRenderer;
    private final SortOrderHandler sortOrderHandler;

    /**
     * Constructs a PlaylistLibraryTable with the specified list of items, 
     * table format, audio player, exception handler, and editor factory. 
     */
    public PlaylistLibraryTable(Playlist playlist, SortedList<T> libraryItems,
            LibraryTableFormat<T> format, AudioPlayer player, 
            SaveLocationExceptionHandler saveLocationExceptionHandler, 
            ShareTableRendererEditorFactory shareTableRendererEditorFactory) {
        super(libraryItems, format, saveLocationExceptionHandler, shareTableRendererEditorFactory);
        
        // Multiple selection is allowed so mouse press/drag selects multiple
        // cells, and a second mouse press is needed to start a drag operation.
        // The easy fix is change to SINGLE_SELECTION mode so that a single 
        // mouse press/drag starts a drag on the row.  The preferred fix is to
        // develop a workaround so that a single mouse press/drag may select a
        // row, and starts a drag operation.
        
        // Disable user sorting on the position index column.  Sorting on this
        // column is unnecessary because the index numbers are always updated 
        // to stay in order.
        getColumnExt(convertColumnIndexToView(PlaylistTableFormat.NUMBER_INDEX)).setSortable(false);
        
        // Install handler to update playlist positions when list is sorted. 
        sortOrderHandler = new SortOrderHandler(playlist, libraryItems);
        sortOrderHandler.install();

        playEditor = new PlayRendererEditor(this, player);
        playRenderer = new PlayRendererEditor(this, player);
        
        getColumnModel().getColumn(PlaylistTableFormat.PLAY_INDEX).setCellEditor(playEditor);
        getColumnModel().getColumn(PlaylistTableFormat.PLAY_INDEX).setCellRenderer(playRenderer);
        getColumnModel().getColumn(PlaylistTableFormat.PLAY_INDEX).setMaxWidth(14);
        getColumnModel().getColumn(PlaylistTableFormat.PLAY_INDEX).setMinWidth(14);
    }

    @Override
    protected void setupCellRenderers(LibraryTableFormat<T> format) {
        super.setupCellRenderers(format);
        
        // Set column header renderers.
        getColumnModel().getColumn(PlaylistTableFormat.LENGTH_INDEX).setHeaderRenderer(new TableCellHeaderRenderer(JLabel.TRAILING));

        // Set column cell renderers.
        getColumnModel().getColumn(PlaylistTableFormat.LENGTH_INDEX).setCellRenderer(new TimeRenderer());
        getColumnModel().getColumn(PlaylistTableFormat.SIZE_INDEX).setCellRenderer(new FileSizeRenderer());
        getColumnModel().getColumn(PlaylistTableFormat.TITLE_INDEX).setCellRenderer(new NameRenderer());
        getColumnModel().getColumn(PlaylistTableFormat.NUMBER_INDEX).setCellRenderer(new PositionRenderer());
        getColumnModel().getColumn(PlaylistTableFormat.QUALITY_INDEX).setCellRenderer(new QualityRenderer());
    }

    @Override
    public void dispose() {
        super.dispose();
        playEditor.dispose();
        playRenderer.dispose();
        sortOrderHandler.dispose();
    }
    
    /**
     * SortOrderHandler handles change events on the playlist and sorted file
     * items to update the position indices in the playlist. 
     */
    private class SortOrderHandler implements PlaylistListener, 
        ListEventListener<T>, Disposable {
        
        private Playlist playlist;
        private SortedList<T> sortedList;
        private boolean sortChanging;
        
        /**
         * Constructs a SortOrderHandler for the specified playlist and sorted
         * list of file items.
         */
        public SortOrderHandler(Playlist playlist, SortedList<T> sortedList) {
            this.playlist = playlist;
            this.sortedList = sortedList;
        }

        /**
         * Installs the handler by adding listeners to its lists.  The handler
         * will not do anything until this method is called.
         */
        public void install() {
            playlist.addPlaylistListener(this);
            sortedList.addListEventListener(this);
        }
        
        /**
         * Handles change event on the playlist to reset the sort order when
         * files are added/deleted/moved in the playlist. 
         */
        @Override
        public void listChanged(Playlist playlist) {
            try {
                // Set indicator to avoid processing sort order change events.
                sortChanging = true;

                // Remove old sort keys to clear sort indicator.
                SortController controller = getSortController();
                if (controller != null) {
                    controller.setSortKeys(null);
                }

                // Sort list by position index.
                sortedList.setComparator(new Comparator<T>() {
                    @Override
                    public int compare(T o1, T o2) {
                        int idx1 = ((PlaylistFileItem) o1).getIndex();
                        int idx2 = ((PlaylistFileItem) o2).getIndex();
                        return (idx1 < idx2) ? -1 : ((idx1 > idx2) ? 1 : 0);
                    }
                });
                
            } finally {
                sortChanging = false;
            }
        }

        /**
         * Handles change event on the sorted list to reorder the playlist 
         * indices when the sort order is changed.
         */
        @Override
        public void listChanged(ListEvent<T> listChanges) {
            // Process sort order change if not caused by playlist change.
            if (listChanges.isReordering() && !sortChanging) {
                // Create sorted file array.
                File[] files = new File[sortedList.size()];
                for (int i = 0; i < sortedList.size(); i++) {
                    LocalFileItem item = sortedList.get(i);
                    files[i] = item.getFile();
                }
                
                // Reorder files in playlist.
                playlist.reorderFiles(files);
            }
        }

        /**
         * Uninstalls the handler from its lists to remove references.
         */
        @Override
        public void dispose() {
            playlist.removePlaylistListener(this);
            sortedList.removeListEventListener(this);
            playlist = null;
            sortedList = null;
        }
    }

    /**
     * Cell renderer for the position column.  PositionRenderer displays the
     * playlist position index as a 1-based integer.
     */
    private static class PositionRenderer extends DefaultLibraryRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, 
                Object value, boolean isSelected, boolean hasFocus, 
                int row, int column) {
            
            super.getTableCellRendererComponent(table, value, isSelected, 
                    hasFocus, row, column);
            
            // Convert position value to 1-based integer.
            if (value instanceof Number) {
                setText(String.valueOf(((Number) value).intValue() + 1));
            }
            
            return this;
        }
    }
}
