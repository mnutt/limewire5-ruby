package org.limewire.ui.swing.library;

import org.limewire.inject.LazyBinder;
import org.limewire.ui.swing.library.image.LibraryImageSubPanelFactory;
import org.limewire.ui.swing.library.image.LibraryImageSubPanelFactoryImpl;
import org.limewire.ui.swing.library.nav.LimeWireUiLibraryNavModule;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableFactoryImpl;
import org.limewire.ui.swing.library.table.ShareTableRendererEditor;
import org.limewire.ui.swing.library.table.ShareTableRendererEditorFactory;
import org.limewire.ui.swing.library.table.menu.MyLibraryPopupMenu;
import org.limewire.ui.swing.library.table.menu.MyLibraryPopupMenuFactory;
import org.limewire.ui.swing.library.table.menu.actions.SharingActionFactory;
import org.limewire.ui.swing.library.table.menu.actions.SharingActionFactoryImpl;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireUiLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        install(new LimeWireUiLibraryNavModule());
        
        bind(LibraryTableFactory.class).toProvider(LazyBinder.newLazyProvider(
                LibraryTableFactory.class, LibraryTableFactoryImpl.class));
        bind(SharingActionFactory.class).toProvider(LazyBinder.newLazyProvider(
                SharingActionFactory.class, SharingActionFactoryImpl.class));
        
        bind(LibraryImageSubPanelFactory.class).to(LibraryImageSubPanelFactoryImpl.class);
        
        bind(ShareTableRendererEditorFactory.class).toProvider(
                FactoryProvider.newFactory(ShareTableRendererEditorFactory.class, ShareTableRendererEditor.class));
        
        bind(MyLibraryPopupMenuFactory.class).toProvider(
                FactoryProvider.newFactory(MyLibraryPopupMenuFactory.class, MyLibraryPopupMenu.class));
        
        bind(FriendLibraryFactory.class).toProvider(
                FactoryProvider.newFactory(FriendLibraryFactory.class, FriendLibraryPanel.class));

        bind(MyLibraryFactory.class).toProvider(
                FactoryProvider.newFactory(MyLibraryFactory.class, MyLibraryPanel.class));
    }
}
