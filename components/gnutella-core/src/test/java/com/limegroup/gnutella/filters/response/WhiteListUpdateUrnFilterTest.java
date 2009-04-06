package com.limegroup.gnutella.filters.response;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.util.BaseTestCase;
import org.jmock.Mockery;
import org.jmock.Expectations;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.version.UpdateHandler;
import com.limegroup.gnutella.version.UpdateCollection;
import com.limegroup.gnutella.version.UpdateCollectionFactory;
import com.google.inject.Injector;

/**
 * Test for the WhiteListUpdateUrnFilter filter
 */
public class WhiteListUpdateUrnFilterTest extends BaseTestCase {

    private static final String MATCHING_SHA1_HASH = "QLFYWY2RI5WZCTEP6MJKR5CAFGP7FQ5X";

    private static final String UPDATE_DATA_XML =
        "<update id='42' timestamp=\"150973213135\">" +
            "<msg for='4.2.18' url='http://limewire.com/hi' style='4' urn='" +
                "urn:bitprint:QLFYIJMMI56ZCTEP6MJKR5CAFGP7FQ5X.VEKXTRSJOTZJLY2IKG5FQ2TCXK26SECFPP4DX7I" + "'>" +
                "<lang id='en'>testivgfhghng testing.</lang>" +
            "</msg>" +
            "<msg for='5.0.11' url='http://limewire.com/lo' style='4' urn='" +
                "urn:bitprint:" + MATCHING_SHA1_HASH + ".VEKXTRSJOTZJLY2IKG5FQ2TCXK26SECFPP4DX7I" + "'>" +
                "<lang id='en'>testing testgfhghghing.</lang>" +
            "</msg>" +
            "<msg for='4.1.2' url='http://limewire.com/hf' style='4' urn='" +
                "urn:bitprint:QLFYIJKLI5WZCTEP6MJKR5CAFGP7FQ5X.VEKXTRSJOTZJLY2IKG5FQ2TCXK26SECFPP4DX7I" + "'>" +
                "<lang id='en'>testifgfgfgng testing.</lang>" +
            "</msg>" +
        "</update>";


    private Mockery context;

    // mock out Response#getUrns
    private Response response;

    // mock out UpdateHandler#getUpdateCollection
    private UpdateHandler updateHandler;

    // ignored thus far because current impl of WhiteListUpdateUrnFilter ignores it
    private QueryReply qr = null;


    public WhiteListUpdateUrnFilterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(WhiteListUpdateUrnFilterTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        response = context.mock(Response.class);
        updateHandler = context.mock(UpdateHandler.class);
    }


    /**
     * Response does not contain a SHA1 URN
     */
    public void testResponseUrnsDoNotContainSha1() throws Exception {
        context.checking(new Expectations() {{
            one(response).getUrns();
            will(returnValue(Collections.emptySet()));
            ignoring(updateHandler);
        }});

        WhiteListUpdateUrnFilter whiteListFilter = new WhiteListUpdateUrnFilter(updateHandler);
        assertFalse(whiteListFilter.allow(qr, response));
    }

    /**
     *  Response contains SHA1 URN, but the update handler contains no update collection
     */
    public void testUpdateHandlerDoesNotContainUpdateCollection() throws Exception {
        // response has at least 1 sha1, updatehandler.getUpdateCollection returns null
        final Set<URN> urnSetWtihSha1 = new HashSet<URN>();
        urnSetWtihSha1.add(URN.createSHA1Urn("urn:sha1:" + MATCHING_SHA1_HASH));

        context.checking(new Expectations() {{
            one(updateHandler).getUpdateCollection();
            will(returnValue(null));
            one(response).getUrns();
            will(returnValue(urnSetWtihSha1));
        }});

        WhiteListUpdateUrnFilter whiteListFilter = new WhiteListUpdateUrnFilter(updateHandler);
        assertFalse(whiteListFilter.allow(qr, response));
    }


    /**
     * Response contains SHA1 URN, and it matches a URN in the update data
     */
    public void testSha1UrnMatchesInUpdateData() throws Exception {

        Injector injector = LimeTestUtils.createInjector();
		UpdateCollectionFactory updateCollectionFactory =
                injector.getInstance(UpdateCollectionFactory.class);

        final UpdateCollection updateColl =
                updateCollectionFactory.createUpdateCollection(UPDATE_DATA_XML);

        final Set<URN> urnSetWtihSha1 = new HashSet<URN>();
        String matchingSha1HashUrnString = "urn:sha1:" + MATCHING_SHA1_HASH;
        urnSetWtihSha1.add(URN.createSHA1Urn(matchingSha1HashUrnString));

        context.checking(new Expectations() {{
            one(updateHandler).getUpdateCollection();
            will(returnValue(updateColl));
            one(response).getUrns();
            will(returnValue(urnSetWtihSha1));
        }});

        WhiteListUpdateUrnFilter whiteListFilter = new WhiteListUpdateUrnFilter(updateHandler);
        assertTrue(whiteListFilter.allow(qr, response));
    }


    /**
     * Response contains SHA1 URN, and it does NOT match any of the URNs in the update data
     */
    public void testSha1UrnDoesNotMatchAnyUrnInUpdateData() throws Exception {
        Injector injector = LimeTestUtils.createInjector();
        UpdateCollectionFactory updateCollectionFactory =
                injector.getInstance(UpdateCollectionFactory.class);

        final UpdateCollection updateColl =
                updateCollectionFactory.createUpdateCollection(UPDATE_DATA_XML);

        final Set<URN> urnSetWtihSha1 = new HashSet<URN>();
        urnSetWtihSha1.add(URN.createSHA1Urn("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

        context.checking(new Expectations() {{
                one(updateHandler).getUpdateCollection();
                will(returnValue(updateColl));
                one(response).getUrns();
                will(returnValue(urnSetWtihSha1));
            }
        });

        WhiteListUpdateUrnFilter whiteListFilter = new WhiteListUpdateUrnFilter(updateHandler);
        assertFalse(whiteListFilter.allow(qr, response));
    }
}
