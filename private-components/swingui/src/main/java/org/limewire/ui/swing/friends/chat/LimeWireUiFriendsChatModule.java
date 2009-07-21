package org.limewire.ui.swing.friends.chat;

import org.limewire.inject.LazyBinder;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManager;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManagerImpl;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryProvider;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class LimeWireUiFriendsChatModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(ChatFrame.class).to(ChatFramePanel.class);
        bind(ChatHyperlinkListenerFactory.class).toProvider(
                FactoryProvider.newFactory(
                        ChatHyperlinkListenerFactory.class, ChatHyperlinkListener.class));        
        bind(ConversationPaneFactory.class).toProvider(
                FactoryProvider.newFactory(
                        ConversationPaneFactory.class, ConversationPane.class));
        bind(FriendAccountConfigurationManager.class).toProvider(LazyBinder.newLazyProvider(
                FriendAccountConfigurationManager.class, FriendAccountConfigurationManagerImpl.class));
    }
}
