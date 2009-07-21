package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.downloads.table.DownloadTable;
import org.limewire.ui.swing.downloads.table.DownloadTableFactory;
import org.limewire.ui.swing.event.DownloadVisibilityEvent;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.SelectAndScrollDownloadEvent;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class MainDownloadPanel extends JPanel {  	
    
    public static final String NAME = "MainDownloadPanel";    
    
    private TrayNotifier notifier;

    private DownloadMediator downloadMediator;
    
    private boolean isInitialized = false;
    private final Provider<DownloadTableFactory> downloadTableFactory;
    private final DownloadListManager downloadListManager;
    private final ArrayList<DownloadVisibilityListener> downloadVisibilityListeners = new ArrayList<DownloadVisibilityListener>();
    
    @Resource private int preferredHeight;
    
    private DownloadTable table;
    
    
    /**
     * Create the panel.
     */
    @Inject
    public MainDownloadPanel(Provider<DownloadTableFactory> downloadTableFactory, 
            DownloadMediator downloadMediator,
            TrayNotifier notifier, 
            DownloadListManager downloadListManager) {
        this.downloadMediator = downloadMediator;
        this.downloadTableFactory = downloadTableFactory;
        this.downloadListManager = downloadListManager;
        this.notifier = notifier;

        GuiUtils.assignResources(this);
        int savedHeight = SwingUiSettings.DOWNLOAD_TRAY_SIZE.getValue();
        int height = savedHeight == 0 ? preferredHeight : savedHeight;
        setPreferredSize(new Dimension(getPreferredSize().width, height));
    }

    @EventSubscriber
	public void handleSelectAndScroll(SelectAndScrollDownloadEvent event) {
        table.selectAndScrollTo(event.getSelectedURN());
        if(getVisibleRect().height < table.getRowHeight()){
            alertDownloadVisibilityListeners(true);
        }
    }

    
    @Inject
    public void register() {              
        downloadMediator.getDownloadList().addListEventListener(new VisibilityListListener());
        DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.addSettingListener(new SettingListener() {
           @Override
            public void settingChanged(SettingEvent evt) {
               SwingUtils.invokeLater(new Runnable() {
                   @Override
                    public void run() {
                       updateVisibility(downloadMediator.getDownloadList());
                    }
               });
            } 
        });
        
        //we have to eagerly initialize the table when the ALWAYS_SHOW_DOWNLOAD_TRAY setting
        //is set to true on startup, otherwise the table space will be empty and the lines will
        //be put in the first time a download comes in, which looks a little weird
        if(DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.getValue()) {
            initialize();
        }
    }
    
    public void addDownloadVisibilityListener(DownloadVisibilityListener listener){
        downloadVisibilityListeners.add(listener);
    }
    
    public void removeDownloadVisibilityListener(DownloadVisibilityListener listener){
        downloadVisibilityListeners.remove(listener);
    }

    //Lazily initialized - initialize() is called when the first downloadItem is added to the list.  
    private void initialize() {
        isInitialized = true;
        setLayout(new BorderLayout());

        table = downloadTableFactory.get().create(downloadMediator.getDownloadList());
        table.setTableHeader(null);
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        add(pane, BorderLayout.CENTER);

        // handle individual completed downloads
        initializeDownloadListeners(downloadListManager);
        
        EventAnnotationProcessor.subscribe(this);
    }
    
    public List<DownloadItem> getSelectedDownloadItems(){
        return table.getSelectedItems();
    }
    
    public int getDefaultPreferredHeight(){
        return preferredHeight;
    }

    
    private void initializeDownloadListeners(final DownloadListManager downloadListManager) {
        // handle individual completed downloads
        downloadListManager.addPropertyChangeListener(new DownloadPropertyListener());

        downloadListManager.getSwingThreadSafeDownloads().addListEventListener(
                new ListEventListener<DownloadItem>() {
                    @Override
                    public void listChanged(ListEvent<DownloadItem> listChanges) {
                        // only show the notification messages if the download panel is not invisible
                        if (!shouldShowNotification()) {
                            return;
                        }

                        while (listChanges.next()) {
                            if (listChanges.getType() == ListEvent.INSERT) {
                                int index = listChanges.getIndex();
                                final DownloadItem downloadItem = listChanges.getSourceList().get(index);
                                notifier.showMessage(createInsertNotification(downloadItem));
                            }
                        }
                        
                    }
                });
    }
    
    private Notification createInsertNotification(final DownloadItem downloadItem){
        return new Notification(I18n.tr("Download Started"),
                downloadItem.getFileName(), new AbstractAction(I18n.tr("Show")) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        setSize(getPreferredSize().width, preferredHeight);
                        table.selectAndScrollTo(downloadItem.getUrn());
                    }
                });
    }
    
    private boolean shouldShowNotification(){
        return !isShowing() || getVisibleRect().height < table.getRowHeight();
    }
    


    private class DownloadPropertyListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent event) {
            if (event.getPropertyName().equals(DownloadListManager.DOWNLOAD_COMPLETED)) {
                final DownloadItem downloadItem = (DownloadItem) event.getNewValue();
                notifier.showMessage(new Notification(I18n.tr("Download Complete"), downloadItem.getFileName(), 
                        new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ActionMap map = Application.getInstance().getContext().getActionManager()
                                .getActionMap();
                        map.get("restoreView").actionPerformed(e);

                        if (downloadItem.isLaunchable()) {
                            DownloadItemUtils.launch(downloadItem);
                        }
                    }
                }));
            }
        }
    }

    /**
     * Initializes the download panel contents the first time the list changes (when the first DownloadItem is added).  
     * Adjusts visibility of the panel depending on whether or not the list is empty.
     */
    private class VisibilityListListener implements ListEventListener<DownloadItem> {
      
        @Override
        public void listChanged(ListEvent<DownloadItem> listChanges) {
            EventList sourceList = listChanges.getSourceList(); 
            updateVisibility(sourceList);
        }
    }
    
    private void updateVisibility(EventList sourceList) {
        if(!isInitialized){
            initialize();
        }
        
        int downloadCount = sourceList.size();
        
        if(DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.getValue() && !isVisible()){
            alertDownloadVisibilityListeners(true);
        } else if(DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.getValue()){
            //Do nothing - it is already set.
            return;
        } else if (downloadCount == 0 && isVisible()) {
            alertDownloadVisibilityListeners(false);
        } else if (downloadCount > 0 && !isVisible()) {
            alertDownloadVisibilityListeners(true);
        }
    }
    
    /**
     * Ignores DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY
     */
    private void alertDownloadVisibilityListeners(boolean isVisible) {
        for (DownloadVisibilityListener listener : downloadVisibilityListeners) {
            listener.updateVisibility(new DownloadVisibilityEvent(isVisible));
        }

    }
}
