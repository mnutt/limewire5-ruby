package org.limewire.ui.swing.library.table;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.internal.Nullable;

public class ShareTableRendererEditor extends TableRendererEditor implements Configurable{
    @Resource private Font shareButtonFont;    
    
    @Resource private Icon p2pNotSharedIcon;
    @Resource private Icon friendsNotSharedIcon;
    
    @Resource private Icon p2pSharedIcon;
    @Resource private Icon friendsSharedIcon;
    
    @Resource private Icon p2pDisabledIcon;
    @Resource private Icon friendsDisabledIcon;

    private IconButton p2pButton;
    private IconButton friendsButton;
    private LocalFileItem fileItem;
    
    private final XMPPService xmppService;
    
    private final ToolTipMouseListener p2pTooltipListener;
    private final ToolTipMouseListener friendsTooltipListener;
    
    @Inject
    public ShareTableRendererEditor(@Nullable @Assisted Action shareAction, XMPPService xmppService){
        GuiUtils.assignResources(this);
        
        this.xmppService = xmppService;
        
        p2pButton = new IconButton(p2pNotSharedIcon);
        friendsButton = new IconButton(friendsNotSharedIcon);
        friendsButton.setFont(shareButtonFont);
        
        p2pButton.setDisabledIcon(p2pDisabledIcon);
        friendsButton.setDisabledIcon(friendsDisabledIcon);
        
        p2pButton.addActionListener(shareAction);
        friendsButton.addActionListener(shareAction);
        friendsButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        friendsButton.setVerticalTextPosition(SwingConstants.CENTER);
    
        this.p2pTooltipListener = new ToolTipMouseListener(p2pButton);
        this.friendsTooltipListener = new ToolTipMouseListener(friendsButton);
        setLayout(new MigLayout("insets 2 5 2 5, hidemode 3, aligny 50%"));
        
        add(p2pButton);
        add(friendsButton);
    }

    @Override
    public Component doTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if(value instanceof LocalFileItem) {
            configure((LocalFileItem)value, isSelected);
            return this;
        } else {
            return emptyPanel;
        }
    }

    @Override
    public Component doTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        if(value instanceof LocalFileItem) {
            configure((LocalFileItem)value, true);
            return this;
        } else {
            return emptyPanel;
        }
    }
    
    @Override
    public void configure(LocalFileItem item, boolean isRowSelected) {
        friendsButton.setVisible(xmppService.isLoggedIn()); // don't show if not logged in
        
        fileItem = item;
        
        p2pButton.removeMouseListener(p2pTooltipListener);
        friendsButton.removeMouseListener(friendsTooltipListener);
        
        if(!item.isShareable()) {
            p2pButton.setEnabled(false);
            friendsButton.setEnabled(false);
            
            p2pButton.setToolTipText(I18n.tr("This file cannot be shared."));
            friendsButton.setToolTipText(I18n.tr("This file cannot be shared."));
            
            p2pButton.addMouseListener(p2pTooltipListener);
            friendsButton.addMouseListener(p2pTooltipListener);
        } else if(item.getCategory() == Category.DOCUMENT) {
            //if the share documents with gnutella option is unchecked, the user must be logged in for the share button to be enabled.
            if(!LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue()) {
                p2pButton.setEnabled(false);
                p2pButton.setToolTipText(I18n.tr("Sharing documents with the P2P Network is disabled."));
                p2pButton.addMouseListener(p2pTooltipListener);
            } else {
                p2pButton.setEnabled(true);
                p2pButton.setToolTipText(I18n.tr("Share this file with the P2P Network."));
            }
            
            friendsButton.setEnabled(true);
            friendsButton.setToolTipText(I18n.tr("Share this file with a friend."));
            
        } else if(item.getCategory() == Category.PROGRAM && !LibrarySettings.ALLOW_PROGRAMS.getValue()) {
            p2pButton.setEnabled(false);
            friendsButton.setEnabled(false);
            
            p2pButton.setToolTipText(I18n.tr("This file cannot be shared."));
            friendsButton.setToolTipText(I18n.tr("This file cannot be shared."));
            
            p2pButton.addMouseListener(p2pTooltipListener);
            friendsButton.addMouseListener(p2pTooltipListener);
        } else {
            p2pButton.setEnabled(true);
            p2pButton.setToolTipText(I18n.tr("Share this file with the P2P Network."));
            friendsButton.setEnabled(true);
            friendsButton.setToolTipText(I18n.tr("Share this file with a friend."));
        }
        
        int friendCount = item.getFriendShareCount();
        if(item.isSharedWithGnutella()) {
            p2pButton.setIcon(p2pSharedIcon);
            friendCount--;
        } else {
            p2pButton.setIcon(p2pNotSharedIcon);
        }
        
        if(friendCount > 0) {
            friendsButton.setIcon(friendsSharedIcon);
            friendsButton.setText(GuiUtils.toLocalizedInteger(friendCount));
        } else {
            friendsButton.setIcon(friendsNotSharedIcon);
            friendsButton.setText("");
        }
    }

    public LocalFileItem getLocalFileItem() {
        return fileItem;
    }
    
    public JComponent getShareEditorWidget(){
        return this;
    }
    
    /**
     * On clicking the given component the tooltip will immediately be displayed. 
     */
    private final class ToolTipMouseListener extends MouseAdapter {
        private final JComponent component;

        private ToolTipMouseListener(JComponent component) {
            this.component = component;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            Action action = component.getActionMap().get("postTip");
            if (action != null) {
                action.actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, "postTip"));
            }
        }
    }
}
