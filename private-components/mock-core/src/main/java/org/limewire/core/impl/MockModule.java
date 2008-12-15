package org.limewire.core.impl;

import org.limewire.core.api.Application;
import org.limewire.core.api.callback.GuiCallbackService;
import org.limewire.core.api.lifecycle.MockLifeCycleModule;
import org.limewire.core.api.magnet.MockMagnetModule;
import org.limewire.core.impl.browse.MockBrowseModule;
import org.limewire.core.impl.callback.MockGuiCallbackService;
import org.limewire.core.impl.connection.MockConnectionModule;
import org.limewire.core.impl.daap.MockDaapModule;
import org.limewire.core.impl.download.MockDownloadModule;
import org.limewire.core.impl.library.MockLibraryModule;
import org.limewire.core.impl.mojito.MockMojitoModule;
import org.limewire.core.impl.monitor.MockMonitorModule;
import org.limewire.core.impl.network.MockNetworkModule;
import org.limewire.core.impl.player.MockPlayerModule;
import org.limewire.core.impl.search.MockSearchModule;
import org.limewire.core.impl.spam.MockSpamModule;
import org.limewire.core.impl.support.MockSupportModule;
import org.limewire.core.impl.updates.MockUpdatesModule;
import org.limewire.core.impl.upload.MockUploadModule;
import org.limewire.core.impl.xmpp.MockXmppModule;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.net.MockNetModule;

import com.google.inject.AbstractModule;

public class MockModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(Application.class).to(MockApplication.class);
        bind(GuiCallbackService.class).to(MockGuiCallbackService.class);
        bind(ServiceRegistry.class).to(MockServiceRegistry.class);

        install(new MockLifeCycleModule());
        install(new MockConnectionModule());
        install(new MockDaapModule());
        install(new MockSpamModule());
        install(new MockSearchModule());
        install(new MockNetworkModule());
        install(new MockDownloadModule());
        install(new MockLibraryModule());
        install(new MockMojitoModule());
        install(new MockMonitorModule());
        install(new MockBrowseModule());
        install(new MockPlayerModule());
        install(new MockXmppModule());
        install(new MockSupportModule());
        install(new MockMagnetModule());
        install(new MockNetModule());
        install(new MockUploadModule());
        install(new MockUpdatesModule());
    }

}
