/**
 * 
 */
package com.limegroup.gnutella.gui.actions;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.search.SearchMediator;
import com.limegroup.gnutella.util.QueryUtils;

/**
 * Provides a keyword search action.
 * <p>
 * After the search has been sent the search panel is focused.
 */
public class SearchForKeywordsAction extends AbstractAction {

	private String keywords;
	
	/**
	 * Constructs an action that searches a space separated list of keywords.
	 * 
	 * @param keywords the keywords should already be processed through
	 * {@link QueryUtils#createQueryString(String)} and be longer than
	 * 2 characters.
	 */
	public SearchForKeywordsAction(String keywords) {
		this.keywords = keywords;
		putValue(Action.NAME, MessageFormat.format
				(I18n.tr("Search for Keywords: {0}"), 
						new Object[] { keywords }));
	}
	
	public void actionPerformed(ActionEvent e) {
		SearchMediator.triggerSearch(keywords);
		GUIMediator.instance().setWindow(GUIMediator.Tabs.SEARCH);
	}
}
