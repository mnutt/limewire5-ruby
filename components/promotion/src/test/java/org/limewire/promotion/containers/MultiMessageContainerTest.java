package org.limewire.promotion.containers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

public class MultiMessageContainerTest extends BaseTestCase {
    public MultiMessageContainerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(MultiMessageContainerTest.class);
    }

    public void testEncodeDecodeCycle() throws InterruptedException {
        MultiMessageContainer message = new MultiMessageContainer();

        PromotionMessageContainer promoMessage1 = getPromoMessage("promo1");
        PromotionMessageContainer promoMessage2 = getPromoMessage("promo2");

        List<MessageContainer> list = new ArrayList<MessageContainer>();
        list.add(promoMessage1);
        list.add(promoMessage2);

        message.setWrappedMessages(list);

        List<MessageContainer> list2 = message.getWrappedMessages();

        assertEquals(list.size(), list2.size());

        PromotionMessageContainer decodedMessage1 = (PromotionMessageContainer) list2.get(0);
        PromotionMessageContainer decodedMessage2 = (PromotionMessageContainer) list2.get(1);

        assertEquals(promoMessage1.getDescription(), decodedMessage1.getDescription());
        assertEquals(promoMessage2.getDescription(), decodedMessage2.getDescription());
    }

    public void testEmptyCycle() {
        MultiMessageContainer message = new MultiMessageContainer();
        message.setWrappedMessages(new ArrayList<MessageContainer>());
        List<MessageContainer> list = message.getWrappedMessages();
        assertEquals(0, list.size());
    }

    private PromotionMessageContainer getPromoMessage(String fillerString) {
        PromotionMessageContainer message = new PromotionMessageContainer();
        message.setUniqueID(System.currentTimeMillis());

        message.setOptions(new PromotionMessageContainer.PromotionOptions());
        message.setDescription(fillerString);
        message.setKeywords(fillerString);
        message.setURL(fillerString);

        try {
            TimeUnit.MILLISECONDS.sleep(1);
        } catch (InterruptedException ignored) {
        }
        return message;
    }
}
