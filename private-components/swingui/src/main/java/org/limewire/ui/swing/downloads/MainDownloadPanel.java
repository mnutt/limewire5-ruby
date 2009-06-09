package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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
import org.limewire.ui.swing.dnd.DownloadableTransferHandler;
import org.limewire.ui.swing.downloads.table.DownloadTable;
import org.limewire.ui.swing.downloads.table.DownloadTableFactory;
import org.limewire.ui.swing.event.DownloadVisibilityEvent;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.SelectAndScrollDownloadEvent;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

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
    private final Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler;
    private final DownloadListManager downloadListManager;
    
    @Resource private int preferredHeight;
    
    private DownloadTable table;
    
    
	/**
	 * Create the panel
	 */
	@Inject
	public MainDownloadPanel(Provider<DownloadTableFactory> downloadTableFactory, 
	        DownloadMediator downloadMediator,
	        TrayNotifier notifier, 
	        DownloadListManager downloadListManager,
	        Provider<SaveLocationExceptionHandler> saveLocationExceptionHandler) {
	    this.downloadMediator = downloadMediator;
	    this.downloadTableFactory = downloadTableFactory;
	    this.downloadListManager = downloadListManager;
		this.notifier = notifier;
		this.saveLocationExceptionHandler = saveLocationExceptionHandler;
		
		GuiUtils.assignResources(this);
		
		setPreferredSize(new Dimension(getPreferredSize().width, preferredHeight));
    }

	@EventSubscriber
	public void handleSelectAndScroll(SelectAndScrollDownloadEvent event) {
	    table.selectAndScrollTo(event.getSelectedURN());
	    if(getVisibleRect().height < table.getRowHeight()){
	        new DownloadVisibilityEvent(true).publish();
	    }
	}
	    
    
    @Inject
    public void register() {              
        downloadMediator.getDownloadList().addListEventListener(new VisibilityListListener());
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

        setTransferHandler(new DownloadableTransferHandler(downloadListManager, saveLocationExceptionHandler));

        // handle individual completed downloads
        initializeDownloadListeners(downloadListManager);
        
        EventAnnotationProcessor.subscribe(this);
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
            if(!isInitialized){
                initialize();
            }
            
            int downloadCount = listChanges.getSourceList().size();
            if (downloadCount == 0 && isVisible()) {
                new DownloadVisibilityEvent(false).publish();
            } else if (downloadCount > 0 && !isVisible()) {
                new DownloadVisibilityEvent(true).publish();
            }
        }
    }
 
}
