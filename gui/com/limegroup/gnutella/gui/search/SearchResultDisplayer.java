package com.limegroup.gnutella.gui.search;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.TabbedPaneUI;

import org.limewire.core.settings.QuestionsHandler;
import org.limewire.core.settings.SearchSettings;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.io.GUID;
import org.limewire.util.DebugRunnable;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.ProgTabUIFactory;
import com.limegroup.gnutella.gui.RefreshListener;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;
import com.limegroup.gnutella.library.FileManager;

/**
 * This class handles the display of search results.
 */
final class SearchResultDisplayer implements ThemeObserver, RefreshListener {

	/**
	 * <tt>JPanel</tt> containing the primary components of the search result
	 * display.
	 */
	private final JPanel MAIN_PANEL = new BoxPanel(BoxPanel.Y_AXIS);

	/**
	 * The main tabbed pane for displaying different search results.
	 */
	private JTabbedPane tabbedPane = new JTabbedPane();

    /** The contents of tabbedPane. 
     *  INVARIANT: entries.size()==# of tabs in tabbedPane 
     *  LOCKING: +obtain entries' monitor before adjusting number of 
     *            outstanding searches, i.e., the number of tabs
     *           +obtain a ResultPanel's monitor before adding or removing 
     *            results + to prevent deadlock, never obtain ResultPanel's
     *            lock if holding entries'.
     */
    private static final List<ResultPanel> entries = new ArrayList<ResultPanel>();

    /** Results is a panel that displays either a JTabbedPane when lots of
     *  results exist OR a blank ResultPanel when nothing is showing.
     *  Use switcher to switch between the two.  The first entry is the
     *  blank results panel; the second is the tabbed panel. */
    private final JPanel results = new JPanel();
    
    /**
     * The layout that switches between the dummy result panel
     * and the JTabbedPane.
     */
    private final CardLayout switcher = new CardLayout();
    
    /**
     * The dummy result panel, used when no searches are active.
     */
    private final ResultPanel DUMMY;
    
    /**
     * The overlay panel to use when no searches are active.
     */
    private final OverlayAd OVERLAY;
    
    /**
     * The listener to notify about the currently displaying search
     * changing.
     *
     * TODO: Allow more than one.
     */
    private ChangeListener _activeSearchListener;
    
    /**
     * Listener for events on the tabbedpane.
     */
    private final PaneListener PANE_LISTENER = new PaneListener();


    @InspectablePrimitive("selectedResultTabsCount")
    private static volatile int selectedResultTabsCount = 0;
    
	/**
	 * Constructs the search display elements.
	 */
	SearchResultDisplayer() {
        MAIN_PANEL.setMinimumSize(new Dimension(0,0));
        
        // make the results panel take up as much space as possible
        // for when the window is resized. 
        results.setPreferredSize(new Dimension(10000, 10000));
        results.setLayout(switcher);
        OVERLAY = new OverlayAd();
		DUMMY = new ResultPanel(OVERLAY);
		JPanel mainScreen = new JPanel(new BorderLayout());
        mainScreen.add(DUMMY.getComponent(), BorderLayout.CENTER);
        results.add("dummy", mainScreen);
        switcher.first(results);    

        setupTabbedPane();
        
        MAIN_PANEL.add(results);
        
        ThemeMediator.addThemeObserver(this);
        CancelSearchIconProxy.updateTheme();
	}
	
	/**
	 * Sets the listener for what searches are currently displaying.
	 */
	void setSearchListener(ChangeListener listener) {
	    _activeSearchListener = listener;
    }
    
    /**
     * Iterates through each displayed ResultPanel and fires an update.
     */
    void updateResults() {
        for(int i = 0; i < entries.size(); i++)
            entries.get(i).refresh();
    }

    /** 
     * @modifies tabbed pane, entries
     * @effects adds an entry for a search for stext with GUID guid
     *  to the tabbed pane.  This is used both for normal searching 
     *  and browsing.  Returns the ResultPanel added.
     */
    ResultPanel addResultTab(GUID guid, SearchInformation info) {
		ResultPanel panel=new ResultPanel(guid, info);
		return addResultPanelInternal(panel, info.getTitle());
    }

    /** Adds a tab of your files to the UI. */
    ResultPanel addMyFilesResultTab(String title) {
        FileManager fileManager = GuiCoreMediator.getFileManager();
        
        ResultPanel panel = new MySharedFilesResultPanel(title, fileManager);
        return addResultPanelInternal(panel, title);
    }    
    
    /**
     * Create a new JTabbedPane and add the necessary
     * listeners.
     * 
     */
    private void setupTabbedPane () {
        if (tabbedPane != null) {
            tabbedPane.removeMouseListener(PANE_LISTENER);
            tabbedPane.removeMouseMotionListener(PANE_LISTENER);
            tabbedPane.removeChangeListener(PANE_LISTENER);
        }
        
        tabbedPane = new JTabbedPane();
        ProgTabUIFactory.extendUI(tabbedPane);
        tabbedPane.setRequestFocusEnabled(false);
        results.add("tabbedPane",tabbedPane);
        
        tabbedPane.addMouseListener(PANE_LISTENER);
        tabbedPane.addMouseMotionListener(PANE_LISTENER);
        tabbedPane.addChangeListener(PANE_LISTENER);
    }
    
    /**
     * When a problem occurs with the JTabbedPane, we can
     * reset it (and hopefully circumvent the problem). We
     * first get all of the components and their respective
     * titles from the current tabbed pane, create a new
     * tabbed pane and add all of the components and titles
     * back in.
     * 
     */
    private void resetTabbedPane () {
        ArrayList<ResultPanel> ents = new ArrayList<ResultPanel>();
        ArrayList<Component> tabs = new ArrayList<Component>();
        ArrayList<String> titles = new ArrayList<String>();
        
        for (int i = 0; i < tabbedPane.getTabCount() && i < entries.size(); ++i) {
            tabs.add(tabbedPane.getComponent(i));
            titles.add(tabbedPane.getTitleAt(i));
            ents.add(entries.get(i));
        }
        
        tabbedPane.removeAll();
        entries.clear();
        
        setupTabbedPane();
        
        for (int i = 0; i < tabs.size(); ++i) {
          entries.add(ents.get(i));
          tabbedPane.addTab(titles.get(i), tabs.get(i));
        }
    }
    
    private ResultPanel addResultPanelInternal(ResultPanel panel, String title) {
        entries.add(panel);
        
        // XXX: LWC-1214 (hack)
        try {
            tabbedPane.addTab(title,
                    CancelSearchIconProxy.createSelected(),
                    panel.getComponent());
        }
        catch (ArrayIndexOutOfBoundsException e) {
            resetTabbedPane();
            
            entries.add(panel);
            tabbedPane.addTab(title,
                    CancelSearchIconProxy.createSelected(),
                    panel.getComponent());
        }
        
        // XXX: LWC-1088 (hack)
        try {
            tabbedPane.setSelectedIndex(entries.size()-1);
        }
        catch (java.lang.ArrayIndexOutOfBoundsException aioobe) {
            resetTabbedPane();
            tabbedPane.setSelectedIndex(entries.size()-1);
        }
        catch (java.lang.IndexOutOfBoundsException ioobe) {
            resetTabbedPane();
            tabbedPane.setSelectedIndex(entries.size()-1);
            
            // This will happen under OS X in apple.laf.CUIAquaTabbedPaneTabState.getIndex().
            // we grab all of the components from the current 
            // tabbed pane, create a new tabbed pane, and dump
            // the components back into it.
            //
            // For steps-to-reproduce, see:
            // https://www.limewire.org/jira/browse/LWC-1088
        }
        
        //Remove an old search if necessary
        if (entries.size() > SearchSettings.PARALLEL_SEARCH.getValue())
            killSearchAtIndex(0);

        GUIMediator.instance().setSearching(true);
        OVERLAY.searchPerformed();
        switcher.last(results);  //show tabbed results

        // If there are lots of tabs, this ensures everything
        // is properly visible. 
        MAIN_PANEL.revalidate();

        return panel;
    }

    /**
     * If i rp is no longer the i'th panel of this, returns silently. Otherwise
     * adds line to rp under the given group. Updates the count on the tab in
     * this and restarts the spinning lime.
     * 
     * @requires this is called from Swing thread, group is null or similar to
     *           line and already in rp
     * @modifies this
     */
    void addQueryResult(byte[] replyGUID, SearchResult line, ResultPanel rp) {

        //Actually add the line.   Must obtain rp's monitor first.
        if(!rp.matches(new GUID(replyGUID)))//GUID of rp!=replyGuid
            throw new IllegalArgumentException("guids don't match");
        rp.add(line);

        int resultPanelIndex = -1;
        // Search for the ResultPanel to verify it exists.
        resultPanelIndex = entries.indexOf(rp);

        // If we couldn't find it, silently exit.
        if( resultPanelIndex == -1 ) return;
        
        //Update index on tab.  Don't forget to add 1 since line hasn't
        //actually been added!
        tabbedPane.setTitleAt(resultPanelIndex, titleOf(rp));
    }
    
    /**
     * Adds the specified ChangeListener to the list of listeners
     * on the JTabbedPane.
     */
    void addChangeListener(ChangeListener listener) {
        tabbedPane.addChangeListener(listener);
    }
    
    /**
     * Adds the specified FocusListener to the list of listeners
     * of the JTabbedPane.
     */
    void addFocusListener(FocusListener listener) {
        tabbedPane.addFocusListener(listener);
    }

	/**
	 * Shows the popup menu that displays various options to the user.
	 */
	void showMenu(MouseEvent e) {
        ResultPanel rp = getSelectedResultPanel();
        if(rp != null) {
            JPopupMenu menu = rp.createPopupMenu(new TableLine[0]);
            Point p = e.getPoint();
            if(menu != null) {
                try {
                    menu.show(MAIN_PANEL, p.x+1, p.y-6);
                } catch(IllegalComponentStateException icse) {
                    // happens occasionally, ignore.
                }
            }
        }
    }

	/**
	 * Returns the currently selected <tt>ResultPanel</tt> instance.
	 *
	 * @return the currently selected <tt>ResultPanel</tt> instance,
	 *  or <tt>null</tt> if there is no currently selected panel
	 */
	ResultPanel getSelectedResultPanel() {
        int i=tabbedPane.getSelectedIndex();
        if(i==-1)
            return null;
        try {
            return entries.get(i);
        } catch(IndexOutOfBoundsException e){
            return null;
        }
	}

    /**
     * Returns the <tt>ResultPanel</tt> for the specified GUID.
     *
     * @param rguid the guid to search for
     * @return the ResultPanel that matches the specified GUID, or null
     *  if none match.
     */
     ResultPanel getResultPanelForGUID(GUID rguid) {
        for (int i=0; i<entries.size(); i++) {
            ResultPanel rp = entries.get(i);
            if (rp.matches(rguid)) //order matters: rp may be a dummy guid.
                return rp;
        }
		return null;
	}

	/**
	 * Returns the <tt>ResultPanel</tt> at the specified index.
	 * 
	 * @param index the index of the desired <tt>ResultPanel</tt>
	 * @return the <tt>ResultPanel</tt> at the specified index
	 */
	ResultPanel getPanelAtIndex(int index) {
        return entries.get(index);
	}

	/**
	 * Returns the index in the list of search panels that corresponds
	 * to the specified guid, or -1 if the specified guid does not
	 * exist.
	 *
	 * @param rguid the guid to search for
	 * @return the index of the specified guid, or -1 if it does not
	 *  exist.
	 */
	int getIndexForGUID(GUID rguid) {
        for (int i=0; i<entries.size(); i++) {
            ResultPanel rp = entries.get(i);
            if (rp.matches(rguid)) //order matters: rp may be a dummy guid.
                return i;
        }
		return -1;
	}
	
	/**
	 * Get index for point.
	 */
	int getIndexForPoint(int x, int y) {
        TabbedPaneUI ui = tabbedPane.getUI();
        return ui.tabForCoordinate(tabbedPane, x, y);
    }

    /**
     * @modifies tabbed pane, entries
     * @effects removes the currently selected result window (if any)
     *  from this
     */
    void killSearch() {
        int i=tabbedPane.getSelectedIndex();
        
        if (i >= entries.size()) {
            resetTabbedPane();
            i = tabbedPane.getSelectedIndex();
        }
        
        if (i==-1)  //nothing selected?!
            return;
        
        killSearchAtIndex(i);
    }

    /**
     * @modifies tabbed pane, entries
     * @effects removes the window at i from this
     */
    void killSearchAtIndex(int i) {
        ResultPanel killed = entries.remove(i);
        final GUID killedGUID = new GUID(killed.getGUID());
        BackgroundExecutorService.schedule(new DebugRunnable(new Runnable() {
            public void run() {
                GuiCoreMediator.getSearchServices().stopQuery(killedGUID);
            }
        }));
        
        try {
            tabbedPane.removeTabAt(i);
        } catch(IllegalArgumentException iae) {
            // happens occasionally on osx w/ java 1.4.2_05, ignore.
        } catch (ArrayIndexOutOfBoundsException oob) {
            // happens occassionally on os x because of apple.laf.*
            resetTabbedPane();
            tabbedPane.removeTabAt(i);
        }
        
        fixIcons();
        SearchMediator.searchKilled(killed);
        ThemeMediator.removeThemeObserver(killed);
        
        if (entries.size()==0) {
            try {
                switcher.first(results); //show dummy table
            } catch(ArrayIndexOutOfBoundsException aioobe) {
                //happens on jdk1.5 beta w/ windows XP, ignore.
            }
			GUIMediator.instance().setSearching(false);
        } else {
		    checkToStopLime();
        }
    }

    /**
     * Notification that a browse host for the given GUID has failed.
     *
     * Removes the panel associated with that search.
     */
    void browseHostFailed(GUID guid) {
        int i = getIndexForGUID(guid);
        if (i > -1) {
            ResultPanel rp = getPanelAtIndex(i);
            killSearchAtIndex(i);
            GUIMediator.showError(I18n.tr("Could not browse host {0}.", rp.getTitle()),
                    QuestionsHandler.BROWSE_HOST_FAILED);
        }
    }

    /**
     * @modifies spinning lime state
     * @effects If all searches are stopped, then the Lime stops spinning.
     */
	void checkToStopLime() {
		ResultPanel panel;
		long now = System.currentTimeMillis();

		// Decide if we definitely can stop the lime
		boolean stopLime = true;
		for (int i=0; i<entries.size(); i++) {
			panel = entries.get(i);
            stopLime &= panel.isStopped() ||
                        panel.calculatePercentage(now) >= 1d;
		}
        
		if ( stopLime ) {
			GUIMediator.instance().setSearching(false);
		}
	}

    /**
     * called by ResultPanel when the views are changed. Used to set the
     * tab to indicate the correct number of TableLines in the current
     * view.
     */
    void setTabDisplayCount(ResultPanel rp){
        Object panel;
        int i=0;
        boolean found = false;
        for(;i<entries.size();i++){//safe its synchronized
            panel = entries.get(i);
            if (panel == rp){
                found = true;
                break;
            }
        }
        if(found)//find the number of lines in model
            tabbedPane.setTitleAt(i, titleOf(rp));
    }
    
    private void fixIcons() {
        int sel = tabbedPane.getSelectedIndex();
        for(int i = 0; i < entries.size() && i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setIconAt(i, i == sel ? 
                CancelSearchIconProxy.createSelected() :
                CancelSearchIconProxy.createPlain());
        }
    }

	/**
	 * Accessor for the <tt>ResultPanel</tt> instance that shows no active
	 * searches.
	 *
	 * @return the <tt>ResultPanel</tt> instance that shows no active
	 * searches
	 */
    ResultPanel getDummyResultPanel(){
		return DUMMY;
    }

	/**
	 * Returns the <tt>JComponent</tt> instance containing all of the search
	 * result ui components.
	 *
	 * @return the <tt>JComponent</tt> instance containing all of the search
	 *  result ui components
	 */
	JComponent getComponent() {
		return MAIN_PANEL;
	}

	// inherit doc comment
	public void updateTheme() {
	    ProgTabUIFactory.extendUI(tabbedPane);
		DUMMY.updateTheme();
		OVERLAY.updateTheme();
		CancelSearchIconProxy.updateTheme();
		fixIcons();
		for(Iterator i = entries.iterator(); i.hasNext(); ) {
			ResultPanel curPanel = (ResultPanel)i.next();
			curPanel.updateTheme();
		}
	}
    
    /**
     * Every second, redraw only the tab portion of the TabbedPane
     * and determine if we should stop the lime spinning.
     */
    public void refresh() {
        checkToStopLime();
        
        if(tabbedPane.isVisible() && tabbedPane.isShowing()) {
            Rectangle allBounds = tabbedPane.getBounds();
            Component comp = null;
            try {
                comp = tabbedPane.getSelectedComponent();
            } catch(ArrayIndexOutOfBoundsException aioobe) {
                resetTabbedPane();
                comp = tabbedPane.getSelectedComponent();
                // happens on OSX occasionally, ignore.
            }
            if(comp != null) {
                Rectangle compBounds = comp.getBounds();
                // The length of the tab rectangle will extend
                // over the bounds of the entire TabbedPane
                // up to 1 before the y scale of the visible component.
                Rectangle allTabs = new Rectangle(allBounds.x, allBounds.y,
                                            allBounds.width, compBounds.y-1);
                tabbedPane.repaint(allTabs);
            }
        }
    }
    
    /**
     * Returns the title of the specified ResultPanel.
     */
    private String titleOf(ResultPanel rp) {
        int current = rp.filteredSources();
        int total = rp.totalSources();
        if(current < total)
            return rp.getTitle() + " ("  + current + "/" + total + ")";
        else
            return rp.getTitle() + " (" + total + ")";
    }    
    
    /**
     * Listens for events on the JTabbedPane and dispatches commands.
     */
    private class PaneListener implements MouseListener, MouseMotionListener,
                                          ChangeListener {

    	/**
    	 * The last index that was rolled over.
    	 */
    	private int lastIdx = -1;
    	
    	private int lastClickedIndex = -1; 

        /**
         * Either closes the selected tab or notifies the listener
         * that a tab was clicked.
         */
        public void mouseClicked(MouseEvent e) {
            if(tryPopup(e))
                return;
                
    		if(SwingUtilities.isLeftMouseButton(e)) {
        		int x = e.getX();
        		int y = e.getY();
        		int idx;
        		
                idx = shouldKillIndex(x, y);
			    if(idx != -1) {
			        lastIdx = -1;
    			    killSearchAtIndex(idx);
        		} else {
        		    int index = getIndexForPoint(x, y);
        		    // count for inspection as selection change
        		    if (index != lastClickedIndex) {
        		        lastClickedIndex = index;
        		        ++selectedResultTabsCount;
        		    }
        		}
        		
        		if(idx == -1)
        		    stateChanged(null);
            }   
    	}
    	
    	/**
    	 * Redoes the icons on the tab which this is over.
    	 */
    	public void mouseMoved(MouseEvent e) {
    	    int x = e.getX();
    	    int y = e.getY();
	        int idx = shouldKillIndex(x, y);
	        if(idx != lastIdx && lastIdx != -1)
	            resetIcon();
	        
	        if(idx != -1) {
                tabbedPane.setIconAt(idx, CancelSearchIconProxy.createArmed());
                lastIdx = idx;
            }
        }
        
        /**
         * Returns the index of the tab if the coordinates x,y can close it.
         * Otherwise returns -1.
         */
        private int shouldKillIndex(int x, int y) {
            int idx = getIndexForPoint(x, y);
            if(idx != -1) {
                Icon icon = tabbedPane.getIconAt(idx);
                if(icon != null && icon instanceof CancelSearchIconProxy)
                    if(((CancelSearchIconProxy)icon).shouldKill(x, y))
                        return idx;
            }
            return -1;
        }
        
        /**
         * Resets the last armed icon.
         */
        private void resetIcon() {
            if(lastIdx != -1 && lastIdx < tabbedPane.getTabCount()) {
                if(lastIdx == tabbedPane.getSelectedIndex())
                    tabbedPane.setIconAt(lastIdx, CancelSearchIconProxy.createSelected());
                else
                    tabbedPane.setIconAt(lastIdx, CancelSearchIconProxy.createPlain());
                lastIdx = -1;
            }
        }

        public void mousePressed(MouseEvent e) { tryPopup(e); }    
        public void mouseReleased(MouseEvent e) { tryPopup(e); }
        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) { resetIcon(); }
        public void mouseDragged(MouseEvent e) {}
        
        /**
         * Shows the popup if this was a popup trigger.
         */
        private boolean tryPopup(final MouseEvent e) {
            if ( e.isPopupTrigger() ) {
                // make sure the given tab is selected.
                int idx = getIndexForPoint(e.getX(), e.getY());
                if(idx != -1) {
                    try {
                        tabbedPane.setSelectedIndex(idx);
                    }
                    catch (ArrayIndexOutOfBoundsException aioobe) {
                        resetTabbedPane();
                        tabbedPane.setSelectedIndex(idx);
                    }
                }
                showMenu(e);
                return true;
            }
            return false;
        }
    
        /**
         * Forwards events to the activeSearchListener.
         */
        public void stateChanged(ChangeEvent e) {
           _activeSearchListener.stateChanged(e);
           fixIcons();
        }
    }

}
