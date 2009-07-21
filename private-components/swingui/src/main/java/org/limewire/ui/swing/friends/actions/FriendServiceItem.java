package org.limewire.ui.swing.friends.actions;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.jdesktop.application.Resource;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;

public class FriendServiceItem extends JLabel {

    @Resource private Font font;
    @Resource private Color foreground;
    @Resource private Color background;
    @Resource private Color separatorForeground;
    
    @Inject
    public FriendServiceItem(EventBean<FriendConnectionEvent> friendConnectionEventBean) {
        
        GuiUtils.assignResources(this);
        
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        if (friendConnection != null && friendConnection.isLoggedIn()) {
            setFont(font);
            setForeground(foreground);
            setBackground(background);
            setOpaque(true);
            
            int insets = 10;
            if (OSUtils.isLinux()) {
                insets = 10;
            } 
            else {
                insets = 18;
            }
            
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0, separatorForeground), 
                    BorderFactory.createEmptyBorder(0,insets,0,0)));
            setIconTextGap(4);
            
            setMaximumSize(new Dimension(9999,22));
            setPreferredSize(new Dimension(120,22));
            
            String name = friendConnection.getConfiguration().getCanonicalizedLocalID();
            setText(name);
            setToolTipText(name);
            setIcon(friendConnection.getConfiguration().getIcon());
        } else {
            setVisible(false);
        }
    }
}