package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import org.limewire.core.settings.SearchSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.gui.SizedWholeNumberField;
import com.limegroup.gnutella.gui.WholeNumberField;

/**
 * This class defines the panel in the options window that allows the user
 * to change the maximum number of dowloads to allow at any one time.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class MaximumSearchesPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Maximum Searches");
    
    public final static String LABEL = I18n.tr("You can set the maximum number of simultaneous searches you can perform.");

	/**
	 * Constant for the key of the locale-specific <code>String</code> for the 
	 * label on the component that allows to user to change the setting for
	 * this <tt>PaneItem</tt>.
	 */
	private final String OPTION_LABEL = I18nMarker.marktr("Maximum Searches:");


	/**
	 * Handle to the <tt>WholeNumberField</tt> where the user selects the
	 * time to live for outgoing searches.
	 */
	private WholeNumberField _maxSearchesField;

	/**
	 * The stored value to allow rolling back changes.
	 */
    private int _maxSearchesString;

	/**
	 * The constructor constructs all of the elements of this
	 * <tt>AbstractPaneItem</tt>.
	 * 
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *        superclass uses to generate locale-specific keys
	 */
	public MaximumSearchesPaneItem() {
	    super(TITLE, LABEL);

		_maxSearchesField = new SizedWholeNumberField();
		LabeledComponent comp = new LabeledComponent(OPTION_LABEL,
				_maxSearchesField, LabeledComponent.LEFT_GLUE,
				LabeledComponent.LEFT);

		add(comp.getComponent());
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Sets the options for the fields in this <tt>PaneItem</tt> when the 
	 * window is shown.
	 */
	@Override
    public void initOptions() {
        _maxSearchesString  = SearchSettings.PARALLEL_SEARCH.getValue();
        _maxSearchesField.setValue(_maxSearchesString);
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
        final int maxSearches = _maxSearchesField.getValue();
        if(maxSearches != _maxSearchesString) {
            try {
                SearchSettings.PARALLEL_SEARCH.setValue(maxSearches);
                _maxSearchesString = maxSearches;
            }
            catch (IllegalArgumentException iae) {
				throw new IOException();
            }
        }
        return false;
	}
	
    public boolean isDirty() {
        return SearchSettings.PARALLEL_SEARCH.getValue() != _maxSearchesField.getValue();
    }
}
