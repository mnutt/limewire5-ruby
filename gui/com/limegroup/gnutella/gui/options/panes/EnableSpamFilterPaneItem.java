package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import org.limewire.core.settings.SearchSettings;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.I18n;

/**
 * This class gives the user the option of whether or not to enable LimeWire's
 * internal spam filter
 */
// 2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class EnableSpamFilterPaneItem extends AbstractPaneItem {
    
    public final static String TITLE = I18n.tr("Display");
    
    public final static String LABEL = I18n.tr("You can disable LimeWire\'s junk filter and delete all previously collected filter data here, if your junk filter is rating too many files as spam.");

    /** Display search results in place */
    private JRadioButton DISPLAY_IN_PLACE = new JRadioButton();
    
    /** Move spam results to the bottom */
    private JRadioButton MOVE_TO_BOTTOM = new JRadioButton();
    
    /** Hide spam results */
    private JRadioButton HIDE_SPAM = new JRadioButton();

    public EnableSpamFilterPaneItem() {
        super(TITLE, LABEL);

        DISPLAY_IN_PLACE.setText(I18n.tr("Display junk in place"));
        MOVE_TO_BOTTOM.setText(I18n.tr("Display junk at the bottom of search results"));
        HIDE_SPAM.setText(I18n.tr("Do not display junk"));

        ButtonGroup group = new ButtonGroup();
        group.add(DISPLAY_IN_PLACE);
        group.add(MOVE_TO_BOTTOM);
        group.add(HIDE_SPAM);
                
        BoxPanel buttonPanel = new BoxPanel();
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(DISPLAY_IN_PLACE);
        buttonPanel.add(MOVE_TO_BOTTOM);
        buttonPanel.add(HIDE_SPAM);

        add(buttonPanel);
    }

    @Override
    public void initOptions() {
        switch (SearchSettings.DISPLAY_JUNK_MODE.getValue()) {
        case SearchSettings.DISPLAY_JUNK_IN_PLACE:
            DISPLAY_IN_PLACE.setSelected(true);
            break;
        case SearchSettings.HIDE_JUNK:
            HIDE_SPAM.setSelected(true);
            break;
        default:
            MOVE_TO_BOTTOM.setSelected(true);
            break;
        }
    }

    @Override
    public boolean applyOptions() throws IOException {
        if (DISPLAY_IN_PLACE.isSelected()) {
            SearchSettings.DISPLAY_JUNK_MODE.setValue(SearchSettings.DISPLAY_JUNK_IN_PLACE);
        } else if (HIDE_SPAM.isSelected()) {
            SearchSettings.DISPLAY_JUNK_MODE.setValue(SearchSettings.HIDE_JUNK);
        } else {
            SearchSettings.DISPLAY_JUNK_MODE.setValue(SearchSettings.MOVE_JUNK_TO_BOTTOM);
        }
        return false;
    }

    public boolean isDirty() {
        boolean modeChange = false;
        if (DISPLAY_IN_PLACE.isSelected()) {
            modeChange = SearchSettings.DISPLAY_JUNK_MODE.getValue() != SearchSettings.DISPLAY_JUNK_IN_PLACE;
        } else if (HIDE_SPAM.isSelected()) {
            modeChange = SearchSettings.DISPLAY_JUNK_MODE.getValue() != SearchSettings.HIDE_JUNK;
        } else {
            modeChange = SearchSettings.DISPLAY_JUNK_MODE.getValue() != SearchSettings.MOVE_JUNK_TO_BOTTOM;
        }       
        return modeChange;
    }
}
