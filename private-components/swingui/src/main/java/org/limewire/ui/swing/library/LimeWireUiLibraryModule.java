package org.limewire.ui.swing.library;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.library.image.LibraryImageSubPanelFactory;
import org.limewire.ui.swing.library.image.LibraryImageSubPanelFactoryImpl;
import org.limewire.ui.swing.library.nav.LimeWireUiLibraryNavModule;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableFactoryImpl;
import org.limewire.ui.swing.library.table.ShareTableRendererEditor;
import org.limewire.ui.swing.library.table.ShareTableRendererEditorFactory;
import org.limewire.ui.swing.library.table.menu.actions.SharingActionFactory;
import org.limewire.ui.swing.library.table.menu.actions.SharingActionFactoryImpl;
import org.limewire.ui.swing.properties.PropertiesFactory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        install(new LimeWireUiLibraryNavModule());
        
        bind(FriendLibraryFactory.class).toProvider(
                FactoryProvider.newFactory(FriendLibraryFactory.class, FriendLibraryPanel.class));
        bind(EmptyLibraryFactory.class).toProvider(
                FactoryProvider.newFactory(EmptyLibraryFactory.class, EmptyLibraryPanel.class));
        bind(FriendLibraryMediatorFactory.class).toProvider(
                FactoryProvider.newFactory(FriendLibraryMediatorFactory.class, FriendLibraryMediator.class));
        bind(LibraryTableFactory.class).to(LibraryTableFactoryImpl.class);
        bind(SharingActionFactory.class).to(SharingActionFactoryImpl.class);
        bind(LibraryImageSubPanelFactory.class).to(LibraryImageSubPanelFactoryImpl.class);
        
        bind(new TypeLiteral<PropertiesFactory<LocalFileItem>>(){}).to(LocalFileItemPropertiesFactory.class);
        bind(new TypeLiteral<PropertiesFactory<RemoteFileItem>>(){}).to(RemoteFileItemPropertiesFactory.class);
        
        bind(ShareTableRendererEditorFactory.class).toProvider(
                FactoryProvider.newFactory(ShareTableRendererEditorFactory.class, ShareTableRendererEditor.class));
    }
}
