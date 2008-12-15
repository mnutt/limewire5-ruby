package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import org.limewire.core.settings.SearchSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.I18n;

/**
 * This class defines the panel in the options window that allows the user
 * to select the quality of search results to display to the user.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class SearchQualityPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Search Result Quality");
    
    public final static String LABEL = I18n.tr("You can select the \"quality\" of search results to display. Four star results indicate that the host returning the result is not firewalled and has free upload slots.");

	/**
	 * Constant handle to the radio button for selecting only four star
	 * results.
	 */
	private final JRadioButton FOUR_STAR_BUTTON = new JRadioButton();

	/**
	 * Constant handle to the radio button for selecting only three and four 
	 * star results.
	 */
	private final JRadioButton THREE_AND_FOUR_STAR_BUTTON = 
		new JRadioButton();

	/**
	 * Constant handle to the radio button for selecting only two, three and 
	 * four star results.
	 */
	private final JRadioButton TWO_THREE_AND_FOUR_STAR_BUTTON = 
		new JRadioButton();

	/**
	 * Constant handle to the radio button for showing all results.
	 */
	private final JRadioButton ALL_RESULTS_BUTTON = new JRadioButton();

	/**
	 * The stored value to allow rolling back changes.
	 */

	/**
	 * The constructor constructs all of the elements of this 
	 * <tt>AbstractPaneItem</tt>.
	 *
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *            superclass uses to generate locale-specific keys
	 */
	public SearchQualityPaneItem() {
	    super(TITLE, LABEL);

		String fourStarLabelKey = 
		    I18nMarker.marktr("Show Only Four Star Results");
		String threeAndFourStarLabelKey = 
		    I18nMarker.marktr("Show Only Three and Four Star Results");
		String twoThreeAndFourStarLabelKey = 
		    I18nMarker.marktr("Show Only Two, Three, and Four Star Results");
		String showAllResultsLabelKey =
		    I18nMarker.marktr("Show All Qualities");

		String fourStarLabel = 
		    I18n.tr(fourStarLabelKey);
		String threeAndFourStarLabel = 
		    I18n.tr(threeAndFourStarLabelKey);
		String twoThreeAndFourStarLabel = 
		    I18n.tr(twoThreeAndFourStarLabelKey);
		String showAllResultsLabel = 
		    I18n.tr(showAllResultsLabelKey);

		FOUR_STAR_BUTTON.setText(fourStarLabel);
		THREE_AND_FOUR_STAR_BUTTON.setText(threeAndFourStarLabel);
		TWO_THREE_AND_FOUR_STAR_BUTTON.setText(twoThreeAndFourStarLabel);
		ALL_RESULTS_BUTTON.setText(showAllResultsLabel);

		// Group the radio buttons.
		ButtonGroup group = new ButtonGroup();
		group.add(FOUR_STAR_BUTTON);
		group.add(THREE_AND_FOUR_STAR_BUTTON);
		group.add(TWO_THREE_AND_FOUR_STAR_BUTTON);
		group.add(ALL_RESULTS_BUTTON);

		BoxPanel buttonPanel = new BoxPanel();
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(FOUR_STAR_BUTTON);
		buttonPanel.add(THREE_AND_FOUR_STAR_BUTTON);
		buttonPanel.add(TWO_THREE_AND_FOUR_STAR_BUTTON);
		buttonPanel.add(ALL_RESULTS_BUTTON);

		add(buttonPanel);
	}

	/**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
	 *
	 * Sets the options for the fields in this <tt>PaneItem</tt> when the 
	 * window is shown.
	 */
	@Override
    public void initOptions() {
		int minQuality = SearchSettings.MINIMUM_SEARCH_QUALITY.getValue();
		switch(minQuality) {
		case 3:
			FOUR_STAR_BUTTON.setSelected(true);
			break;
		case 2:
			THREE_AND_FOUR_STAR_BUTTON.setSelected(true);
			break;
		case 1:
			TWO_THREE_AND_FOUR_STAR_BUTTON.setSelected(true);
			break;
		default:
			ALL_RESULTS_BUTTON.setSelected(true);
			break;
		}
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
		int quality = 3;
		if(FOUR_STAR_BUTTON.isSelected()) {
			quality = 3;
		} else if(THREE_AND_FOUR_STAR_BUTTON.isSelected()) {
			quality = 2;
		} else if(TWO_THREE_AND_FOUR_STAR_BUTTON.isSelected()) {
			quality = 1;
		} else {
			quality = 0;
		}			
        SearchSettings.MINIMUM_SEARCH_QUALITY.setValue(quality);
        return false;
	}
	
	public boolean isDirty() {
	    switch(SearchSettings.MINIMUM_SEARCH_QUALITY.getValue()) {
	    case 3: return !FOUR_STAR_BUTTON.isSelected();
	    case 2: return !THREE_AND_FOUR_STAR_BUTTON.isSelected();
	    case 1: return !TWO_THREE_AND_FOUR_STAR_BUTTON.isSelected();
	    case 0: return !ALL_RESULTS_BUTTON.isSelected();
	    default: return true;
	    }
    }
}
