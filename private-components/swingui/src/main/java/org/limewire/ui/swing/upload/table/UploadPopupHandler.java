package org.limewire.ui.swing.upload.table;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.TableCellEditor;

import org.limewire.core.api.Category;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.I18n;

public class UploadPopupHandler implements TablePopupHandler {
    private int popupRow = -1;


    private JPopupMenu popupMenu;
    private JMenuItem launchMenuItem;
    private JMenuItem playMenuItem;
    private JMenuItem removeMenuItem;
    private JMenuItem showInLibraryMenuItem;
    private JMenuItem locateMenuItem;
    private JMenuItem cancelMenuItem;
    private JMenuItem propertiesMenuItem;

    private MenuListener menuListener;
    
    private UploadItem menuItem;


    private UploadTable table;


    private UploadActionHandler actionHandler;
    
    public UploadPopupHandler(UploadTable table, UploadActionHandler actionHandler){
        this.table = table;
        this.actionHandler = actionHandler;
        
        popupMenu = new JPopupMenu();
        
        menuListener = new MenuListener();

        showInLibraryMenuItem = new JMenuItem(I18n.tr("Jump to File in Library"));
        showInLibraryMenuItem.setActionCommand(UploadActionHandler.LIBRARY_COMMAND);
        showInLibraryMenuItem.addActionListener(menuListener);        
        
        cancelMenuItem = new JMenuItem(I18n.tr("Cancel Upload"));
        cancelMenuItem.setActionCommand(UploadActionHandler.CANCEL_COMMAND);
        cancelMenuItem.addActionListener(menuListener);

        launchMenuItem = new JMenuItem(I18n.tr("Launch file"));
        launchMenuItem.setActionCommand(UploadActionHandler.LAUNCH_COMMAND);
        launchMenuItem.addActionListener(menuListener);
        
        playMenuItem = new JMenuItem(I18n.tr("Play file"));
        playMenuItem.setActionCommand(UploadActionHandler.PLAY_COMMAND);
        playMenuItem.addActionListener(menuListener);
        
        removeMenuItem = new JMenuItem(I18n.tr("Remove from list"));
        removeMenuItem.setActionCommand(UploadActionHandler.REMOVE_COMMAND);
        removeMenuItem.addActionListener(menuListener);
        
        locateMenuItem = new JMenuItem(I18n.tr("Locate file"));
        locateMenuItem.setActionCommand(UploadActionHandler.LOCATE_COMMAND);
        locateMenuItem.addActionListener(menuListener);
        
        propertiesMenuItem = new JMenuItem(I18n.tr("View File Info"));
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
        menuItem = table.getUploadItem(popupRow);
        
        popupMenu.removeAll();
        UploadState state = menuItem.getState();
        
        if(state == UploadState.DONE || state == UploadState.UNABLE_TO_UPLOAD){
            popupMenu.add(removeMenuItem);
        } else {
            popupMenu.add(cancelMenuItem);
        }
        
        popupMenu.addSeparator();
        
        if (menuItem.getCategory() == Category.VIDEO || menuItem.getCategory() == Category.AUDIO) {
            popupMenu.add(playMenuItem);
        } else if (menuItem.getCategory() != Category.PROGRAM) {
            popupMenu.add(launchMenuItem);
        }
        popupMenu.add(locateMenuItem);
        popupMenu.add(showInLibraryMenuItem);

        popupMenu.addSeparator();
        
        popupMenu.add(propertiesMenuItem);
        
        popupMenu.show(component, x, y);
    }
    
    protected int getPopupRow(int x, int y){
        return table.rowAtPoint(new Point(x, y));
    }
  
    
    private class MenuListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            actionHandler.performAction(e.getActionCommand(), menuItem);
            //must cancel editing
            Component comp = table.getEditorComponent();
            if(comp!=null && comp instanceof TableCellEditor){
                ((TableCellEditor)comp).cancelCellEditing();
            }
        }
    }


}
