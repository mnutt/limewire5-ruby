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
    private JMenuItem libraryMenuItem;
    private JMenuItem cancelMenuItem;
    private JMenuItem resumeMenuItem;
    private JMenuItem tryAgainMenuItem;
    private JMenuItem propertiesMenuItem;
    private JMenuItem shareMenuItem;
    private JMenuItem playMenuItem;
    private JMenuItem viewMenuItem;
    
    private MenuListener menuListener;
    private DownloadActionHandler actionHandler;
    private DownloadItem downloadItem;
    private AbstractDownloadTable table;

    public DownloadPopupHandler(DownloadActionHandler actionHandler, AbstractDownloadTable table) {
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

        launchMenuItem = new JMenuItem(I18n.tr("Launch File"));
        launchMenuItem.setActionCommand(DownloadActionHandler.LAUNCH_COMMAND);
        launchMenuItem.addActionListener(menuListener);

        removeMenuItem = new JMenuItem(I18n.tr("Remove from List"));
        removeMenuItem.setActionCommand(DownloadActionHandler.REMOVE_COMMAND);
        removeMenuItem.addActionListener(menuListener);

        locateMenuItem = new JMenuItem(I18n.tr("Locate on Disk"));
        locateMenuItem.setActionCommand(DownloadActionHandler.LOCATE_COMMAND);
        locateMenuItem.addActionListener(menuListener);

        libraryMenuItem = new JMenuItem(I18n.tr("Locate in Library"));
        libraryMenuItem.setActionCommand(DownloadActionHandler.LIBRARY_COMMAND);
        libraryMenuItem.addActionListener(menuListener);

        
        propertiesMenuItem = new JMenuItem(I18n.tr("View File Info"));
        propertiesMenuItem.setActionCommand(DownloadActionHandler.PROPERTIES_COMMAND);
        propertiesMenuItem.addActionListener(menuListener);

        previewMenuItem = new JMenuItem(I18n.tr("Preview File"));
        previewMenuItem.setActionCommand(DownloadActionHandler.PREVIEW_COMMAND);
        previewMenuItem.addActionListener(menuListener);

        shareMenuItem = new JMenuItem(I18n.tr("Share File"));
        shareMenuItem.setActionCommand(DownloadActionHandler.SHARE_COMMAND);
        shareMenuItem.addActionListener(menuListener);

        playMenuItem = new JMenuItem(I18n.tr("Play"));
        playMenuItem.setActionCommand(DownloadActionHandler.PLAY_COMMAND);
        playMenuItem.addActionListener(menuListener);

        viewMenuItem = new JMenuItem(I18n.tr("View"));
        viewMenuItem.setActionCommand(DownloadActionHandler.LAUNCH_COMMAND);
        viewMenuItem.addActionListener(menuListener);

    }

    @Override
    public boolean isPopupShowing(int row) {
        return popupMenu.isVisible() && row == popupRow;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        popupRow = getPopupRow(x, y);
        downloadItem = table.getDownloadItem(popupRow);

        popupMenu.removeAll();
        DownloadState state = downloadItem.getState();

        // add pause to all pausable states
        if (state.isPausable()) {
            popupMenu.add(pauseMenuItem);
            popupMenu.addSeparator();
        }

        switch (state) {

        case DONE:
            if (downloadItem.getCategory() != Category.PROGRAM && downloadItem.getCategory() != Category.OTHER) {
                if (downloadItem.getCategory() == Category.AUDIO
                        || downloadItem.getCategory() == Category.VIDEO) {
                    popupMenu.add(playMenuItem).setEnabled(downloadItem.isLaunchable());
                } else if (downloadItem.getCategory() == Category.IMAGE
                        || downloadItem.getCategory() == Category.DOCUMENT) {
                    popupMenu.add(viewMenuItem);
                } else {
                    popupMenu.add(launchMenuItem);
                }
            }
            popupMenu.add(shareMenuItem);
            popupMenu.addSeparator();
            popupMenu.add(locateMenuItem);
            popupMenu.add(libraryMenuItem);
            popupMenu.addSeparator();
            popupMenu.add(removeMenuItem);
            break;

        case CONNECTING:
        case ERROR:
        case FINISHING:
        case LOCAL_QUEUED:
        case REMOTE_QUEUED:
            if (downloadItem.getCategory() != Category.PROGRAM && downloadItem.getCategory() != Category.OTHER) {
                popupMenu.add(previewMenuItem).setEnabled(downloadItem.isLaunchable());
            }
            popupMenu.add(libraryMenuItem);
            popupMenu.add(new JSeparator());
            popupMenu.add(cancelMenuItem);
            break;

        case DOWNLOADING:
            if (downloadItem.getCategory() != Category.PROGRAM && downloadItem.getCategory() != Category.OTHER) {
                popupMenu.add(previewMenuItem).setEnabled(downloadItem.isLaunchable());
            }
            popupMenu.add(libraryMenuItem);
            popupMenu.add(new JSeparator());
            popupMenu.add(cancelMenuItem);
            break;

        case PAUSED:
            popupMenu.add(resumeMenuItem);
            popupMenu.addSeparator();
            if (downloadItem.getCategory() != Category.PROGRAM && downloadItem.getCategory() != Category.OTHER) {
                popupMenu.add(previewMenuItem).setEnabled(downloadItem.isLaunchable());
            }
            popupMenu.add(libraryMenuItem);
            popupMenu.add(new JSeparator());

            popupMenu.add(cancelMenuItem);
            break;

        case STALLED:
            popupMenu.add(tryAgainMenuItem);
            popupMenu.addSeparator();
            if (downloadItem.getCategory() != Category.PROGRAM && downloadItem.getCategory() != Category.OTHER) {
                popupMenu.add(previewMenuItem).setEnabled(downloadItem.isLaunchable());
            }
            popupMenu.add(libraryMenuItem);
            popupMenu.add(new JSeparator());
            popupMenu.add(cancelMenuItem);
            break;
        }

        popupMenu.add(new JSeparator());
        popupMenu.add(propertiesMenuItem);

        popupMenu.show(component, x, y);
    }

    protected int getPopupRow(int x, int y) {
        return table.rowAtPoint(new Point(x, y));
    }

    private class MenuListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            actionHandler.performAction(e.getActionCommand(), downloadItem);
            // must cancel editing
            Component comp = table.getEditorComponent();
            if (comp != null && comp instanceof TableCellEditor) {
                ((TableCellEditor) comp).cancelCellEditing();
            }
        }
    }

}
