package com.limegroup.bittorrent;

import org.limewire.bittorrent.TorrentManager;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.inject.GuiceUtils;
import org.limewire.inspection.InspectionUtils;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.ActivityCallbackAdapter;
import com.limegroup.gnutella.LimeWireCoreModule;

public class LazyTorrentManagerTest extends LimeTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        BittorrentSettings.LIBTORRENT_ENABLED.set(true);
    }

    @Override
    protected void tearDown() throws Exception {
        BittorrentSettings.LIBTORRENT_ENABLED.set(false);
    }
    
    public void testInspections() throws Exception {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new LimeWireCoreModule(
                ActivityCallbackAdapter.class));
        GuiceUtils.loadEagerSingletons(injector);
        assertEquals("NOT_INITIALIZED", InspectionUtils.inspectValue("org.limewire.libtorrent.LazyTorrentManager,torrentManagerStatus", injector, true));
        injector.getInstance(TorrentManager.class).isValid();
        assertEquals("LOADED", InspectionUtils.inspectValue("org.limewire.libtorrent.LazyTorrentManager,torrentManagerStatus", injector, true));
    }
}
