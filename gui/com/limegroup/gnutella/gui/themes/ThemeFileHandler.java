package com.limegroup.gnutella.gui.themes;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.setting.ColorSetting;
import org.limewire.setting.FontNameSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.SettingsFactory;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.util.LimeWireUtils;


/**
 * This class contains key/value pairs for the current "theme."  The
 * theme defines values for the colors, fonts, etc, of the application.
 */
public final class ThemeFileHandler {
    
    private static Log LOG = LogFactory.getLog(ThemeFileHandler.class);

	/**
	 * The current theme version.
	 */
	private static final int CURRENT_VERSION = 2;

	/**
	 * Handle to the <tt>SettingsFactory</tt> for theme settings.
	 */
	private static SettingsFactory FACTORY;	
	
    /**
     * Private constructor to ensure that this class is not constructed.
     */
    private ThemeFileHandler() {}

	static {
		reload(false);
	}

	/**
	 * Reloads the file from disk to read values from.
	 */
	public static void reload(boolean force) {
  		File themeFile = ThemeSettings.THEME_FILE.getValue();
  		if( !ThemeSettings.isThemeFile(themeFile) )
  		    ThemeSettings.THEME_FILE.revertToDefault();
  		
  		themeFile = ThemeSettings.THEME_FILE.getValue();
        File themeDir = ThemeSettings.extractThemeDir(themeFile);
        
        boolean forceNewLWTP = false;
        File themeVersionFile = new File(themeDir, "version.txt");

        // If the theme directory already exists, we might want to still
        // forcibly updated, if 'updateIfVersionOff' is still us the last
        // version that unzipped this theme was an old version.
        if(!force && themeDir.isDirectory()) {
            String contents = contentsOf(themeVersionFile);
            if(contents == null || !contents.equals(LimeWireUtils.getLimeWireVersion())) {
                force = true;
                forceNewLWTP = true;
            }
        }
        
        // if the theme hasn't already been expanded, then it's 
        // probably a new theme, so expand it
        if(force || !themeDir.isDirectory()) {
            // if the theme file didn't exist and could not be extracted
        	// from the themes jar, revert to default.
            // This is done here instead of above because
            // it doesn't matter if the file doesn't exist
            // if we've already expanded it once in the past.
            if( forceNewLWTP || !themeFile.exists() ) {
            	if (!ThemeSettings.JAR_THEME_NAMES.contains(themeFile.getName())) {
            		ThemeSettings.THEME_FILE.revertToDefault();
            		themeFile = ThemeSettings.THEME_FILE.getValue();
            		themeDir = ThemeSettings.extractThemeDir(themeFile);
            	}
            	try {
            	    CommonUtils.copyResourceFile(themeFile.getName(), themeFile, true);
            	} catch(IOException iox) {
            	    LOG.warn("Couldn't extract theme!", iox);
            	}
            }   
            // unpack the zip -- if an error occurred, revert to the default
            // theme & unpack that if necessary.
            if(themeFile.exists() && !ThemeSettings.expandTheme(themeFile, themeDir, force, false)) {
                ThemeSettings.THEME_FILE.revertToDefault();
                themeFile = ThemeSettings.THEME_FILE.getValue();
                themeDir = ThemeSettings.extractThemeDir(themeFile);
                if(!themeDir.isDirectory())
                    ThemeSettings.expandTheme(themeFile, themeDir, false, true);
            }
        }

		final File THEME_PROPS = new File(themeDir, "theme.txt");
		handleFactory(THEME_PROPS);
		ThemeSettings.THEME_DIR.setValue(themeDir);
		
		if(forceNewLWTP) {
		    writeVersion(themeVersionFile);
		}
		
		if(OSUtils.isMacOSX()) {
			File f = new File(CommonUtils.getUserSettingsDir(), "useBrushedMetal");
			if(ThemeSettings.isBrushedMetalTheme()) {
				try {
					FileUtils.touch(f);
				} catch(IOException oops) {}
			} else {
				f.delete();
			}
		}
	}
	
	/** Writes the current version into a file. */
	private static void writeVersion(File file) {
	    FileOutputStream fos = null;
	    try {
	        fos = new FileOutputStream(file);
	        fos.write(LimeWireUtils.getLimeWireVersion().getBytes("UTF8"));
	        fos.flush();
	    } catch(IOException ignored) {
	    } finally {
	        IOUtils.close(fos);
	    }
	}
	

    
    /** Reads a the contents of the first 20 bytes of a file, as a String. */
    private static String contentsOf(File file) {
        if(!file.exists()) {
            return "";
        }
        
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            byte[] bytes = new byte[(int)Math.min(file.length(), 20)];
            raf.readFully(bytes);
            return new String(bytes, "UTF8");
        } catch(IOException iox) {
            return "";
        } finally {
            IOUtils.close(raf);
        }
    }
	
	
	/**
	 * Determines if the current theme is current.
	 */
	public static boolean isCurrent() {
	    return CURRENT_VERSION <= VERSION.getValue();
    }

	/**
	 * Either creates the factory or reloads it as needed.
	 */
	private static void handleFactory(File file) {
		if(FACTORY == null) {
			FACTORY = new SettingsFactory(file);				
		} else {
		    FACTORY.changeFile(file);
		    fixFontSettings();
		}
    }
		
    /**
     * Fixes the font settings.
     */
    private static void fixFontSettings() {
		// Fix some of the font settings to not be small bold & verdana
		checkFontSetting(CONTROL_TEXT_FONT_NAME,
		                 CONTROL_TEXT_FONT_STYLE,
		                 CONTROL_TEXT_FONT_SIZE);
		                 
		checkFontSetting(SYSTEM_TEXT_FONT_NAME,
		                 SYSTEM_TEXT_FONT_STYLE,
		                 SYSTEM_TEXT_FONT_SIZE);
		                 
		checkFontSetting(USER_TEXT_FONT_NAME,
		                 USER_TEXT_FONT_STYLE,
		                 USER_TEXT_FONT_SIZE);
		                 
		checkFontSetting(MENU_TEXT_FONT_NAME,
		                 MENU_TEXT_FONT_STYLE,
		                 MENU_TEXT_FONT_SIZE);
		                 
		checkFontSetting(WINDOW_TITLE_FONT_NAME,
		                 WINDOW_TITLE_FONT_STYLE,
		                 WINDOW_TITLE_FONT_SIZE);
		                 
		checkFontSetting(SUB_TEXT_FONT_NAME,
		                 SUB_TEXT_FONT_STYLE,
		                 SUB_TEXT_FONT_SIZE);
	}
	
	private static void checkFontSetting(FontNameSetting font, 
	                                     IntSetting style,
	                                     IntSetting size) {
        if(font.getValue().toLowerCase(Locale.US).equals("verdana") &&
           style.getValue() == Font.BOLD && size.getValue() == 10) {
            font.setValue("dialog");
            style.setValue(Font.PLAIN);
            size.setValue(11);
        }
    }
           

	/////////////////// FONTS //////////////////////
	/**
	 * Setting for the control text font name.
	 */
	public static final FontNameSetting CONTROL_TEXT_FONT_NAME =
		FACTORY.createFontNameSetting("CONTROL_TEXT_FONT_NAME", "dialog");

	/**
	 * Setting for the control text font style.
	 */
	public static final IntSetting CONTROL_TEXT_FONT_STYLE =
		FACTORY.createIntSetting("CONTROL_TEXT_FONT_STYLE", 1);

	/**
	 * Setting for the control text font size.
	 */
	public static final IntSetting CONTROL_TEXT_FONT_SIZE =
		FACTORY.createIntSetting("CONTROL_TEXT_FONT_SIZE", 11);

	/**
	 * Setting for the system text font name.
	 */
	public static final FontNameSetting SYSTEM_TEXT_FONT_NAME =
		FACTORY.createFontNameSetting("SYSTEM_TEXT_FONT_NAME", "dialog");

	/**
	 * Setting for the system text font style.
	 */
	public static final IntSetting SYSTEM_TEXT_FONT_STYLE =
		FACTORY.createIntSetting("SYSTEM_TEXT_FONT_STYLE", 0);

	/**
	 * Setting for the system text font size.
	 */
	public static final IntSetting SYSTEM_TEXT_FONT_SIZE =
		FACTORY.createIntSetting("SYSTEM_TEXT_FONT_SIZE", 11);	

	/**
	 * Setting for the user text font name.
	 */
	public static final FontNameSetting USER_TEXT_FONT_NAME =
		FACTORY.createFontNameSetting("USER_TEXT_FONT_NAME", "dialog");

	/**
	 * Setting for the user text font style.
	 */
	public static final IntSetting USER_TEXT_FONT_STYLE =
		FACTORY.createIntSetting("USER_TEXT_FONT_STYLE", 0);

	/**
	 * Setting for the user text font size.
	 */
	public static final IntSetting USER_TEXT_FONT_SIZE =
		FACTORY.createIntSetting("USER_TEXT_FONT_SIZE", 11);	

	/**
	 * Setting for the menu text font name.
	 */
	public static final FontNameSetting MENU_TEXT_FONT_NAME =
		FACTORY.createFontNameSetting("MENU_TEXT_FONT_NAME", "dialog");

	/**
	 * Setting for the menu text font style.
	 */
	public static final IntSetting MENU_TEXT_FONT_STYLE =
		FACTORY.createIntSetting("MENU_TEXT_FONT_STYLE", 1);

	/**
	 * Setting for the menu text font size.
	 */
	public static final IntSetting MENU_TEXT_FONT_SIZE =
		FACTORY.createIntSetting("MENU_TEXT_FONT_SIZE", 11);	

	/**
	 * Setting for the window title font name.
	 */
	public static final FontNameSetting WINDOW_TITLE_FONT_NAME =
		FACTORY.createFontNameSetting("WINDOW_TITLE_FONT_NAME", "dialog");

	/**
	 * Setting for the window title font style.
	 */
	public static final IntSetting WINDOW_TITLE_FONT_STYLE =
		FACTORY.createIntSetting("WINDOW_TITLE_FONT_STYLE", 1);

	/**
	 * Setting for the window title font size.
	 */
	public static final IntSetting WINDOW_TITLE_FONT_SIZE =
		FACTORY.createIntSetting("WINDOW_TITLE_FONT_SIZE", 11);	

	/**
	 * Setting for the sub text font name.
	 */
	public static final FontNameSetting SUB_TEXT_FONT_NAME =
		FACTORY.createFontNameSetting("SUB_TEXT_FONT_NAME", "dialog");

	/**
	 * Setting for the sub text font style.
	 */
	public static final IntSetting SUB_TEXT_FONT_STYLE =
		FACTORY.createIntSetting("SUB_TEXT_FONT_STYLE", 0);

	/**
	 * Setting for the sub text font size.
	 */
	public static final IntSetting SUB_TEXT_FONT_SIZE =
		FACTORY.createIntSetting("SUB_TEXT_FONT_SIZE", 10);

	/////////////////// END FONTS //////////////////////

	/**
	 * Setting for the primary 1 Color.
	 */
	public static final ColorSetting PRIMARY1_COLOR = 
		FACTORY.createColorSetting("PRIMARY1_COLOR", 
								   new Color(74,110,188));

	/**
	 * Setting for the primary 2 Color.
	 */
	public static final ColorSetting PRIMARY2_COLOR = 
		FACTORY.createColorSetting("PRIMARY2_COLOR", 
								   new Color(135,145,170));

	/**
	 * Setting for the primary 3 Color.
	 */
	public static final ColorSetting PRIMARY3_COLOR = 
		FACTORY.createColorSetting("PRIMARY3_COLOR", 
								   new Color(216,225,244));

	/**
	 * Setting for the secondary 1 Color.
	 */
	public static final ColorSetting SECONDARY1_COLOR =
		FACTORY.createColorSetting("SECONDARY1_COLOR", 
								   new Color(50,68,107));

	/**
	 * Setting for the secondary 2 Color.
	 */
	public static final ColorSetting SECONDARY2_COLOR =
		FACTORY.createColorSetting("SECONDARY2_COLOR", 
								   new Color(167,173,190));

	/**
	 * Setting for the secondary 3 Color.
	 */
	public static final ColorSetting SECONDARY3_COLOR =
		FACTORY.createColorSetting("SECONDARY3_COLOR", 
								   new Color(199,201,209));

	/**
	 * Setting for the window 1 Color.
	 */
	public static final ColorSetting WINDOW1_COLOR =
		FACTORY.createColorSetting("WINDOW1_COLOR", 
								   new Color(0,0,0));

	/**
	 * Setting for the window 2 Color.
	 */
	public static final ColorSetting WINDOW2_COLOR =
		FACTORY.createColorSetting("WINDOW2_COLOR", 
								   new Color(199,201,209));

	/**
	 * Setting for the window 3 Color.
	 */
	public static final ColorSetting WINDOW3_COLOR =
		FACTORY.createColorSetting("WINDOW3_COLOR", 
								   new Color(199,201,209));

	/**
	 * Setting for the window 4 Color.
	 */
	public static final ColorSetting WINDOW4_COLOR =
		FACTORY.createColorSetting("WINDOW4_COLOR", 
								   new Color(0,0,0));

	/**
	 * Setting for the window 5 Color.
	 */
	public static final ColorSetting WINDOW5_COLOR =
		FACTORY.createColorSetting("WINDOW5_COLOR", 
								   new Color(0,0,0));

	/**
	 * Setting for the window 6 Color.
	 */
	public static final ColorSetting WINDOW6_COLOR =
		FACTORY.createColorSetting("WINDOW6_COLOR", 
								   new Color(255,255,255));

	/**
	 * Setting for the window 7 Color.
	 */
	public static final ColorSetting WINDOW7_COLOR =
		FACTORY.createColorSetting("WINDOW7_COLOR", 
								   new Color(255,255,255));

	/**
	 * Setting for the window 8 Color.
	 */
	public static final ColorSetting WINDOW8_COLOR =
		FACTORY.createColorSetting("WINDOW8_COLOR", 
								   new Color(0,0,0));

	/**
	 * Setting for the window 9 Color.
	 */
	public static final ColorSetting WINDOW9_COLOR =
		FACTORY.createColorSetting("WINDOW9_COLOR", 
								   new Color(0,0,0));	

	/**
	 * Setting for the window 10 Color.
	 */
	public static final ColorSetting WINDOW10_COLOR =
		FACTORY.createColorSetting("WINDOW10_COLOR", 
								   new Color(0,0,0));

	/**
	 * Setting for the window 11 Color.
	 */
	public static final ColorSetting WINDOW11_COLOR =
		FACTORY.createColorSetting("WINDOW11_COLOR", 
								   new Color(0,0,0));

	/**
	 * Setting for the window 12 Color.
	 */
	public static final ColorSetting WINDOW12_COLOR =
		FACTORY.createColorSetting("WINDOW12_COLOR", 
								   new Color(199,201,209));							

	/**
	 * Setting for the table header background Color.
	 */
	public static final ColorSetting TABLE_HEADER_BACKGROUND_COLOR =
		FACTORY.createColorSetting("TABLE_HEADER_BACKGROUND_COLOR",
								   new Color(117, 142, 197));

	/**
	 * Setting for the table odd row Color Color.
	 */
	public static final ColorSetting TABLE_BACKGROUND_COLOR =
		FACTORY.createColorSetting("TABLE_BACKGROUND_COLOR", 
								   new Color(255,255,255));
								   
    /**
     * Setting for the table even row Color.
     */
    public static final ColorSetting TABLE_ALTERNATE_COLOR =
        FACTORY.createColorSetting("TABLE_ALTERNATE_COLOR",
                                    new Color(248, 248, 255));

	/**
     * Setting for the table odd row Color Color for special search results.
     */
    public static final ColorSetting TABLE_SPECIAL_BACKGROUND_COLOR =
        FACTORY.createColorSetting("TABLE_SPECIAL_BACKGROUND_COLOR", 
                                    new Color(255,243,193));
                                   
    /**
     * Setting for the table even row Color for special search results.
     */
    public static final ColorSetting TABLE_SPECIAL_ALTERNATE_COLOR =
        FACTORY.createColorSetting("TABLE_SPECIAL_ALTERNATE_COLOR",
                                    new Color(255,222,102));    

	/**
	 * Setting for the not sharing label Color.
	 */
	public static final ColorSetting NOT_SHARING_LABEL_COLOR =
		FACTORY.createColorSetting("NOT_SHARING_LABEL_COLOR", 
								   new Color(208, 0, 5));

	/**
	 * Setting for the search result speed Color.
	 */
	public static final ColorSetting SEARCH_RESULT_SPEED_COLOR =
		FACTORY.createColorSetting("SEARCH_RESULT_SPEED_COLOR", 
								   new Color(7,170,0));


	/**
	 * Setting for the search result speed Color.
	 */
	public static final ColorSetting SEARCH_SPAM_RESULT_COLOR =
		FACTORY.createColorSetting("SEARCH_SPAM_RESAULT_COLOR", 
								   new Color(255,125,125));

	/**
	 * Setting for the playlist "playing song" Color.
	 */
	public static final ColorSetting PLAYING_SONG_COLOR =
		FACTORY.createColorSetting("PLAYING_SONG_COLOR", 
								   new Color(7,170,0));
	/**
	 * Setting for the search ip address Color.
	 */
	public static final ColorSetting SEARCH_IP_COLOR =
		FACTORY.createColorSetting("SEARCH_IP_COLOR", 
								   new Color(0,0,0));

	/**
	 * Setting for the search ip private address Color.
	 */
	public static final ColorSetting SEARCH_PRIVATE_IP_COLOR =
		FACTORY.createColorSetting("SEARCH_PRIVATE_IP_COLOR", 
								   new Color(255, 0, 0));
    
    /**
     * Setting for the search ip selected private address Color.
     */
    public static final ColorSetting SEARCH_SELECTED_PRIVATE_IP_COLOR =
        FACTORY.createColorSetting("SEARCH_SELECTED_PRIVATE_IP_COLOR",
                                    SEARCH_PRIVATE_IP_COLOR.getValue());

    /**
     * Setting for the top of the filter title color.
     */
    public static final ColorSetting FILTER_TITLE_TOP_COLOR =
        FACTORY.createColorSetting("SEARCH_FILTER_TITLE_TOP_COLOR",
                            TABLE_HEADER_BACKGROUND_COLOR.getValue());

    /**
     * Setting for the filter title color.
     */
    public static final ColorSetting FILTER_TITLE_COLOR =
        FACTORY.createColorSetting("SEARCH_FILTER_TITLE_COLOR",
                            TABLE_HEADER_BACKGROUND_COLOR.getValue());
                                   
    /**
     * Setting for the background grid color.
     */
    public static final ColorSetting SEARCH_GRID_COLOR = 
        FACTORY.createColorSetting("SEARCH_GRID_COLOR",
                            new Color(0, 0, 0));
                            
    /**
     * Setting for the top search panel background color.
     */
    public static final ColorSetting SEARCH_PANEL_BG_1 =
        FACTORY.createColorSetting("SEARCH_PANEL_BG_1",
            TABLE_HEADER_BACKGROUND_COLOR.getValue());
            
    /**
     * Setting for the bottom search panel background color.
     */
    public static final ColorSetting SEARCH_PANEL_BG_2 =
        FACTORY.createColorSetting("SEARCH_PANEL_BG_2",
            TABLE_HEADER_BACKGROUND_COLOR.getValue());
            
    /**
     * The current theme version.
     */
    public static final IntSetting VERSION = 
        FACTORY.createIntSetting("THEME_VERSION", 0);
        
    static {
        fixFontSettings();
    }

	/*
	public static void main(String[] args) {
		ThemeSettings.reload();
	}	
	*/
}
