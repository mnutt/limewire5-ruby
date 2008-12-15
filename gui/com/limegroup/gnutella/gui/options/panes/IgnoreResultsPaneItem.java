package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.limewire.core.settings.FilterSettings;

import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.ListEditor;

/**
 * This class defines the panel in the options window that allows the user
 * set add and remove words from a list of words to ignore when they 
 * appear in search results.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class IgnoreResultsPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Filter Results");
    
    public final static String LABEL = I18n.tr("You can filter out search results containing specific words.");

	/**
	 * Constant handle to the <tt>ListEditor</tt> that adds and removes
	 * word to ignore.
	 */
	private final ListEditor RESULTS_LIST = new ListEditor();

	/**
	 * The constructor constructs all of the elements of this 
	 * <tt>AbstractPaneItem</tt>.
	 *
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *            superclass uses to generate strings
	 */
	public IgnoreResultsPaneItem() {
	    super(TITLE, LABEL);
	    
		add(RESULTS_LIST);
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Sets the options for the fields in this <tt>PaneItem</tt> when the 
	 * window is shown.
	 */
	@Override
    public void initOptions() {
		String[] bannedWords = FilterSettings.BANNED_WORDS.getValue();
		RESULTS_LIST.setModel(new Vector<String>(Arrays.asList(bannedWords)));
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Applies the options currently set in this window, displaying an
	 * error message to the user if a setting could not be applied.
	 *
	 * @throws IOException if the options could not be applied for some reason
	 */
	@Override
    public boolean applyOptions() throws IOException {
		Vector model = RESULTS_LIST.getModel();
		String[] bannedResults = new String[model.size()];
		model.copyInto(bannedResults);		
		
        FilterSettings.BANNED_WORDS.setValue(bannedResults);
		GuiCoreMediator.getSpamServices().adjustSpamFilters();
        return false;
	}

    public boolean isDirty() {
      List model = Arrays.asList(FilterSettings.BANNED_WORDS.getValue());
      return !model.equals(RESULTS_LIST.getModel());
    }
}
