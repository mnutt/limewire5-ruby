package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;
import static org.limewire.ui.swing.util.I18n.trn;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.limewire.collection.MultiIterable;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.LimeComboBoxFactory;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.util.Objects;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class SearchResultFromWidget extends JPanel {

    private final LimeComboBox comboBox;
    private final JPopupMenu comboBoxMenu;
    
    private final RemoteHostActions fromActions;
    
    private List<RemoteHost> people = Collections.emptyList();
    private List<RemoteHost> poppedUpPeople = Collections.emptyList();
    
    private final boolean isClassicView;
    
    @AssistedInject
    SearchResultFromWidget(LimeComboBoxFactory comboBoxFactory,
                           RemoteHostActions fromActions,
                           @Assisted boolean isClassicView) {
        this.fromActions = Objects.nonNull(fromActions, "fromActions");
        this.isClassicView = isClassicView;
        this.comboBox = comboBoxFactory.createMiniComboBox();
        this.comboBoxMenu = new JPopupMenu();
        this.comboBox.overrideMenu(this.comboBoxMenu);
        comboBoxMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                if(!poppedUpPeople.equals(people)) {
                    comboBox.setClickForcesVisible(true);
                }
            }
            
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if(!poppedUpPeople.equals(people)) {
                    comboBox.setClickForcesVisible(true);
                }
            }
            
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                comboBox.setClickForcesVisible(false);
                poppedUpPeople = people;
                updateMenus();
            }
        });
        
        this.layoutComponents();
        this.setOpaque(false);
    }
    
    /** A name for actions when they're the only action available on a host. */
    private static final String SINGULAR_ACTION_NAME = "singularActionName";
    
    private Action getChatAction(final RemoteHost person) {
        return new AbstractAction(tr("Chat")) {
            {
                putValue(SINGULAR_ACTION_NAME, tr("Chat with {0}", person.getFriendPresence().getFriend().getRenderName()));
            }
            
            @Override
            public void actionPerformed(ActionEvent e) {
                fromActions.chatWith(person);
            }
        };
    }

    private Action getLibraryAction(final RemoteHost person) {
        return new AbstractAction(tr("View Files")) {
            {
                putValue(SINGULAR_ACTION_NAME, tr("View Files of {0}", person.getFriendPresence().getFriend().getRenderName()));
            }
        
            @Override
            public void actionPerformed(ActionEvent e) {
                fromActions.viewLibraryOf(person);
            }
        };
    }

    private Action getSharingAction(final RemoteHost person) {
        return new AbstractAction(tr("Share")) {
            {
                putValue(SINGULAR_ACTION_NAME, tr("Share With {0}", person.getFriendPresence().getFriend().getRenderName()));
            }
        
            @Override
            public void actionPerformed(ActionEvent e) {
                fromActions.showFilesSharedBy(person);
            }
        };
    }

    private void layoutComponents() {
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

        add(this.comboBox, BorderLayout.EAST);
    }

    
    public void setPeople(Collection<RemoteHost> people) {
        this.people = new LinkedList<RemoteHost>(people);
        this.comboBox.setText(getFromText());
    }

    private String getFromText() {
        if(people.size() == 0) {
            return tr("nobody");
        } else {
            boolean foundFriend = false;
            boolean foundAnon = false;
            for(RemoteHost host : people) {
                if(host.getFriendPresence().getFriend().isAnonymous()) {
                    foundAnon = true;
                } else {
                    foundFriend = true;
                }
                
                if(foundAnon && foundFriend) {
                    // no need to search anymore.
                    break;
                }
            }
            if(foundFriend && foundAnon) {
                if(isClassicView) {
                    return trn("{0} Person", "{0} People", people.size());
                } else {
                    return trn("Person", "People", people.size());
                }
            } else if(foundFriend) {
                if(isClassicView) {
                    return trn("{0} Friend", "{0} Friends", people.size());
                } else {
                    return trn("Friend", "Friends", people.size());
                }
            } else { // foundAnon
                if(isClassicView) {
                    return trn("{0} P2P User", "{0} P2P Users", people.size());
                } else {
                    return trn("P2P User", "P2P Users", people.size());
                }
            }
        }
    }

    private JMenuItem createItem(Action a) {
        JMenuItem item = new JMenuItem(a);        
        comboBox.decorateMenuComponent(item);        
        return item;
    }
    
    private void updateMenus() {        
        comboBoxMenu.removeAll();        
        if (people.size() == 0) {
            return; // menu has no items
        }

        List<JMenuItem> friends = new ArrayList<JMenuItem>();
        List<JMenuItem> friendsDisabled = new ArrayList<JMenuItem>();
        List<JMenuItem> p2pUsers = new ArrayList<JMenuItem>();
        List<JMenuItem> p2pUsersDisabled = new ArrayList<JMenuItem>();
        for (RemoteHost person : people) {
            JMenu submenu = new JMenu(person.getFriendPresence().getFriend().getRenderName());
            comboBox.decorateMenuComponent(submenu);

            if (person.isChatEnabled()) {
                submenu.add(createItem(getChatAction(person)));
            }
            if (person.isBrowseHostEnabled()) {
                submenu.add(createItem(getLibraryAction(person)));
            }
            if (person.isSharingEnabled()) {
                submenu.add(createItem(getSharingAction(person)));
            }

            JMenuItem itemToAdd = submenu;
            // If we only added one item, remove the parent menu and make this it.
            if (submenu.getMenuComponentCount() == 1) {
                itemToAdd = (JMenuItem) submenu.getMenuComponent(0);
                Action action = itemToAdd.getAction();
                // Replace the name with the singular name.
                action.putValue(Action.NAME, action.getValue(SINGULAR_ACTION_NAME));
            } else if (submenu.getMenuComponentCount() == 0) {
                itemToAdd = new JMenuItem(submenu.getText());
                itemToAdd.setEnabled(false);
            }

            if (person.getFriendPresence().getFriend().isAnonymous()) {
                if (itemToAdd.isEnabled()) {
                    p2pUsers.add(itemToAdd);
                } else {
                    p2pUsersDisabled.add(itemToAdd);
                }
            } else {
                if (itemToAdd.isEnabled()) {
                    friends.add(itemToAdd);
                } else {
                    friendsDisabled.add(itemToAdd);
                }
            }
        }

        // Now go back through our submenus & add them in.
        if (friends.size() > 0 && p2pUsers.size() > 0) {
            JLabel label = new JLabel(tr("Friends"));
            label.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
            comboBoxMenu.add(label);
            for (JMenuItem friend : new MultiIterable<JMenuItem>(friends, friendsDisabled)) {
                comboBoxMenu.add(friend);
            }
            label = new JLabel(tr("P2P Users"));
            label.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
            comboBoxMenu.add(label);
            for (JMenuItem p2pUser : new MultiIterable<JMenuItem>(p2pUsers, p2pUsersDisabled)) {
                comboBoxMenu.add(p2pUser);
            }
        } else if (friends.size() > 0) {
            for (JMenuItem friend : new MultiIterable<JMenuItem>(friends, friendsDisabled)) {
                comboBoxMenu.add(friend);
            }
        } else if (p2pUsers.size() > 0) {
            for (JMenuItem p2pUser : new MultiIterable<JMenuItem>(p2pUsers, p2pUsersDisabled)) {
                comboBoxMenu.add(p2pUser);
            }
        }
    }
    
    @Override
    public String getToolTipText(){
        return comboBox.getText();
    }
}