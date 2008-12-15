package com.limegroup.gnutella.gui;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.util.OSUtils;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.gui.notify.NotifyUserProxy;
import com.limegroup.gnutella.gui.themes.LimeLookAndFeel;
import com.limegroup.gnutella.gui.themes.LimePlasticTheme;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.themes.ThemeSettings;

/**
 * Manages application resources, including the custom <tt>LookAndFeel</tt>,
 * the locale-specific <tt>String</tt> instances, and any <tt>Icon</tt>
 * instances needed by the application.
 */
// 2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class ResourceManager {

    /**
     * Instance of this <tt>ResourceManager</tt>, following singleton.
     */
    private static ResourceManager _instance;

    /**
     * Constant for the relative path of the gui directory.
     */
    private static final String GUI_PATH = "org/limewire/gui/";

    /**
     * Constant for the relative path of the resources directory.
     */
    private static final String RESOURCES_PATH = GUI_PATH + "resources/";

    /**
     * Constant for the relative path of the images directory.
     */
    private static final String IMAGES_PATH = GUI_PATH + "images/";

    /**
     * Boolean status that controls whever the shared <tt>Locale</tt> instance
     * needs to be loaded, and locale-specific options need to be setup.
     */
    private static boolean _localeOptionsSet;

    /**
     * Static variable for the loaded <tt>Locale</tt> instance.
     */
    private static Locale _locale;

    /**
     * Boolean for whether or not the font-size has been reduced.
     */
    private static boolean _fontReduced = false;

    /**
     * The default MetalTheme.
     */
    private static MetalTheme _defaultTheme = null;

    /**
     * Whether or not LimeWire was started in the 'brushed metal' look.
     */
    private final boolean BRUSHED_METAL;

    /**
     * Whether or not the system tray was able to load.
     */
    private final boolean LOADED_TRAY_LIBRARY;

    /**
     * Whether or not the jdic associations were able to load.
     */
    private final boolean LOADED_JDIC_LIBRARY;

    /** Cache of theme images (name as String -> image as ImageIcon) */
    private static final Map<String, ImageIcon> THEME_IMAGES = new HashMap<String, ImageIcon>();

    
    /** Marked true in the event of an error in the load/save of any settings file */ 
    private static boolean loadFailureEncountered = false;   
    
    /**
     * Statically initialize necessary resources.
     */
    static {
        resetLocaleOptions();
    }

    static void resetLocaleOptions() {
        _localeOptionsSet = false;
        setLocaleOptions();
    }

    static void setLocaleOptions() {
        if (!_localeOptionsSet) {
            if (ApplicationSettings.LANGUAGE.getValue().equals(""))
                ApplicationSettings.LANGUAGE.setValue("en");
            _locale = new Locale(ApplicationSettings.LANGUAGE.getValue(),
                    ApplicationSettings.COUNTRY.getValue(),
                    ApplicationSettings.LOCALE_VARIANT.getValue());
            StringUtils.setLocale(_locale);
            I18n.setLocale(_locale);
            _localeOptionsSet = true;
        }
    }

    /**
     * Returns the <tt>Locale</tt> instance currently in use.
     * 
     * @return the <tt>Locale</tt> instance currently in use
     */
    static Locale getLocale() {
        return _locale;
    }

    
    /**
     * Indicated if a failure has occurred for delayed reporting 
     */
    public static boolean hasLoadFailure() {
        return loadFailureEncountered;
    }
    
    /**
     * Resets the failure flag 
     */
    public static void resetLoadFailure() {
        loadFailureEncountered = false;
    }
    
    
    
    /**
     * Returns the XML resource bundle for the given schema.
     * 
     * @param String schema name (not the URI but name returned by
     *        LimeXMLSchema.getDisplayString)
     * @return ResourceBundle
     */
    static ResourceBundle getXMLResourceBundle(String name) {
        return ResourceBundle.getBundle("xml.display." + name, _locale);
    }

    /**
     * Serves as a single point of access for any icons that should be accessed
     * directly from the file system for themes.
     * 
     * @param name The name of the image (excluding the extension) to locate.
     * @return a new <tt>ImageIcon</tt> instance for the specified file, or
     *         <tt>null</tt> if the resource could not be loaded
     */
    static final ImageIcon getThemeImage(final String name) {
        if (name == null)
            throw new NullPointerException("null image name");

        ImageIcon icon = null;

        // First try to get theme image from cache
        icon = THEME_IMAGES.get(name);
        if (icon != null)
            return icon;

        File themeDir = ThemeSettings.THEME_DIR.getValue();

        // Next try to get from themes.
        icon = getImageFromURL(new File(themeDir, name).getPath(), true);
        if (icon != null && icon.getImage() != null) {
            THEME_IMAGES.put(name, icon);
            return icon;
        }

        // Then try to get from org/limewire/gui/images resources
        icon = getImageFromURL(IMAGES_PATH + name, false);
        if (icon != null && icon.getImage() != null) {
            THEME_IMAGES.put(name, icon);
            return icon;
        }

        // no resource? error.
        throw new MissingResourceException(
                "image: " + name + " doesn't exist.", null, null);

    }

    /**
     * Retrieves an icon from the specified path in the filesystem.
     */
    static final ImageIcon getImageFromResourcePath(String loc) {
        return getImageFromURL(loc, false);
    }

    /**
     * Retrieves an icon from a URL-style path.
     * 
     * If 'file' is true, location is treated as a file, otherwise it is treated
     * as a resource.
     * 
     * This tries, in order, the exact location, the location as a png, and the
     * location as a gif.
     */
    private static final ImageIcon getImageFromURL(String location, boolean file) {
        // try exact filename first.
        URL img = toURL(location, file);
        if (img != null)
            return new ImageIcon(img);

        // try with png second
        img = toURL(location + ".png", file);
        if (img != null)
            return new ImageIcon(img);

        // try with gif third
        img = toURL(location + ".gif", file);
        if (img != null)
            return new ImageIcon(img);

        return null;
    }

    /**
     * Makes a URL out of a location, as either a file or a resource.
     */
    @SuppressWarnings("deprecation")
    private static final URL toURL(String location, boolean file) {
        if (file) {
            File f = new File(location);
            if (f.exists()) {
                try {
                    return f.toURL();
                } catch (MalformedURLException murl) {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return getURL(location);
        }
    }

    /**
     * Returns a new <tt>URL</tt> instance for the specified file in the
     * "resources" directory.
     * 
     * @param FILE_NAME the name of the resource file
     * @return a new <tt>URL</tt> instance for the desired file, or
     *         <tt>null</tt> if the <tt>URL</tt> could not be loaded
     */
    static URL getURLResource(final String FILE_NAME) {
        return ResourceManager.getURL(RESOURCES_PATH + FILE_NAME);
    }

    /**
     * Returns a new <tt>URL</tt> instance for the resource at the specified
     * local path. The path should be the full path within the jar file, such
     * as:
     * <p>
     * 
     * org/limewire/gui/images/searching.gif
     * <p>
     * 
     * @param PATH the path to the resource file within the jar
     * @return a new <tt>URL</tt> instance for the desired file, or
     *         <tt>null</tt> if the <tt>URL</tt> could not be loaded
     */
    private static URL getURL(final String PATH) {
        ClassLoader cl = ResourceManager.class.getClassLoader();
        if (cl == null) {
            return ClassLoader.getSystemResource(PATH);
        }
        URL url = cl.getResource(PATH);
        if (url == null) {
            return ClassLoader.getSystemResource(PATH);
        }
        return url;
    }

    /**
     * Instance accessor following singleton.
     */
    public static synchronized final ResourceManager instance() {
        if (_instance == null)
            _instance = new ResourceManager();
        return _instance;
    }

    /**
     * Private constructor to ensure that a <tt>ResourceManager</tt> cannot be
     * constructed from outside this class.
     */
    private ResourceManager() {
        if (!ThemeFileHandler.isCurrent() || !ThemeSettings.isValid()) {
            ThemeSettings.THEME_FILE.revertToDefault();
            ThemeSettings.THEME_DIR.revertToDefault();
            ThemeFileHandler.reload(false);
        }

        String bMetal = System.getProperty("apple.awt.brushMetalLook");
        BRUSHED_METAL = bMetal != null && bMetal.equalsIgnoreCase("true");

        themeChanged();
        ResourceManager.setFontSizes(ThemeSettings.FONT_SIZE_INCREMENT.getValue());
        try {
            validateLocaleAndFonts(Locale.getDefault());
        } catch (NullPointerException npe) {
            // ignore, can't do much about it -- internal ignorable error.
        }

        if (OSUtils.isWindows() || OSUtils.isLinux()) {
            boolean loaded = false;
            try {
                System.loadLibrary("tray");
                loaded = true;
            } catch (UnsatisfiedLinkError ule) {
            }
            LOADED_TRAY_LIBRARY = loaded;
            loaded = false;
            try {
                System.loadLibrary("jdic");
                loaded = true;
            } catch (UnsatisfiedLinkError ule) {
            }
            LOADED_JDIC_LIBRARY = loaded;
        } else {
            LOADED_TRAY_LIBRARY = false;
            LOADED_JDIC_LIBRARY = false;
        }
    }

    /**
     * Validates the locale, determining if the current locale's resources can
     * be displayed using the current fonts. If not, then the locale is reset to
     * English.
     * 
     * This prevents the UI from appearing as all boxes.
     */
    public static void validateLocaleAndFonts(Locale locale) {
        // OSX can always display everything, and if it can't,
        // we have no way of correcting things 'cause canDisplayUpTo
        // is broken on it.
        if (OSUtils.isMacOSX())
            return;

        String s = locale.getDisplayName();
        if (!checkUIFonts("dialog", s)) {
            // if it couldn't display, revert the locale to english.
            ApplicationSettings.LANGUAGE.setValue("en");
            ApplicationSettings.COUNTRY.setValue("");
            ApplicationSettings.LOCALE_VARIANT.setValue("");
            GUIMediator.resetLocale();
        }

        // Ensure that the Table.font can always display intl characters
        // since we can always get i18n stuff there, but only if we'd actually
        // be capable of displaying an intl character with the font...
        // unicode string == country name of simplified chinese
        String i18n = "\u4e2d\u56fd";
        checkFont("TextField.font", "dialog", i18n, true);
        checkFont("Table.font", "dialog", i18n, true);
        checkFont("ProgressBar.font", "dialog", i18n, true);
        checkFont("TabbedPane.font", "dialog", i18n, true);
    }

    /**
     * Alters all Fonts in UIManager to use Dialog, to correctly display foreign
     * strings.
     */
    private static boolean checkUIFonts(String newFont, String testString) {
        String[] comps = new String[] { "TextField.font", "PasswordField.font",
                "TextArea.font", "TextPane.font", "EditorPane.font",
                "FormattedTextField.font", "Button.font", "CheckBox.font",
                "RadioButton.font", "ToggleButton.font", "ProgressBar.font",
                "ComboBox.font", "InternalFrame.titleFont", "DesktopIcon.font",
                "TitledBorder.font", "Label.font", "List.font",
                "TabbedPane.font", "Table.font", "TableHeader.font",
                "MenuBar.font", "Menu.font", "Menu.acceleratorFont",
                "MenuItem.font", "MenuItem.acceleratorFont", "PopupMenu.font",
                "CheckBoxMenuItem.font", "CheckBoxMenuItem.acceleratorFont",
                "RadioButtonMenuItem.font",
                "RadioButtonMenuItem.acceleratorFont", "Spinner.font",
                "Tree.font", "ToolBar.font", "OptionPane.messageFont",
                "OptionPane.buttonFont", "ToolTip.font", };

        boolean displayable = false;
        for (int i = 0; i < comps.length; i++)
            displayable |= checkFont(comps[i], newFont, testString, false);

        // Then do it the automagic way.
        // note that this could work all the time (without requiring the above)
        // if Java 1.4 didn't introduce Locales, and it could even still work
        // if they offered a way to get all the keys of possible resources.
        for (Map.Entry<Object, Object> next : UIManager.getDefaults().entrySet()) {
            if (next.getValue() instanceof Font) {
                Font f = (Font) next.getValue();
                if (f != null && !newFont.equalsIgnoreCase(f.getName())) {
                    if (!GUIUtils.canDisplay(f, testString)) {
                        f = new Font(newFont, f.getStyle(), f.getSize());
                        if (GUIUtils.canDisplay(f, testString)) {
                            next.setValue(f);
                            displayable = true;
                        }
                    }
                }
            }
        }

        return displayable;
    }

    /**
     * Updates the font of a given fontName to be newName.
     */
    private static boolean checkFont(String fontName, String newName,
            String testString, boolean force) {
        boolean displayable = true;
        Font f = UIManager.getFont(fontName);
        if (f != null && !newName.equalsIgnoreCase(f.getName())) {
            if (!GUIUtils.canDisplay(f, testString) || force) {
                f = new Font(newName, f.getStyle(), f.getSize());
                if (GUIUtils.canDisplay(f, testString))
                    UIManager.put(fontName, f);
                else
                    displayable = false;
            }
        } else if (f != null) {
            displayable = GUIUtils.canDisplay(f, testString);
        } else {
            displayable = false;
        }
        return displayable;
    }

    /**
     * Determines if the tray library has loaded.
     */
    public boolean isTrayLibraryLoaded() {
        return LOADED_TRAY_LIBRARY;
    }
    
    /** Determines if a system tray icon is available. */
    public boolean isTrayIconAvailable() {
        return isTrayLibraryLoaded() && NotifyUserProxy.instance().supportsSystemTray();
    }

    /**
     * Determines if the jdic library has loaded.
     */
    public boolean isJdicLibraryLoaded() {
        return LOADED_JDIC_LIBRARY;
    }

    /**
     * Determines if the brushed metal property is set.
     */
    public boolean isBrushedMetalSet() {
        return BRUSHED_METAL;
    }

    /**
     * Updates to the current theme.
     */
    public void themeChanged() {
        THEME_IMAGES.clear();
        try {
            if (ThemeSettings.isOtherTheme()) {
                // in case this is using metal ...
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                if (_defaultTheme != null)
                    MetalLookAndFeel.setCurrentTheme(_defaultTheme);

                String other = ThemeSettings.getOtherLF();
                UIManager.setLookAndFeel(other);
            } else if (ThemeSettings.isNativeTheme()) {
                if (OSUtils.isWindows() && isPlasticWindowsAvailable()) {
                    try {
                        UIManager.setLookAndFeel("com.jgoodies.plaf.windows.ExtWindowsLookAndFeel");
                    } catch (NullPointerException npe) {
                        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    }
                } else
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

                if (OSUtils.isMacOSX()) {
                    if (!_fontReduced) {
                        _fontReduced = true;
                        reduceFont("Label.font");
                        reduceFont("Table.font");
                    }

                    UIManager.put("List.focusCellHighlightBorder",
                            BorderFactory.createEmptyBorder(1, 1, 1, 1));
                    UIManager.put("ScrollPane.border",
                            BorderFactory.createMatteBorder(1, 1, 1, 1, Color.lightGray));
                }

                // HACK: On Windows sometimes, JFileChooser can't
                // show because some icons throw an AIOOBE
                // when they're retrieved. To workaround this,
                // we install our own version of those icons.
                if (OSUtils.isWindows()) {
                    replaceIconIfFailing("FileChooser.upFolderIcon",
                            "upFolderIconVistaFix");
                    replaceIconIfFailing("FileChooser.detailsViewIcon",
                            "detailsViewIconVistaFix");
                    replaceIconIfFailing("FileChooser.listViewIcon",
                            "listViewIconVistaFix");
                    replaceIconIfFailing("FileChooser.newFolderIcon",
                            "newFolderIconVistaFix");
                }
            } else {
                if (isPlasticAvailable()) {
                    if (_defaultTheme == null)
                        _defaultTheme = getDefaultTheme();

                    LimePlasticTheme.installThisTheme();
                    UIManager
                            .setLookAndFeel("com.jgoodies.plaf.plastic.PlasticXPLookAndFeel");
                    LimeLookAndFeel.installUIManagerDefaults();
                } else {
                    UIManager.setLookAndFeel(new LimeLookAndFeel());
                }
            }

            UIManager
                    .put("Tree.leafIcon", UIManager.getIcon("Tree.closedIcon"));

            // remove split pane borders
            UIManager
                    .put("SplitPane.border", BorderFactory.createEmptyBorder());

            if (!OSUtils.isMacOSX())
                UIManager.put("Table.focusRowHighlightBorder", UIManager
                        .get("Table.focusCellHighlightBorder"));

            UIManager.put("Table.focusCellHighlightBorder", BorderFactory
                    .createEmptyBorder(1, 1, 1, 1));

            // Add a bolded text version of simple text.
            Font normal = UIManager.getFont("Table.font");
            FontUIResource bold = new FontUIResource(normal.getName(),
                    Font.BOLD, normal.getSize());
            UIManager.put("Table.font.bold", bold);
            UIManager.put("Tree.rowHeight", 0);

        } catch (UnsupportedLookAndFeelException e) {
            throw new ExceptionInInitializerError(e);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        } catch (InstantiationException e) {
            throw new ExceptionInInitializerError(e);
        } catch (IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
        
        wrapDesktopProperties();
    }
    
    private void wrapDesktopProperties() {
        if(!OSUtils.isWindows())
            return;
        
        Class<?> desktopProperty;
        // See if we can get a handle to the 'DesktopProperty' class.
        try {
            desktopProperty = Class.forName("com.sun.java.swing.plaf.windows.DesktopProperty");
        } catch(ClassNotFoundException cnfe) {
            return;
        }
        
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        synchronized(defaults) {
            for(Iterator<Map.Entry<Object, Object>> i = defaults.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<Object, Object> next = i.next();
                Object value = next.getValue();
                if(next.getValue() instanceof UIDefaults.ActiveValue) {
                    UIDefaults.ActiveValue activeValue = (UIDefaults.ActiveValue)value;
                    if(value != null && desktopProperty.isAssignableFrom(value.getClass())) {
                        next.setValue(new ActiveDesktopProperty(activeValue));
                    }
                }
            }
        }
    }
    
    /**
     * Replaces an icon resource in UIManager with a different resource if the
     * original resource can't be retrieved correctly.
     * 
     * @param resource
     * @param replacementName
     */
    private void replaceIconIfFailing(String resource, String replacementName) {
        try {
            UIManager.getIcon(resource);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            UIManager.put(resource, getThemeImage(replacementName));
        }
    }

    /**
     * Gets the current theme (or defaults to MetalTheme) if possible.
     */
    private MetalTheme getDefaultTheme() {
        MetalTheme theme = null;
        try {
            theme = (MetalTheme) MetalLookAndFeel.class.getMethod(
                    "getCurrentTheme", (Class[]) null).invoke(null,
                    (Object[]) null);
        } catch (IllegalAccessException iae) {
        } catch (InvocationTargetException ite) {
        } catch (NoSuchMethodException nsme) {
        }

        if (theme == null)
            theme = new DefaultMetalTheme();

        return theme;
    }

    /**
     * Determines if the PlasticXP Theme is available.
     */
    private boolean isPlasticAvailable() {
        try {
            Class plastic = Class
                    .forName("com.jgoodies.plaf.plastic.PlasticXPLookAndFeel");
            return plastic != null;
        } catch (ClassNotFoundException cnfe) {
            return false;
        }
    }

    /**
     * Determines if the Plastic Windows Theme is available.
     */
    private boolean isPlasticWindowsAvailable() {
        try {
            Class plastic = Class
                    .forName("com.jgoodies.plaf.windows.ExtWindowsLookAndFeel");
            return plastic != null;
        } catch (ClassNotFoundException cnfe) {
            return false;
        }
    }

    /**
     * Updates the component to use the native UI resource.
     */
    static ComponentUI getNativeUI(JComponent c) {
        ComponentUI ret = null;
        String name = UIManager.getSystemLookAndFeelClassName();
        if (name != null) {
            try {
                Class clazz = Class.forName(name);
                LookAndFeel lf = (LookAndFeel) clazz.newInstance();
                lf.initialize();
                UIDefaults def = lf.getDefaults();
                ret = def.getUI(c);
            } catch (ExceptionInInitializerError e) {
            } catch (ClassNotFoundException e) {
            } catch (LinkageError e) {
            } catch (IllegalAccessException e) {
            } catch (InstantiationException e) {
            } catch (SecurityException e) {
            } catch (ClassCastException e) {
            }
        }

        // if any of those failed, default to the current UI.
        if (ret == null)
            ret = UIManager.getUI(c);

        return ret;
    }

    /**
     * Reduces the size of a font in UIManager.
     */
    private static void reduceFont(String name) {
        Font oldFont = UIManager.getFont(name);
        FontUIResource newFont = new FontUIResource(oldFont.getName(), oldFont
                .getStyle(), oldFont.getSize() - 2);
        UIManager.put(name, newFont);
    }

    public static void setFontSizes(float fontSizeIncrement) {
        if (fontSizeIncrement == 0f) {
            return;
        }
        
        Map<Font, Font> newFontByOldFont = new HashMap<Font, Font>();
        String[] fontKeys = new String[] {
                "Button.font",
                "CheckBox.font",
                "ComboBox.font",
                "DesktopIcon.font",
                "Label.font",
                "List.font",
                "ProgressBar.font",
                "RadioButton.font",
                "Slider.font",
                "Spinner.font",
                "TabbedPane.font",
                "TitledBorder.font",
                "ToggleButton.font",
                "CheckBoxMenuItem.font",
                "Menu.font",
                "MenuBar.font",
                "MenuItem.font",
                "PopupMenu.font",
                "RadioButtonMenuItem.font",
                "ToolBar.font",
                "ToolTip.font",
                "EditorPane.font",
                "FormattedTextField.font",
                "PasswordField.font",
                "Table.font",
                "TableHeader.font",
                "TextArea.font",
                "TextField.font",
                "TextPane.font",
                "Tree.font",
                "InternalFrame.titleFont",
        };
        List<Object> props = new ArrayList<Object>(fontKeys.length * 2);
        for (String fontKey : fontKeys) {
            Font font = UIManager.getFont(fontKey);
            Font newFont = newFontByOldFont.get(font);
            if (newFont == null) {
                if (font == null) {
                    continue;
                }
                newFont = new FontUIResource(font.deriveFont(font.getSize() + fontSizeIncrement));
                newFontByOldFont.put(font, newFont);
            }
            props.add(fontKey);
            props.add(newFont);
        }
        UIManager.getDefaults().putDefaults(props.toArray());
    }

    /** Wraps an ActiveValue to ensure creating the value is done in the Swing thread. */
    private static class ActiveDesktopProperty implements UIDefaults.ActiveValue, Runnable {
        private final UIDefaults.ActiveValue delegate;
        private volatile UIDefaults table;
        private volatile Object result;
        
        ActiveDesktopProperty(UIDefaults.ActiveValue value) {
            this.delegate = value;
        }
            
        public Object createValue(UIDefaults table) {
            if(SwingUtilities.isEventDispatchThread()) {
                return delegate.createValue(table);
            } else {
                this.table = table;
                // Try to run it in the Swing thread,
                // but if we can't, load the value directly
                // (and pray).
                try {
                    SwingUtilities.invokeAndWait(this);
                } catch (InterruptedException e) {
                    return delegate.createValue(table);
                } catch (InvocationTargetException e) {
                    return delegate.createValue(table);
                }
                return result;
            }
        }
        
        public void run() {
            this.result = delegate.createValue(table);
        }
    }
    
}
