package com.limegroup.gnutella.gui.tabs;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JSplitPane;

import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.search.SearchMediator;
import com.limegroup.gnutella.gui.tables.ComponentMediator;

/**
 * This class constructs the search/download tab, including all UI elements.
 */
public final class SearchDownloadTab extends AbstractTab {
	
	/**
	 * Split pane for the split between the search input panel and the 
	 * search results panel.
	 */
	private final JSplitPane SPLIT_PANE;
	
	/**
	 * Split pane for the split between the search and download sections
	 * of the window.
	 */
	private final JSplitPane SEARCH_DOWNLOAD_SPLIT_PANE;

	/**
	 * Constructs the tab for searches and downloads.
	 *
	 * @param SEARCH_MEDIATOR the <tt>SearchMediator</tt> instance for 
	 *  obtaining the necessary ui components to add
	 * @param DOWNLOAD_MEDIATOR the <tt>DownloadMediator</tt> instance for 
	 *  obtaining the necessary ui components to add
	 */
	public SearchDownloadTab(final SearchMediator searchMediator,
							 final ComponentMediator DOWNLOAD_MEDIATOR) {
		super(I18n.tr("Search"), 
		        I18n.tr("Search and Download Files"), "search_tab");

        SEARCH_DOWNLOAD_SPLIT_PANE =
            new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                           SearchMediator.getResultComponent(),
                           DOWNLOAD_MEDIATOR.getComponent());
        SEARCH_DOWNLOAD_SPLIT_PANE.setContinuousLayout(true);
        SEARCH_DOWNLOAD_SPLIT_PANE.setOneTouchExpandable(true);
		SEARCH_DOWNLOAD_SPLIT_PANE.setPreferredSize(new Dimension(200,200));
		SEARCH_DOWNLOAD_SPLIT_PANE.setDividerLocation(1000);

		JComponent searchBoxPanel = SearchMediator.getSearchComponent();
		int width = OSUtils.isMacOSX() ? 220 : 100;
		searchBoxPanel.setPreferredSize(new Dimension(width,100));
        
		
        SPLIT_PANE = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
									searchBoxPanel,
									SEARCH_DOWNLOAD_SPLIT_PANE);
		SPLIT_PANE.setDividerSize(0);
	}

	/**
	 * Sets the location of the search/download divider.
	 *
	 * @param loc the location to set the divider to
	 */
	public void setDividerLocation(int loc) {
		SEARCH_DOWNLOAD_SPLIT_PANE.setDividerLocation(loc);
	}

	/**
	 * Sets the location of the search/download divider.
	 *
	 * @param loc the location to set the divider to
	 */
	public void setDividerLocation(double loc) {
		SEARCH_DOWNLOAD_SPLIT_PANE.setDividerLocation(loc);
	}
	
	/**
	 * Returns the divider location of the search/download divider.
	 * @return
	 */
	public int getDividerLocation() {
		return SEARCH_DOWNLOAD_SPLIT_PANE.getDividerLocation();
	}

	@Override
    public void storeState(boolean state) {
		// the search tab can never be invisible, so this isn't necessary
	}

	@Override
    public JComponent getComponent() {
		return SPLIT_PANE;
	}
	
	@Override
    public void mouseClicked() {
	    SearchMediator.showSearchInput();
    }	    
}
