package org.limewire.ui.swing.upload.table;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.TableCellEditor;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.I18n;


public class UploadPopupHandler implements TablePopupHandler {
    private int popupRow = -1;

    private final LibraryManager libraryManager;

    private JPopupMenu popupMenu;
    private JMenuItem launchMenuItem;
    private JMenuItem playMenuItem;
    private JMenuItem removeMenuItem;
    private JMenuItem showInLibraryMenuItem;
    private JMenuItem locateOnDiskMenuItem;
    private JMenuItem cancelMenuItem;
    private JMenuItem propertiesMenuItem;

    private MenuListener menuListener;
    
    private WeakReference<UploadItem> uploadItemReference;


    private UploadTable table;


    private UploadActionHandler actionHandler;
    
    public UploadPopupHandler(UploadTable table, UploadActionHandler actionHandler, LibraryManager libraryManager){
        this.libraryManager = libraryManager;
        this.table = table;
        this.actionHandler = actionHandler;
        
        popupMenu = new JPopupMenu();
        
        menuListener = new MenuListener();

        showInLibraryMenuItem = new JMenuItem(I18n.tr("Locate in Library"));
        showInLibraryMenuItem.setActionCommand(UploadActionHandler.LIBRARY_COMMAND);
        showInLibraryMenuItem.addActionListener(menuListener);        
        
        cancelMenuItem = new JMenuItem(I18n.tr("Cancel"));
        cancelMenuItem.setActionCommand(UploadActionHandler.CANCEL_COMMAND);
        cancelMenuItem.addActionListener(menuListener);

        launchMenuItem = new JMenuItem(I18n.tr("Launch File"));
        launchMenuItem.setActionCommand(UploadActionHandler.LAUNCH_COMMAND);
        launchMenuItem.addActionListener(menuListener);
        
        playMenuItem = new JMenuItem(I18n.tr("Play File"));
        playMenuItem.setActionCommand(UploadActionHandler.PLAY_COMMAND);
        playMenuItem.addActionListener(menuListener);
        
        removeMenuItem = new JMenuItem(I18n.tr("Remove from List"));
        removeMenuItem.setActionCommand(UploadActionHandler.REMOVE_COMMAND);
        removeMenuItem.addActionListener(menuListener);
        
        locateOnDiskMenuItem = new JMenuItem(I18n.tr("Locate on Disk"));
        locateOnDiskMenuItem.setActionCommand(UploadActionHandler.LOCATE_ON_DISK_COMMAND);
        locateOnDiskMenuItem.addActionListener(menuListener);
        
        propertiesMenuItem = new JMenuItem(I18n.tr("View File Info..."));
        propertiesMenuItem.setActionCommand(UploadActionHandler.PROPERTIES_COMMAND);
        propertiesMenuItem.addActionListener(menuListener);
    }
    
    @Override
    public boolean isPopupShowing(int row) {
        return popupMenu.isVisible() && row == popupRow;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        popupRow = getPopupRow(x, y);
        UploadItem uploadItem = table.getUploadItem(popupRow);
        
        popupMenu.removeAll();
        UploadState state = uploadItem.getState();
        
        if(state == UploadState.BROWSE_HOST || state == UploadState.BROWSE_HOST_DONE || state == UploadState.DONE || state == UploadState.UNABLE_TO_UPLOAD){
            popupMenu.add(removeMenuItem);
        } else {
            popupMenu.add(cancelMenuItem);
        }
        
        if (state != UploadState.BROWSE_HOST && state != UploadState.BROWSE_HOST_DONE) {
            popupMenu.addSeparator();

            if (uploadItem.getCategory() == Category.VIDEO || uploadItem.getCategory() == Category.AUDIO) {
                popupMenu.add(playMenuItem);
            } else if (uploadItem.getCategory() != Category.PROGRAM && uploadItem.getCategory() != Category.OTHER) {
                popupMenu.add(launchMenuItem);
            }
            popupMenu.add(locateOnDiskMenuItem);
            popupMenu.add(showInLibraryMenuItem).setEnabled(libraryManager.getLibraryManagedList().contains(uploadItem.getUrn()));

            popupMenu.addSeparator();

            popupMenu.add(propertiesMenuItem);
        }
        
        popupMenu.show(component, x, y);
        
        uploadItemReference = new WeakReference<UploadItem>(uploadItem);
    }
    
    protected int getPopupRow(int x, int y){
        return table.rowAtPoint(new Point(x, y));
    }
  
    
    private class MenuListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            UploadItem uploadItem = uploadItemReference.get();
            if (uploadItem != null) {
                actionHandler.performAction(e.getActionCommand(), uploadItem);
            }
            //must cancel editing
            Component comp = table.getEditorComponent();
            if(comp!=null && comp instanceof TableCellEditor){
                ((TableCellEditor)comp).cancelCellEditing();
            }
        }
    }


}
