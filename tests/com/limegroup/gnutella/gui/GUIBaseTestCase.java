package com.limegroup.gnutella.gui;

import java.io.File;
import java.util.Locale;

import org.limewire.core.settings.ApplicationSettings;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.limegroup.gnutella.util.LimeTestCase;

/**
 * Base class for gui tests that rely on the "gui" folder to be the
 * working directory.
 */
public abstract class GUIBaseTestCase extends LimeTestCase {

    public static Module GUI_CORE_MEDIATOR_INJECTION = new AbstractModule() {
        @Override
        protected void configure() {
            requestStaticInjection(GuiCoreMediator.class);
        }
    };
    
	private static String savedWorkingDir;
	
	public GUIBaseTestCase(String name) {
		super(name);
	}
    
    public static void setLocaleSettings(Locale locale) {
        ApplicationSettings.LANGUAGE.setValue(locale.getLanguage());
        ApplicationSettings.COUNTRY.setValue(locale.getCountry());
        ApplicationSettings.LOCALE_VARIANT.setValue(locale.getVariant());
        GUIMediator.resetLocale();
    }

	public static void globalSetUp() throws Exception {
        DefaultErrorCatcher.install();
		savedWorkingDir = System.getProperty("user.dir");
		File guiDir = getGUIDir();
		System.setProperty("user.dir", guiDir.getAbsolutePath());
        GUIMediator.safeInvokeAndWait(new Runnable() {
            public void run() {
                ResourceManager.instance();
            }
        });
	}
	
	public static void globalTearDown() {
		System.setProperty("user.dir", savedWorkingDir);
	}
    
    @Override
    protected void setUp() throws Exception {
        setLocaleSettings(Locale.US);
    }
	
}
