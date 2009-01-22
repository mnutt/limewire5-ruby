package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TooManyListenersException;
import java.util.Set;
import java.util.HashSet;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.effect.LayerEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;
import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.dnd.GhostDropTargetListener;
import org.limewire.ui.swing.dnd.MyLibraryTransferHandler;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.player.PlayerPanel;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class MyLibraryPanel extends LibraryPanel implements EventListener<FriendEvent> {
    
    @Resource(key="LibraryPanel.selectionPanelBackgroundOverride")
    private Color selectionPanelBackgroundOverride = null;
    @Resource
    private Icon closeButton;
    @Resource
    private Icon closeHoverButton;
    
    private final LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;
    private final PlayerPanel playerPanel;
    private final LibraryManager libraryManager;
    private final LibraryNavigator libraryNavigator;
    private final Map<Category, LibraryOperable<LocalFileItem>> selectableMap;
    private final ShareWidgetFactory shareFactory;    
   
    private ShareWidget<Category> categoryShareWidget = null;
    private ShareWidget<LocalFileItem[]> multiShareWidget = null;
    
    /** Map of JXLayers and categories they exist in */
    private Map<Category, JXLayer> map = new HashMap<Category, JXLayer>();
    
    private Timer repaintTimer;
    private ListenerSupport<XMPPConnectionEvent> connectionListeners;
    private final ListenerSupport<FriendEvent> knownFriendsListeners;
    
    // set of known friends helps keep correct share numbers
    private final Set<String> knownFriends;

    @Inject
    public MyLibraryPanel(LibraryManager libraryManager,
                          LibraryNavigator libraryNavigator,
                          IconManager iconManager,
                          LibraryTableFactory tableFactory,
                          CategoryIconManager categoryIconManager,
                          ShareWidgetFactory shareFactory,
                          LimeHeaderBarFactory headerBarFactory,
                          PlayerPanel player, 
                          GhostDragGlassPane ghostPane,
                          ListenerSupport<XMPPConnectionEvent> connectionListeners,
                          ShareListManager shareListManager,
                          @Named("known") ListenerSupport<FriendEvent> knownFriendsListeners) {
        
        super(headerBarFactory);
        
        GuiUtils.assignResources(this);
        
        this.libraryManager = libraryManager;
        this.libraryNavigator = libraryNavigator;
        this.tableFactory = tableFactory;
        this.categoryIconManager = categoryIconManager;    
        this.shareFactory = shareFactory;
        this.playerPanel = player;
        this.selectableMap = new EnumMap<Category, LibraryOperable<LocalFileItem>>(Category.class);
        this.connectionListeners = connectionListeners;
        
        if (selectionPanelBackgroundOverride != null) { 
            getSelectionPanel().setBackground(selectionPanelBackgroundOverride);
        }

        getHeaderPanel().setText(I18n.tr("My Library"));
        
        categoryShareWidget = shareFactory.createCategoryShareWidget();
        multiShareWidget = shareFactory.createMultiFileShareWidget();
        createMyCategories(libraryManager.getLibraryManagedList());
        selectFirstVisible();

        this.knownFriends = new HashSet<String>();
        this.knownFriends.add(Friend.P2P_FRIEND_ID);
        this.knownFriendsListeners = knownFriendsListeners;
        this.knownFriendsListeners.addListener(this);
        getSelectionPanel().updateCollectionShares(knownFriends);
        
        addHeaderComponent(playerPanel, "cell 0 0, grow");
        playerPanel.setMaximumSize(new Dimension(999,999));
        playerPanel.setPreferredSize(new Dimension(999,999));

        
        setTransferHandler(new MyLibraryTransferHandler(null, libraryManager.getLibraryManagedList()));
        try {
            getDropTarget().addDropTargetListener(new GhostDropTargetListener(this,ghostPane));
        } catch (TooManyListenersException ignoreException) {            
        }      
        
        shareListManager.getCombinedShareList().getModel().addListEventListener(new ListEventListener<LocalFileItem>(){
            @Override
            public void listChanged(ListEvent<LocalFileItem> listChanges) {
                //coalesces repaint calls. Updates usually come in bulk, ie you sign on/off,
                // share a collection, etc..
                if(repaintTimer.isRunning())
                    repaintTimer.restart();
                else
                    repaintTimer.start();
            }
        });
        
        repaintTimer = new Timer(250, new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                MyLibraryPanel.this.repaint();
                repaintTimer.stop();
            }
        });
    }

    @Override
    public void handleEvent(FriendEvent event) {
        Friend friend = event.getSource();

        switch (event.getType()) {
            case ADDED:
                knownFriends.add(friend.getId());
                break;
            case REMOVED:
            case DELETE:
                knownFriends.remove(friend.getId());
                break;
            default:
                return;
        }
        getSelectionPanel().updateCollectionShares(knownFriends);
    }
    
    private void createMyCategories(LibraryFileList libraryFileList) {
        for(Category category : Category.getCategoriesInOrder()) {        
            CategorySelectionCallback callback = null;
            if (category == Category.AUDIO) {
                callback = new CategorySelectionCallback() {
                    @Override
                    public void categorySelected(Category category, boolean state) {
                        playerPanel.setVisible(state);
                    }
                };
            }
            
            FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(libraryFileList.getSwingModel(), new CategoryFilter(category));
            addCategory(categoryIconManager.getIcon(category), category, 
                    createMyCategoryAction(category, filtered), filtered, callback);
            addDisposable(filtered);
            addLibraryInfoBar(category, filtered);
        }
    }

    private JComponent createMyCategoryAction(final Category category, EventList<LocalFileItem> filtered) {        
        //TODO: can this be a singleton??? 
        final ShareWidget<File> fileShareWidget = shareFactory.createFileShareWidget();
        addDisposable(fileShareWidget);             
        JScrollPane scrollPane;        
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));
        if (category != Category.IMAGE) {
            LibraryTable<LocalFileItem> table = tableFactory.createMyTable(category, filterList);
            table.enableMyLibrarySharing(fileShareWidget);
            table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(getTableModel(table)));
            selectableMap.put(category, table);
            scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());    
            addDisposable(table);
        } else { //Category.IMAGE 
            scrollPane = new JScrollPane();
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            LibraryImagePanel imagePanel = tableFactory.createImagePanel(filterList, scrollPane, fileShareWidget);
            selectableMap.put(category, imagePanel);
            scrollPane.setViewportView(imagePanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());            
            addDisposable(imagePanel);
        }           
        // only do this if the message hasn't been shown before
        if(SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.getValue() == true) {
            connectionListeners.addListener(new EventListener<XMPPConnectionEvent>() {
                @Override
                @SwingEDTEvent
                public void handleEvent(XMPPConnectionEvent event) {
                    switch(event.getType()) { 
                    case CONNECTED:
                        if(SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.getValue() == true) {
                            JPanel panel = new JPanel(new MigLayout("fill"));
                            panel.setOpaque(false);
                            panel.add(getMessageComponent(), "align 50% 40%");
                            JXLayer layer = map.get(category);
                            layer.getGlassPane().add(panel);
                            layer.getGlassPane().setVisible(true);
                            if(!SwingUiSettings.HAS_LOGGED_IN_AND_SHOWN_LIBRARY.getValue()) {
                                libraryNavigator.selectLibrary();
                                SwingUiSettings.HAS_LOGGED_IN_AND_SHOWN_LIBRARY.setValue(true);
                            }
                        }
                    }
                }
            });
            // this shouldn't be necesarry, the panel above isn't fill the viewport if we don't set this at
            // jxlayer creation time. 
            LockableUI blurUI = new LockedUI(category);
            JXLayer<JComponent> jxlayer = new JXLayer<JComponent>(scrollPane, blurUI);
            map.put(category, jxlayer);
            return jxlayer;
        }
        return scrollPane;
    }
    
    @Override
    protected <T extends FileItem> JComponent createCategoryButton(Action action, Category category, FilterList<T> filteredAllFileList) {
        MySelectionPanel component = new MySelectionPanel(action, new ShareCategoryAction(category), category, this);
        addNavigation(component.getButton());
        return component;
    }
    
    @SuppressWarnings("unchecked")
    private LibraryTableModel<LocalFileItem> getTableModel(LibraryTable table){
        return (LibraryTableModel<LocalFileItem>)table.getModel();
    }   
    
    @Override
    public void dispose() {
        super.dispose();
        
        categoryShareWidget.dispose();
        multiShareWidget.dispose();
        knownFriendsListeners.removeListener(this);
    }
    
    private static class MyLibraryDoubleClickHandler implements TableDoubleClickHandler{
        private LibraryTableModel<LocalFileItem> model;

        public MyLibraryDoubleClickHandler(LibraryTableModel<LocalFileItem> model){
            this.model = model;
        }

        @Override
        public void handleDoubleClick(int row) {
            File file = model.getFileItem(row).getFile();
            switch (model.getFileItem(row).getCategory()){
            case AUDIO:
                PlayerUtils.playOrLaunch(file);
                break;
            case OTHER:
            case PROGRAM:
                NativeLaunchUtils.launchExplorer(file);
                break;
            case IMAGE:
                //TODO: image double click
            case VIDEO:
            case DOCUMENT:
                NativeLaunchUtils.safeLaunchFile(file);
            }
        }
    }
    
    /**
     * Display the Share Collection widget when pressed
     */
    private class ShareCategoryAction extends AbstractAction {

        private Category category;
        
        public ShareCategoryAction(Category category) {
            this.category = category;
            
            putValue(Action.NAME, I18n.tr("share"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Share collection"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if(LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue()) {
                SelectAllable<LocalFileItem> selectAllable = selectableMap.get(category);
                selectAllable.selectAll();
                List<LocalFileItem> selectedItems = selectAllable.getSelectedItems();

                if (selectedItems.size() > 0) {
                    multiShareWidget.setShareable(selectedItems.toArray(new LocalFileItem[selectedItems.size()]));
                    multiShareWidget.show(null);
                } else {
                   JPopupMenu popup = new JPopupMenu();
                   popup.add(new JLabel(I18n.tr("Add files to My Library from Tools > Options to share them")));
                   //move popup 15 pixels to the right so the mouse doesn't obscure the first word
                   popup.show(MyLibraryPanel.this, getMousePosition().x + 15, getMousePosition().y);
                }
            } else {
                categoryShareWidget.setShareable(category);
                categoryShareWidget.show((JComponent)e.getSource());
            }
        }
    }
    
    //TODO: use a button painter and JXButton
    private static class MySelectionPanel extends JPanel {
        @Resource Color selectedBackground;
        @Resource Color selectedTextColor;
        @Resource Color textColor;
        @Resource Font shareButtonFont;
        
        private JButton button;
        private HyperlinkButton shareButton;
        private LibraryPanel libraryPanel;
        
        public MySelectionPanel(Action action, Action shareAction, Category category, LibraryPanel panel) {
            super(new MigLayout("insets 0, fill, hidemode 3"));

            this.libraryPanel = panel;
            
            GuiUtils.assignResources(this);
            setOpaque(false);

            button = new JButton(action);           
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(2,8,2,0));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setOpaque(false);
            button.getAction().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        if(Boolean.TRUE.equals(evt.getNewValue())) {
                            setOpaque(true);
                            setBackground(selectedBackground);
                            button.setForeground(selectedTextColor);
                            if(shareButton != null) {
                                shareButton.setVisible(true);
                            }
                        } else {
                            setOpaque(false);
                            button.setForeground(textColor);
                            if(shareButton != null) {
                                shareButton.setVisible(false);
                            }
                        }
                        repaint();
                    } else if(evt.getPropertyName().equals("enabled")) {
                        boolean value = (Boolean)evt.getNewValue();
                        setVisible(value);
                        //select first category if this category is hidden
                        if(value == false && button.getAction().getValue(Action.SELECTED_KEY) != null && 
                                button.getAction().getValue(Action.SELECTED_KEY).equals(Boolean.TRUE)) {
                            libraryPanel.selectFirstVisible();
                        }
                    }
                }
                    
            });
            
            add(button, "growx, push");
            
            // only add a share category button if its an audio/video/image category
            if(category == Category.AUDIO || category == Category.VIDEO || category == Category.IMAGE) {
                shareButton = new HyperlinkButton(shareAction);
                shareButton.setBorder(BorderFactory.createEmptyBorder(2,0,2,4));
                shareButton.setVisible(false);
                shareButton.setFont(shareButtonFont);
                add(shareButton, "wrap");
            }
        }
        
        public JButton getButton() {
            return button;
        }
    }

    public LibraryFileList getLibrary() {
        return libraryManager.getLibraryManagedList();
    }

    public void selectItem(File file, Category category) {
        select(category);
        selectableMap.get(category).selectAndScrollTo(file);
    }

    public File getNextItem(File file, Category category) {
        return selectableMap.get(category).getNextItem(file);
    }

    public File getPreviousItem(File file, Category category) {
        return selectableMap.get(category).getPreviousItem(file);
    }

    public void selectItem(URN urn, Category category) {
        select(category);
        selectableMap.get(category).selectAndScrollTo(urn);        
    }    
    
    /**
     * Creates a locked layer over a table. This layer prevents the user from
     * interacting with the contents underneath it.
     */
    private class LockedUI extends LockableUI {
        private JXPanel panel;
        
        public LockedUI(Category category, LayerEffect... lockedEffects) {
            panel = new JXPanel(new MigLayout("aligny 50%, alignx 50%"));
            panel.setVisible(false);
        }
        
        @SuppressWarnings("unchecked")
        public void installUI(JComponent c) {
            super.installUI(c);
            JXLayer<JComponent> l = (JXLayer<JComponent>) c;
            l.getGlassPane().setLayout(new BorderLayout());
            l.getGlassPane().add(panel, BorderLayout.CENTER);
        }
        
        @SuppressWarnings("unchecked")
        public void uninstall(JComponent c) {
            super.uninstallUI(c);
            JXLayer<JComponent> l = (JXLayer<JComponent>) c;
            l.getGlassPane().setLayout(new FlowLayout());
            l.getGlassPane().remove(panel);
        }
        
        public void setLocked(boolean isLocked) {
            super.setLocked(isLocked);
            panel.setVisible(isLocked);
        }
        
        @Override
        public Cursor getLockedCursor() {
            return Cursor.getDefaultCursor();
        }
    }
    
    /**
     * Creates the MessageComponent when the user first signs in.
     */
    public MessageComponent getMessageComponent() {
        MessageComponent messageComponent;
        messageComponent = new MessageComponent(6, 22, 18, 6);
        
        JLabel headerLabel = new JLabel(I18n.tr("What Now?"));
        messageComponent.decorateHeaderLabel(headerLabel);
        
        JLabel minLabel = new JLabel(I18n.tr("Share entire categories or individual files with your friends."));
        messageComponent.decorateSubLabel(minLabel);
        
        JLabel secondMinLabel = new JLabel(I18n.tr("Chat with them about using LimeWire 5"));
        messageComponent.decorateSubLabel(secondMinLabel);
        
        JButton cancelButton = new JButton(closeButton);
        cancelButton.setBorder(BorderFactory.createEmptyBorder());
        cancelButton.setRolloverIcon(closeHoverButton);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelButton.setContentAreaFilled(false);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                for(JXLayer layer : map.values()) {
                    layer.getGlassPane().setVisible(false);
                }
                SwingUiSettings.SHOW_FRIEND_OVERLAY_MESSAGE.setValue(false);
            }
        });

        messageComponent.addComponent(cancelButton, "span, alignx right");
        messageComponent.addComponent(headerLabel, "push, wrap");
        messageComponent.addComponent(minLabel, "wrap, gapright 16");
        messageComponent.addComponent(secondMinLabel, "gapright 16");

        return messageComponent;
    }
}
