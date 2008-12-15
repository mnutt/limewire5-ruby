package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;
import org.limewire.util.OSUtils;

public class MyLibraryMultipleSelectionPopupMenu extends JPopupMenu {
   
    private List<LocalFileItem> fileItems;

    final private LibraryManager libraryManager;

    private ShareWidget<LocalFileItem[]> unshareWidget;
    private ShareWidget<LocalFileItem[]> shareWidget;

//    final private JMenuItem gnutellaShareItem;
//    final private JMenuItem gnutellaUnshareItem;
//    final private JMenu friendShareSubMenu;
//    final private JMenu friendUnshareSubMenu;
    

    //private Component repaintComponent;

private ShareWidgetFactory shareFactory;

    public MyLibraryMultipleSelectionPopupMenu(Category category, LibraryManager libraryManager,
            ShareWidgetFactory shareFactory, Component repaintComponent) {
        this.libraryManager = libraryManager;
        this.shareFactory = shareFactory;
        //this.repaintComponent = repaintComponent;
    
        
//        gnutellaShareItem = new JMenuItem(gnutellaShareAction);
//        gnutellaUnshareItem = new JMenuItem(gnutellaUnshareAction);
//        friendShareSubMenu = new JMenu(I18n.tr("Share with Friends"));
//        friendUnshareSubMenu = new JMenu(I18n.tr("Unshare with Friends"));
        
        initialize(category);
    }

    public void setFileItems(List<LocalFileItem> items) {
        this.fileItems = items;
//        friendShareSubMenu.setVisible(friendList.size() > 0);
//        friendUnshareSubMenu.setVisible(friendList.size() > 0);
      //  resetFriends();
    }
    
//    private void resetFriends() {
//       friendShareSubMenu.removeAll();
//       for (SharingTarget target : friendList){            
//           friendShareSubMenu.add(new FriendShareAction(target.getFriend()));
//           friendUnshareSubMenu.add(new FriendUnshareAction(target.getFriend()));
//       }
//       
//    }
    
    private void initialize(Category category) {
        add(new RemoveAction());
        add(new DeleteAction());
        add(new JSeparator());
        add(new ShareAction());
        add(new UnshareAction());
//        add(gnutellaShareItem);
//        gnutellaShareItem.setEnabled(isGnutellaSharable(category));
//        add(friendShareSubMenu);
  //      add(new JSeparator());
//        add(gnutellaUnshareItem);
//        gnutellaUnshareItem.setEnabled(isGnutellaSharable(category));
//        add(friendUnshareSubMenu);

    }
    
//    private boolean isGnutellaSharable(Category category){
//        return category != Category.DOCUMENT || LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue();
//    }

    private LocalFileItem[] createFileItemArray(){
        return fileItems.toArray(new LocalFileItem[fileItems.size()]);
    }    
    
    private class UnshareAction extends AbstractAction {
        public UnshareAction() {
            super(I18n.tr("Unshare"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final LocalFileItem[] fileItemArray = createFileItemArray();
            if(unshareWidget == null){                
                unshareWidget = shareFactory.createMultiFileUnshareWidget();
            }
            unshareWidget.setShareable(fileItemArray);
            unshareWidget.show(GuiUtils.getMainFrame());
          //  repaintComponent.repaint();
        }
    };

    private class ShareAction extends AbstractAction {
        public ShareAction() {
            super(I18n.tr("Share"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final LocalFileItem[] fileItemArray = createFileItemArray();

            if(shareWidget == null){                
                shareWidget = shareFactory.createMultiFileShareWidget();
            }
            shareWidget.setShareable(fileItemArray);
            shareWidget.show(GuiUtils.getMainFrame());
            //repaintComponent.repaint();
        }
    };

    private class RemoveAction extends AbstractAction {
        public RemoveAction() {
            super(I18n.tr("Remove from library"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final LocalFileItem[] fileItemArray = createFileItemArray();
            for (LocalFileItem fileItem : fileItemArray) {
                if (!fileItem.isIncomplete()) {
                    libraryManager.getLibraryManagedList().removeFile(fileItem.getFile());
                }
            }
        }
    };

    // TODO combine with MyLibraryManager deleteAction
    private class DeleteAction extends AbstractAction {
        public DeleteAction() {
            super(I18n.tr("Delete files"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final LocalFileItem[] fileItemArray = createFileItemArray();

            BackgroundExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (LocalFileItem fileItem : fileItemArray) {
                        if (!fileItem.isIncomplete()) {
                            FileUtils.unlockFile(fileItem.getFile());
                            libraryManager.getLibraryManagedList().removeFile(fileItem.getFile());
                            FileUtils.delete(fileItem.getFile(), OSUtils.supportsTrash());
                        }
                    }
                }

            });
        }
    };
    

//   private Action gnutellaUnshareAction = new AbstractAction(I18n.tr("Unshare with P2P Network")) {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            final LocalFileItem[] fileItemArray = createFileItemArray();
//            for (LocalFileItem fileItem : fileItemArray) {
//                if (!fileItem.isIncomplete()) {
//                    shareListManager.getGnutellaShareList().removeFile(fileItem.getFile());
//                }
//            }
//            repaintComponent.repaint();
//        }
//    };
//
//    private Action gnutellaShareAction = new AbstractAction(I18n.tr("Share with P2P Network")) {
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            final LocalFileItem[] fileItemArray = createFileItemArray();
//            for (LocalFileItem fileItem : fileItemArray) {
//                if (!fileItem.isIncomplete()) {
//                    shareListManager.getGnutellaShareList().addFile(fileItem.getFile());
//                }
//            }
//            repaintComponent.repaint();      
//        }
//    };
//    
//    private class FriendUnshareAction extends AbstractAction {
//        private Friend friend;
//        public FriendUnshareAction(Friend friend){
//            super(friend.getRenderName());
//            this.friend = friend;
//        }
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            final LocalFileItem[] fileItemArray = createFileItemArray();
//            for (LocalFileItem fileItem : fileItemArray) {
//                if (!fileItem.isIncomplete()) {
//                    shareListManager.getFriendShareList(friend).removeFile(fileItem.getFile());
//                }
//            }
//            repaintComponent.repaint();
//        }
//    };
//
//    private class FriendShareAction extends AbstractAction {
//        private Friend friend;
//        public FriendShareAction(Friend friend){
//            super(friend.getRenderName());
//            this.friend = friend;
//        }
//        @Override
//        public void actionPerformed(ActionEvent e) {
//            final LocalFileItem[] fileItemArray = createFileItemArray();            
//            for (LocalFileItem fileItem : fileItemArray) {
//                if (!fileItem.isIncomplete()) {
//                    FriendFileList friendFileList = shareListManager.getOrCreateFriendShareList(friend);
//                    File file = fileItem.getFile();
//                    friendFileList.addFile(file);
//                }
//            }
//            repaintComponent.repaint();      
//        }
//    };

}
