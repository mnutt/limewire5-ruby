package org.limewire.promotion.containers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.GGEP;

public class MessageContainerParser {

    private static Set<MessageContainer> parsers = new HashSet<MessageContainer>();

    static {
        addParser(new SignedMessageContainer());
        addParser(new MultiMessageContainer());
        addParser(new PromotionMessageContainer());
        addParser(new BucketMessageContainer());
    }

    /**
     * Pass an instance of a message container, and it will be added to the
     * static list of parsers. This should only be needed when you are creating
     * mock containers for unit tests. Otherwise your container should be
     * officially added to this class's static constructor.
     */
    public static void addParser(MessageContainer containerType) {
        parsers.add(containerType);
    }

    /**
     * @return a message container parsed from the given GGEP, or an exception
     *         if there is a problem parsing the container.
     */
    public MessageContainer parse(GGEP ggepMessage) throws BadGGEPBlockException {
        byte[] type = ggepMessage.get(MessageContainer.TYPE_KEY);
        for (MessageContainer container : parsers) {
            if (Arrays.equals(container.getType(), type)) {
                // Found a parser type that should handle this GGEP!
                try {
                    MessageContainer instance = container.getClass().newInstance();
                    instance.decode(ggepMessage);
                    return instance;
                } catch (InstantiationException ex) {
                    throw new BadGGEPBlockException("InstantiationException caught."
                            + ex.getMessage());
                } catch (IllegalAccessException ex) {
                    throw new BadGGEPBlockException("IllegalAccessException caught."
                            + ex.getMessage());
                }
            }
        }
        throw new BadGGEPBlockException("No parsers understood passed in type.");
    }

    /**
     * @return a message container parsed from the given bytes (which should be
     *         a raw GGEP), or an exception if there is a problem parsing the
     *         container
     */
    public MessageContainer parse(byte[] rawMessage) throws BadGGEPBlockException {
        return parse(new GGEP(rawMessage, 0));
    }

}
