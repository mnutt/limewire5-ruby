package com.limegroup.gnutella.downloader;

import java.util.Locale;

import org.limewire.core.settings.DownloadSettings;
import org.limewire.util.BaseTestCase;


public class SelectionStrategyFactoryTest extends BaseTestCase {

    public SelectionStrategyFactoryTest(String name) {
        super(name);
    }


    public void testGetStrategyForWithAllLocales() {
        for (Locale locale : Locale.getAvailableLocales()) {
            Locale.setDefault(locale);
            for (String extension : DownloadSettings.PREVIEWABLE_EXTENSIONS.get()) {
                String upperCaseExtension = extension.toUpperCase(Locale.US);
                SelectionStrategy strategy = SelectionStrategyFactory.getStrategyFor(upperCaseExtension, 500);
                assertTrue("Failed for locale: " + locale, strategy instanceof BiasedRandomDownloadStrategy);
            }
        }
    }

}
