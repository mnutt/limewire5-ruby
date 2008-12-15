package com.limegroup.gnutella;

import junit.framework.Test;

public class EndpointTest extends com.limegroup.gnutella.util.LimeTestCase {
    public EndpointTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(EndpointTest.class);
    }

    public void testLegacy() throws Exception {
        try {
            new Endpoint(":6347");
            fail("endpoint should not have been created");
        } catch (IllegalArgumentException exc) {}
        
        try {
            new Endpoint("abc:cas");
            fail("endpoint should not have been created");
        } catch (IllegalArgumentException exc) {}

        try {
            new Endpoint("abc");
            fail("endpoint should not have been created");
        } catch(IllegalArgumentException exc) {}

        try {
            new Endpoint("abc:");
            fail("endpoint should not have been created");
        } catch(IllegalArgumentException exc) {}

        try {
            new Endpoint("abc:7");
            fail("endpoint should not have been created");
        } catch(IllegalArgumentException exc) {}
        
        try {
            new Endpoint("0.0.0.0");
            fail("endpoint should not have been created");
        } catch(IllegalArgumentException exc) {}

        ////////////////////////// Subnet Tests ////////////////
        //These tests are incomplete since the methods are somewhat trivial.
        Endpoint e1;
        Endpoint e2;
        e1=new Endpoint("172.16.0.0",1);    e2=new Endpoint("172.16.0.1",1);
        assertTrue(e1.isSameSubnet(e2));
        assertTrue(e2.isSameSubnet(e1));
        e2=new Endpoint("18.239.0.1",1);
        assertTrue(! e2.isSameSubnet(e1));
        assertTrue(! e1.isSameSubnet(e2));

        e1=new Endpoint("192.168.0.1",1);    e2=new Endpoint("192.168.0.2",1);
        assertTrue(e1.isSameSubnet(e2));
        assertTrue(e2.isSameSubnet(e1));
        e2=new Endpoint("192.168.1.1",1);
        assertTrue(! e2.isSameSubnet(e1));
        assertTrue(! e1.isSameSubnet(e2));
    }
        
    public void testValidatedNumeric() {
        //Allowed 
        check("www.limewire.org:6346", false, true); //no verification
        //check("not a url:6346", false, true);
        check("10.255.25.0:6346", true, true);       //verified valid IP   
        check("64.61.25.172", true, true);

        //Disallowed
        //check("<html>hello</html>", true, false);    //not even close!
        check("18.239.0.144.215", true, false);      //too many dots
        check("18.A.0.144", true, false);            //not numeric
        check("18.256.0.144:6346", true, false);     //value too big
        check("18.10.-1.144:6346", true, false);     //value too small
        check("127.0.0.1:ABC", false, false);        //old rules still apply
    }

    /** Checks that an exception is thrown iff allowed==false. */
    private static void check(String hostAndPort, 
                              boolean requireNumeric, 
                              boolean allowed) {
        try {
            new Endpoint(hostAndPort, requireNumeric);
            assertTrue("Allowed: \""+hostAndPort+"\"", allowed);
        } catch (IllegalArgumentException e) {
            assertTrue("Disallowed: \""+hostAndPort+"\"", !allowed);
        }
    }
}
