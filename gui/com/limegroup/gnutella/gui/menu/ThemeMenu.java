package com.limegroup.gnutella.gui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.UIManager;

import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.actions.AbstractAction;
import com.limegroup.gnutella.gui.actions.OpenLinkAction;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeSettings;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * The menu to be used for themes.
 */
final class ThemeMenu extends AbstractMenu {
    
    /**
     * The client property to use for theme changing items.
     */
    private static final String THEME_PROPERTY = "THEME_NAME";
    
    /**
     * The client property to use for theme changing when using 'other' L&Fs.
     */
    private static final String THEME_CLASSNAME = "THEME_CLASSNAME";
    
    /**
     * The listener for changing the theme.
     */
    private static final Action THEME_CHANGER =  new ThemeChangeAction();
    
    /**
     * The ButtonGroup to store the theme options in.
     */
    private static final ButtonGroup GROUP = new ButtonGroup();
    
    /**
     * Constructs the menu.
     */
    ThemeMenu() {
        super(I18n.tr("&Apply Skins"));
        
        addMenuItem(new OpenLinkAction("http://www.limewire.com/skins2", 
                I18n.tr("&Get More Skins"),
                I18n.tr("Find more skins from limewire.com")));
        
        addMenuItem(new RefreshThemesAction());
        
        
        JMenuItem def = addMenuItem(THEME_CHANGER);            
        final Object defaultVal = ThemeSettings.THEME_DEFAULT.getAbsolutePath();
        def.putClientProperty(THEME_PROPERTY, defaultVal);
        
        // Add a listener to set the new theme as selected.
        def.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setSelection(defaultVal);
            }
        });
        
        addSeparator();
        
        addThemeItems();
    }
    
    /**
     * Sets the default theme.
     */
    private static void setSelection(Object value) {
        Enumeration<AbstractButton> items = GROUP.getElements();
        while(items.hasMoreElements()) {
            JMenuItem item = (JMenuItem)items.nextElement();
            if(value.equals(item.getClientProperty(THEME_PROPERTY))) {
                item.setSelected(true);
                break;
            }
        }
    }        
    
    /**
     * Scans through the theme directory for .lwtp files & adds them
     * as menu items to the menu. Also adds themes inside the themes jar.
     */ 
    private void addThemeItems() {
        File themeDir = ThemeSettings.THEME_DIR_FILE;
        if(!themeDir.exists()) return;
       
        Set<Object> allThemes = new TreeSet<Object>(new ThemeComparator());
        
        String[] copiedThemes = themeDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase(Locale.US).endsWith("." + ThemeSettings.EXTENSION);
            }
        });
        if(copiedThemes != null && copiedThemes.length > 0)
            allThemes.addAll(Arrays.asList(copiedThemes));
        
        allThemes.addAll(ThemeSettings.JAR_THEME_NAMES);
        for (Iterator i = allThemes.iterator(); i.hasNext();) {
            if (!isAllowedThemeFilename((String) i.next())) 
                i.remove();
        }
        addInstalledLFs(allThemes);
        
        if(allThemes.isEmpty())
            return;
        
        String otherClassName = ThemeSettings.getOtherLF();
        
        for(Object next : allThemes) {
            File themeFile;
            JMenuItem theme;
            
            if(next instanceof String) {
                themeFile = new File(themeDir, (String)next);
                theme = new JRadioButtonMenuItem(ThemeSettings.formatName(themeFile.getName()));
                if( themeFile.equals(ThemeSettings.THEME_FILE.getValue()) )
                    theme.setSelected(true);
            } else {
                themeFile = new File(themeDir, ThemeSettings.OTHER_THEME_NAME);
                UIManager.LookAndFeelInfo lfi = (UIManager.LookAndFeelInfo)next;
                theme = new JRadioButtonMenuItem(lfi.getName());
                if( themeFile.equals(ThemeSettings.THEME_FILE.getValue()) &&
                    otherClassName != null && lfi.getClassName().equals(otherClassName) )
                    theme.setSelected(true);
                theme.putClientProperty(THEME_CLASSNAME, lfi.getClassName());
            }
                
            theme.setFont(AbstractMenu.FONT);
            GROUP.add(theme);
            theme.addActionListener(THEME_CHANGER);
            theme.putClientProperty(THEME_PROPERTY, themeFile.getAbsolutePath());
            MENU.add(theme);
        }
    }
    
    /**
     * Removes all items in the group from the menu.  Used for refreshing.
     */
    private void removeThemeItems() {
        Enumeration<AbstractButton> items = GROUP.getElements();
        List<JMenuItem> removed = new LinkedList<JMenuItem>();
        while(items.hasMoreElements()) {
            JMenuItem item = (JMenuItem)items.nextElement();
            MENU.remove(item);
            removed.add(item);
        }
        
        for(JMenuItem item : removed)
            GROUP.remove(item);
    }
    
    /**
     * Refreshes the theme menu options to those on the disk.
     */
    private class RefreshThemesAction extends AbstractAction {
        
        public RefreshThemesAction() {
            super(I18n.tr("&Refresh Skins"));
            putValue(LONG_DESCRIPTION, I18n.tr("Reload available skins from disk"));
    }
    
    	public void actionPerformed(ActionEvent e) {
            removeThemeItems();
            addThemeItems();
    	}
    }    
    
    /**
     * Action that is also used as action listener.
     */
    protected static class ThemeChangeAction extends AbstractAction {
        
        public ThemeChangeAction() {
            super(I18n.tr("Use &Default"));
            putValue(LONG_DESCRIPTION, I18n.tr("Use your default skin"));
        }
        
        public void actionPerformed(ActionEvent e) {
            JMenuItem item = (JMenuItem)e.getSource();
            String themePath = (String)item.getClientProperty(THEME_PROPERTY);
            String className = (String)item.getClientProperty(THEME_CLASSNAME);
    	    ThemeMediator.changeTheme(new File(themePath), className);
        }
    }
    
    /**
     * Simple class to sort the theme lists.
     */
    private static class ThemeComparator implements Comparator<Object> {
        public int compare(Object a, Object b) {
            String name1, name2;
            if(a instanceof String)
                name1 = ThemeSettings.formatName((String)a);
            else
                name1 = ((UIManager.LookAndFeelInfo)a).getName();
                
            if(b instanceof String)
                name2 = ThemeSettings.formatName((String)b);
            else
                name2 = ((UIManager.LookAndFeelInfo)b).getName();

            return name1.compareTo(name2);
        }
    }
    
    /**
     * Adds installed LFs to the list.
     */
    private static void addInstalledLFs(Set<Object> themes) {
        UIManager.LookAndFeelInfo[] lfs = UIManager.getInstalledLookAndFeels();
        if(lfs == null)
            return;
            
        for(int i = 0; i < lfs.length; i++) {
            UIManager.LookAndFeelInfo l = lfs[i];
            if(l.getClassName().equals("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"))
                continue;
            if(l.getClassName().startsWith("apple"))
                continue;
            if(l.getClassName().equals("com.sun.java.swing.plaf.gtk.GTKLookAndFeel") &&
               OSUtils.isLinux())
                continue;
            if(l.getClassName().equals("com.sun.java.swing.plaf.motif.MotifLookAndFeel"))
                continue;
                
            themes.add(l);
        }
    }
    
    /**
     * Checks if <code>name</code> is a theme filename which can be shown.
     */
    private boolean isAllowedThemeFilename(String name) {
    	// don't allow anything that isn't a theme file
    	if(!name.endsWith(ThemeSettings.EXTENSION))
    		return false;
    	
    	// if this is one of the old 'default_X' themes
    	// we used to ship with, ignore it.
    	if(name.startsWith("default_"))
    		return false;
    	
    	// don't allow the 'other' theme to show.
    	if(name.equals(ThemeSettings.OTHER_THEME_NAME))
    		return false;
    	
    	// only allow the osx theme if we're on osx.
    	if(!OSUtils.isMacOSX() && 
    			name.equals(ThemeSettings.PINSTRIPES_OSX_THEME_NAME))
    		return false;
    	
    	// only allow the brushed metal theme if we're on
    	// osx (with 10.3+, which is below our minimum)
    	if(!OSUtils.isMacOSX() && 
                name.equals(ThemeSettings.BRUSHED_METAL_OSX_THEME_NAME))
    		return false;
    	
    	// only allow the windows theme if we're on windows.
    	if(!OSUtils.isWindows() &&
    			name.equals(ThemeSettings.WINDOWS_LAF_THEME_NAME))
    		return false;
    	
    	// only show pro theme if we're on pro.
    	if(!LimeWireUtils.isPro() &&
    			name.equals(ThemeSettings.PRO_THEME_NAME))
    		return false;
    	
    	// only show GTK theme on linux with 1.5  
    	if(name.equals(ThemeSettings.GTK_LAF_THEME_NAME) &&   
    			!OSUtils.isLinux())  
    		return false;  
    	
    	// everything's okay -- allow it.                
    	return true;
    }
    
}
