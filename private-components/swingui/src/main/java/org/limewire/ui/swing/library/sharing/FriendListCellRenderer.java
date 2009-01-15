package org.limewire.ui.swing.library.sharing;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import org.limewire.core.api.friend.Friend;

class FriendListCellRenderer extends DefaultListCellRenderer {
    
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        
        Friend friend = ((SharingTarget) value).getFriend();
        String text;
        
        if (((SharingTarget) value).isGnutellaNetwork() || friend.getName() == null) {
            text = friend.getRenderName();
        } else {
            StringBuilder stringBuilder = new StringBuilder("<HTML>").append(friend.getRenderName());
            stringBuilder.append(" <font color=#808080>&#60;").append(friend.getId()).append("&#62;</font></HTML>");
            text = stringBuilder.toString();
        }
        
        return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
    }
}
