package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.VerticalLayout;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Drop down combo box for filtering My Library with a Sharing View.
 * The view remains My Library but only shows files that are currently shared
 * with the selected friend.
 */
public class SharingFilterComboBox extends LimeComboBox {

    @Resource
    private Icon gnutellaIcon;
    @Resource
    private Icon friendIcon;
    @Resource
    private Color labelColor;
    @Resource
    private Font labelFont;
    @Resource
    private Color menuSelectionColor;
    
    private final Set<Friend> menuList = new HashSet<Friend>();

    private final LibraryListSourceChanger listChanger;
    private final MyLibraryPanel myLibraryPanel;
    private final ShareListManager shareListManager;
    
    private ScrollablePopupMenu menu;
    
    private JComponent subMenuText;
        
    public SharingFilterComboBox(LibraryListSourceChanger listChanger, MyLibraryPanel myLibraryPanel, ShareListManager shareListManager) {
        this.listChanger = listChanger;
        this.myLibraryPanel = myLibraryPanel;
        this.shareListManager = shareListManager;
        
        GuiUtils.assignResources(this);
        
        menu = new ScrollablePopupMenu();
        
        overrideMenu(menu);
        
        subMenuText = decorateLabel(new JLabel(I18n.tr("with:")));
        
        SharingListener listener = new SharingListener();
        menu.addPopupMenuListener(listener);
    }
    
    /**
	 * Selects a friend in the combo box. This is a convience method for
	 * selecting a friend share view programatically outside of My Library.
	 */
    public void selectFriend(Friend friend) {
        listChanger.setFriend(friend);

        MenuAction action = new MenuAction(friend, 0, null);
        fireChangeEvent(action);
    }
    
    /**
	 * Adds a friend to the list to be displayed.
	 */
    public void addFriend(Friend friend) {
        menuList.add(friend);
    }
    
    /**
	 * Removes a friend from the list to be displayed.
	 */
    public void removeFriend(Friend friend) {
        menuList.remove(friend);
        if(menuList.size() == 0)
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    myLibraryPanel.showAllFiles();  
                }
            });
    }
    
    private JComponent decorateLabel(JComponent component) {
        component.setForeground(labelColor);
        component.setFont(labelFont);
        component.setBorder(BorderFactory.createEmptyBorder(0,2,0,0));
        return component;
    }
         
    /**
     * MenuAction for a shared list
     */
    private class MenuAction extends AbstractAction {
        private final Friend friend;
        
        public MenuAction(Friend friend, int count, Icon icon) {
            this.friend = friend;
            if(friend != null)
                putValue(Action.NAME, friend.getRenderName() + "  (" + count + ")");
            putValue(Action.SMALL_ICON, icon);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            listChanger.setFriend(friend);
            
            SharingFilterComboBox.this.fireChangeEvent(this);
        }

        public String toString() {
            return friend.getRenderName();
        }
    }
    
    /**
     * Listens for combo box selection and creates the 
     * menu of shared lists to choose from.
     */
    private class SharingListener implements PopupMenuListener {        
        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {}
        
        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
        
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            menu.removeAll();        
            menu.add(subMenuText);
           
            menu.add(new MenuAction(SharingTarget.GNUTELLA_SHARE.getFriend(), shareListManager.getGnutellaShareList().size(), gnutellaIcon));
            
            List<Friend> sortedFriends = new ArrayList<Friend>(menuList);
            Collections.sort(sortedFriends, new FriendComparator(shareListManager));
            for(Friend friend : sortedFriends) {
                menu.add(new MenuAction(friend, shareListManager.getOrCreateFriendShareList(friend).size(), friendIcon));
            }
            
            //ensure the menu is properlly sized since it may have changed since the last time
            menu.validate();
        }
    }
    
    /**
     * Compares two friends names for sorting. Friends that are being shared with
     * appear at the top of the list in alphabetical order. Friends that are
     * not being shared with appear below that in alphabetical order.
     */
    private static class FriendComparator implements Comparator<Friend> {
        private final ShareListManager shareListManager;
        
        public FriendComparator(ShareListManager shareListManager) {
            this.shareListManager = shareListManager;
        }
        
        @Override
        public int compare(Friend o1, Friend o2) {
            if(o1 == o2) {
                return 0;
            } else {
                FriendFileList fileList1 = shareListManager.getOrCreateFriendShareList(o1);
                FriendFileList fileList2 = shareListManager.getOrCreateFriendShareList(o2);
                if((fileList1.size() > 0 && fileList2.size() > 0) || 
                        (fileList1.size() == 0 && fileList2.size() == 0))
                    return o1.getRenderName().compareToIgnoreCase(o2.getRenderName());
                else if(fileList1.size() > 0 && fileList2.size() == 0)
                    return -1;
                else 
                    return 1;
            }
        }
    }
    
    /**
     * A scrollable PopupMenu. Creates a PopupMenu that will have a vertical scrollbar
     * when the number of items in it becomes too large. 
     */
    private class ScrollablePopupMenu extends JPopupMenu implements ActionListener, MouseListener {

        private JScrollPane scrollPane;
        private JXPanel panel;
        private Border buttonBorder = BorderFactory.createEmptyBorder(3,11,3,3);
        
        public ScrollablePopupMenu() {
            setLayout(new BorderLayout());

            panel = new JXPanel(){
                @Override
                public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
                    return 20;
                }
                @Override
                public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
                    return 20;
                }
            };
            panel.setScrollableTracksViewportHeight(false);
            panel.setScrollableTracksViewportWidth(false);
            panel.setMinimumSize(new Dimension(100,20));
            panel.setLayout(new VerticalLayout());
            
            init();
        }
        
        private void init() {
            super.removeAll();
            
            scrollPane = new JScrollPane();
            scrollPane.setViewportView(panel);
            scrollPane.setBorder(null);
            scrollPane.setMinimumSize(new Dimension(240, 40));
           
            scrollPane.setMaximumSize(new Dimension(scrollPane.getMaximumSize().width, 350));
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            super.add(scrollPane, BorderLayout.CENTER);
        }
        
        @Override
        public JMenuItem add(JMenuItem menuItem) {
            //convert this MenuItem action into a button
            panel.add(createButton(menuItem.getAction()));
            return menuItem;
        }
               
        @Override
        public JMenuItem add(Action action) {
            panel.add(createButton(action));
            return null;
        }
        
        @Override
        public Component add(Component comp) {
            panel.add(comp);
            return comp;
        }
        
        /**
         * Button that handles menu actions. If MenuItem's are
         * added directly to the panel, the popupMenu will loose
         * focus when they are selected. All MenuItem's must be
         * converted into a JButton instead.
         */
        private JButton createButton(Action action) {
            JButton b = new JButton(action);
            b.addMouseListener(this);
            b.setHorizontalAlignment(SwingConstants.LEFT);
            b.setBorder(buttonBorder);
            b.setFocusPainted(false);
            b.setContentAreaFilled(false);
            b.setBackground(menuSelectionColor);
            b.addActionListener(this);
            return b;
        }       
        
        @Override
        public void show(Component invoker, int x, int y) {
            validate();

            this.setInvoker(invoker);

            Point invokerOrigin = invoker.getLocationOnScreen();
            this.setLocation((int) invokerOrigin.getX() + x, (int) invokerOrigin.getY() + y);
            this.setVisible(true);
        }
        
        @Override
        public void validate() {
            int maxsize = scrollPane.getMaximumSize().height;
            int realsize = panel.getPreferredSize().height;

            // if the scrollbar height is larger than the panel preferred height, just use the panel dimensions
            if(maxsize > realsize) {
                scrollPane.setPreferredSize(panel.getPreferredSize());
            } else {
                scrollPane.setPreferredSize(new Dimension(panel.getPreferredSize().width + 20, scrollPane.getMaximumSize().height));
            }
            super.validate();
        }

        
        @Override
        public void removeAll() {
            for(Component component : getComponents()) {
                if(component instanceof JButton) {
                    component.removeMouseListener(this);
                    ((JButton)component).removeActionListener(this);
                }
            }
            panel.removeAll();
        }
        
        
        @Override
        public Component[] getComponents() {
            return panel.getComponents();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            //hide the popup when a button(menuItem) is acted upon
            setVisible(false);
        }


        @Override
        public void mouseEntered(MouseEvent e) {
            // show mouse over highlights typically found in popup menus
            ((JComponent) e.getComponent()).setOpaque(true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            ((JComponent) e.getComponent()).setOpaque(false);
        }

        @Override
        public void mouseClicked(MouseEvent e) {}
        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {}
    }
}
