package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Action;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.Resource;
import org.jdesktop.application.SessionStorage;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.core.api.Application;
import org.limewire.core.impl.MockModule;
import org.limewire.inject.Modules;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.LimeWireSwingUiModule;
import org.limewire.ui.swing.browser.LimeMozillaInitializer;
import org.limewire.ui.swing.components.LimeJFrame;
import org.limewire.ui.swing.event.AboutDisplayEvent;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.ExitApplicationEvent;
import org.limewire.ui.swing.event.OptionsDisplayEvent;
import org.limewire.ui.swing.event.RestoreViewEvent;
import org.limewire.ui.swing.menu.LimeMenuBar;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.shell.ShellAssociationManager;
import org.limewire.ui.swing.tray.TrayExitListener;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.SaveDirectoryHandler;
import org.limewire.ui.swing.wizard.SetupWizard;
import org.limewire.util.OSUtils;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.MozillaInitialization.InitStatus;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Stage;

/**
 * The entry point for the Swing UI.  If the real core is desired,
 * start from integrated-ui/../Main.  The main method in this class
 * uses the mock-core.
 */
public class AppFrame extends SingleFrameApplication {
    private static final Log LOG = LogFactory.getLog(AppFrame.class);    
    public static final String STARTUP = "startup";
    private boolean isStartup = false;

    @Inject private static volatile Injector injector;

    private static volatile boolean started;

    /** Default background color for panels */
    @Resource private Color bgColor;
    @Resource private Color glassPaneColor;
    
    // Icons for JFileChooser bug workaround on Vista. 
    @Resource private Icon upFolderVistaFixIcon;
    @Resource private Icon detailsViewVistaFixIcon;
    @Resource private Icon listViewVistaFixIcon;
    @Resource private Icon newFolderVistaFixIcon;
    
    @Inject private Application application;
    @Inject private LimeWireSwingUI ui;
    @Inject private SetupWizard setupWizard;
    @Inject private Provider<OptionsDialog> options;
    @Inject private FramePositioner framePositioner;
    @Inject private TrayNotifier trayNotifier;
    @Inject private LimeMenuBar limeMenuBar;
    @Inject private DelayedShutdownHandler delayedShutdownHandler;
    
    private OptionsDialog lastOptionsDialog;
    
    /** Starts with the mock core. */
    public static void main(String[] args) {
        launch(AppFrame.class, args);
    }

    /** Returns true if the UI has initialized & successfully been shown. */
    public static boolean isStarted() {
        return started;
    }
    
    /**
     * JDesktop's built-in storage isn't what we want -- don't use it.
     * There's issues with it restoring sizes/position if you closed
     * while minimized or other weirdness.  Unfortunately, changing
     * it is package-private.
     */
    private void changeSessionStorage(ApplicationContext ctx, SessionStorage storage) {
        try {
            Method m = ctx.getClass().getDeclaredMethod("setSessionStorage", SessionStorage.class);
            m.setAccessible(true);
            m.invoke(ctx, storage);
        } catch(Throwable oops) {}
    }
    
    @Override
    protected void initialize(String[] args) {
        changeSessionStorage(getContext(), new NullSessionStorage(getContext()));
        
        GuiUtils.assignResources(this);

        initUIDefaults();
        
        // Because we use a browser heavily, which is heavyweight,
        // we must disable all lightweight popups.
        if(MozillaInitialization.getStatus() != InitStatus.FAILED) {
            JPopupMenu.setDefaultLightWeightPopupEnabled(false);
            ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        }
        
        isStartup = args.length > 0 && STARTUP.equals(args[0]);
    }

    @Override
    protected void startup() {        
        String title = getContext().getResourceMap().getString("Application.title");
        JFrame frame = new FirstVizIgnorer(title);
        frame.setName("mainFrame");
        getMainView().setFrame(frame);

        // Create the Injector for the UI.
        assert ui == null;
        createUiInjector();
        assert ui != null;

        if(isStartup || SwingUiSettings.MINIMIZE_TO_TRAY.getValue()) {
            trayNotifier.showTrayIcon();            
        } else {
            trayNotifier.hideTrayIcon();
        }
        getMainFrame().setJMenuBar(limeMenuBar);

        // Install handler for shutdown after transfers.
        delayedShutdownHandler.install(this);
        
        addExitListener(new TrayExitListener(trayNotifier));
        addExitListener(new ShutdownListener(getMainFrame(), application));
        
        framePositioner.initialize(getMainFrame());
       
        // This will NOT actually show it -- we're purposely
        // doing this to more explicitly control when it goes visible.
        // Unfortunately, we have to call show in order to setup things
        // on the ui.
        show(ui);
        
        // Set the window position just before we become visible.
        framePositioner.setWindowPosition();
        
        // Set visible if this isn't being run from startup --
        // otherwise, minimize.
        if (isStartup) {
            minimizeToTray();
        } else {
            getMainFrame().setVisible(true);
        }
        
        ui.goHome();
        ui.focusOnSearch();

        started = true;
    }
    
    @Override
    protected void ready() {
        if (setupWizard.shouldShowWizard()) {
            JXPanel glassPane = new JXPanel();
            glassPane.setOpaque(false);
            glassPane.setBackgroundPainter(new AbstractPainter<JComponent>() {
                @Override
                protected void doPaint(Graphics2D g, JComponent object, int width, int height) {
                    g.setPaint(glassPaneColor);
                    g.fillRect(0, 0, width, height);
                }
            });
            getMainFrame().setGlassPane(glassPane);

            ui.hideMainPanel();
            glassPane.setVisible(true);
            setupWizard.showDialogIfNeeded(getMainFrame());
            glassPane.setVisible(false);
            ui.showMainPanel();
        }
        
        validateSaveDirectory();

        EventAnnotationProcessor.subscribe(this);

        // Now that the UI is ready to use, update it's priority a bit.
        Thread eventThread = Thread.currentThread();
        eventThread.setPriority(eventThread.getPriority() + 1);

        new ShellAssociationManager().validateFileAssociations(getMainFrame());

        ui.loadProNag();
    }

    @EventSubscriber
    public void handleShowAboutWindow(AboutDisplayEvent event) {
        new AboutWindow(getMainFrame(), application).showDialog();
    }
    
    @EventSubscriber
    public void handleShowOptionsDialog(OptionsDisplayEvent event) {
        if(lastOptionsDialog == null) {
            lastOptionsDialog = options.get();
        }
        
        if (!lastOptionsDialog.isVisible()) {
            lastOptionsDialog.initOptions();
            lastOptionsDialog.setLocationRelativeTo(GuiUtils.getMainFrame());
            lastOptionsDialog.setVisible(true);
        }
    }
    
    @EventSubscriber
    public void handleExitApplication(ExitApplicationEvent event) {
        exit(event.getActionEvent());
    }
    
    @EventSubscriber
    public void handleRestoreView(RestoreViewEvent event) {
        if(!SwingUiSettings.MINIMIZE_TO_TRAY.getValue()) {
            trayNotifier.hideTrayIcon();
        }
        getMainFrame().setVisible(true);
        getMainFrame().setState(Frame.NORMAL);
        getMainFrame().toFront();
    }
    
    @Action
    public void showAboutWindow() { // DO NOT CHANGE THIS METHOD NAME!  
        handleShowAboutWindow(null);
    }
    
    @Action
    public void minimizeToTray() { // DO NOT CHANGE THIS METHOD NAME!  
        getMainFrame().setState(Frame.ICONIFIED);
        getMainFrame().setVisible(!trayNotifier.supportsSystemTray());
    }    

    @Action
    public void restoreView() { // DO NOT CHANGE THIS METHOD NAME!
        handleRestoreView(null);
    }

    /**
     * Action method to exit the application after all transfers are completed.
     */
    @Action
    public void shutdownAfterTransfers() { // DO NOT CHANGE THIS METHOD NAME!
        delayedShutdownHandler.shutdownAfterTransfers();
    }
    
    public Injector createUiInjector() {
        Module thiz = new AbstractModule() {
            @Override
            protected void configure() {
                bind(AppFrame.class).toInstance(AppFrame.this);
            }
        };
        if (injector == null) {
            LimeMozillaInitializer.initialize();
            injector = Guice.createInjector(Stage.PRODUCTION, new MockModule(), new LimeWireSwingUiModule(false), thiz);
            return injector;
        } else {
            List<Module> modules = new ArrayList<Module>();
            modules.add(thiz);
            modules.add(new LimeWireSwingUiModule(injector.getInstance(Application.class).isProVersion()));
            modules.add(Modules.providersFrom(injector)); // Add all the parent bindings
            return Guice.createInjector(Stage.PRODUCTION, modules);
        }
    }
    
    /**
     * Sets the custom default UI color and behavior properties
     */
    private void initUIDefaults() {       
        if (OSUtils.isMacOSX()) {
            initMacUIDefaults();
        }
        
        if (OSUtils.isWindows()) {
            verifyWindowsLAF();
        }
        
        initBackgrounds();
        
        // Set default selection colours
        Color selectionBackground = new Color(0xc2e986);
        UIManager.put("TextField.selectionBackground", selectionBackground);
        UIManager.put("PasswordField.selectionBackground", selectionBackground);
        UIManager.put("EditorPane.selectionBackground", selectionBackground);
        UIManager.put("TextArea.selectionBackground", selectionBackground);
        UIManager.put("Menu.selectionBackground", selectionBackground);
        UIManager.put("MenuItem.selectionBackground", selectionBackground);
        UIManager.put("CheckBoxMenuItem.selectionBackground", selectionBackground);
        UIManager.put("RadioButtonMenuItem.selectionBackground", selectionBackground);
        
        // Set the menu item highlight colors to avoid contrast issues with
        //  new highlight background in default XP theme
        Color selectionForeground = Color.BLACK;
        UIManager.put("TextField.selectionForeground", selectionForeground);
        UIManager.put("PasswordField.selectionForeground", selectionForeground);
        UIManager.put("EditorPane.selectionForeground", selectionForeground);
        UIManager.put("TextArea.selectionForeground", selectionForeground);
        UIManager.put("Menu.selectionForeground", selectionForeground);
        UIManager.put("MenuItem.selectionForeground", selectionForeground);
        UIManager.put("CheckBoxMenuItem.selectionForeground", selectionForeground);
        UIManager.put("RadioButtonMenuItem.selectionForeground", selectionForeground);
        
        
        // Necessary to allow popups to behave normally.
        UIManager.put("PopupMenu.consumeEventOnClose", false);
        
        // FIX FOR SUN BUG 6449933: On Windows sometimes, JFileChooser cannot 
        // display because some icons throw an AIOOBE when they are retrieved.
        // To workaround this, we install our own version of those icons.
        if (OSUtils.isWindows()) {
            replaceIconIfFailing("FileChooser.upFolderIcon", upFolderVistaFixIcon);
            replaceIconIfFailing("FileChooser.detailsViewIcon", detailsViewVistaFixIcon);
            replaceIconIfFailing("FileChooser.listViewIcon", listViewVistaFixIcon);
            replaceIconIfFailing("FileChooser.newFolderIcon", newFolderVistaFixIcon);
        }
    }
    
    /**
     * Sets some mac only UI settings.
     */
    private void initMacUIDefaults() {
        UIManager.put("MenuItemUI", "javax.swing.plaf.basic.BasicMenuItemUI");
        UIManager.put("CheckBoxMenuItemUI", "javax.swing.plaf.basic.BasicCheckBoxMenuItemUI");
        UIManager.put("RadioButtonMenuItemUI", "javax.swing.plaf.basic.BasicRadioButtonMenuItemUI");
    }
        
    /**
     * Changes all default background colors that are equal to Panel.background to the
     * bgColor set in properties. Also sets Table.background.
     */
    private void initBackgrounds() {
        ColorUIResource bgColorResource = new ColorUIResource(bgColor);
        Color oldBgColor = UIManager.getDefaults().getColor("Panel.background");
        UIDefaults uiDefaults = UIManager.getDefaults();
        Enumeration<?> enumeration = uiDefaults.keys();
        while (enumeration.hasMoreElements()) {
            Object key = enumeration.nextElement();
            if (key.toString().indexOf("background") != -1) {
                if (uiDefaults.get(key).equals(oldBgColor)) {
                    UIManager.getDefaults().put(key, bgColorResource);
                }
            }
        }

        uiDefaults.put("Table.background", bgColorResource);
    }
    
    /**
     * Replaces an icon resource in UIManager with the specified replacement
     * icon if the original resource cannot be retrieved correctly.
     */
    private void replaceIconIfFailing(String resource, Icon replacementIcon) {
        try {
            UIManager.getIcon(resource);
        } catch (ArrayIndexOutOfBoundsException aioobe) {
            UIManager.put(resource, replacementIcon);
        }
    }
        
    /** Ensures the save directory is valid. */
    private void validateSaveDirectory() {        
        // Make sure the save directory is valid.
        SaveDirectoryHandler.validateSaveDirectoryAndPromptForNewOne();
    }
    
    /**
     * Verify that the Windows LAF will load properly.  If not, then this 
     * method installs the cross-platform LAF.  This is a workaround for Sun
     * Bug IDs 6629522, 6588271, 6622760, 6637885, which can cause an NPE when 
     * retrieving the checkbox icon width. 
     */
    private void verifyWindowsLAF() {
        // Skip if installed LAF is not Windows LAF.
        String lafName = UIManager.getLookAndFeel().getClass().getName();
        if (!UIManager.getSystemLookAndFeelClassName().equals(lafName)) {
            return;
        }
        
        boolean lafValid = true;

        try {
            // Create checkbox with sample text, and get its preferred size.  
            // This should force a call to get the width of the checkbox icon 
            // in the Windows LAF.  The icon class is 
            // WindowsIconFactory$CheckBoxIcon.  The icon can also be retrieved
            // by a call to WindowsCheckBoxUI.getDefaultIcon(). (JIRA LWC-2302)
            JCheckBox checkBox = new JCheckBox("Verify");
            checkBox.getPreferredSize();
            
        } catch (NullPointerException npe) {
            LOG.error("Windows XP LAF error", npe);
            lafValid = false;
        }
        
        // Install cross-platform LAF if Windows LAF is not valid.
        if (!lafValid) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                LOG.error("Unable to install LAF", ex);
            }
        }
    }
    
    private static class ShutdownListener implements ExitListener {
        private final Application application;
        private final JFrame mainFrame;
        
        public ShutdownListener(JFrame mainFrame, Application application) {
            this.mainFrame = mainFrame;
            this.application = application;
        }        
        
        @Override
        public boolean canExit(EventObject event) {
            return true;
        }
        
        @Override
        public void willExit(EventObject event) {
            mainFrame.setVisible(false);
            System.out.println("Shutting down...");
            application.stopCore();
            System.out.println("Shut down");
        }
        
    }
    
    private static class FirstVizIgnorer extends LimeJFrame {
        /** false if we haven't shown once yet. */
        private boolean shownOnce = false;

        public FirstVizIgnorer(String title) {
            super(title);
        }

        //this is a bit hacky and ugly but necessary because SingleFrameApplication.show(Component) calls setVisible
        //and we don't want that to happen.  Unfortunately show() also calls private methods so we can't just override it.
        @Override
        public void setVisible(boolean visible) {
            // Ignore the first call.
            if(!shownOnce) {
                shownOnce = true;
            } else {
                super.setVisible(visible);
            }
        }
        
        @SuppressWarnings("deprecation")
        @Override
        public void hide() {
            try {
                super.hide();
            } catch(Throwable t) {
                // Ignored... Internal JDK bugs causing this to error
                // out, which ends up stopping us from closing LW.
            }
        }

    }
}
