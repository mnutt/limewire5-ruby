package org.limewire.promotion.containers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.limewire.util.StringUtils;

/**
 * A container that extends the {@link MultiMessageContainer}, adding start and
 * end validity dates along with an enforcement of the types of messages that
 * will be contained within this message.
 */
public class BucketMessageContainer extends MultiMessageContainer {
    private static final String KEY_VALID_START = "vs";

    private static final String KEY_VALID_END = "ve";

    private static final String KEY_NAME = "vn";

    private static final String KEY_BUCKET_NUMBER = "vu";

    @Override
    public byte[] getType() {
        return StringUtils.toUTF8Bytes("BKIT");
    }

    /**
     * Set a unique name for this bucket, such as "[0103]20080201", for future
     * lookups. Defaults to currentTimeMillis/1000. Cannot be null.
     */
    public void setName(final String bucketName) {
        put(KEY_NAME, bucketName);
    }

    public String getName() {
        final String name = getString(KEY_NAME);
        if (name == null) {
            setName((System.currentTimeMillis() / 1000) + "");
            return getName();
        }
        return name;
    }

    /**
     * Sets the earliest valid start date for all contained promotion messages.
     * If a contained message also has a start date, the later of the two dates
     * will be the correct value. This date defaults to Date(Long.MAX_VALUE), so
     * this date must be set explicitly for this bucket to ever be valid. Cannot
     * be null.
     */
    public void setValidStart(Date date) {
        put(KEY_VALID_START, date);
    }

    /**
     * @return the earliest valid start date for all contained promotion
     *         messages. If never set, defaults to Date(Long.MAX_VALUE), so this
     *         date must be set explicitly for this bucket to ever be valid.
     */
    public Date getValidStart() {
        Date date = getDate(KEY_VALID_START);
        if (date == null)
            return new Date(Long.MAX_VALUE);
        return date;
    }

    /**
     * Sets the latest valid end date for all contained promotion messages. If a
     * contained message also has an end date, the earlier of the two dates will
     * be the correct value.This date defaults to Date(0), so this date must be
     * set explicitly for this bucket to ever be valid. Cannot be null.
     */
    public void setValidEnd(Date date) {
        put(KEY_VALID_END, date);
    }

    /**
     * @return the earliest valid start date for all contained promotion
     *         messages. If never set, defaults to Date(Long.MAX_VALUE), so this
     *         date must be set explicitly for this bucket to ever be valid.
     */
    public Date getValidEnd() {
        Date date = getDate(KEY_VALID_END);
        if (date == null)
            return new Date(0);
        return date;
    }

    /**
     * Overrides the super version to enforce via RTE that all messages are
     * instances of {@link PromotionMessageContainer}.
     * 
     * @see MultiMessageContainer#setWrappedMessages(List)
     */
    @Override
    public void setWrappedMessages(final List<MessageContainer> messages) {
        for (MessageContainer message : messages) {
            if (!(message instanceof PromotionMessageContainer))
                throw new RuntimeException("All wrapped messages must be of type "
                        + PromotionMessageContainer.class.getName());
        }
        super.setWrappedMessages(messages);
    }

    /**
     * Wraps the {@link #getWrappedMessages()} method and returns a correctly
     * cast list, with any non- {@link PromotionMessageContainer} instances
     * filtered out of the list.
     */
    public List<PromotionMessageContainer> getPromoMessages() {
        final List<PromotionMessageContainer> list = new ArrayList<PromotionMessageContainer>();
        for (MessageContainer message : getWrappedMessages()) {
            if (message instanceof PromotionMessageContainer)
                list.add((PromotionMessageContainer) message);
        }
        return list;
    }

    /**
     * @param bucket Records the bucket number that this collection of messages
     *        represents
     */
    public void setBucketNumber(int bucket) {
        put(KEY_BUCKET_NUMBER, bucket);
    }

    /**
     * @return The bucket number as set, or -1 if the bucket wasn't set or there
     *         was a problem parsing.
     */
    public int getBucketNumber() {
        Long num = getLong(KEY_BUCKET_NUMBER);
        if (num == null)
            return -1;
        return num.intValue();
    }
}
