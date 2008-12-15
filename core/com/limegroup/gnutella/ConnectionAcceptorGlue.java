package com.limegroup.gnutella;

import org.limewire.core.settings.LWSSettings;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceStage;
import org.limewire.net.ConnectionDispatcher;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.browser.ControlRequestAcceptor;
import com.limegroup.gnutella.browser.LocalHTTPAcceptor;
import com.limegroup.gnutella.downloader.PushDownloadManager;
import com.limegroup.gnutella.lws.server.LWSManager;

@Singleton
class ConnectionAcceptorGlue {

    private final ConnectionDispatcher externalDispatcher;
    private final ConnectionDispatcher localDispatcher;
    
    private final LocalHTTPAcceptor localHttpAcceptor;
    private final HTTPAcceptor externalHttpAcceptor;
    private final PushDownloadManager pushDownloadManager;
    private final TorrentManager torrentManager;
    private final ControlRequestAcceptor controlRequestAcceptor;
    private final LWSManager lwsManager;

    @Inject
    public ConnectionAcceptorGlue(
            @Named("global") ConnectionDispatcher externalDispatcher,
            @Named("local") ConnectionDispatcher localDispatcher,
            HTTPAcceptor externalHttpAcceptor,
            LocalHTTPAcceptor localHttpAcceptor,
            PushDownloadManager pushDownloadManager,
            TorrentManager torrentManager,
            ControlRequestAcceptor controlRequestAcceptor,
            LWSManager lwsManager) {
        this.externalDispatcher = externalDispatcher;
        this.localDispatcher = localDispatcher;
        this.externalHttpAcceptor = externalHttpAcceptor;
        this.pushDownloadManager = pushDownloadManager;
        this.torrentManager = torrentManager;
        this.localHttpAcceptor = localHttpAcceptor;
        this.controlRequestAcceptor = controlRequestAcceptor;
        this.lwsManager = lwsManager;
    }

    @Inject
    @SuppressWarnings({"unused", "UnusedDeclaration"})
    private void register(org.limewire.lifecycle.ServiceRegistry registry) {
        // TODO: This really should be a bunch of services that depend on the
        //       dispatchers being started.  We workaround that by starting
        //       them in the LATE stage, which assumes the dispatchers
        //       are started in EARLY or NORMAL.
        registry.register(new Service() {
            public String getServiceName() {
                return org.limewire.i18n.I18nMarker.marktr("Connection Dispatching");
            }

            public void initialize() {
            };

            public void start() {
                externalDispatcher.addConnectionAcceptor(externalHttpAcceptor, false,
                        externalHttpAcceptor.getHttpMethods());
                externalDispatcher.addConnectionAcceptor(pushDownloadManager, false, "GIV");
                torrentManager.initialize(externalDispatcher);
                localDispatcher.addConnectionAcceptor(localHttpAcceptor, true, localHttpAcceptor
                        .getHttpMethods());
                localDispatcher.addConnectionAcceptor(controlRequestAcceptor,
                            true, "MAGNET", "TORRENT");
                externalDispatcher.addConnectionAcceptor(controlRequestAcceptor,
                            true, "MAGNET","TORRENT");
                
                if (LWSSettings.LWS_IS_ENABLED.getValue()) {
                    localHttpAcceptor.registerHandler("/" + LWSManager.PREFIX + "*",  lwsManager.getHandler());
                }

            }

            public void stop() {
            };
        }).in(ServiceStage.LATE);
    }

}
