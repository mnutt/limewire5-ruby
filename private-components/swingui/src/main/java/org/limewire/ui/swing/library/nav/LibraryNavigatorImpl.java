package org.limewire.ui.swing.library.nav;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.VerticalLayout;
import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.dnd.FriendLibraryNavTransferHandler;
import org.limewire.ui.swing.dnd.MyLibraryNavTransferHandler;
import org.limewire.ui.swing.friends.login.FriendsSignInPanel;
import org.limewire.ui.swing.library.AllFriendsLibraryPanel;
import org.limewire.ui.swing.library.Disposable;
import org.limewire.ui.swing.library.FriendLibraryMediator;
import org.limewire.ui.swing.library.FriendLibraryMediatorFactory;
import org.limewire.ui.swing.library.MyLibraryPanel;
import org.limewire.ui.swing.library.P2PNetworkSharingPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import net.miginfocom.swing.MigLayout;

@Singleton
class LibraryNavigatorImpl extends JXPanel implements LibraryNavigator {
    
    private final NavPanel myLibrary;
    private final NavPanel p2pNetwork;
    private final NavPanel allFriends;
    private final NavList limewireList;
    private final NavList onlineList;
    private final NavList offlineList;
    private final NavList[] allLists;
    
    private final MyLibraryPanel myLibraryPanel;
    private final Navigator navigator;
    private final ShareListManager shareListManager;
    private final FriendLibraryMediatorFactory friendLibraryMediatorFactory;
    private final NavPanelFactory navPanelFactory;
    
    private Friend selectedFriend = null;
    private static final Log LOG = LogFactory.getLog(LibraryNavigatorImpl.class);

    @Inject
    LibraryNavigatorImpl(Navigator navigator,
            RemoteLibraryManager remoteLibraryManager,
            DownloadListManager downloadListManager,
            ShareListManager shareListManager,
            MyLibraryPanel myLibraryPanel,
            P2PNetworkSharingPanel p2pNetworkSharingPanel,
            AllFriendsLibraryPanel allFriendsLibraryPanel,
            NavPanelFactory navPanelFactory,
            FriendLibraryMediatorFactory friendLibraryMediatorFactory,
            FriendsSignInPanel friendsPanel,
            SaveLocationExceptionHandler saveLocationExceptionHandler) {
        
        setMinimumSize(new Dimension(150, 0));
        setMaximumSize(new Dimension(150, 999));
        setPreferredSize(new Dimension(150, 999));
        
        this.myLibraryPanel = myLibraryPanel;
        this.shareListManager = shareListManager;
        this.limewireList = new NavList();
        this.onlineList = new NavList();
        this.offlineList = new OfflineNavList();
        this.allLists = new NavList[] { limewireList, onlineList, offlineList };
        this.navPanelFactory = navPanelFactory;
        this.friendLibraryMediatorFactory = friendLibraryMediatorFactory;
        this.navigator = navigator;
        
        limewireList.setTitleText(I18n.tr("On LimeWire"));
        onlineList.setTitleText(I18n.tr("Online"));
        offlineList.setTitleText(I18n.tr("Offline"));
        
        setOpaque(false);
        setScrollableTracksViewportHeight(false);
        
        myLibrary = initializePanel(I18n.tr("My Library"), myLibraryPanel, "LibraryNavigator.myLibrary");
        myLibrary.updateLibraryState(myLibraryPanel.getLibrary().getState());
        myLibrary.setTransferHandler(new MyLibraryNavTransferHandler(downloadListManager, myLibraryPanel.getLibrary(), saveLocationExceptionHandler));
        myLibraryPanel.getLibrary().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("state")) {
                    myLibrary.updateLibraryState((LibraryState)evt.getNewValue());
                }
            }
        });
        
        p2pNetwork = initializePanel(I18n.tr("P2P Network"), p2pNetworkSharingPanel, "LibraryNavigator.p2pNetwork");
        p2pNetwork.setTransferHandler(new FriendLibraryNavTransferHandler(shareListManager.getGnutellaShareList()));
        
        allFriends = initializePanel(I18n.tr("All Friends"), allFriendsLibraryPanel, "LibraryNavigator.allFriends");

        setLayout(new MigLayout("insets 0, fill, gap 2"));

        
        JXPanel friendsListPanel = new JXPanel(new VerticalLayout(2));
        friendsListPanel.setOpaque(false);
        friendsListPanel.setScrollableTracksViewportHeight(false);
        JScrollPane scrollableNav = new JScrollPane(friendsListPanel);
        scrollableNav.setOpaque(false);
        scrollableNav.getViewport().setOpaque(false);
        scrollableNav.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollableNav.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollableNav.setBorder(null);
        
        addItem(myLibrary, this, "growx, wmin 0, wrap", null, p2pNetwork.getAction());
        addItem(p2pNetwork, this, "growx, wmin 0, wrap", myLibrary.getAction(), allFriends.getAction());
        addItem(allFriends, this, "growx, wmin 0, wrap", p2pNetwork.getAction(), new MoveAction(limewireList, true));
        addItem(friendsPanel, this, "growx, wmin 0, wrap", null, null);
        addItem(scrollableNav,this, "grow, wmin 0, wrap",  null, null);
        addItem(limewireList, friendsListPanel, "", allFriends.getAction(), new MoveAction(onlineList, true));
        addItem(onlineList,  friendsListPanel, "", new MoveAction(limewireList, false), new MoveAction(offlineList, true));
        addItem(offlineList, friendsListPanel, "", new MoveAction(onlineList, false), null);

        new AbstractListEventListener<FriendLibrary>() {
            @Override
            protected void itemAdded(FriendLibrary item, int idx, EventList<FriendLibrary> source) {
                LOG.debugf("friend library {0} added ...", item.getFriend().getId());  
                NavPanel panel = getPanelForFriend(item.getFriend());
                if(panel != null) {
                    LOG.debugf("... removing existing friend library {0}", item.getFriend().getId());  
                    panel.getParentList().removePanel(panel);
                } else {
                    LOG.debugf("... creating new friend nav panel {0}", item.getFriend().getId()); 
                    panel = createFriendNavPanel(item.getFriend());
                }
                
                limewireList.addNavPanel(panel);
                updatePanel(item, panel);
            }
            
            @Override
            protected void itemRemoved(FriendLibrary item, int idx, EventList<FriendLibrary> source) {
                LOG.debugf("friend library {0} removed ...", item.getFriend().getId()); 
                Friend friend = item.getFriend();
                if(friend.isAnonymous()) {
                    NavPanel panel = limewireList.removePanelForFriend(item.getFriend());
                    if(panel != null) {
                        disposeNavPanel(panel);
                    }
                } else {
                    NavPanel panel = limewireList.getPanelForFriend(item.getFriend());
                    if(panel != null && panel.getFriendLibrary() == item) {
                        LOG.debugf("... removing matching friend library {0}", item.getFriend().getId()); 
                        // extra check is needed b/c when
                        // glazedlist batches up updates, add/remove events
                        // can get dispatched out of order
                        limewireList.removePanel(panel);
                        panel.removeBrowse();
                        onlineList.addNavPanel(panel); // Assume still online.
                    } else {
                        // else probably signed off & cleared the lists.
                        LOG.debugf("... friend library {0} was already removed", item.getFriend().getId()); 
                    }
                }
            }
            
            @Override
            protected void itemUpdated(FriendLibrary item, FriendLibrary prior, int idx, EventList<FriendLibrary> source) {
                NavPanel panel = limewireList.getPanelForFriend(item.getFriend());                
                if(panel != null) {
                    LOG.debugf("updating navpanel for {0} to state {1}", item.getFriend().getId(), item.getState());  
                    updatePanel(item, panel);
                } else {
                    LOG.debugf("null navpanel for {0}", item.getFriend().getId());    
                }
            }
            
            void updatePanel(FriendLibrary item, NavPanel panel) {
                panel.updateLibrary(item);
            }
        }.install(remoteLibraryManager.getSwingFriendLibraryList());
    }
    
    private void addItem(JComponent item, JComponent parent, String constraints, Action upAction, Action downAction) {
        parent.add(item, constraints);
        if(upAction != null) {
            item.getActionMap().put(NavKeys.MOVE_UP, upAction);
        }
        if(downAction != null) {
            item.getActionMap().put(NavKeys.MOVE_DOWN, downAction);
        }
    }    
    
    private NavPanel initializePanel(String title, JComponent component, String name) {
        NavPanel panel = navPanelFactory.createNavPanel(createAction(title, component), null, null);
        panel.setTitle(title);
        panel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NavKeys.MOVE_DOWN);
        panel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), NavKeys.MOVE_UP);
        panel.setName(name);                
        return panel;
    }    
    
    @Inject void register(@Named("known") ListenerSupport<FriendEvent> knownListeners,
                          @Named("available") ListenerSupport<FriendEvent> availListeners) {
        
        knownListeners.addListener(new EventListener<FriendEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendEvent event) {
                NavPanel panel = getPanelForFriend(event.getSource());
                switch(event.getType()) {
                case ADDED:
                    if(panel == null) {
                        LOG.debugf("creating new friend nav panel {0}", event.getSource().getId()); 
                        panel = createFriendNavPanel(event.getSource());
                        offlineList.addNavPanel(panel);
                    }
                    break;
                case REMOVED:
                    if(panel != null) {
                        LOG.debugf("removing matching friend library {0}", event.getSource().getId()); 
                        panel.getParentList().removePanel(panel);
                        disposeNavPanel(panel);
                    }
					break;
                }
            }
        });
        
        availListeners.addListener(new EventListener<FriendEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendEvent event) {
                NavPanel panel = getPanelForFriend(event.getSource());
                switch(event.getType()) {
                case ADDED:
                    if(panel == null) {
                        panel = createFriendNavPanel(event.getSource());
                        onlineList.addNavPanel(panel);
                    } else if(panel.getParentList() == offlineList) {
                        offlineList.removePanel(panel);
                        onlineList.addNavPanel(panel);
                    }
                    break;
                case REMOVED:
                    if(panel != null) {
                        panel.getParentList().removePanel(panel);
                        offlineList.addNavPanel(panel);
                    }
					break;
                }
            }
        });
        
    }
    
    private NavPanel getPanelForFriend(Friend friend) {
        for(NavList list : allLists) {
            NavPanel panel = list.getPanelForFriend(friend);
            if(panel != null) {
                return panel;
            }
        }
        return null;
    }
    
    @Override
    public JXPanel getComponent() {
        return this;
    }
    
    private Action createAction(String title, JComponent component) {
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, title, component);
        Action action = NavigatorUtils.getNavAction(navItem);
        return action;
    }
    
    @Override
    public void selectFriendLibrary(Friend friend) {
        for(NavList list : allLists) {
            if(list.selectFriendLibrary(friend) != null) {
                break;
            }
        }
    }
    
    @Override
    public void selectFriendShareList(Friend friend) {
        for(NavList list : allLists) {
            if(list.selectFriendShareList(friend) != null) {
                break;
            }
        }
    }

    @Override
    public void selectInLibrary(URN urn, Category category) {
        myLibrary.select();
        myLibraryPanel.selectItem(urn, category);
    }
    
    @Override
    public void selectInLibrary(File file, Category category) {
        myLibrary.select();
        myLibraryPanel.selectItem(file, category);
    }
    
    @Override
    public File getPreviousInLibrary(File file, Category category) {
        return myLibraryPanel.getPreviousItem(file, category);
    }
    
    @Override
    public File getNextInLibrary(File file, Category category) {
        return myLibraryPanel.getNextItem(file, category);
    }
    
    private void ensureFriendVisible(Friend friend) {
        for(NavList list : allLists) {
            if(list.ensureFriendVisible(friend) != null) {
                break;
            }
        }
    }
    
    private void disposeNavPanel(NavPanel navPanel) {
        navPanel.removeBrowse();
        navigator.getNavItem(NavCategory.LIBRARY, navPanel.getFriend().getId()).remove();
    }
    
    private NavPanel createFriendNavPanel(Friend friend) {
        final FriendLibraryMediator component = friendLibraryMediatorFactory.createMediator(friend);
        NavPanel navPanel = navPanelFactory.createNavPanel(createFriendAction(navigator, friend, component), 
                friend, component);
        navPanel.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                component.showLibraryCard();
            }
        });
        navPanel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NavKeys.MOVE_DOWN);
        navPanel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), NavKeys.MOVE_UP);
        navPanel.setTransferHandler(new FriendLibraryNavTransferHandler(shareListManager.getOrCreateFriendShareList(friend)));
        
        return navPanel;
    }
    
    private Action createFriendAction(Navigator navigator, Friend friend, JComponent component) {
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, friend.getId(), component);
        Action action = NavigatorUtils.getNavAction(navItem);
        return decorateAction(action, navItem, (Disposable)component, friend);
    }
    
    private Action decorateAction(Action action, NavItem navItem, final Disposable disposable, final Friend friend) {        
        navItem.addNavItemListener(new NavItemListener() {
            @Override
            public void itemRemoved() {
                disposable.dispose();
            }
            
            @Override
            public void itemSelected(boolean selected) {
                if(selected) {
                    selectedFriend = friend;
                    ensureFriendVisible(friend);
                } else {
                    selectedFriend = null;
                }
            }
        });
        return action;
    }
    
    private class MoveAction extends AbstractAction {
        private final NavList navList;
        private final boolean selectFirst;
        
        MoveAction(NavList navList, boolean selectFirst) {
            this.navList = navList;
            this.selectFirst = selectFirst;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if(selectFirst) {
                navList.selectFirst();
            } else {
                navList.selectLast();
            }
        }
    }
    
    @Override
    public Friend getSelectedFriend() {
        return selectedFriend;
    }
}
