package com.limegroup.gnutella.gui.actions;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.limewire.util.I18NConvert;
import org.limewire.util.NameValue;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.search.NamedMediaType;
import com.limegroup.gnutella.gui.search.SearchInformation;
import com.limegroup.gnutella.gui.search.SearchMediator;
import com.limegroup.gnutella.util.QueryUtils;

/**
 * A class that triggers an xml search query for a single xml field and its
 * value.
 * <p>
 * After the search request has been sent the search panel is focused.
 */
public class SearchXMLFieldAction extends AbstractAction {

	private NameValue<?> displayPair;
	private String name;
	private String value;
	private NamedMediaType nm;

	/**
	 * Constructs an xml field search action.
	 * @param displayPair used for constructing the name of the action
	 * @param name the name of the xml field
	 * @param value the value of the xmls field
	 * @param nm the mediatype whose xml schema is used for the xml query
	 */
	public SearchXMLFieldAction(NameValue<?> displayPair, String name, 
			String value,
			NamedMediaType nm) {
		this.displayPair = displayPair;
		this.name = name;
		this.value = value;
		this.nm = nm;
		String formatted = MessageFormat.format(
                I18n.tr("Search for {0}: {1}"),
				new Object[] { displayPair.getName(), displayPair.getValue() }
        );
        if(formatted.length() > 80)
            formatted = formatted.substring(0, 80) + "...";
		
		putValue(Action.NAME, formatted);
	}
	
	public void actionPerformed(ActionEvent e) {
		NameValue<String> namValue = new NameValue<String>(name, I18NConvert.instance().getNorm(value));
		String xml = GuiCoreMediator.getLimeXMLDocumentFactory().createLimeXMLDocument(Collections.singletonList(namValue), nm.getSchema().getSchemaURI()).getXMLString();
		SearchMediator.triggerSearch(SearchInformation.createTitledKeywordSearch
				(QueryUtils.createQueryString(value, true), xml, 
						nm.getMediaType(),
						displayPair.getName() + ": "
						+ displayPair.getValue()));
		GUIMediator.instance().setWindow(GUIMediator.Tabs.SEARCH);
	}
	
}