package org.limewire.ui.swing.friends.chat;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.event.AbstractEDTEvent;

public class MessageReceivedEvent extends AbstractEDTEvent {
    private static final String TOPIC_PREFIX = "chat-";
    private static final Log LOG = LogFactory.getLog(MessageReceivedEvent.class);
    private final Message message;

    public MessageReceivedEvent(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }
    
    public static String buildTopic(String conversationName) {
        return TOPIC_PREFIX + conversationName;
    }
    
    @Override
    public void publish() {
        LOG.debugf("Publishing message: Type: {0} From: {1} Text: {2}", message.getType(), message.getSenderName(), message.toString());
        super.publish(buildTopic(message.getFriendID()));
    }
}
