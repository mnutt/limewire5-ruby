package com.limegroup.gnutella.gui.search;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.MissingResourceException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;

import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.ImageManipulator;
import com.limegroup.gnutella.gui.themes.ThemeSettings;

/**
 * This class acts as a wrapper around the "kill" icon displayed in the
 * search tabs.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class CancelSearchIconProxy implements Icon {
    
    private static final int PLAIN = 0;
    private static final int SELECTED = 1;
    private static final int ARMED = 2;
    
    private static Icon PLAIN_ICON;
    private static Icon SELECTED_ICON;
    private static Icon ARMED_ICON;
	
	/**
	 * The <tt>ImageIcon</tt> for our cancel image.
	 */
	private Icon _cancelIcon;

	/**
	 * The width of the icon in pixels.
	 */
	private int _width;

	/**
	 * The height of the icon in pixels.
	 */
	private int _height;

	/**
	 * The x position of the icon within its tab.
	 */
	private int _x;

	/**
	 * The y position of the icon within its tab.
	 */
	private int _y;
	
	/**
	 * The style of this icon.
	 */
	private final int style;
	
	static CancelSearchIconProxy createPlain() {
	    return new CancelSearchIconProxy(PLAIN);
	}
	
	static CancelSearchIconProxy createSelected() {
	    return new CancelSearchIconProxy(SELECTED);
	}
	
	static CancelSearchIconProxy createArmed() {
	    return new CancelSearchIconProxy(ARMED);
	}

	/**
	 * the constructor loads the image icon and stores the location
	 * and dimensions.
	 */
	CancelSearchIconProxy(int style) {
	    this.style = style;
	    setIcon();
	}
	
	/**
	 * Sets the appropriate icon.
	 */
	private void setIcon() {
        switch(style) {
        case ARMED:
            _cancelIcon = ARMED_ICON;
            break;
        case SELECTED:
            _cancelIcon = SELECTED_ICON;
            break;
        case PLAIN:
            _cancelIcon = PLAIN_ICON;
            break;
        }
		_width  = _cancelIcon.getIconWidth();
		_height = _cancelIcon.getIconHeight();
		_x = 0;
		_y = 0;
	}

	// resets the cached icons for each kind of icon
	static void updateTheme() {
	    GUIMediator.safeInvokeAndWait(new Runnable() {
	        public void run() {

	            if(ThemeSettings.isWindowsTheme() && WindowsXPIcon.isAvailable()) {
	                try {
	                    PLAIN_ICON = new WindowsXPIcon(PLAIN);
	                    SELECTED_ICON = new WindowsXPIcon(SELECTED);
	                    ARMED_ICON = new WindowsXPIcon(ARMED);
	                    return;
	                } catch(IllegalArgumentException iae) {
	                    // couldn't create image to resize
	                } catch(NullPointerException npe) {
	                    // internal windows plaf error
	                } catch(ArithmeticException ae) {
	                    // internal windows error (see https://www.limewire.org/jira/browse/GUI-8)
	                }
	                // if construction failed, fall through...
	            }

	            PLAIN_ICON = GUIMediator.getThemeImage("kill");
	            try {
	                SELECTED_ICON = GUIMediator.getThemeImage("kill_on");
	            } catch(MissingResourceException mre) {
	                SELECTED_ICON = PLAIN_ICON;
	            }
	            ARMED_ICON = SELECTED_ICON;
	        }
	    });
	}

    /**
	 * implements Icon interface.
     * Gets the width of the icon.
	 * 
     * @return the width in pixels of this icon
     */
    public int getIconWidth() {
		return _width;
    }

    /**
	 * implements Icon interface.
     * Gets the height of the icon.
	 *
     * @return the height in pixels of this icon
     */
    public int getIconHeight() {
		return _height;
    }

	/**
	 * implements Icon interface.
	 * forwards the call to the proxied Icon object and stores the
	 * x and y coordinates of the icon.
	 */
	public void paintIcon(Component c, Graphics g, int x, int y) {
		_x = x;
		_y = y;
		_cancelIcon.paintIcon(c, g, x, y);
	}

	/**
	 * Determines whether or not a click at the given x, y position
	 * is a "hit" on the kill search icon.
	 * 
	 * @param x the x location of the mouse event
	 *
	 * @param y the y location of the mouse event
	 *
	 * @return <tt>true</tt> if the mouse event occurred within the 
	 *         bounding rectangle of the icon, <tt>false</tt> otherwise.
	 */
	boolean shouldKill(int x, int y) {
        int xMax = _x + _width;
        int yMax = _y + _height;
		if(!((x >= _x) && (x <= xMax))) return false;
		if(!((y >= _y) && (y <= yMax))) return false;
	
		return true;
	}
	
	/**
	 * A delegate for the windows xp icon.
	 *
	 * This is necessary because the windows icon requires it be drawn
	 * from a JButton, and its InternalFrame designates if it's drawn
	 * as armed/disarmed/selected.  It's also always drawn from 0,0
	 * so the graphics object needs to be translated.
	 */
	private static class WindowsXPIcon implements Icon {
    	// cached windows icon, and a marker null value so we don't
    	// needlessy try to recreate it each time.
    	private static Icon _windowsCloseIcon;
    	private static final Icon NULL = new ImageIcon();
    	
    	/**
    	 * Determines if the Windows icon is available and caches it.
    	 */
    	@SuppressWarnings("unchecked")
        static boolean isAvailable() {
    	    if(!(OSUtils.isWindowsXP() || OSUtils.isWindowsVista()) ||
               !Boolean.TRUE.equals(
                   Toolkit.getDefaultToolkit().getDesktopProperty(
                       "win.xpstyle.themeActive")) ||
               System.getProperty("swing.noxp") != null)
                 return false;
    	    if(_windowsCloseIcon == NULL)
    	        return false;
    	    if(_windowsCloseIcon != null)
    	        return true;
    	        
    	    try {
    	        Class c = Class.forName("com.sun.java.swing.plaf.windows.WindowsIconFactory");
    	        Method m = c.getDeclaredMethod("createFrameCloseIcon");
    	        _windowsCloseIcon = (Icon)m.invoke(c, new Object[0]);
    	        if(_windowsCloseIcon.getIconHeight() == 0 
    	         || _windowsCloseIcon.getIconWidth() == 0) {
        	        _windowsCloseIcon = NULL;
        	        return false;
                } else {
                    return true;
                }   
            } catch(ClassNotFoundException cfnfe) {
            } catch(IllegalAccessException iae) {
            } catch(ExceptionInInitializerError eiie) {
            } catch(SecurityException se) {
            } catch(ClassCastException cce) {
            } catch(NoSuchMethodException nsme) {
            } catch(InvocationTargetException ite) {
            } catch(NoClassDefFoundError ncdfe) {
            }
            
            _windowsCloseIcon = NULL;
            return false;
        }
        
        /**
         * The button to use when drawing the icon.
         */
	    private Component component;
	    
	    /**
	     * The resized version of the icon.
	     */
    	private Icon _resizedIcon;
	    
	    /**
	     * Constructs a new icon with the given button.
	     */
	    WindowsXPIcon(final int style) {
	        JButton button = new JButton();
	        button.getModel().setArmed(false);
	        button.getModel().setPressed(false);
	        button.getModel().setRollover(style == ARMED);
	        button.getModel().setEnabled(true);
	        JInternalFrame frame = new JInternalFrame() {
	            @Override
                public boolean isSelected() { return style != PLAIN; }
	        };
	        frame.getContentPane().add(button);
	        component = button;
	        
	        // set the correct size.
	        Icon icon = ImageManipulator.resize(this, 14, 14);
	        _resizedIcon = icon;
	        
	        // component not needed any more.
	        component = null;
        }
        
        // icon methods.
        public int getIconHeight() {
            if(_resizedIcon != null)
                return _resizedIcon.getIconHeight();
            else
                return _windowsCloseIcon.getIconHeight();
        }
        
        public int getIconWidth() {
            if(_resizedIcon != null)
                return _resizedIcon.getIconWidth();
            else
                return _windowsCloseIcon.getIconWidth();
        }
        
        /**
         * Translates the graphics component prior to drawing, since the
         * icon always draws at 0,0.
         */
        public void paintIcon(Component c, Graphics g, int x, int y) {
            if(_resizedIcon != null) {
                _resizedIcon.paintIcon(c, g, x, y);
            } else {
                g.translate(x, y);
                _windowsCloseIcon.paintIcon(component, g, 0, 0);
                g.translate(-x, -y);
            }
        }
    }   
	    
}

