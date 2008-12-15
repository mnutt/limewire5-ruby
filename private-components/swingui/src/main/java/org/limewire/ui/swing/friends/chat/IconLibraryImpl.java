package org.limewire.ui.swing.friends.chat;

import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Singleton;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
@Singleton
class IconLibraryImpl implements IconLibrary {
    @Resource private Icon available;
    @Resource private Icon away;
    @Resource private Icon chatting;
    @Resource private Icon incomingChat;
    @Resource private Icon endChat;
    @Resource private Icon endChatOverIcon;
    @Resource private Icon doNotDisturb;
    @Resource private Icon chatStatusStub;
    @Resource private Icon library;
    @Resource private Icon sharing;
    @Resource private Icon closeChat;
    @Resource private Icon minimizeNormal;
    @Resource private Icon minimizeOver;
    @Resource private Icon minimizeDown;
    
    public IconLibraryImpl() {
        GuiUtils.assignResources(this);
    }

    public Icon getAvailable() {
        return available;
    }

    public Icon getAway() {
        return away;
    }
    
    public Icon getChatting() {
        return chatting;
    }

    @Override
    public Icon getUnviewedMessages() {
        return incomingChat;
    }

    public Icon getEndChat() {
        return endChat;
    }

    @Override
    public Icon getEndChatOverIcon() {
        return endChatOverIcon;
    }

    public Icon getDoNotDisturb() {
        return doNotDisturb;
    }

    public Icon getChatStatusStub() {
        return chatStatusStub;
    }

    public Icon getLibrary() {
        return library;
    }

    public Icon getSharing() {
        return sharing;
    }
    
    public Icon getCloseChat() {
        return closeChat;
    }

    @Override
    public Icon getMinimizeDown() {
        return minimizeDown;
    }

    @Override
    public Icon getMinimizeNormal() {
        return minimizeNormal;
    }

    @Override
    public Icon getMinimizeOver() {
        return minimizeOver;
    }
}
