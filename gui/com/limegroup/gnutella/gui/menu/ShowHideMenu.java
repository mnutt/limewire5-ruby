package com.limegroup.gnutella.gui.menu;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.Action;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.GUIMediator.Tabs;
import com.limegroup.gnutella.gui.actions.AbstractAction;

/**
 * The menu item that actually displays the options for dynamically
 * showing or hiding tabs.
 */
final class ShowHideMenu extends AbstractMenu {
    

    /**
     * Constructs all of the elements of the <tt>ViewMenu</tt>, in particular
     * the check box menu items and listeners for the various tabs displayed
     * in the main window.
     *
     * @param key the key allowing the <tt>AbstractMenu</tt> superclass to
     *  access the appropriate locale-specific string resources
     */
    ShowHideMenu() {
        super(I18n.tr("Sho&w/Hide"));
        
        for (Tabs tab : Tabs.getOptionalTabs()) {
            addToggleMenuItem(new ShowTabAction(tab), tab.isViewEnabled());
        }
        
        MENU.add(new SearchMenu().getMenu());
    }

	private static class ShowTabAction extends AbstractAction {
		
		/** The tab this listener is using. */
		private final GUIMediator.Tabs tab;

		private ShowTabAction(GUIMediator.Tabs tab) {
		    super(tab.getName());
		    putValue(LONG_DESCRIPTION, I18n.tr("Show {0} Window", tab.getName()));
			this.tab = tab;
			tab.addPropertyChangeListener(new PropertyChangeListener() {
			    public void propertyChange(PropertyChangeEvent evt) {
                    if("name".equals(evt.getPropertyName())) {
                        putValue(Action.NAME, evt.getNewValue());
                        putValue(LONG_DESCRIPTION, I18n.tr("Show {0} Window", evt.getNewValue()));
                    }
			    }
			});
		}

        public void actionPerformed(ActionEvent ae) {
		    AbstractButton button = (AbstractButton)ae.getSource();
			GUIMediator.instance().setTabVisible(tab, button.isSelected());
		    GUIMediator.instance().setWindow(tab);
        }
    }
}