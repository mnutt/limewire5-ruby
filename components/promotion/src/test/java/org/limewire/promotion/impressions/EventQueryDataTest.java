package org.limewire.promotion.impressions;

import junit.framework.Test;

public class EventQueryDataTest extends AbstractEventQueryDataBaseTestCase {

    public EventQueryDataTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(EventQueryDataTest.class);
    }

    public void test0Impressions() {
        getImpressions(0);
    }

    public void test1Impressions() {
        getImpressions(1);
    }

    public void test2Impressions() {
        getImpressions(2);
    }

    public void test3Impressions() {
        getImpressions(3);
    }

    public void test4Impressions() {
        getImpressions(4);
    }

    public void test5Impressions() {
        getImpressions(5);
    }

    public void test6Impressions() {
        getImpressions(6);
    }

    public void test7Impressions() {
        getImpressions(7);
    }

    public void test8Impressions() {
        getImpressions(8);
    }

    public void test9Impressions() {
        getImpressions(9);
    }
    
    public void test100Impressions() {
        getImpressions(100);
    }  
}
