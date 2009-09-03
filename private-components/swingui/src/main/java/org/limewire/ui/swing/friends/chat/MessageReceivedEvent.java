package org.limewire.ui.swing.friends.chat;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.event.AbstractEDTEvent;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.inspection.DataCategory;

public class MessageReceivedEvent extends AbstractEDTEvent {
    private static final String TOPIC_PREFIX = "chat-";
    @SuppressWarnings("unused")
    @InspectablePrimitive(value = "has chatted", category = DataCategory.USAGE)
    private static boolean hasChatted;
    private static final Log LOG = LogFactory.getLog(MessageReceivedEvent.class);
    private final Message message;

    public MessageReceivedEvent(Message message) {
        this.message = message;
        switch (message.getType()) {
            case SENT:
            case RECEIVED:
                hasChatted = true;
        }
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
