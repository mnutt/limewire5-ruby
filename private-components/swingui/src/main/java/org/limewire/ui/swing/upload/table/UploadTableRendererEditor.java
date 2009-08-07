package org.limewire.ui.swing.upload.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXHyperlink;
import org.limewire.core.api.upload.UploadErrorState;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.api.upload.UploadItem.BrowseType;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimeProgressBar;
import org.limewire.ui.swing.components.RemoteHostWidget;
import org.limewire.ui.swing.components.RemoteHostWidgetFactory;
import org.limewire.ui.swing.components.RemoteHostWidget.RemoteWidgetType;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

public class UploadTableRendererEditor extends TableRendererEditor {
    
    private NumberFormat formatter = new DecimalFormat("0.00");
    private CategoryIconManager categoryIconManager;
    
    private JLabel statusLabel;
    private JLabel nameLabel;
    private JLabel iconLabel;
    private RemoteHostWidget browseNameLabel;
    private JXButton cancelButton;
    private JXHyperlink removeLink;
    private WeakReference<UploadItem> editItemReference;
    private LimeProgressBar progressBar;
    private JLabel timeLabel;
    
    @Resource
    private Color linkColor;
    @Resource private Font statusFont;
    @Resource private Font titleFont;
    
    @Resource private Icon cancelIcon;
    @Resource private Icon cancelIconRollover;
    @Resource private Icon cancelIconPressed;
    @Resource private Icon friendBrowseHostIcon;
    @Resource private Icon p2pBrowseHostIcon;
    

    public UploadTableRendererEditor(CategoryIconManager categoryIconManager, ProgressBarDecorator progressBarFactory,
            RemoteHostWidgetFactory remoteHostWidgetFactory){
        GuiUtils.assignResources(this);
        setLayout(new MigLayout("fill, insets 0 0 0 0 , gap 0, novisualpadding"));
        this.categoryIconManager = categoryIconManager;
        
        initializeComponents(progressBarFactory, remoteHostWidgetFactory);
        
        addComponents();
    }


    public void setActionHandler(final UploadActionHandler actionHandler){
        cancelButton.setActionCommand(UploadActionHandler.CANCEL_COMMAND);

        Action cancelAction = new AbstractAction(I18n.tr("Cancel")) {
            @Override
            public void actionPerformed(ActionEvent e) {
               actionHandler.performAction(cancelButton.getActionCommand(), editItemReference.get());
               cancelCellEditing();
            }
        };
        
        cancelButton.addActionListener(cancelAction);
        
        removeLink.setActionCommand(UploadActionHandler.REMOVE_COMMAND);

        Action removeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
               actionHandler.performAction(removeLink.getActionCommand(), editItemReference.get());
               cancelCellEditing();
            }
        };
        
        removeLink.addActionListener(removeAction);
    }

    @Override
    public Component doTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if(value instanceof UploadItem) {
            update((UploadItem)value);
            return this;
        } else {
            return emptyPanel;
        }
    }

    @Override
    public Component doTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        if(value instanceof UploadItem) {
            editItemReference = new WeakReference<UploadItem>((UploadItem)value);
            update(editItemReference.get());
            return this;
        } else {
            return emptyPanel;
        }
    }
    
    private void initializeComponents(ProgressBarDecorator progressBarDecorator,
            RemoteHostWidgetFactory remoteHostWidgetFactory){
        
      //string parameter ensures proper sizing
        nameLabel = new JLabel("NAME");
        nameLabel.setFont(titleFont);
        
        iconLabel = new JLabel();
        
      //string parameter ensures proper sizing
        statusLabel = new JLabel(I18n.tr("Status"));
        statusLabel.setFont(statusFont);
        
        browseNameLabel = remoteHostWidgetFactory.create(RemoteWidgetType.UPLOAD);
        
        cancelButton = new IconButton(cancelIcon, cancelIconRollover, cancelIconPressed);
        
        removeLink = new JXHyperlink();
        removeLink.setText("<HTML><U>" + I18n.tr("Remove") + "</U></HTML>");
        removeLink.setForeground(linkColor);        
        removeLink.setClickedColor(linkColor);
        removeLink.setFont(statusFont);
        
        //string parameter ensures proper sizing
        timeLabel = new JLabel("TIME");
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        
        progressBar = new LimeProgressBar();
        progressBarDecorator.decoratePlain(progressBar);
        Dimension size = new Dimension(400, 16);
        progressBar.setPreferredSize(size);
        progressBar.setMinimumSize(size);
        progressBar.setMaximumSize(size);
        
        setOpaque(false);
    }
    
    private void addComponents() {
        add(iconLabel, "gapleft 10, alignx left, aligny 50%, spany 3, hidemode 3");
        add(nameLabel, "gapleft 10, gaptop 5, aligny bottom, hidemode 2");
        add(cancelButton, "gapright 10, alignx right, aligny 50%, spany 3, push, wrap");
        add(progressBar, "gapleft 10, , gaptop 2, hidemode 3, wrap");
        add(browseNameLabel, "split 4, gapleft 10, gapbottom 5, gaptop 2, aligny top");
        add(statusLabel, "gapleft 10, gapbottom 5, gaptop 2, aligny top");
        add(timeLabel, "push, gaptop 2, gapbottom 5, aligny top, alignx right, hidemode 1");
        add(removeLink, "push, gaptop 2, gapbottom 5, aligny top, alignx right, hidemode 1");
    }
    
    private void update(UploadItem item){
        if (item == null) {
            return;
        }
        
        nameLabel.setVisible(!isBrowseHost(item));
        if (nameLabel.isVisible()) {
            nameLabel.setText(item.getFileName());
        }

        if(item.getUploadItemType() == UploadItemType.GNUTELLA) {
            browseNameLabel.setVisible(true);
            browseNameLabel.setPerson(item.getRemoteHost());
        } else {
            browseNameLabel.setVisible(false);
        }
        statusLabel.setText(getMessage(item));

        if (UploadItemType.GNUTELLA == item.getUploadItemType()) {

            progressBar.setVisible(item.getState() == UploadState.UPLOADING);
            if (progressBar.isVisible()) {
                progressBar.setValue((int) (100 * item.getTotalAmountUploaded() / item
                        .getFileSize()));
            }

            timeLabel.setVisible(item.getState() == UploadState.UPLOADING);
            if (timeLabel.isVisible()) {
                timeLabel.setText(CommonUtils.seconds2time(item.getRemainingUploadTime()));
            }

        } else {
            progressBar.setVisible(false);
            timeLabel.setVisible(false);
        }

        if (item.getState() == UploadState.UPLOADING) {
            nameLabel.setIcon(categoryIconManager.getIcon(item.getCategory()));
            iconLabel.setVisible(false);
        } else {
            nameLabel.setIcon(null);
            if (isBrowseHost(item)) {
                if (item.getBrowseType() == BrowseType.FRIEND) {
                    iconLabel.setIcon(friendBrowseHostIcon);
                } else {
                    iconLabel.setIcon(p2pBrowseHostIcon);
                }
            } else {
                iconLabel.setIcon(categoryIconManager.getIcon(item.getCategory()));
            }
            iconLabel.setVisible(true);
        }
        
        removeLink.setVisible(item.getState() == UploadState.UNABLE_TO_UPLOAD);
        
        if(item.getState() == UploadState.DONE || isBrowseHost(item)){
            cancelButton.setActionCommand(UploadActionHandler.REMOVE_COMMAND);
        } else {
            cancelButton.setActionCommand(UploadActionHandler.CANCEL_COMMAND);
        }
    }
    
    private boolean isBrowseHost(UploadItem item){
       return item.getState() == UploadState.BROWSE_HOST || item.getState() == UploadState.BROWSE_HOST_DONE;
    }
    
    private String getMessage(UploadItem item){
        switch (item.getState()){
        case BROWSE_HOST:
        case BROWSE_HOST_DONE:
            return I18n.tr("Library was browsed");
        case DONE:
            return I18n.tr("Done uploading");
        case UPLOADING:
            
            if(UploadItemType.BITTORRENT == item.getUploadItemType()) {
                int numConnections = item.getNumUploadConnections();
                
                long fileSize = item.getFileSize() == 0 ? 1 : item.getFileSize();
                String ratio = formatter.format(item.getTotalAmountUploaded()/(double)fileSize);
                if(numConnections == 1) {
                    return I18n.tr("Connected to {0} P2P user, uploading at {1} - Ratio {2}", numConnections, GuiUtils.rate2speed(item.getUploadSpeed()), ratio);
                } else {
                    return I18n.tr("Connected to {0} P2P users, uploading at {1} - Ratio {2}", numConnections, GuiUtils.rate2speed(item.getUploadSpeed()), ratio);
                }
            } else {
                return I18n.tr("Uploading - {0} of {1}({2})", GuiUtils.toUnitbytes(item.getTotalAmountUploaded()), 
                        GuiUtils.toUnitbytes(item.getFileSize()), 
                        GuiUtils.rate2speed(item.getUploadSpeed()));
            }
        case QUEUED:
            // {0}: number of uploads before this upload that have to finish
            return I18n.trn("Waiting for {0} upload to finish", "Waiting for {0} uploads to finish", item.getQueuePosition());
        case WAITING:
            return I18n.tr("Waiting for connections...");
        case UNABLE_TO_UPLOAD:
            return getErrorMessage(item.getErrorState());        
        }
        return null;
    }
    
    private String getErrorMessage(UploadErrorState errorState){
        switch(errorState){
        case FILE_ERROR:
            return I18n.tr("Unable to upload: file error"); 
        case INTERRUPTED:
            return I18n.tr("Unable to upload: transfer interrupted"); 
        case LIMIT_REACHED:
            return I18n.tr("Unable to upload: upload limit reached"); 
        }
        return null;
    }

}
