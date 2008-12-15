package com.limegroup.gnutella.version;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Test;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.limewire.core.settings.UpdateSettings;
import org.limewire.security.SettingsProvider;
import org.limewire.util.Clock;
import org.limewire.util.CommonUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.name.Names;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.ClockStub;
import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.http.HttpExecutor;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.messages.vendor.CapabilitiesVMFactory;
import com.limegroup.gnutella.stubs.ScheduledExecutorServiceStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class UpdateHandlerTest extends LimeTestCase {
    
    public UpdateHandlerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UpdateHandlerTest.class);
    }

    private Injector injector;

    private Mockery mockery;

    private ImmediateExecutor backgroundExecutor;

    private ActivityCallback activityCallback;

    private HttpExecutor httpExecutor;

    private CapabilitiesVMFactory capabilitiesVmFactory;

    private ConnectionManager connectionManager;

    private FileManager fileManager;

    private ApplicationServices applicationServices;

    private UpdateCollectionFactory updateCollectionFactory;

    private ClockStub clock;

    private DownloadManager downloadManager;

    private SettingsProvider settingsProvider;

    private UpdateMessageVerifier updateMessageVerifier;

    private File saveFile;

    @Override
    public void setUp() {
        mockery = new Mockery();
        activityCallback = mockery.mock(ActivityCallback.class);
        httpExecutor = mockery.mock(HttpExecutor.class);
        capabilitiesVmFactory = mockery.mock(CapabilitiesVMFactory.class);
        connectionManager = mockery.mock(ConnectionManager.class);
        fileManager = mockery.mock(FileManager.class);
        applicationServices = mockery.mock(ApplicationServices.class);
        updateCollectionFactory = mockery.mock(UpdateCollectionFactory.class);
        downloadManager = mockery.mock(DownloadManager.class);
        backgroundExecutor = new ImmediateExecutor();
        updateMessageVerifier = mockery.mock(UpdateMessageVerifier.class);
        clock = new ClockStub();
        settingsProvider = new SettingsProvider() {
            public long getChangePeriod() {
                return Long.MAX_VALUE;
            }

            public long getGracePeriod() {
                return Long.MAX_VALUE - 1;
            }
        };

        injector = LimeTestUtils.createInjector(new AbstractModule() {
            @Override
            public void configure() {
                bind(ScheduledExecutorService.class).annotatedWith(
                        Names.named("backgroundExecutor")).toInstance(backgroundExecutor);
                bind(ActivityCallback.class).toInstance(activityCallback);
                bind(HttpExecutor.class).toInstance(httpExecutor);
                bind(CapabilitiesVMFactory.class).toInstance(capabilitiesVmFactory);
                bind(ConnectionManager.class).toInstance(connectionManager);
                bind(FileManager.class).toInstance(fileManager);
                bind(ApplicationServices.class).toInstance(applicationServices);
                bind(UpdateCollectionFactory.class).toInstance(updateCollectionFactory);
                bind(Clock.class).toInstance(clock);
                bind(DownloadManager.class).toInstance(downloadManager);
                bind(SettingsProvider.class).toInstance(settingsProvider);
                bind(UpdateMessageVerifier.class).toInstance(updateMessageVerifier);
            }
        });

        saveFile = new File(CommonUtils.getUserSettingsDir(), "version.xml");
        saveFile.delete();
        assertFalse(saveFile.exists());
    }

    /** tests that we set up bindings correctly */
    public void testBindings() throws Exception {
        mockery.checking(new Expectations() {
            {
                ignoring(activityCallback);
                ignoring(httpExecutor);
                ignoring(capabilitiesVmFactory);
                ignoring(connectionManager);
                ignoring(fileManager);
                ignoring(applicationServices);
                ignoring(updateCollectionFactory);
                ignoring(downloadManager);
            }
        });
        UpdateHandlerImpl updateHandler = (UpdateHandlerImpl) injector
                .getInstance(UpdateHandler.class);
        assertEquals("http://update0.limewire.com/v2/update.def", updateHandler.getTimeoutUrl());
        List<String> maxUrls = updateHandler.getMaxUrls();
        for (int i = 0; i < 10; i++)
            assertEquals("http://update" + (i + 1) + ".limewire.com/v2/update.def", maxUrls.get(i));
        assertEquals(10, maxUrls.size());
    }

    public void testMaxTriggersHttpAfterSmallDelay() {
        final AtomicReference<HttpClientListener> httpClientListenerRef = new AtomicReference<HttpClientListener>();
        final UpdateCollection updateCollection = mockery.mock(UpdateCollection.class);
        final HttpGet method = new HttpGet();
        final HttpResponse response = mockery.mock(HttpResponse.class);
        final StatusLine statusLine = mockery.mock(StatusLine.class);
        final Sequence requestSequence = mockery.sequence("Request Sequence");
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(new byte[0]);
                will(returnValue("asdf\n"));
                inSequence(requestSequence);

                one(updateCollectionFactory).createUpdateCollection("asdf\n");
                will(returnValue(updateCollection));
                inSequence(requestSequence);

                atLeast(1).of(updateCollection).getId();
                will(returnValue(Integer.MAX_VALUE));
                inSequence(requestSequence);

                allowing(applicationServices).getMyGUID();
                will(returnValue(new byte[16]));
                inSequence(requestSequence);
            }
        });

        UpdateHandlerImpl h = (UpdateHandlerImpl) injector.getInstance(UpdateHandler.class);
        assertEquals(0, h.getLatestId());
        h.setMaxUrls(Arrays.asList("http://127.0.0.1:9999/update.def"));
        h.setSilentPeriodForMaxHttpRequest(0);
        backgroundExecutor.scheduled = null;
        clock.setNow(12345);
        h.handleNewData(new byte[0], null);
        
        assertGreaterThanOrEquals(1000 * 60, backgroundExecutor.getInitialDelay());
        assertLessThanOrEquals(1000 * 60 * 31, backgroundExecutor.getInitialDelay());
        assertEquals(-1, backgroundExecutor.getPeriod());
        assertNotNull(backgroundExecutor.scheduled);

        mockery.checking(new Expectations() {
            {
                one(httpExecutor).execute(with(new TypeSafeMatcher<HttpGet>() {
                    public void describeTo(org.hamcrest.Description description) {
                        description.appendText("httpMethod");
                    }

                    @Override
                    public boolean matchesSafely(HttpGet item) {
                        assertEquals("GET", item.getMethod());
                        assertTrue(item.getURI().toString(), item.getURI().toString()
                                    .startsWith("http://127.0.0.1:9999/update.def?"));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpParams>() {
                    public void describeTo(org.hamcrest.Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpParams item) {
                        assertEquals(10000, HttpConnectionParams.getConnectionTimeout(item));
                        assertEquals(10000, HttpConnectionParams.getSoTimeout(item));
                        return true;
                    }
                }),
                    with(new TypeSafeMatcher<HttpClientListener>() {
                    public void describeTo(Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpClientListener item) {
                        httpClientListenerRef.set(item);
                        return true;
                    }
                }));
                inSequence(requestSequence);
            }
        });

        backgroundExecutor.scheduled.run();

        assertNotNull(httpClientListenerRef.get());

        mockery.checking(new Expectations() {
            {
                atLeast(1).of(response).getStatusLine();
                will(returnValue(statusLine));
                inSequence(requestSequence);
                
                atLeast(1).of(statusLine).getStatusCode();
                will(returnValue(100));
                inSequence(requestSequence);

                one(httpExecutor).releaseResources(with(same(response)));
                inSequence(requestSequence);
            }
        });

        httpClientListenerRef.get().requestComplete(method, response);
        assertEquals(12345, UpdateSettings.LAST_HTTP_FAILOVER.getValue());
        assertEquals(0, h.getLatestId());
        assertFalse(saveFile.exists());

        mockery.assertIsSatisfied();
    }

    public void testNetworkMaxIsNotSavedToDiskAndNotForwarded() throws Exception {
        final UpdateCollection updateCollection = mockery.mock(UpdateCollection.class);
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(new byte[0]);
                will(returnValue("asdf\n"));

                one(updateCollectionFactory).createUpdateCollection("asdf\n");
                will(returnValue(updateCollection));

                allowing(applicationServices).getMyGUID();
                will(returnValue(new byte[16]));

                atLeast(1).of(updateCollection).getId();
                will(returnValue(Integer.MAX_VALUE));
            }
        });

        UpdateHandlerImpl h = (UpdateHandlerImpl) injector.getInstance(UpdateHandler.class);
        assertEquals(0, h.getLatestId());
        h.setSilentPeriodForMaxHttpRequest(0);
        backgroundExecutor.scheduled = null;
        clock.setNow(12345);
        h.handleNewData(new byte[0], null);
        assertNotNull(backgroundExecutor.scheduled);
        assertEquals(0, h.getLatestId());
        assertFalse(saveFile.exists());

        Thread.sleep(5000); // sleep a bit just to make sure it didn't take a
        // while to save.
        assertEquals(0, h.getLatestId());
        assertFalse(saveFile.exists());

        mockery.assertIsSatisfied();

    }

    public void testHttpMaxIsSavedToDiskAndForwards() throws Exception {
        final AtomicReference<HttpClientListener> httpClientListenerRef = new AtomicReference<HttpClientListener>();
        final UpdateCollection updateCollection = mockery.mock(UpdateCollection.class);
        final Sequence requestSequence = mockery.sequence("Request Sequence");
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(new byte[0]);
                will(returnValue("asdf\n"));
                inSequence(requestSequence);

                one(updateCollectionFactory).createUpdateCollection("asdf\n");
                will(returnValue(updateCollection));
                inSequence(requestSequence);

                atLeast(1).of(updateCollection).getId();
                will(returnValue(Integer.MAX_VALUE));
                inSequence(requestSequence);

                allowing(applicationServices).getMyGUID();
                will(returnValue(new byte[16]));
                inSequence(requestSequence);
            }
        });

        UpdateHandlerImpl h = (UpdateHandlerImpl) injector.getInstance(UpdateHandler.class);
        assertEquals(0, h.getLatestId());
        h.setMaxUrls(Arrays.asList("http://127.0.0.1:9999/update.def"));
        h.setSilentPeriodForMaxHttpRequest(0);
        backgroundExecutor.scheduled = null;
        clock.setNow(12345);
        h.handleNewData(new byte[0], null);
        assertNotNull(backgroundExecutor.scheduled);

        mockery.checking(new Expectations() {
            {
                one(httpExecutor).execute(with(new TypeSafeMatcher<HttpGet>() {
                    public void describeTo(org.hamcrest.Description description) {
                        description.appendText("httpMethod");
                    }

                    @Override
                    public boolean matchesSafely(HttpGet item) {
                        assertEquals("GET", item.getMethod());
                        assertTrue(item.getURI().toString(), item.getURI().toString()
                                .startsWith("http://127.0.0.1:9999/update.def?"));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpParams>() {
                    public void describeTo(org.hamcrest.Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpParams item) {
                        assertEquals(10000, HttpConnectionParams.getConnectionTimeout(item));
                        assertEquals(10000, HttpConnectionParams.getSoTimeout(item));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpClientListener>() {
                    public void describeTo(Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpClientListener item) {
                        httpClientListenerRef.set(item);
                        return true;
                    }
                }));
                inSequence(requestSequence);
            }
        });

        backgroundExecutor.scheduled.run();
        assertNotNull(httpClientListenerRef.get());

        final HttpGet method = new HttpGet();
        final HttpResponse response = mockery.mock(HttpResponse.class);
        final StatusLine statusLine = mockery.mock(StatusLine.class);
        final HttpEntity httpEntity = mockery.mock(HttpEntity.class);
        final UpdateCollection httpCollection = mockery.mock(UpdateCollection.class);
        mockery.checking(new Expectations() {
            {
                atLeast(1).of(response).getStatusLine();
                will(returnValue(statusLine));
                inSequence(requestSequence);
                
                atLeast(1).of(statusLine).getStatusCode();
                will(returnValue(200));
                inSequence(requestSequence);                

                atLeast(1).of(response).getEntity();
                will(returnValue(httpEntity));
                inSequence(requestSequence);
                
                one(httpEntity).getContent();
                byte [] b = new byte[1];
                ByteArrayInputStream bis = new ByteArrayInputStream(b);
                will(returnValue(bis));
                inSequence(requestSequence);

                one(updateMessageVerifier).inflateNetworkData(with(LimeTestUtils.createByteMatcher(b)));
                byte[] inflated = new byte[2];
                inSequence(requestSequence);
                will(returnValue(inflated));

                one(httpExecutor).releaseResources(with(same(response)));
                inSequence(requestSequence);

                one(updateMessageVerifier).getVerifiedData(with(same(inflated)));
                inSequence(requestSequence);
                will(returnValue("http response"));

                one(updateCollectionFactory).createUpdateCollection("http response");
                inSequence(requestSequence);
                will(returnValue(httpCollection));

                atLeast(1).of(httpCollection).getId();
                inSequence(requestSequence);
                will(returnValue(Integer.MAX_VALUE));

                one(httpCollection).getTimestamp();
                inSequence(requestSequence);
                will(returnValue(54321L));

                one(capabilitiesVmFactory).updateCapabilities();
                inSequence(requestSequence);

                one(connectionManager).sendUpdatedCapabilities();
                inSequence(requestSequence);
            }
        });

        httpClientListenerRef.get().requestComplete(method, response);
        assertEquals(12345, UpdateSettings.LAST_HTTP_FAILOVER.getValue());
        assertEquals(54321L, UpdateSettings.LAST_UPDATE_TIMESTAMP.getValue());
        assertEquals(Integer.MAX_VALUE, h.getLatestId());
        assertTrue(saveFile.exists());

        mockery.assertIsSatisfied();
    }


    public void testHttpRequestOverridesLocalIfEqual() throws Exception {
        final AtomicReference<HttpClientListener> httpClientListenerRef = new AtomicReference<HttpClientListener>();
        final UpdateCollection updateCollection = mockery.mock(UpdateCollection.class);
        final Sequence requestSequence = mockery.sequence("Request Sequence");
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(new byte[0]);
                will(returnValue("asdf\n"));
                inSequence(requestSequence);

                one(updateCollectionFactory).createUpdateCollection("asdf\n");
                will(returnValue(updateCollection));
                inSequence(requestSequence);

                atLeast(1).of(updateCollection).getId();
                will(returnValue(Integer.MAX_VALUE));
                inSequence(requestSequence);

                allowing(applicationServices).getMyGUID();
                will(returnValue(new byte[16]));
                inSequence(requestSequence);
            }
        });

        UpdateHandlerImpl h = (UpdateHandlerImpl) injector.getInstance(UpdateHandler.class);
        assertEquals(0, h.getLatestId());
        h.setMaxUrls(Arrays.asList("http://127.0.0.1:9999/update.def"));
        h.setSilentPeriodForMaxHttpRequest(0);
        backgroundExecutor.scheduled = null;
        clock.setNow(12345);
        h.handleNewData(new byte[0], null);
        assertNotNull(backgroundExecutor.scheduled);

        mockery.checking(new Expectations() {
            {
                one(httpExecutor).execute(with(new TypeSafeMatcher<HttpGet>() {
                    public void describeTo(org.hamcrest.Description description) {
                        description.appendText("httpMethod");
                    }

                    @Override
                    public boolean matchesSafely(HttpGet item) {
                        assertEquals("GET", item.getMethod());
                        assertTrue(item.getURI().toString(), item.getURI().toString()
                                .startsWith("http://127.0.0.1:9999/update.def?"));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpParams>() {
                    public void describeTo(org.hamcrest.Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpParams item) {
                        assertEquals(10000, HttpConnectionParams.getConnectionTimeout(item));
                        assertEquals(10000, HttpConnectionParams.getSoTimeout(item));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpClientListener>() {
                    public void describeTo(Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpClientListener item) {
                        httpClientListenerRef.set(item);
                        return true;
                    }
                }));
                inSequence(requestSequence);
            }
        });

        backgroundExecutor.scheduled.run();
        assertNotNull(httpClientListenerRef.get());

        final HttpGet method = new HttpGet();
        final HttpResponse response = mockery.mock(HttpResponse.class);
        final StatusLine statusLine = mockery.mock(StatusLine.class);
        final HttpEntity httpEntity = mockery.mock(HttpEntity.class);
        final UpdateCollection httpCollection = mockery.mock(UpdateCollection.class);
        mockery.checking(new Expectations() {
            {
                atLeast(1).of(response).getStatusLine();
                will(returnValue(statusLine));
                inSequence(requestSequence);
                
                atLeast(1).of(statusLine).getStatusCode();
                will(returnValue(200));
                inSequence(requestSequence);                

                atLeast(1).of(response).getEntity();
                will(returnValue(httpEntity));
                inSequence(requestSequence);
                
                one(httpEntity).getContent();
                byte [] b = new byte[1];
                ByteArrayInputStream bis = new ByteArrayInputStream(b);
                will(returnValue(bis));
                inSequence(requestSequence);

                one(updateMessageVerifier).inflateNetworkData(with(LimeTestUtils.createByteMatcher(b)));
                byte[] inflated = new byte[2];
                inSequence(requestSequence);
                will(returnValue(inflated));

                one(httpExecutor).releaseResources(with(same(response)));
                inSequence(requestSequence);

                one(updateMessageVerifier).getVerifiedData(with(same(inflated)));
                inSequence(requestSequence);
                will(returnValue("http response"));

                one(updateCollectionFactory).createUpdateCollection("http response");
                inSequence(requestSequence);
                will(returnValue(httpCollection));

                atLeast(1).of(httpCollection).getId();
                inSequence(requestSequence);
                will(returnValue(0));

                one(httpCollection).getTimestamp();
                inSequence(requestSequence);
                will(returnValue(54321L));

                one(capabilitiesVmFactory).updateCapabilities();
                inSequence(requestSequence);

                one(connectionManager).sendUpdatedCapabilities();
                inSequence(requestSequence);
            }
        });

        httpClientListenerRef.get().requestComplete(method, response);
        assertEquals(12345, UpdateSettings.LAST_HTTP_FAILOVER.getValue());
        assertEquals(54321L, UpdateSettings.LAST_UPDATE_TIMESTAMP.getValue());
        assertEquals(0, h.getLatestId());
        assertTrue(saveFile.exists());

        mockery.assertIsSatisfied();
    }

    public void testAfterMaxFromHttpNewMaxFromNetworkDoesntRetrigger() throws Exception {
        final AtomicReference<HttpClientListener> httpClientListenerRef = new AtomicReference<HttpClientListener>();
        final UpdateCollection updateCollection = mockery.mock(UpdateCollection.class);
        final Sequence requestSequence = mockery.sequence("Request Sequence");
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(new byte[0]);
                will(returnValue("asdf\n"));
                inSequence(requestSequence);

                one(updateCollectionFactory).createUpdateCollection("asdf\n");
                will(returnValue(updateCollection));
                inSequence(requestSequence);

                atLeast(1).of(updateCollection).getId();
                will(returnValue(Integer.MAX_VALUE));
                inSequence(requestSequence);

                allowing(applicationServices).getMyGUID();
                will(returnValue(new byte[16]));
                inSequence(requestSequence);
            }
        });

        UpdateHandlerImpl h = (UpdateHandlerImpl) injector.getInstance(UpdateHandler.class);
        assertEquals(0, h.getLatestId());
        h.setMaxUrls(Arrays.asList("http://127.0.0.1:9999/update.def"));
        h.setSilentPeriodForMaxHttpRequest(0);
        backgroundExecutor.scheduled = null;
        clock.setNow(12345);
        h.handleNewData(new byte[0], null);
        assertNotNull(backgroundExecutor.scheduled);

        mockery.checking(new Expectations() {
            {
                one(httpExecutor).execute(with(new TypeSafeMatcher<HttpGet>() {
                    public void describeTo(org.hamcrest.Description description) {
                        description.appendText("httpMethod");
                    }

                    @Override
                    public boolean matchesSafely(HttpGet item) {
                        assertEquals("GET", item.getMethod());
                        assertTrue(item.getURI().toString(), item.getURI().toString()
                                .startsWith("http://127.0.0.1:9999/update.def?"));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpParams>() {
                    public void describeTo(org.hamcrest.Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpParams item) {
                        assertEquals(10000, HttpConnectionParams.getConnectionTimeout(item));
                        assertEquals(10000, HttpConnectionParams.getSoTimeout(item));
                        return true;
                    }
                }), with(new TypeSafeMatcher<HttpClientListener>() {
                    public void describeTo(Description description) {
                    }

                    @Override
                    public boolean matchesSafely(HttpClientListener item) {
                        httpClientListenerRef.set(item);
                        return true;
                    }
                }));
                inSequence(requestSequence);
            }
        });

        backgroundExecutor.scheduled.run();
        assertNotNull(httpClientListenerRef.get());

        final HttpGet method = new HttpGet();
        final HttpResponse response = mockery.mock(HttpResponse.class);
        final StatusLine statusLine = mockery.mock(StatusLine.class);
        final HttpEntity httpEntity = mockery.mock(HttpEntity.class);
        final UpdateCollection httpCollection = mockery.mock(UpdateCollection.class);
        mockery.checking(new Expectations() {
            {
                atLeast(1).of(response).getStatusLine();
                will(returnValue(statusLine));
                inSequence(requestSequence);
                
                atLeast(1).of(statusLine).getStatusCode();
                will(returnValue(200));
                inSequence(requestSequence);                

                atLeast(1).of(response).getEntity();
                will(returnValue(httpEntity));
                inSequence(requestSequence);
                
                one(httpEntity).getContent();
                byte [] b = new byte[1];
                ByteArrayInputStream bis = new ByteArrayInputStream(b);
                will(returnValue(bis));
                inSequence(requestSequence);

                one(updateMessageVerifier).inflateNetworkData(with(any(byte [].class)));
                byte[] inflated = new byte[2];
                inSequence(requestSequence);
                will(returnValue(inflated));

                one(httpExecutor).releaseResources(with(same(response)));
                inSequence(requestSequence);

                one(updateMessageVerifier).getVerifiedData(with(same(inflated)));
                inSequence(requestSequence);
                will(returnValue("http response"));

                one(updateCollectionFactory).createUpdateCollection("http response");
                inSequence(requestSequence);
                will(returnValue(httpCollection));

                atLeast(1).of(httpCollection).getId();
                inSequence(requestSequence);
                will(returnValue(Integer.MAX_VALUE));

                one(httpCollection).getTimestamp();
                inSequence(requestSequence);
                will(returnValue(54321L));

                one(capabilitiesVmFactory).updateCapabilities();
                inSequence(requestSequence);

                one(connectionManager).sendUpdatedCapabilities();
                inSequence(requestSequence);
            }
        });

        httpClientListenerRef.get().requestComplete(method, response);
        assertEquals(12345, UpdateSettings.LAST_HTTP_FAILOVER.getValue());
        assertEquals(54321L, UpdateSettings.LAST_UPDATE_TIMESTAMP.getValue());
        assertEquals(Integer.MAX_VALUE, h.getLatestId());
        assertTrue(saveFile.exists());
        
        clock.setNow(999999);        
        saveFile.delete();
        
        backgroundExecutor.clear();
        final UpdateCollection secondUpdateCollection = mockery.mock(UpdateCollection.class);
        mockery.checking(new Expectations() {
            {
                one(updateMessageVerifier).getVerifiedData(new byte[0]);
                will(returnValue("asdf\n"));
                inSequence(requestSequence);

                one(updateCollectionFactory).createUpdateCollection("asdf\n");
                will(returnValue(secondUpdateCollection));
                inSequence(requestSequence);

                atLeast(1).of(secondUpdateCollection).getId();
                will(returnValue(Integer.MAX_VALUE));
                inSequence(requestSequence);
            }
        });
        h.handleNewData(new byte[0], null);
        
        assertEquals(12345, UpdateSettings.LAST_HTTP_FAILOVER.getValue());
        assertEquals(54321L, UpdateSettings.LAST_UPDATE_TIMESTAMP.getValue());
        assertEquals(Integer.MAX_VALUE, h.getLatestId());
        assertFalse(saveFile.exists());
        assertNull(backgroundExecutor.getRunnable());

        mockery.assertIsSatisfied();
    }

    private class ImmediateExecutor extends ScheduledExecutorServiceStub {

        private volatile Runnable scheduled;
        private volatile long initialDelay = -1;
        private volatile long period = -1;
        private volatile TimeUnit timeUnit;
        
        Runnable getRunnable() {
            return scheduled;
        }
        
        long getInitialDelay() {
            return initialDelay;
        }
        
        long getPeriod() {
            return period;
        }
        
        TimeUnit getTimeUnit() {
            return timeUnit;
        }
        
        void clear() {
            scheduled = null;
            initialDelay = -1;
            period = -1;
            timeUnit = null;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            scheduled = command;
            this.initialDelay = delay;
            return null;
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                long period, TimeUnit unit) {
            scheduled = command;
            this.initialDelay = -1;
            this.period = period;
            this.timeUnit = unit;
            return null;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                long delay, TimeUnit unit) {
            scheduled = command;
            this.initialDelay = -1;
            this.period = delay;
            this.timeUnit = unit;
            return null;
        }

    }
}
