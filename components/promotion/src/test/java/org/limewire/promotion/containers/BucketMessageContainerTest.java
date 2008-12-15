package org.limewire.promotion.containers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class BucketMessageContainerTest extends BaseTestCase {
    public BucketMessageContainerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(BucketMessageContainerTest.class);
    }

    public void testNameCycle() {
        BucketMessageContainer message = new BucketMessageContainer();
        assertNotNull(message.getName());
        message.setName("beano");
        assertEquals("beano", message.getName());
    }

    public void testStartDateCycle() {
        BucketMessageContainer message = new BucketMessageContainer();
        assertEquals(Long.MAX_VALUE, message.getValidStart().getTime());

        Date date = new Date();
        message.setValidStart(date);
        assertEquals(date, message.getValidStart());
    }

    public void testEndDateCycle() {
        BucketMessageContainer message = new BucketMessageContainer();
        assertEquals(0, message.getValidEnd().getTime());

        Date date = new Date();
        message.setValidEnd(date);
        assertEquals(date, message.getValidEnd());
    }

    public void testBadMessageListInsert() {
        BucketMessageContainer message = new BucketMessageContainer();
        List<MessageContainer> list = new ArrayList<MessageContainer>();
        list.add(message);
        try {
            message.setWrappedMessages(list);
            fail("Expected an exception here.");
        } catch (RuntimeException expected) {
        }
    }

}
