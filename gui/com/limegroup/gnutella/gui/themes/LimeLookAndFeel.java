package com.limegroup.gnutella.gui.themes;

import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;

import com.jgoodies.plaf.plastic.PlasticButtonUI;
import com.limegroup.gnutella.gui.GUIMediator;

/**
 * Controls the look and feel of the application.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class LimeLookAndFeel extends MetalLookAndFeel {
	/**
	 * The constructor simply sets the color theme for the application.
	 */
	public LimeLookAndFeel() {
		setCurrentTheme(new LimeTheme());
	}

	@Override
    public String getDescription() {
		return "Lime Look and Feel";
	}

	@Override
    public String getID() {
		return "GoLime";
	}

	@Override
    public String getName() {
		return "Lime Look and Feel";
	}

	@Override
    public boolean isNativeLookAndFeel() {
		return false;
	}
	
	@Override
    public boolean isSupportedLookAndFeel() {
		return true;
	}

	/**
	 * Set the ui delegate classes for main component classes.
	 */
	@Override
    protected void initClassDefaults(UIDefaults table) {
		super.initClassDefaults(table);		
		table.put("ButtonUI", PlasticButtonUI.class.getName());
        table.put("ClassLoader", getClass().getClassLoader());
	}

	/**
	 * Set the finer-grained ui settings for stock component ui items.
	 */
	@Override
    protected void initComponentDefaults(UIDefaults table) {
		super.initComponentDefaults(table);
		
		Object[] defaults = LimeLookAndFeel.getComponentDefaults(table);
		table.putDefaults(defaults);
    }
    
    /**
     * Installs the component defaults on the UIManager.
     */
    public static void installUIManagerDefaults() {
        Object[] defaults = getComponentDefaults(null);
        for(int i = 0; i < defaults.length; i += 2)
            UIManager.put(defaults[i], defaults[i+1]);
    }
    
    /**
     * Gets the defaults for components in the lime look & feel.
     */
    private static Object[] getComponentDefaults(UIDefaults table) {

		Icon questionIcon  = GUIMediator.getThemeImage("question");
		Icon errorIcon     = GUIMediator.getThemeImage("warning");	
		Icon infoIcon      = GUIMediator.getThemeImage("still_lime");
		Icon openDirIcon   = GUIMediator.getThemeImage("dir_open");
		Icon closedDirIcon = GUIMediator.getThemeImage("dir_closed");
		
		if(table == null)
		    table = UIManager.getDefaults();
		
		Object labelFont = table.get("Label.font");
		Object buttonFont = table.get("Button.font");
		if(!System.getProperty("os.name").startsWith("Windows")) {
		    labelFont = new FontUIResource("Dialog", Font.PLAIN, 11);
		    buttonFont = labelFont;
        }
        
		
		Object[] defaults = {
		    "Button.font",                    buttonFont,
		    "Button.is3DEnabled",             Boolean.TRUE,

		    "CheckBox.foreground",            getSystemTextColor(),

		    "OptionPane.questionIcon",        questionIcon,
		    "OptionPane.errorIcon",           errorIcon,
		    "OptionPane.informationIcon",     infoIcon,

		    "ProgressBar.selectionForeground",new ColorUIResource(ThemeFileHandler.WINDOW4_COLOR.getValue()),
            "ProgressBar.selectionBackground",new ColorUIResource(ThemeFileHandler.WINDOW4_COLOR.getValue()),

		    "Label.font",                     labelFont,

		    "Table.foreground",               getUserTextColor(),
		    "Table.selectionForeground",      getUserTextColor(),
		    "Table.selectionBackground",      new ColorUIResource(ThemeFileHandler.SECONDARY3_COLOR.getValue()),
		    "Table.focusCellForeground",      getUserTextColor(),
		    "Table.focusCellBackground",      new ColorUIResource(ThemeFileHandler.SECONDARY3_COLOR.getValue()),
		    
		    "TableHeader.background",         new ColorUIResource(ThemeFileHandler.TABLE_HEADER_BACKGROUND_COLOR.getValue()),
		    "TableHeader.cellBorder",         new BorderUIResource(BorderFactory.createRaisedBevelBorder()),
		    "TableHeader.cellPressedBorder",  new BorderUIResource(BorderFactory.createLoweredBevelBorder()),
		    
		    "TextField.background",           new ColorUIResource(ThemeFileHandler.WINDOW7_COLOR.getValue()),
		    
		    "ToolTip.foreground",             getControlTextColor(),
		    "ToolTip.background",             getControl(),
		    
		    "Tree.foreground",                getUserTextColor(),
		    "Tree.expandedIcon",              openDirIcon,
		    "Tree.collapsedIcon",             closedDirIcon,
		    "Tree.rightChildIndent",          new Integer(6),
            "Tree.selectionForeground",       getUserTextColor(),
		    "Tree.selectionBackground",       new ColorUIResource(ThemeFileHandler.SECONDARY3_COLOR.getValue()),
        };
        
        return defaults;
	}
	    
}
