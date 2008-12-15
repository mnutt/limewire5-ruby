package org.limewire.ui.swing.downloads.table;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.table.TableCellEditor;

import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.table.TablePopupHandler;
import org.limewire.ui.swing.util.I18n;

public class DownloadPopupHandler implements TablePopupHandler {
    private int popupRow = -1;


    private JPopupMenu popupMenu;
    private JMenuItem launchMenuItem;
    private JMenuItem previewMenuItem;
    private JMenuItem removeMenuItem;
    private JMenuItem pauseMenuItem;
    private JMenuItem locateMenuItem;
    private JMenuItem cancelMenuItem;
    private JMenuItem resumeMenuItem;
    private JMenuItem tryAgainMenuItem;
    private JMenuItem propertiesMenuItem;
    private JMenuItem shareMenuItem;

    private MenuListener menuListener;


    private DownloadActionHandler actionHandler;
    
    private DownloadItem menuItem;


    private AbstractDownloadTable table;
    
    public DownloadPopupHandler(DownloadActionHandler actionHandler, AbstractDownloadTable table){
        this.actionHandler = actionHandler;
        this.table = table;
        
        popupMenu = new JPopupMenu();
        
        menuListener = new MenuListener();

        pauseMenuItem = new JMenuItem(I18n.tr("Pause"));
        pauseMenuItem.setActionCommand(DownloadActionHandler.PAUSE_COMMAND);
        pauseMenuItem.addActionListener(menuListener);
        
        
        cancelMenuItem = new JMenuItem(I18n.tr("Cancel Download"));
        cancelMenuItem.setActionCommand(DownloadActionHandler.CANCEL_COMMAND);
        cancelMenuItem.addActionListener(menuListener);

        
        resumeMenuItem = new JMenuItem(I18n.tr("Resume"));
        resumeMenuItem.setActionCommand(DownloadActionHandler.RESUME_COMMAND);
        resumeMenuItem.addActionListener(menuListener);

        
        tryAgainMenuItem = new JMenuItem(I18n.tr("Try Again"));
        tryAgainMenuItem.setActionCommand(DownloadActionHandler.TRY_AGAIN_COMMAND);
        tryAgainMenuItem.addActionListener(menuListener);

        launchMenuItem = new JMenuItem(I18n.tr("Launch file"));
        launchMenuItem.setActionCommand(DownloadActionHandler.LAUNCH_COMMAND);
        launchMenuItem.addActionListener(menuListener);
        
        removeMenuItem = new JMenuItem(I18n.tr("Remove from list"));
        removeMenuItem.setActionCommand(DownloadActionHandler.REMOVE_COMMAND);
        removeMenuItem.addActionListener(menuListener);
        
        locateMenuItem = new JMenuItem(I18n.tr("Locate file"));
        locateMenuItem.setActionCommand(DownloadActionHandler.LOCATE_COMMAND);
        locateMenuItem.addActionListener(menuListener);
        
        propertiesMenuItem = new JMenuItem(I18n.tr("View File Info"));
        propertiesMenuItem.setActionCommand(DownloadActionHandler.PROPERTIES_COMMAND);
        propertiesMenuItem.addActionListener(menuListener);

        previewMenuItem =  new JMenuItem(I18n.tr("Preview File"));
        previewMenuItem.setActionCommand(DownloadActionHandler.PREVIEW_COMMAND);
        previewMenuItem.addActionListener(menuListener);
        
        shareMenuItem =  new JMenuItem(I18n.tr("Share File"));
        shareMenuItem.setActionCommand(DownloadActionHandler.SHARE_COMMAND);
        shareMenuItem.addActionListener(menuListener);
    }
    
    @Override
    public boolean isPopupShowing(int row) {
        return popupMenu.isVisible() && row == popupRow;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        popupRow = getPopupRow(x, y);
        menuItem = table.getDownloadItem(popupRow);
       
        popupMenu.removeAll();
        DownloadState state = menuItem.getState();
        
        //add pause to all pausable states
        if(state.isPausable()){
            popupMenu.add(pauseMenuItem);
        }

        switch (state) {

        case DONE:
            if (isLaunchable(menuItem)) {
                popupMenu.add(launchMenuItem);
            }
            popupMenu.add(shareMenuItem);
            popupMenu.add(locateMenuItem);
            popupMenu.add(removeMenuItem);
            break;

        case CONNECTING:
        case ERROR:
        case FINISHING:
        case LOCAL_QUEUED:
        case REMOTE_QUEUED:
            if (isLaunchable(menuItem)) {
                popupMenu.add(previewMenuItem);
            }
            popupMenu.add(locateMenuItem);
            popupMenu.add(new JSeparator());
            popupMenu.add(cancelMenuItem);
            break;

        case DOWNLOADING:
            if (isLaunchable(menuItem)) {
                popupMenu.add(previewMenuItem);
            }
            popupMenu.add(locateMenuItem);
            popupMenu.add(new JSeparator());
            popupMenu.add(cancelMenuItem);
            break;

        case PAUSED:
            popupMenu.add(resumeMenuItem);
            if (isLaunchable(menuItem)) {
                popupMenu.add(previewMenuItem);
            }
            popupMenu.add(locateMenuItem);
            popupMenu.add(new JSeparator());

            popupMenu.add(cancelMenuItem);
            break;

        case STALLED:
            popupMenu.add(tryAgainMenuItem);
            if (isLaunchable(menuItem)) {
                popupMenu.add(previewMenuItem);
            }
            popupMenu.add(locateMenuItem);
            popupMenu.add(new JSeparator());
            popupMenu.add(cancelMenuItem);
            break;
        }

        popupMenu.add(new JSeparator());
        popupMenu.add(propertiesMenuItem);

        popupMenu.show(component, x, y);
    }
    
    protected int getPopupRow(int x, int y){
        return table.rowAtPoint(new Point(x, y));
    }
    private boolean isLaunchable(DownloadItem item){
        return item.getCategory() != Category.PROGRAM && item.isLaunchable();
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
