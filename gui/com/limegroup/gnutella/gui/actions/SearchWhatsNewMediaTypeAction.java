package com.limegroup.gnutella.gui.actions;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.search.NamedMediaType;
import com.limegroup.gnutella.gui.search.SearchInformation;
import com.limegroup.gnutella.gui.search.SearchMediator;

/**
 * Generic action which triggers a "What's New" search for a named mediatype.
 * <p>
 * After the search has been sent, the search panel is focused.
 */
public class SearchWhatsNewMediaTypeAction extends AbstractAction {

	private NamedMediaType nm;
	
	/**
	 * Constructs an action which starts a "What's New" search for a named
	 * mediatype
	 * @param nm the mediatype of the search
	 */
	public SearchWhatsNewMediaTypeAction(NamedMediaType nm) {
		this.nm = nm;	
		putValue(Action.NAME, MessageFormat.format
				(I18n.tr("Search \"What's New\" for: {0}"),
						new Object[] { nm.getName() }));
	}
	
	public void actionPerformed(ActionEvent e) {
		SearchMediator.triggerSearch(SearchInformation.createWhatsNewSearch
				(nm.getName(), nm.getMediaType()));
		GUIMediator.instance().setWindow(GUIMediator.Tabs.SEARCH);
	}
	
}