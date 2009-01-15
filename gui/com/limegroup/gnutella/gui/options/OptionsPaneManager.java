package com.limegroup.gnutella.gui.options;

import java.awt.CardLayout;
import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.options.panes.SharedDirPaneItem;
import com.limegroup.gnutella.gui.options.panes.StoreSaveDirPaneItem;

/**
 * Manages the main options window that displays the various options 
 * windows.<p>
 *
 * This class also stores all of the main options panels to access
 * all of them regardless of how many there are or what their
 * specific type is.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class OptionsPaneManager {

	/**
	 * Constant for the main panel of the options window.
	 */
	private final JPanel MAIN_PANEL = new JPanel();

	/**
	 * Constant for the <tt>CardLayout</tt> used in the main panel.
	 */
	private final CardLayout CARD_LAYOUT = new CardLayout();

	/**
	 * Constant for the <tt>ArrayList</tt> containing all of the visible
	 * <tt>OptionsPane</tt> instances.
	 */
	private final List<OptionsPane> OPTIONS_PANE_LIST = new ArrayList<OptionsPane>();
	
	/**
	 * Stores the already created option panes by key.
	 */
	private final Map<String, OptionsPane> panesByKey = new HashMap<String, OptionsPane>();
	
	/**
	 * The factory which option panes are created from.
	 */
	private final OptionsPaneFactory FACTORY = new OptionsPaneFactory();
	
	/**
	 * The constructor sets the layout and adds all of the <tt>OptionPane</tt>
	 * instances.
	 */
	public OptionsPaneManager() {
		MAIN_PANEL.setLayout(CARD_LAYOUT);		
    }

	/**
	 * Shows the options pane speficied by its title.
	 * <p>
	 * Lazily creates the options pane if it was not shown before. Its options
	 * are initialized before it is shown. 
	 * 
	 * @param node the name of the <code>Component</code> to show
	 */
	public final void show(final OptionsTreeNode node) {
		if (!panesByKey.containsKey(node.getTitleKey())) {
			OptionsPane pane = FACTORY.createOptionsPane(node);
			pane.initOptions();
			addPane(pane);
			panesByKey.put(node.getTitleKey(), pane);
			
			// If this was the 'SAVED' key, then also load shared,
			// since setting save stuff requires that sharing be updated also.
			if(node.getTitleKey().equals(OptionsConstructor.SAVE_BASIC_KEY) && !panesByKey.containsKey(OptionsConstructor.SHARED_BASIC_KEY)) {
			    OptionsPane shared = FACTORY.createOptionsPane(node);
			    shared.initOptions();
			    addPane(shared);
			    panesByKey.put(node.getTitleKey(), shared);
			}
		}
		CARD_LAYOUT.show(MAIN_PANEL, node.getTitleKey());
	}

	/**
	 * Sets the options for each <tt>OptionPane</tt> instance in the 
	 * <tt>ArrayList</tt> of <tt>OptionPane</tt>s when the window is shown.
	 */
	public void initOptions() {
	    FACTORY.getSharedPane().initOptions();
		for (int i = 0, size = OPTIONS_PANE_LIST.size(); i < size; i++) {
			OptionsPane op = OPTIONS_PANE_LIST.get(i);
			op.initOptions();
		}
	}

	/**
	 * Applies the current settings in the options windows, storing them
	 * to disk.  This method delegates to the <tt>OptionsPaneManager</tt>.
	 *
	 * @throws IOException if the options could not be fully applied
	 */
	public final void applyOptions() throws IOException {
        boolean restartRequired = false;
        
		for (int i = 0, size = OPTIONS_PANE_LIST.size(); i < size; i++) {
			OptionsPane op = OPTIONS_PANE_LIST.get(i);
            restartRequired |= op.applyOptions();
		}
		
		// Apply the share directories after everything else has been applied.
		SharedDirPaneItem sharedPane = FACTORY.getSharedPane();
	    StoreSaveDirPaneItem storePane = FACTORY.getStorePane();
		if(sharedPane.isDirty() || storePane.isDirty()) {
//		    GuiCoreMediator.getFileManager().loadWithNewDirectories
//		    (sharedPane.getDirectoriesToShare(), sharedPane.getDirectorieToExclude());
		    sharedPane.resetDirtyState();
		    storePane.resetDirtyState();
        }
	
        if(restartRequired)
            GUIMediator.showMessage(I18n.tr("One or more options will take effect the next time LimeWire is restarted."));
        
        
//        SettingsWarningManager.checkSettingsLoadSaveFailure();
	}
	
	/**
	 * Determines if any of the panes are dirty.
	 */
    public final boolean isDirty() {
        for (int i = 0, size = OPTIONS_PANE_LIST.size(); i < size; i++) {
            OptionsPane op = OPTIONS_PANE_LIST.get(i);
            if (op.isDirty())
                return true;
        }
        return false;
    }
	
	/**
	 * Returns the main <code>Component</code> for this class.
	 *
	 * @return a <code>Component</code> instance that is the main component
	 *         for this class.
	 */
	public final Component getComponent() {
		return MAIN_PANEL;
	}

	/**
	 * Adds the speficied window to the CardLayout based on its title.
	 *
	 * @param window the <code>OptionsPane</code> to add
	 */
	public final void addPane(final OptionsPane pane) {
		MAIN_PANEL.add(pane.getContainer(), pane.getName());
		OPTIONS_PANE_LIST.add(pane);
	}
}
