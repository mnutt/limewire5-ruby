package org.limewire.ui.swing.library.nav;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.icon.EmptyIcon;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.BusyPainter;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.ActionLabel;
import org.limewire.ui.swing.library.FriendLibraryMediator;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.menu.actions.ChatAction;
import org.limewire.ui.swing.painter.GenericBarPainter;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import net.miginfocom.swing.MigLayout;

public class NavPanel extends JXPanel {
    
    @Resource private Icon removeLibraryIcon;
    @Resource private Icon failedRemoveLibraryIcon;
    @Resource private Icon failedRemoveLibraryHoverIcon;
    @Resource private Icon removeLibraryHoverIcon;
    
    @Resource private Color selectedBackgroundGradientTop = PainterUtils.TRASPARENT;
    @Resource private Color selectedBackgroundGradientBottom = PainterUtils.TRASPARENT;
    
    @Resource private Color selectedTextColor;
    @Resource private Font failedTextFont;
    @Resource private Font textFont;
    @Resource private Color textColor;        
    
    private final RemoteLibraryManager remoteLibraryManager;
    private final FriendLibraryMediator libraryPanel;
    private final LibraryNavigator libraryNavigator;
    
    private final Friend friend;
    private FriendLibrary friendLibrary;
    private final ActionLabel categoryLabel;
    private final JXBusyLabel statusIcon;
    private final Action action;
    
    private final AbstractPainter<JXPanel> selectedPainter;
    
    private NavList parentList;
    private MouseListener removeListener;     
    
    private boolean failed;
    
    private LibraryState lastLibraryState;
    
    private final Provider<ChatAction> chatActionProvider;

    @AssistedInject
    NavPanel(@Assisted Action action,
             @Assisted Friend friend,
             @Assisted FriendLibraryMediator libraryPanel,
            RemoteLibraryManager remoteLibraryManager,
            LibraryNavigator libraryNavigator,
            Provider<ChatAction> chatActionProvider) {
        super(new MigLayout("insets 0, gap 0, fill"));
        
        GuiUtils.assignResources(this);
        
        setOpaque(false);
        
        this.chatActionProvider = chatActionProvider;
        this.action = action;
        this.friend = friend;           
        this.libraryPanel = libraryPanel;
        this.remoteLibraryManager = remoteLibraryManager;        
        this.libraryNavigator = libraryNavigator;
        
        selectedPainter = new GenericBarPainter<JXPanel>(new GradientPaint(0,0, selectedBackgroundGradientTop,
                0,1, selectedBackgroundGradientBottom));
        
        categoryLabel = new ActionLabel(action, false);
        categoryLabel.setFont(textFont);
        categoryLabel.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
        categoryLabel.setMinimumSize(new Dimension(0, 20));
        categoryLabel.setMaximumSize(new Dimension(Short.MAX_VALUE, 20));
        if(friend != null) {
            categoryLabel.setText(friend.getRenderName());
            String toolTipText = getToolTipText(friend);
            categoryLabel.setToolTipText(toolTipText);
            
            if(!friend.isAnonymous()) {
                categoryLabel.addMouseListener(new ContextMenuListener());
            }
        }
        statusIcon = new JXBusyLabel(new Dimension(12, 12));
        statusIcon.setOpaque(false);
        
        add(categoryLabel, "gapbefore 0, grow, push, alignx left");
        add(statusIcon, "alignx right, gapafter 4, hidemode 3, wrap");
        unbusy(false);
        
        // Make it so when you hover over an anonymous browse,
        // you can cancel it early.
        if(friend != null && friend.isAnonymous()) {
            MouseListener listener = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if(lastLibraryState == LibraryState.LOADING) {
                        showRemoveIcon();
                    }
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    if(lastLibraryState == LibraryState.LOADING) {
                        showBusyIcon();
                    }
                }
            };
            addMouseListener(listener);
            categoryLabel.addMouseListener(listener);
            statusIcon.addMouseListener(listener);
        }
        
        action.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                    if(Boolean.TRUE.equals(evt.getNewValue())) {
                        requestFocus();
                        setBackgroundPainter(selectedPainter);
                        categoryLabel.setForeground(selectedTextColor);
                        categoryLabel.setFont(textFont);
                        setOpaque(true);
                        repaint();
                    } else {
                        setBackgroundPainter(null);
                        categoryLabel.setForeground(textColor);
                        categoryLabel.setFont(textFont);
                        setOpaque(false);
                        repaint();
                    }
                }
            }
        });
    }

    private String getToolTipText(Friend friend) {
        StringBuffer toolTipText = new StringBuffer();
        String name = friend.getName();
        String id = friend.isAnonymous() ? "" : friend.getId();
        
        if(!StringUtils.isEmpty(name) && !StringUtils.isEmpty(id) && !name.equals(id)) {
            toolTipText.append("<html>").append(name).append("<br>").append(id).append("</html>");
        } else if(!StringUtils.isEmpty(name)) {
            toolTipText.append(name);
        } else if(!StringUtils.isEmpty(id)) {
            toolTipText.append(id);
        }
        return toolTipText.toString();
    }
    
    void setTopGap(int topgap) {
        Border border = categoryLabel.getBorder();
        Insets insets = border.getBorderInsets(categoryLabel);
        categoryLabel.setBorder(BorderFactory.createEmptyBorder(topgap, insets.left, insets.bottom, insets.right));
    }
    
    void setBottomGap(int bottomgap) {
        Border border = categoryLabel.getBorder();
        Insets insets = border.getBorderInsets(categoryLabel);
        categoryLabel.setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left, bottomgap, insets.right));
    }
    
    void setTitle(String text) {
        categoryLabel.setText(text);
    }
    
    Action getAction() {
        return action;
    }
    
    public void setPanelIcon(Icon icon) {
        categoryLabel.setIcon(icon);
    }
    
    public void setPanelFont(Font font) {
        this.textFont = font;
        if(!Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY))) {
            categoryLabel.setFont(font);
        }
    }
    
    public void setFontColor(Color color) {
        this.textColor = color;
        if(!Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY))) {
            categoryLabel.setForeground(color);
        }
    }
    
    public void setSelectedFontColor(Color color) {
        this.selectedTextColor = color;
        if(Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY))) {
            categoryLabel.setForeground(color);
        }    
    }
    
    NavList getParentList() {
        return parentList;
    }
    
    void setParentList(NavList parentList) {
        this.parentList = parentList;
    }
    
    void addActionListener(ActionListener listener) {
        categoryLabel.addActionListener(listener);
    }
    
    private void showBusyIcon() {
        removeEjectListener();        
        BusyPainter painter = statusIcon.getBusyPainter();
        statusIcon.setIcon(new EmptyIcon(12, 12));
        statusIcon.setBusyPainter(painter);
        statusIcon.setVisible(true);
        statusIcon.setBusy(true);
    }
    
    private void showRemoveIcon() {
        addEjectListener();
        statusIcon.setVisible(true);
        statusIcon.setBusy(false);
        if (failed) {
           statusIcon.setIcon(failedRemoveLibraryIcon);
        } else {
            statusIcon.setIcon(removeLibraryIcon);
        }       
    }
    
    private void showBlankIcon() {
        removeEjectListener();
        statusIcon.setVisible(false);
        statusIcon.setBusy(false);
        statusIcon.setIcon(new EmptyIcon(12, 12));
    }
    
    private void unbusy(boolean failed) {
        this.failed = failed;
        if(friend != null && friend.isAnonymous()) {
            showRemoveIcon();     
        } else {
            showBlankIcon();
        }
    }
    
    private void removeEjectListener() {
        if(removeListener != null) {
            statusIcon.removeMouseListener(removeListener);
            removeListener = null;
        }
    }
    
    private void addEjectListener() {
        if (removeListener == null) {
            removeListener = new ActionHandListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { 
                    remoteLibraryManager.removeFriendLibrary(friend);
                }
            }) {
                @Override
                public void mouseEntered(MouseEvent e) {
                    super.mouseEntered(e);
                    if (failed) {
                        statusIcon.setIcon(failedRemoveLibraryHoverIcon);
                    } else {
                        statusIcon.setIcon(removeLibraryHoverIcon);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    super.mouseExited(e);
                    if (failed) {
                        statusIcon.setIcon(failedRemoveLibraryIcon);
                    } else {
                        statusIcon.setIcon(removeLibraryIcon);
                    }
                }
            };
            statusIcon.addMouseListener(removeListener);
        }
    }
    
    public void updateLibraryState(LibraryState libraryState) {
        this.lastLibraryState = libraryState;
        switch(libraryState) {
        case FAILED_TO_LOAD:
            categoryLabel.setFont(failedTextFont);
            unbusy(true);
            break;
        case LOADED:
            categoryLabel.setFont(textFont);
            unbusy(false);
            break;
        case LOADING:
            categoryLabel.setFont(textFont);
            showBusyIcon();
            break;
        }
    }
    
    public void updateLibrary(FriendLibrary friendLibrary) {
        this.friendLibrary = friendLibrary;
        updateLibraryState(friendLibrary.getState());
        libraryPanel.updateLibraryPanel(friendLibrary.getSwingModel(), friendLibrary.getState());
    }
    
    public boolean isSelected() {
        return Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY));
    }
    
    public void select() { 
        action.actionPerformed(null);
    }
    
    public void showLibraryCard() {
        libraryPanel.showLibraryCard();
    }
    
    public void removeBrowse() {
        if(libraryPanel != null) {
            unbusy(false);
        }
    }
    
    public Friend getFriend() {
        return friend;
    }

    public FriendLibrary getFriendLibrary() {
        return friendLibrary;
    }
    
    private class ContextMenuListener extends MousePopupListener {
        @Override
        public void handlePopupMouseEvent(MouseEvent e) {
            JPopupMenu menu = new JPopupMenu();
            menu.add(new JMenuItem(new AbstractAction(I18n.tr("What I'm Sharing")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    libraryNavigator.selectFriendShareList(friend);
                }                
            }));
            
            
            ChatAction chatAction = chatActionProvider.get();
            chatAction.setFriend(friend);
            menu.add(new JMenuItem(chatAction));
            menu.show((Component) e.getSource(), e.getX() + 3, e.getY() + 3);
        }
    }
}
