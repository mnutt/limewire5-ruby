package com.limegroup.gnutella.lws.server;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;


import com.limegroup.gnutella.downloader.LWSIntegrationServices;
import com.limegroup.gnutella.util.LimeWireUtils;

import junit.framework.Test;
import junit.textui.TestRunner;

/**
 * Tests the <code>GetInfo</code> command.
 */
public class GetInfoTest extends AbstractCommunicationSupportWithNoLocalServer {

    public GetInfoTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(GetInfoTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    public void testGetInfo() {
        String res = sendCommandToClient("GetInfo", EMPTY_ARGS);
        //
        // This is going to be of the form
        //
        // ( <name> '=' <value> '\t' )*
        //
        Map<String, String> props = new HashMap<String, String>();
        for (StringTokenizer st = new StringTokenizer(res, "\t", false); st.hasMoreTokens();) {
            String[] parts = st.nextToken().split("=");
            String key = parts[0];
            String val = parts[1];
            props.put(key, val);
        }
        //
        // These could possibly change but currently are the properties we
        // return for this call
        //
        eq(props, LimeWireUtils.isAlphaRelease(), LWSIntegrationServices.Info.IsAlphaRelease);
        eq(props, LimeWireUtils.getMinorVersionNumber(),LWSIntegrationServices.Info.MinorVersionNumber);
        eq(props, LimeWireUtils.isPro(), LWSIntegrationServices.Info.IsPro);
        eq(props, LimeWireUtils.getMajorVersionNumber(), LWSIntegrationServices.Info.MajorVersionNumber);
        eq(props, LimeWireUtils.getServiceVersionNumber(), LWSIntegrationServices.Info.ServiceVersionNumber);
        eq(props, LimeWireUtils.isBetaRelease(), LWSIntegrationServices.Info.IsBetaRelease);
        eq(props, LimeWireUtils.getVendor(), LWSIntegrationServices.Info.Vendor);
        eq(props, LimeWireUtils.getLimeWireVersion(), LWSIntegrationServices.Info.Version);

    }

    /**
     * Asserts that the String value of <code>want</code> is mapped to in
     * <code>props</code> by the value of <code>haveKey</code>.
     */
    private void eq(Map<String, String> props, Object want, LWSIntegrationServices.Info haveKey) {
        assertEquals(String.valueOf(want), props.get(haveKey.getValue()));
    }

}
