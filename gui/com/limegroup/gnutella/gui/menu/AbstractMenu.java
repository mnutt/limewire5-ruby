package com.limegroup.gnutella.gui.menu;

import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.plaf.FontUIResource;

import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.actions.AbstractAction;
import com.limegroup.gnutella.gui.actions.ToggleSettingAction;
import com.limegroup.gnutella.gui.themes.ThemeSettings;

/**
 * Provides a skeletal implementation of the <tt>Menu</tt> interface to 
 * minimize the necessary work in classes that extend <tt>AbstractMenu</tt>.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
abstract class AbstractMenu implements Menu {
    
	/**
	 * The font menus should use.
	 */
	static final Font FONT = new FontUIResource(new Font("Dialog", Font.PLAIN, 11 + ThemeSettings.FONT_SIZE_INCREMENT.getValue()));

	/**
	 * Constant handle to the <tt>JMenu</tt> instance for this 
	 * <tt>AbstractMenu</tt>.
	 */
	protected final JMenu MENU;

	/**
	 * Creates a new <tt>AbstractMenu</tt>, using the <tt>key</tt> 
	 * argument for setting the locale-specific title and 
	 * accessibility text.
	 *
	 * @param key the key for locale-specific string resources unique
	 *            to the menu
	 */
	protected AbstractMenu(String name) {
	    // using an action here to get the mnemonic parsed
	    MENU = new JMenu(new MenuAction(name));
		MENU.setFont(FONT);
	}

	/**
	 * Returns the <tt>JMenu</tt> instance for this <tt>AbstractMenu</tt>.
	 * 
	 * @return the <tt>JMenu</tt> instance for this <tt>AbstractMenu</tt>	
	 */
	public JMenu getMenu() {
		return MENU;
	}

	protected JMenuItem addMenuItem(Action action) {
	    JMenuItem item = new JMenuItem(action);
	    item.setFont(FONT);
	    MENU.add(item);
	    return item;
	}
	
    protected JMenuItem addToggleMenuItem(Action action, boolean selected) {
        JMenuItem item;
        if (OSUtils.isMacOSX()) {
            item = new JRadioButtonMenuItem(action);
        } else {
            item = new JCheckBoxMenuItem(action);
        }
        item.setFont(FONT);
        item.setSelected(selected);
        MENU.add(item);
        return item;
    }
	
	protected JMenuItem addToggleMenuItem(ToggleSettingAction action) {
	    JMenuItem item = addToggleMenuItem(action, action.getSetting().getValue());
	    return item;
	}

	/**
	 * Adds a separator to the <tt>JMenu</tt> instance.
	 */
	protected void addSeparator() {
		MENU.addSeparator();
	}

	private static class MenuAction extends AbstractAction {
	    public MenuAction(String name) {
	        super(name);
        }
	    public void actionPerformed(ActionEvent e) {
		}
	}
}
