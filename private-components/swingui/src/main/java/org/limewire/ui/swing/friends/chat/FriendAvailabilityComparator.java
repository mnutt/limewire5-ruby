package org.limewire.ui.swing.friends.chat;

import java.util.Comparator;

import org.limewire.util.Objects;

class FriendAvailabilityComparator implements Comparator<ChatFriend> {

    @Override
    public int compare(ChatFriend a, ChatFriend b) {
        boolean a_chatting = a.isChatting();
        boolean b_chatting = b.isChatting();
        
        if(a_chatting && b_chatting) {
            return new Long(a.getChatStartTime()).compareTo(new Long(b.getChatStartTime()));
        } else if(a_chatting) {
            return -1;
        } else if (b_chatting) {
            return 1;
        } 
        
        int a_mode_index = a.getMode().getOrder();
        int b_mode_index = b.getMode().getOrder();
        
        if (a_mode_index > b_mode_index) {
            return 1;
        } else if (b_mode_index > a_mode_index) {
            return -1;
        }
        String a_name = a.getName();
        String b_name = b.getName();
        int compare = Objects.compareToNullIgnoreCase(a_name, b_name, false);
        return compare;
    }
    
}