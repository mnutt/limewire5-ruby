package com.limegroup.gnutella.gui.menu;

import java.awt.event.ActionEvent;

import org.limewire.core.settings.UISettings;
import org.limewire.setting.BooleanSetting;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.actions.ToggleSettingAction;
import com.limegroup.gnutella.gui.search.SearchMediator;

/**
 * Options for the search tab.
 */
final class SearchMenu extends AbstractMenu {
    

    /**
     * Constructs the SearchMenu options.
     *
     * @param key the key allowing the <tt>AbstractMenu</tt> superclass to
     *  access the appropriate locale-specific string resources
     */
    SearchMenu() {
        super(I18n.tr("&Search"));
    
        addToggleMenuItem(new ShowSearchComponentAction(
                    UISettings.SEARCH_RESULT_FILTERS, I18n.tr("Result &Filters"),
                    I18n.tr("Show Search Result Filters")));
    }
    
    private static class ShowSearchComponentAction extends ToggleSettingAction {
        
        ShowSearchComponentAction(BooleanSetting set, String name, String description) {
            super(set, name, description);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            SearchMediator.rebuildInputPanel();
        }
    }
}