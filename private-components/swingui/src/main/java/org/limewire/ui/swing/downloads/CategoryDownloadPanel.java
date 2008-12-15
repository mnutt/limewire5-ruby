package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.VerticalLayout;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.CompoundHighlighter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateMatcher;
import org.limewire.ui.swing.downloads.table.DownloadTable;
import org.limewire.ui.swing.downloads.table.DownloadTableFactory;
import org.limewire.ui.swing.table.MouseableTable.MenuHighlightPredicate;
import org.limewire.ui.swing.table.MouseableTable.TableColors;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class CategoryDownloadPanel extends JPanel {

    private final DownloadTableFactory downloadTableFactory;
    
	private Highlighter evenTableHighlighter;
	private Highlighter oddTableHighlighter;
	private JPanel tablePanel = new JPanel(new VerticalLayout());

	private List<DownloadTable> tables = new ArrayList<DownloadTable>();

	private TableColors colors;

	@AssistedInject
	private CategoryDownloadPanel(DownloadTableFactory downloadTableFactory, @Assisted EventList<DownloadItem> list) {
	    this.downloadTableFactory = downloadTableFactory;
	    
	    colors = new TableColors();
	    
	    //HighlightPredicate.EVEN and HighlightPredicate.ODD are zero based
        evenTableHighlighter = new CompoundHighlighter(
               new ColorHighlighter(HighlightPredicate.EVEN, colors.evenColor, colors.evenForeground, colors.selectionColor, colors.selectionForeground),
               new ColorHighlighter(HighlightPredicate.ODD, colors.oddColor, colors.oddForeground, colors.selectionColor, colors.selectionForeground) );
        //oddHighlighter reverses color scheme
        oddTableHighlighter = new CompoundHighlighter(
                new ColorHighlighter(HighlightPredicate.ODD,colors.evenColor, colors.evenForeground, colors.selectionColor, colors.selectionForeground), 
                new ColorHighlighter(HighlightPredicate.EVEN, colors.oddColor, colors.oddForeground, colors.selectionColor, colors.selectionForeground));
      
		setLayout(new BorderLayout());
        JScrollPane pane = new JScrollPane(tablePanel);
        pane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		add(pane);
		 		
		addTable(list, I18n.tr("Finished"), DownloadState.DONE,
				DownloadState.FINISHING);
		addTable(list, I18n.tr("Active"),
				DownloadState.DOWNLOADING, DownloadState.CONNECTING);
		addTable(list, I18n.tr("Inactive"), DownloadState.PAUSED,
				DownloadState.REMOTE_QUEUED, DownloadState.LOCAL_QUEUED);
		addTable(list, I18n.tr("Unsuccessful"),
				DownloadState.ERROR, DownloadState.STALLED);
		
		updateStriping();
	}

	/**
	 * Creates a DownloadTable and adds it to a collapsible pane with the title 
	 * 
	 * @param model the model of the table
	 * @param title the title of the panel displaying this table
	 * @param state
	 */
	private void addTable(EventList<DownloadItem> list, String title,
			DownloadState first, DownloadState... rest) {
		final JXCollapsiblePane collapsePane = new JXCollapsiblePane();
		collapsePane.setLayout(new BorderLayout());
		EventList<DownloadItem> filterList = GlazedListsFactory.filterList(list, new DownloadStateMatcher(first, rest));
		
		final DownloadTable table = downloadTableFactory.create(filterList);
		tables.add(table);
		table.addMouseListener(new MultiTableMouseListener());
	
		collapsePane.add(table, BorderLayout.CENTER);
		
		tablePanel.add(collapsePane);		
	}
	
    /**
     * deselects other tables when a table is selected
     */
    private class MultiTableMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!e.isPopupTrigger()) {
                JTable selectedTable = (JTable) e.getComponent();
                for (JTable table : tables) {
                    if (table != selectedTable) {
                        table.clearSelection();
                    }
                }
            }
        }
    }
    

    private void updateStriping() {
		int length = 0;
		
		for (DownloadTable table : tables) {
						
			if (table.getRowCount() > 0 && table.isVisible()) {
				if (length % 2 == 0) {
					table.setHighlighters(evenTableHighlighter);
				} else {
					table.setHighlighters(oddTableHighlighter);
				}
				
                 table.addHighlighter(new ColorHighlighter(new MenuHighlightPredicate(table),
                        colors.menuRowColor, colors.menuRowForeground, colors.menuRowColor,
                        colors.menuRowForeground));

                length += table.getRowCount();
			}
			
		}
	}

}
