package org.limewire.ui.swing.library;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.LayoutManager;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

class LibrarySelectionPanel extends JPanel implements Disposable {
    
    @Resource private Color backgroundColor;
    
    private Map<String, Disposable> map = new HashMap<String, Disposable>();
    
    /**used to fill empty space and hold the info bar at the bottom of the screen. */
    private final JPanel selectionGrow = new JPanel(new MigLayout("fill"));
    
    private final CardLayout cardLayout = new CardLayout();
    private JPanel cardPanel = new JPanel(cardLayout);

    public LibrarySelectionPanel() {
        super(new MigLayout("insets 0, gap 0, fill, wrap, hidemode 3", "[125!]", ""));
        init();
    }
    
    private void init() {
        GuiUtils.assignResources(this);
        setBackground(backgroundColor);
        
        selectionGrow.setOpaque(false);
        selectionGrow.add(cardPanel, "dock south");        
        
        add(selectionGrow, "dock south, aligny baseline, growy");
    }
    

    public void updateLayout(LayoutManager layout) {
        super.setLayout(layout);
        add(selectionGrow, "dock south, aligny baseline, growy");
    }
    
    @SuppressWarnings("unchecked")
    public<T extends FileItem> void addCard(Category category, EventList<T> fileList, FriendFileList friendFileList, FilterList<T> sharedList, boolean isFriendView) {
        InfoPanel panel = new InfoPanel(category, fileList, friendFileList, sharedList, isFriendView);
        map.put(category.name(), panel);
        cardPanel.add(panel, category.name());
    }
    
    /**
	 * Selects which category info panel to show. If category
	 * is null, hides the entire card panel since there is no
     * category visible in this library.
	 */
    public void showCard(Category category) {
        if(category == null) 
            cardPanel.setVisible(false);
        else {
            if(!cardPanel.isVisible())
                cardPanel.setVisible(true);
            cardLayout.show(cardPanel, category.name());
        }
    }
    
    /**
     * Creates a subPanel in the inner nav. This panel displays file information 
     * about the particular category that is currently selected.
     */
    private class InfoPanel<T extends FileItem> extends JPanel implements Disposable, ListEventListener<T>, SettingListener {
        @Resource
        private Color backgroundColor;
        @Resource
        private Color fontColor;
        @Resource
        private Font categoryFont;
        @Resource
        private Font smallFont;
        
        private JLabel categoryLabel;
        private JLabel totalLabel;
        private JLabel sharingLabel;
        private JLabel collectionLabel;
        
        private final EventList<T> fileList;
        private final FilterList<T> sharedList;
        private final FriendFileList friendList;
        private final Category category;
        
        public InfoPanel(Category category, EventList<T> fileList, FriendFileList friendList, FilterList<T> sharedList, boolean isFriendView) {
            super(new MigLayout("fillx, gap 0, insets 5 10 5 0, hidemode 3"));
            
            GuiUtils.assignResources(this);
                        
            this.fileList = fileList;
            this.friendList = friendList;
            this.sharedList = sharedList;
            this.category = category;
            
            categoryLabel = new JLabel(I18n.tr("{0} Info", category));
            categoryLabel.setFont(categoryFont);
            categoryLabel.setForeground(fontColor);

            setBackground(backgroundColor);
            add(categoryLabel, "wrap, gapbottom 2");
            
            initTotalLabel(fileList);
            initSharedList(sharedList);
            if(!isFriendView && sharedList == null) 
                initCollectionListener(category);
        }
        
        /** 
         * Displays the total number of files in this category list. 
         */
        private void initTotalLabel(final EventList<T> fileList) {
            totalLabel = new JLabel();
            totalLabel.setFont(smallFont);
            totalLabel.setForeground(fontColor);
            setTotalLabel(fileList.size());
            add(totalLabel, "wrap");
            fileList.addListEventListener(this);
        }
        
        /**
         * Display the number of files that are shared in this list.
         */
        private void initSharedList(final FilterList<T> fileList) {
            if(fileList != null) {
                sharingLabel = new JLabel();
                sharingLabel.setFont(smallFont);
                sharingLabel.setForeground(fontColor);
                setSharingLabel(fileList.size());
                add(sharingLabel, "wrap");
                fileList.addListEventListener(this);
            }
        }
        
        /**
         * Displays the number of collections that are shared of this category
         */
        private void initCollectionListener(Category category) {
            if(category == Category.AUDIO) {
                createCollectionLabel();
                setCollectionLabel(LibrarySettings.SHARE_NEW_AUDIO_ALWAYS.getValue().length);
                LibrarySettings.SHARE_NEW_AUDIO_ALWAYS.addSettingListener(new SettingListener(){
                    @Override
                    public void settingChanged(SettingEvent evt) {
                        SwingUtilities.invokeLater(new Runnable(){
                            public void run() {
                                setCollectionLabel(LibrarySettings.SHARE_NEW_AUDIO_ALWAYS.getValue().length);                                
                            }
                        });
                    }
                 });
            } else if(category == Category.VIDEO) {
                createCollectionLabel();
                setCollectionLabel(LibrarySettings.SHARE_NEW_VIDEO_ALWAYS.getValue().length);
                LibrarySettings.SHARE_NEW_VIDEO_ALWAYS.addSettingListener(new SettingListener(){
                    @Override
                    public void settingChanged(SettingEvent evt) {
                        SwingUtilities.invokeLater(new Runnable(){
                            public void run() {
                                setCollectionLabel(LibrarySettings.SHARE_NEW_VIDEO_ALWAYS.getValue().length);                                
                            }
                        });
                    }
                 });
            } else if(category == Category.IMAGE) { 
                createCollectionLabel();
                setCollectionLabel(LibrarySettings.SHARE_NEW_IMAGES_ALWAYS.getValue().length);
                LibrarySettings.SHARE_NEW_IMAGES_ALWAYS.addSettingListener(new SettingListener(){
                    @Override
                    public void settingChanged(SettingEvent evt) {
                        SwingUtilities.invokeLater(new Runnable(){
                            public void run() {
                                setCollectionLabel(LibrarySettings.SHARE_NEW_IMAGES_ALWAYS.getValue().length);                                
                            }
                        });
                    }
                 });
            }
        }
        
        private void createCollectionLabel() {
            collectionLabel = new JLabel();
            collectionLabel.setFont(smallFont);
            collectionLabel.setForeground(fontColor);
            setCollectionLabel(0);
            add(collectionLabel, "wrap");
            collectionLabel.setVisible(!LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue());
            LibrarySettings.SNAPSHOT_SHARING_ENABLED.addSettingListener(this);
        }
        
        private void setTotalLabel(int total) {
            totalLabel.setText(I18n.tr("Total: {0}", total));
        }
        
        private void setSharingLabel(int count) {
            if(LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue()) {
                sharingLabel.setText(I18n.tr("Sharing: {0}", count));
            } else {
                switch(category) {
                case AUDIO:
                case VIDEO:
                case IMAGE:
                    if(friendList.isCategoryAutomaticallyAdded(category)) {
                        sharingLabel.setText(I18n.tr("Sharing: all"));
                        break;
                    }
                default:
                    sharingLabel.setText(I18n.tr("Sharing: {0}", count));
                }
            }
        }
        
        private void setCollectionLabel(int count) {
            if(collectionLabel != null)
                collectionLabel.setText(I18n.tr("Sharing Collection: {0}", count));
        }   

        @Override
        public void dispose() {
            if(fileList != null)
                fileList.removeListEventListener(this);
            if(sharedList != null)
                sharedList.removeListEventListener(this);
            if(sharedList == null)
                LibrarySettings.SNAPSHOT_SHARING_ENABLED.removeSettingListener(this);
        }

        @Override
        public void listChanged(ListEvent<T> listChanges) {
            if(fileList != null)
                setTotalLabel(fileList.size());
            if(sharedList != null)
                setSharingLabel(sharedList.size());
        }

        @Override
        public void settingChanged(SettingEvent evt) {
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    collectionLabel.setVisible(!LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue());                    
                }
            });
        }
    }

    @Override
    public void dispose() {
        for(Disposable disposable : map.values())
            disposable.dispose();
    }
}
