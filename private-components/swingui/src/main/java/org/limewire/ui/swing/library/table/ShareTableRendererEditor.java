package org.limewire.ui.swing.library.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ShareTableRendererEditor extends TableRendererEditor implements Configurable{
   // @Resource
   // private Icon shareGnutellaIcon;
   // @Resource
   // private Icon shareFriendsIcon;
    @Resource
    private Font shareButtonFont;
    @Resource
    private Color shareForegroundColor;
    @Resource
    private Color shareMouseOverColor;

  //  private JLabel gnutellaLabel;
    private JLabel friendsLabel;
    private HyperLinkButton shareButton;
    private LocalFileItem fileItem;
    
    private final XMPPService xmppService;
    
    @AssistedInject
    public ShareTableRendererEditor(@Assisted Action shareAction, XMPPService xmppService){
        GuiUtils.assignResources(this);
        this.xmppService = xmppService;
        
       // gnutellaLabel = new JLabel(shareGnutellaIcon);
        friendsLabel = new JLabel();//(shareFriendsIcon);
        shareButton = new HyperLinkButton(I18n.tr("share"));
        shareButton.setFont(shareButtonFont);
        shareButton.setFocusPainted(false);
        shareButton.setBorder(null);
        shareButton.setContentAreaFilled(false);
        shareButton.setForeground(shareForegroundColor);
        shareButton.setMouseOverColor(shareMouseOverColor);
        
        FontUtils.underline(shareButton);
        shareButton.addActionListener(shareAction);

        friendsLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        friendsLabel.setIconTextGap(4);
        
        setLayout(new MigLayout("insets 2 5 2 5, hidemode 0, aligny 50%"));
      //  add(gnutellaLabel);
        add(friendsLabel, "pushx");
        add(shareButton);
    }

    @Override
    public Component doTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        configure((LocalFileItem)value, isSelected);
        return this;
    }

    @Override
    public Component doTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        configure((LocalFileItem)value, true);
        return this;
    }
    
    @Override
    public void configure(LocalFileItem item, boolean isRowSelected) {
        fileItem = item;
       // gnutellaLabel.setVisible(item.isSharedWithGnutella());
        int friendCount = item.getFriendShareCount();
        if(item.isSharedWithGnutella()){
            friendCount++;
        }
        friendsLabel.setVisible(friendCount > 0 && !item.isIncomplete());
        friendsLabel.setText(GuiUtils.toLocalizedInteger(item.getFriendShareCount()));
        shareButton.setVisible(item.isShareable() && !item.isIncomplete());
        
        if(item.getCategory() == Category.DOCUMENT && (!LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue() && !xmppService.isLoggedIn())) {
            //if the share documents with gnutella option is unchecked, the user must be logged in for the share button to be enabled.
            shareButton.setVisible(false);
        } else if(item.getCategory() == Category.PROGRAM && !LibrarySettings.ALLOW_PROGRAMS.getValue()) {
            shareButton.setVisible(false);
        }
    }

    public LocalFileItem getLocalFileItem() {
        return fileItem;
    }
    
    public JButton getShareButton(){
        return shareButton;
    }
}
