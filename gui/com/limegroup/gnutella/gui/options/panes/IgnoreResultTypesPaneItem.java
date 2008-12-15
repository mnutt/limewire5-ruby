package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.JCheckBox;

import org.limewire.core.settings.FilterSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.search.SearchMediator;

/**
 * This class defines the panel in the options window that allows the user
 * to filter out general types of search results, such as search results
 * containing "adult content."
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class IgnoreResultTypesPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Filter Result Types");
    
    public final static String LABEL = I18n.tr("You can specify general types of search results to filter.");

	/**
	 * Handle to the check box for ignoring adult content.
	 */
	private JCheckBox IGNORE_ADULT_CHECK_BOX = new JCheckBox();

	/**
	 * Handle to the check box for ignoring htm/html files.
	 */
	private JCheckBox IGNORE_HTML_CHECK_BOX = new JCheckBox();

	/**
	 * Handle to the check box for ignoring .vbs files.
	 */
	private JCheckBox IGNORE_VBS_CHECK_BOX = new JCheckBox();
	
	/**
	 * Handle to the check box for ignoring .asf and .wmv files.
	 */
	private JCheckBox IGNORE_WMV_ASF_CHECK_BOX = new JCheckBox();

	/**
	 * Key for the locale-specifis string for the adult content check box 
	 * label.
	 */
	private String ADULT_BOX_LABEL = I18nMarker.marktr("Ignore Adult Content");

	/**
	 * Key for the locale-specifis string for the html file check box 
	 * label.
	 */
	private String HTML_BOX_LABEL = I18nMarker.marktr("Ignore .htm/.html Files");

	/**
	 * Key for the locale-specifis string for the vbs file check box 
	 * label.
	 */
	private String VBS_BOX_LABEL = I18nMarker.marktr("Ignore .vbs Files");
	
	/**
	 * Key for the locale-specific string for the wmv and asf file check box label.
	 */
	private String WMV_ASF_BOX_LABEL = I18nMarker.marktr("Ignore .wmv/.asf files");

	/**
	 * The constructor constructs all of the elements of this 
	 * <tt>AbstractPaneItem</tt>.
	 *
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *            superclass uses to generate strings
	 */
	public IgnoreResultTypesPaneItem() {
	    super(TITLE, LABEL);
	    
		IGNORE_ADULT_CHECK_BOX.setText(I18n.tr(ADULT_BOX_LABEL));
		IGNORE_HTML_CHECK_BOX.setText(I18n.tr(HTML_BOX_LABEL));
		IGNORE_VBS_CHECK_BOX.setText(I18n.tr(VBS_BOX_LABEL));
		IGNORE_WMV_ASF_CHECK_BOX.setText(I18n.tr(WMV_ASF_BOX_LABEL));
		
		add(IGNORE_ADULT_CHECK_BOX);
		add(IGNORE_HTML_CHECK_BOX);
		add(IGNORE_VBS_CHECK_BOX);
		add(IGNORE_WMV_ASF_CHECK_BOX);
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Sets the options for the fields in this <tt>PaneItem</tt> when the 
	 * window is shown.
	 */
	@Override
    public void initOptions() {
		IGNORE_ADULT_CHECK_BOX.setSelected(FilterSettings.FILTER_ADULT.getValue());
		IGNORE_HTML_CHECK_BOX.setSelected(FilterSettings.FILTER_HTML.getValue());
		IGNORE_VBS_CHECK_BOX.setSelected(FilterSettings.FILTER_VBS.getValue());
		IGNORE_WMV_ASF_CHECK_BOX.setSelected(FilterSettings.FILTER_WMV_ASF.getValue());
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
	    boolean adultChanged = false;
	    adultChanged = FilterSettings.FILTER_ADULT.getValue() !=
	                   IGNORE_ADULT_CHECK_BOX.isSelected();
		FilterSettings.FILTER_ADULT.setValue(IGNORE_ADULT_CHECK_BOX.isSelected());
		FilterSettings.FILTER_VBS.setValue(IGNORE_VBS_CHECK_BOX.isSelected());
		FilterSettings.FILTER_HTML.setValue(IGNORE_HTML_CHECK_BOX.isSelected());
		FilterSettings.FILTER_WMV_ASF.setValue(IGNORE_WMV_ASF_CHECK_BOX.isSelected());
		GuiCoreMediator.getSpamServices().adjustSpamFilters();
		if(adultChanged)
		    SearchMediator.rebuildInputPanel();
        return false;
	}
	
    public boolean isDirty() {
        return FilterSettings.FILTER_ADULT.getValue() != IGNORE_ADULT_CHECK_BOX.isSelected() ||
               FilterSettings.FILTER_VBS.getValue() != IGNORE_VBS_CHECK_BOX.isSelected() ||
               FilterSettings.FILTER_HTML.getValue() != IGNORE_HTML_CHECK_BOX.isSelected() ||
               FilterSettings.FILTER_WMV_ASF.getValue() != IGNORE_WMV_ASF_CHECK_BOX.isSelected();
    }	
}
