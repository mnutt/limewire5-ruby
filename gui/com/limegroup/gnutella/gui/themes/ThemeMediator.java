package com.limegroup.gnutella.gui.themes;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MultiLineLabel;
import com.limegroup.gnutella.gui.ResourceManager;
import com.limegroup.gnutella.gui.SplashWindow;
import com.limegroup.gnutella.gui.TipOfTheDayMediator;
import com.limegroup.gnutella.gui.notify.NotifyUserProxy;


/**
 * Class that mediates between themes and LimeWire.
 */
public class ThemeMediator {
    
    /**
     * <tt>List</tt> of <tt>ThemeObserver</tt> classes to notify of
     * ui components of theme changes.
     */
    private static final List<ThemeObserver> THEME_OBSERVERS = new LinkedList<ThemeObserver>();

    /**
     * Changes the current theme to the theme in <code>newTheme</code>.
     */
    public static void changeTheme(File newTheme) {
	    changeTheme(newTheme, null, false);
    }
    
    /**
     * Changes the current theme to the given theme using the classname for 'other' themes.
     */
    public static void changeTheme(File newTheme, String className) {
        changeTheme(newTheme, className, false);
    }
    
    /**
     * Changes the current theme, possibly allowing reload.
     */
    public static void changeTheme(File newTheme, boolean forceReload) {
        changeTheme(newTheme, null, forceReload);
    }

    /**
     * Changes the current theme to the theme in <code>newTheme</code>.
     */
    public static void changeTheme(File newTheme, String className, boolean forceReload) {
    	if(!newTheme.isFile()) {
        	if (ThemeSettings.JAR_THEME_NAMES.contains(newTheme.getName())) {
        	    try {
        	        CommonUtils.copyResourceFile(newTheme.getName(), newTheme, true);
        	    } catch(IOException iox) {
        	        throw new RuntimeException("couldn't extract theme!", iox);
        	    }
        	}
        }
        
        if(!newTheme.isFile()) {
			GUIMediator.showError(I18n.tr("The skin file you selected does not exist or is invalid. Please choose another file, or use the default."));
			return;
    	}
    	
    	if(className != null && !className.equals("")) {
    	    try {
    	        Class.forName(className);
    	    } catch(ClassNotFoundException cnfe) {
    	        GUIMediator.showError(I18n.tr("The skin file you selected does not exist or is invalid. Please choose another file, or use the default."));
    	        return;
    	    }
    	} else {
    	    className = "";
    	}

        String oldClassName = ThemeSettings.getOtherLF();
        if(oldClassName == null)
            oldClassName = "";
            
    	if(!ThemeSettings.THEME_FILE.getValue().equals(newTheme) ||
    	   !className.equals(oldClassName)) {
    	    File oldTheme = ThemeSettings.THEME_FILE.getValue();
    	    ThemeSettings.THEME_FILE.setValue(newTheme);
    	    ThemeFileHandler.reload(forceReload);
            ThemeSettings.setOtherLF(className);
    	    if(!ThemeFileHandler.isCurrent()) {
        		ThemeSettings.THEME_FILE.setValue(oldTheme);
        		ThemeFileHandler.reload(forceReload);
        		showThemeError(newTheme.getName());
    	    } else {
        		boolean isPinstripes = ThemeSettings.isPinstripesTheme();
        		boolean wasMetal = ResourceManager.instance().isBrushedMetalSet();
        		boolean isMetal = ThemeSettings.isBrushedMetalTheme();
        		if((isPinstripes && wasMetal) || (isMetal && !wasMetal))
        		    GUIMediator.showMessage(I18n.tr("To try your new skin, you must restart LimeWire."));
        		else {
        		    GUIMediator.setAppVisible(false);
        		    SplashWindow.instance().setVisible(true);
        		    SplashWindow.instance().refreshImage();
        		    ResourceManager.instance().themeChanged();
        
        		    updateComponentHierarchy();
        		    
        		    updateThemeObservers();
        		    GUIMediator.setAppVisible(true);
        		    SplashWindow.instance().setVisible(false);
        		}
    	    }
    	}     
    }
    
    public static void updateComponentHierarchy() {
        SwingUtilities.updateComponentTreeUI(GUIMediator.getMainOptionsComponent());
        TipOfTheDayMediator.instance().updateComponentTreeUI();
        SwingUtilities.updateComponentTreeUI(GUIMediator.getAppFrame());
        SwingUtilities.updateComponentTreeUI(GUIMediator.getTrayMenu());
        NotifyUserProxy.instance().updateUI();
        updateThemeObservers();
    }

    /**
     * Adds the specified <tt>ThemeObserver</tt> instance to the list of
     * <tt>ThemeObserver</tt>s that should be notified whenever the theme
     * changes.
     *
     * @param observer the <tt>ThemeObserver</tt> to add to the notification
     *  list
     */
    public static void addThemeObserver(ThemeObserver observer) {
	    THEME_OBSERVERS.add(observer);
    }

    /**
     * Removes the specified <tt>ThemeObserver</tt> instance from the list
     * of <tt>ThemeObserver</tt>s.  This is necessary to allow the removed
     * component to be garbage-collected.
     *
     * @param observer the <tt>ThemeObserver</tt> to remove from the
     *  notification list
     */
    public static void removeThemeObserver(ThemeObserver observer) {
        THEME_OBSERVERS.remove(observer);
    }

    /**
     * Updates all theme observers.
     */
    public static void updateThemeObservers() {
        for(ThemeObserver curObserver : THEME_OBSERVERS)
    	    curObserver.updateTheme();

        GUIMediator.getMainOptionsComponent().validate();
        GUIMediator.getAppFrame().validate();
    }

    /**
     * Shows a dialog with an error message about selecting a theme.
     */
    public static void showThemeError(String name) {
        Dimension size = new Dimension(300, 100);
        
        final JDialog d = new JDialog(GUIMediator.getAppFrame());
        d.setModal(true);
        d.setResizable(false);
        d.setTitle(I18n.tr("Invalid Skin"));
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
        BoxPanel body = new BoxPanel(BoxPanel.Y_AXIS);
        
        JPanel text = new BoxPanel(BoxPanel.Y_AXIS);
        MultiLineLabel label = new MultiLineLabel(
						  I18n.tr("The skin you selected is out of date. Please download the latest version of the skin."), 250);
    	label.setFont(new Font("Dialog", Font.BOLD, 12));
    	text.add(Box.createVerticalGlue());
    	text.add(GUIUtils.center(label));
    	text.add(Box.createVerticalGlue());
    		
    	BoxPanel buttons = new BoxPanel(BoxPanel.X_AXIS);
    	
    	JButton getNew = new JButton(I18n.tr(
								   "Get New Skins"));
        JButton later = new JButton(I18n.tr(
								  "Later"));
        getNew.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			GUIMediator.openURL("http://www.limewire.com/skins2");
    		    d.dispose();
    		    d.setVisible(false);
    		}
	    });
        later.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    		    d.dispose();
    		    d.setVisible(false);
    		}
	    });
    		
    	buttons.add(getNew);
    	buttons.add(GUIMediator.getHorizontalSeparator());
    	buttons.add(later);
    		
    	body.add(text);
        body.add(buttons);
        body.setPreferredSize(size);
        d.getContentPane().add(body);
        d.pack();
        d.setLocationRelativeTo(GUIMediator.getAppFrame());
        d.setVisible(true);
    }
}
