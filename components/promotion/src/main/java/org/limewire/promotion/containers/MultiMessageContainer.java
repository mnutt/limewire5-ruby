package org.limewire.promotion.containers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.GGEP;
import org.limewire.io.IOUtils;
import org.limewire.util.StringUtils;

/**
 * Wraps multiple messages into one message, generally so you can stuff a bunch
 * of messages inside a {@link SignedMessageContainer}.
 */
public class MultiMessageContainer extends MapMessageContainer {
    private static final String KEY_WRAPPED_BYTES = "W";

    public byte[] getType() {
        return StringUtils.toUTF8Bytes("MULT");
    }

    @Override
    public void decode(final GGEP rawGGEP) throws BadGGEPBlockException {
        if (!rawGGEP.hasKey(KEY_WRAPPED_BYTES))
            throw new BadGGEPBlockException("Missing wrappedBytes");
        super.decode(rawGGEP);
    }

    /**
     * @return the wrapped bytes, or a new empty array if there is a problem
     *         loading the array.
     */
    private byte[] getWrappedBytes() {
        final byte[] bytes = getBytes(KEY_WRAPPED_BYTES);
        try {
            if (bytes != null)
                return IOUtils.inflate(bytes);
        } catch (IOException ignored) {
        }
        return new byte[0];
    }

    /**
     * @return a list of wrapped messages, in the same order they were added.
     *         This method does not cache results, so a second call will create
     *         a new list with new instances of the wrapped messages.
     */
    public List<MessageContainer> getWrappedMessages() {
        final List<MessageContainer> list = new ArrayList<MessageContainer>();
        final byte[] bytes = getWrappedBytes();
        if (bytes.length > 0) {
            final int[] nextOffset = new int[1];
            int offset = 0;
            final MessageContainerParser parser = new MessageContainerParser();
            while (offset < bytes.length) {
                try {
                    list.add(parser.parse(new GGEP(bytes, offset, nextOffset)));
                    offset = nextOffset[0];
                } catch (BadGGEPBlockException ex) {
                    throw new RuntimeException("Parsing error: ", ex);
                }
            }
        }
        return list;
    }

    /**
     * Takes the given list and encodes it into the wrapping system of this
     * message, so later changes to the given list will NOT be reflected by this
     * container.
     */
    public void setWrappedMessages(final List<MessageContainer> messages) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (MessageContainer message : messages) {
            try {
                out.write(message.encode());
            } catch (IOException ex) {
                throw new RuntimeException("IOException? WTF?", ex);
            }
        }
        put(KEY_WRAPPED_BYTES, IOUtils.deflate(out.toByteArray()));
    }
}
