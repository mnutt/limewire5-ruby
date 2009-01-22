package org.limewire.ui.swing;

import org.limewire.ui.swing.callback.GuiCallbackImpl;
import org.limewire.ui.swing.components.LimeWireUiComponentsModule;
import org.limewire.ui.swing.dock.LimeWireUiDockModule;
import org.limewire.ui.swing.downloads.LimeWireUiDownloadsModule;
import org.limewire.ui.swing.friends.LimeWireUiFriendsModule;
import org.limewire.ui.swing.images.LimeWireUiImagesModule;
import org.limewire.ui.swing.library.LimeWireUiLibraryModule;
import org.limewire.ui.swing.library.sharing.LimeWireUiLibrarySharingModule;
import org.limewire.ui.swing.mainframe.LimeWireUiMainframeModule;
import org.limewire.ui.swing.nav.LimeWireUiNavModule;
import org.limewire.ui.swing.options.LimeWireUiOptionsModule;
import org.limewire.ui.swing.painter.LimeWireUiPainterModule;
import org.limewire.ui.swing.player.LimeWireUiPlayerModule;
import org.limewire.ui.swing.properties.LimeWireUiPropertiesModule;
import org.limewire.ui.swing.search.LimeWireUiSearchModule;
import org.limewire.ui.swing.statusbar.LimeWireUiStatusbarModule;
import org.limewire.ui.swing.tray.LimeWireUiTrayModule;
import org.limewire.ui.swing.upload.LimeWireUiUploadModule;
import org.limewire.ui.swing.util.LimeWireUiUtilModule;
import org.limewire.ui.swing.wizard.LimeWireUiWizardModule;

import com.google.inject.AbstractModule;

public class LimeWireSwingUiModule extends AbstractModule {
    
    private final boolean isPro;
    
    public LimeWireSwingUiModule(boolean isPro) {
        this.isPro = isPro;
    }
    
    @Override
    protected void configure() {
        bind(GuiCallbackImpl.class);
        install(new LimeWireUiUtilModule());
        install(new LimeWireUiSearchModule());
        install(new LimeWireUiNavModule());
        install(new LimeWireUiMainframeModule());
        install(new LimeWireUiTrayModule());
        install(new LimeWireUiFriendsModule());
        install(new LimeWireUiPlayerModule());
        install(new LimeWireUiImagesModule());
        install(new LimeWireUiLibraryModule());
        install(new LimeWireUiLibrarySharingModule());
        install(new LimeWireUiDownloadsModule());
        install(new LimeWireUiOptionsModule());
        install(new LimeWireUiStatusbarModule());
        install(new LimeWireUiPainterModule(isPro));
        install(new LimeWireUiComponentsModule());
        install(new LimeWireUiUploadModule());
        install(new LimeWireUiDockModule());
        install(new LimeWireUiWizardModule());
        install(new LimeWireUiPropertiesModule());
    }
}