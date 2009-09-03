package com.limegroup.gnutella.filters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.SimpleTimeZone;

import junit.framework.Test;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.HttpParams;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.limewire.core.settings.FilterSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;
import org.limewire.util.Visitor;

import com.google.inject.util.Providers;
import com.limegroup.gnutella.SpamServices;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;

public class URNBlacklistManagerImplTest extends LimeTestCase {

    // This file contains a single random URN and a valid signature
    private File validFile =
        TestUtils.getResourceFile("com/limegroup/gnutella/resources/urns.good");
    // This file contains the same signature, but the URN has been modified
    private File invalidFile =
        TestUtils.getResourceFile("com/limegroup/gnutella/resources/urns.bad");
    private URNBlacklistManagerImpl urnBlacklistManager;
    private Mockery context;
    private HttpExecutor httpExecutor;
    private HttpParams defaultParams;
    private SpamServices spamServices;
    private StubVisitor visitor;

    public URNBlacklistManagerImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(URNBlacklistManagerImplTest.class);
    }

    @Override
    public void setUp() {
        // This URL won't actually be hit, but we need to supply one
        FilterSettings.URN_BLACKLIST_UPDATE_URLS.set(
                new String[] {"http://127.0.0.1/"});
        context = new Mockery();
        httpExecutor = context.mock(HttpExecutor.class);
        defaultParams = context.mock(HttpParams.class);
        spamServices = context.mock(SpamServices.class);
        visitor = new StubVisitor();
        urnBlacklistManager = new URNBlacklistManagerImpl(
                Providers.of(httpExecutor), Providers.of(defaultParams),
                Providers.of(spamServices));
        context.assertIsSatisfied();
    }

    @Override
    public void tearDown() {
        FilterSettings.URN_BLACKLIST_UPDATE_URLS.revertToDefault();
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.revertToDefault();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.revertToDefault();
        urnBlacklistManager.getFile().delete();
    }

    public void testChecksForUpdatesAtStartupIfNeeded() {
        long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
        }});
        urnBlacklistManager.start();
        context.assertIsSatisfied();
    }

    public void testDoesNotCheckForUpdatesAtStartupIfNotNeeded() {
        long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now + 1000);
        urnBlacklistManager.start();
        context.assertIsSatisfied();        
    }

    public void testLoadingURNsTriggersCheckIfFileIsMissing() {
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
        }});
        urnBlacklistManager.loadURNs(visitor);
        assertFalse(urnBlacklistManager.getFile().exists());
        assertTrue(visitor.urns.isEmpty());
        context.assertIsSatisfied();
    }

    public void testLoadingURNsTriggersCheckIfFileIsEmpty() throws Exception {
        // Create an empty file
        assertTrue(urnBlacklistManager.getFile().createNewFile());
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
        }});
        urnBlacklistManager.loadURNs(visitor);
        assertTrue(urnBlacklistManager.getFile().exists());
        assertTrue(visitor.urns.isEmpty());
        context.assertIsSatisfied();        
    }

    public void testLoadingURNsTriggersCheckIfFileIsCorrupt() throws Exception {
        // Copy all but the last byte, so the file is invalid
        File f = urnBlacklistManager.getFile();
        FileUtils.copy(validFile, validFile.length() - 1, f);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
        }});
        urnBlacklistManager.loadURNs(visitor);
        assertTrue(urnBlacklistManager.getFile().exists());
        assertTrue(visitor.urns.isEmpty());
        context.assertIsSatisfied();        
    }

    public void testLoadingURNsTriggersCheckIfSignatureIsBad() throws Exception {
        FileUtils.copy(invalidFile, urnBlacklistManager.getFile());
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
        }});
        urnBlacklistManager.loadURNs(visitor);
        assertTrue(urnBlacklistManager.getFile().exists());
        assertTrue(visitor.urns.isEmpty());
        context.assertIsSatisfied();        
    }

    public void testLoadingURNsReturnsDataIfSignatureIsGood() throws Exception {
        FileUtils.copy(validFile, urnBlacklistManager.getFile());
        urnBlacklistManager.loadURNs(visitor);
        assertTrue(urnBlacklistManager.getFile().exists());
        assertEquals(1, visitor.urns.size());
        context.assertIsSatisfied();
    }

    public void testEmptyURLSettingSetsNextUpdateTime() throws Exception {
        FilterSettings.URN_BLACKLIST_UPDATE_URLS.set(new String[0]);
        long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        urnBlacklistManager.start();
        context.assertIsSatisfied();
        assertNextUpdateTimeIsSet(now);
    }

    public void testFailedHeadRequestSetsNextUpdateTime() {
        long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        final HttpResponse response = context.mock(HttpResponse.class);
        final StatusLine statusLine = context.mock(StatusLine.class);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new FailedRequestAction(response));
            allowing(response).getStatusLine();
            will(returnValue(statusLine));
        }});
        urnBlacklistManager.start();
        context.assertIsSatisfied();
        assertNextUpdateTimeIsSet(now);
    }

    public void testMissingLastModifiedHeaderSetsNextUpdateTime() {
        long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        final HttpResponse response = context.mock(HttpResponse.class);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getFirstHeader("Last-Modified");
            will(returnValue(null));
        }});
        urnBlacklistManager.start();
        context.assertIsSatisfied();
        assertNextUpdateTimeIsSet(now);
    }

    public void testStaleLastModifiedHeaderSetsNextUpdateTime() {
        final long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.set(now - 2000);
        final HttpResponse response = context.mock(HttpResponse.class);
        final Header header = context.mock(Header.class);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getFirstHeader("Last-Modified");
            will(returnValue(header));
            allowing(header).getValue();
            will(returnValue(dateString(now - 3000))); // Stale
        }});
        urnBlacklistManager.start();
        context.assertIsSatisfied();
        assertNextUpdateTimeIsSet(now);
    }

    public void testFreshLastModifiedHeaderTriggersGetRequest() {
        final long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.set(now - 2000);
        final HttpResponse response = context.mock(HttpResponse.class);
        final Header header = context.mock(Header.class);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getFirstHeader("Last-Modified");
            will(returnValue(header));
            allowing(header).getValue();
            will(returnValue(dateString(now))); // Fresh
            one(httpExecutor).execute(with(any(HttpGet.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
        }});
        urnBlacklistManager.start();
        context.assertIsSatisfied();
    }

    public void testFailedGetRequestSetsNextUpdateTime() {
        final long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.set(now - 2000);
        final HttpResponse response = context.mock(HttpResponse.class);
        final Header header = context.mock(Header.class);
        final StatusLine statusLine = context.mock(StatusLine.class);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getFirstHeader("Last-Modified");
            will(returnValue(header));
            allowing(header).getValue();
            will(returnValue(dateString(now))); // Fresh
            one(httpExecutor).execute(with(any(HttpGet.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new FailedRequestAction(response));
            allowing(response).getStatusLine();
            will(returnValue(statusLine));
        }});
        urnBlacklistManager.start();
        context.assertIsSatisfied();        
        assertNextUpdateTimeIsSet(now);
    }

    public void testMissingBodySetsNextUpdateTime() {
        final long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.set(now - 2000);
        final HttpResponse response = context.mock(HttpResponse.class);
        final Header header = context.mock(Header.class);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getFirstHeader("Last-Modified");
            will(returnValue(header));
            allowing(header).getValue();
            will(returnValue(dateString(now))); // Fresh
            one(httpExecutor).execute(with(any(HttpGet.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getEntity();
            will(returnValue(null));
        }});
        urnBlacklistManager.start();
        context.assertIsSatisfied();        
        assertNextUpdateTimeIsSet(now);        
    }

    public void testBodyIsWrittenToFileAndSpamFiltersAreReloaded()
    throws IOException {
        final long now = System.currentTimeMillis();
        FilterSettings.NEXT_URN_BLACKLIST_UPDATE.set(now - 1000);
        FilterSettings.LAST_URN_BLACKLIST_UPDATE.set(now - 2000);
        File f = urnBlacklistManager.getFile();
        f.delete();
        final HttpResponse response = context.mock(HttpResponse.class);
        final Header header = context.mock(Header.class);
        final HttpEntity entity = context.mock(HttpEntity.class);
        context.checking(new Expectations() {{
            one(httpExecutor).execute(with(any(HttpHead.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getFirstHeader("Last-Modified");
            will(returnValue(header));
            allowing(header).getValue();
            will(returnValue(dateString(now))); // Fresh
            one(httpExecutor).execute(with(any(HttpGet.class)),
                    with(any(HttpParams.class)),
                    with(any(HttpClientListener.class)));
            will(new SuccessfulRequestAction(response));
            one(response).getEntity();
            will(returnValue(entity));
            one(entity).writeTo(with(any(FileOutputStream.class)));
            one(spamServices).reloadSpamFilters();
        }});
        assertFalse(f.exists());
        urnBlacklistManager.start();
        assertTrue(f.exists());
        context.assertIsSatisfied();
        assertNextUpdateTimeIsSet(now);
    }

    private void assertNextUpdateTimeIsSet(long now) {
        long last = FilterSettings.LAST_URN_BLACKLIST_UPDATE.getValue();
        long next = FilterSettings.NEXT_URN_BLACKLIST_UPDATE.getValue();
        assertGreaterThanOrEquals(now, last);
        assertGreaterThan(now, next);
    }

    private String dateString(long time) {
        // Never trust a class with 'simple' in its name
        SimpleDateFormat sdf =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        return sdf.format(new Date(time));
    }

    private static class FailedRequestAction extends CustomAction {

        private final HttpResponse response;

        FailedRequestAction(HttpResponse response) {
            super("Failed HTTP request");
            this.response = response;
        }

        @Override
        public Object invoke(Invocation invocation) {
            HttpUriRequest request =
                (HttpUriRequest)invocation.getParameter(0);
            HttpClientListener listener =
                (HttpClientListener)invocation.getParameter(2);
            listener.requestFailed(request, response, new IOException("Mock"));
            return null;
        }
    }

    private static class SuccessfulRequestAction extends CustomAction {

        private final HttpResponse response;

        SuccessfulRequestAction(HttpResponse response) {
            super("Successful HTTP request");
            this.response = response;
        }

        @Override
        public Object invoke(Invocation invocation) {
            HttpUriRequest request =
                (HttpUriRequest)invocation.getParameter(0);
            HttpClientListener listener =
                (HttpClientListener)invocation.getParameter(2);
            listener.requestComplete(request, response);
            return null;
        }
    }

    private static class StubVisitor implements Visitor<String> {
        ArrayList<String> urns = new ArrayList<String>();

        @Override
        public boolean visit(String s) {
            urns.add(s);
            return true;
        }
    }
}
