package org.limewire.ui.swing.library.image;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.api.playlist.PlaylistManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.dnd.MyLibraryTransferHandler;
import org.limewire.ui.swing.friends.login.FriendsSignInPanel;
import org.limewire.ui.swing.images.ImageCellRenderer;
import org.limewire.ui.swing.images.ImageList;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.library.LibraryListSourceChanger;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.table.ShareTableRendererEditor;
import org.limewire.ui.swing.library.table.ShareTableRendererEditorFactory;
import org.limewire.ui.swing.library.table.menu.MyImageLibraryPopupHandler;
import org.limewire.ui.swing.library.table.menu.actions.SharingActionFactory;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPService;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventSelectionModel;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LibraryImageSubPanelFactoryImpl implements LibraryImageSubPanelFactory {

    private ThumbnailManager thumbnailManager;
    
    private Dimension subPanelDimension = new Dimension(ThumbnailManager.WIDTH,28);

    private LibraryManager libraryManager;
    
    private final ShareTableRendererEditorFactory shareTableRendererEditorFactory;

    private final PropertiesFactory<LocalFileItem> localFilePropFactory;
    private final SharingActionFactory sharingActionFactory;
    private final FriendsSignInPanel friendSignInPanel;
    private final ShareListManager shareListManager;
    private final XMPPService xmppService;
    private final ComboBoxDecorator comboDecorator;
    private final LibraryNavigator libraryNavigator;
    private final PlaylistManager playlistManager;
    
    @Inject
    public LibraryImageSubPanelFactoryImpl(ThumbnailManager thumbnailManager, LibraryManager libraryManager, 
            ShareTableRendererEditorFactory shareTableRendererEditorFactory, SharingActionFactory sharingActionFactory, 
            PropertiesFactory<LocalFileItem> localFilePropFactory, ShareListManager shareListManager,
            XMPPService xmppService, FriendsSignInPanel friendSignInPanel,
            ComboBoxDecorator comboDecorator, 
            LibraryNavigator libraryNavigator, PlaylistManager playlistManager) {
        this.thumbnailManager = thumbnailManager;
        this.libraryManager = libraryManager;
        this.shareTableRendererEditorFactory = shareTableRendererEditorFactory;
        this.sharingActionFactory = sharingActionFactory;
        this.localFilePropFactory = localFilePropFactory;
        this.shareListManager = shareListManager;
        this.xmppService = xmppService;
        this.friendSignInPanel = friendSignInPanel;
        this.comboDecorator = comboDecorator;
        this.libraryNavigator = libraryNavigator;
        this.playlistManager = playlistManager;
    }
    
    @Override
    public LibraryImageSubPanel createMyLibraryImageSubPanel(File parentFolder,
            EventList<LocalFileItem> eventList, LocalFileList fileList,
            ShareWidget<File> shareWidget, LibraryListSourceChanger listChanger) {

        LibraryImageFolderComboBox comboBox = new LibraryImageFolderComboBox(xmppService, sharingActionFactory, friendSignInPanel);
        comboDecorator.decorateLinkComboBox(comboBox);
        
        LibraryImageSubPanel panel = new LibraryImageSubPanel(parentFolder, eventList, fileList, comboBox);
        panel.setPopupHandler(new MyImageLibraryPopupHandler(panel, sharingActionFactory, libraryManager, localFilePropFactory, xmppService, libraryNavigator, playlistManager));
        comboBox.setSelectAllable(panel);

        ImageList list = panel.getImageList();
        list.setImageCellRenderer(enableMyLibraryRenderer(list));
        panel.setImageEditor(enableMyLibraryEditor(shareWidget, panel));
        TransferHandler transferHandler = new MyLibraryTransferHandler(getSelectionModel(list), libraryManager.getLibraryManagedList(), shareListManager, listChanger);
        list.setTransferHandler(transferHandler);
        panel.setTransferHandler(transferHandler);
        return panel;
    }
    
    @SuppressWarnings("unchecked")
    private EventSelectionModel<LocalFileItem> getSelectionModel(ImageList list){
        return (EventSelectionModel<LocalFileItem>) list.getSelectionModel();
    }
    
    private ImageCellRenderer enableMyLibraryRenderer(ImageList imageList) {
        ImageCellRenderer renderer = new LibraryImageCellRenderer(imageList.getFixedCellWidth(), imageList.getFixedCellHeight() - 2, thumbnailManager);
        renderer.setOpaque(false);
        JComponent buttonRenderer = shareTableRendererEditorFactory.createShareTableRendererEditor(null);
        buttonRenderer.setOpaque(false);
        buttonRenderer.setPreferredSize(subPanelDimension);
        buttonRenderer.setSize(subPanelDimension);
        renderer.setButtonComponent(buttonRenderer);
        
        return renderer;
    }
    
    private TableRendererEditor enableMyLibraryEditor(ShareWidget<File> shareWidget, LibraryImageSubPanel parent){
        ShareAction action = new ShareAction(I18n.tr("Sharing"), shareWidget, parent);
        ShareTableRendererEditor shareEditor = shareTableRendererEditorFactory.createShareTableRendererEditor(action);
        action.setEditor(shareEditor);
        shareEditor.setPreferredSize(subPanelDimension);
        shareEditor.setSize(subPanelDimension);
        shareEditor.setOpaque(false);
        shareEditor.setVisible(false);
                
        return shareEditor;
    }

    private static class ShareAction extends AbstractAction {
        private ShareTableRendererEditor shareEditor;
        private ShareWidget<File> shareWidget;
        private LibraryImageSubPanel parent;

        public ShareAction(String text, ShareWidget<File> shareWidget, LibraryImageSubPanel parent){
            super(text);
            this.shareWidget = shareWidget;
            this.parent = parent;
        }
        
        public void setEditor(ShareTableRendererEditor editor) {
            this.shareEditor = editor;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            shareWidget.setShareable(shareEditor.getLocalFileItem().getFile());
          
            JComponent source = (JComponent)e.getSource();
            Point convertedLocation = 
                SwingUtilities.convertPoint(shareEditor, source.getLocation(), parent.getImageList());
            selectImage(convertedLocation);
            shareWidget.show(source);
            shareEditor.cancelCellEditing();
        }
        
        private void selectImage(Point point) {
            int index = parent.getImageList().locationToIndex(point);
            if(index > -1)
                parent.getImageList().setSelectedIndex(index);
        }
    }
}
