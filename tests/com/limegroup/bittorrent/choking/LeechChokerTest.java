package com.limegroup.bittorrent.choking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.States;
import org.limewire.collection.NECallable;
import org.limewire.core.settings.BittorrentSettings;

import junit.framework.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.limegroup.bittorrent.Chokable;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.UploadServices;
import com.limegroup.gnutella.stubs.ScheduledExecutorServiceStub;
import com.limegroup.gnutella.util.LimeTestCase;

public class LeechChokerTest extends LimeTestCase {

    public LeechChokerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LeechChokerTest.class);
    }
    
    private Mockery mockery;
    @Override
    public void setUp() throws Exception {
        mockery = new Mockery();
    }
    
    /**
     * tests that the choker uses the provided scheduler service when started, executed and shutdown.
     */
    public void testStartRunShutdown() throws Exception {
        final ScheduledExecutorService ses = mockery.mock(ScheduledExecutorService.class);
        Module m = new AbstractModule() {
            @Override
            public void configure() {
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("nioExecutor")).toInstance(ses);
            }
        };

        final ScheduledFuture chokerFuture = mockery.mock(ScheduledFuture.class);
        final ScheduledFuture chokerFuture2 = mockery.mock(ScheduledFuture.class);
        
        NECallable<List<? extends Chokable>> emptyCallable = new NECallable<List<? extends Chokable>>() {
            public List<? extends Chokable> call() {
                return Collections.emptyList();
            }
        };
        
        Injector inj = LimeTestUtils.createInjector(m);
        ChokerFactory factory = inj.getInstance(ChokerFactory.class);
        final Choker c = factory.getChoker(emptyCallable, false);
        
        
        final States sesState = mockery.states("sesStates").startsAs("empty");
        mockery.checking(new Expectations() {{
            one(ses).schedule(c, 10 * 1000, TimeUnit.MILLISECONDS);
            when(sesState.is("empty"));
            will(returnValue(chokerFuture));
            then(sesState.is("scheduled"));
            one(ses).schedule(c, 10 * 1000, TimeUnit.MILLISECONDS);
            when(sesState.is("scheduled"));
            will(returnValue(chokerFuture2));
            then(sesState.is("rescheduled"));
            one(chokerFuture2).cancel(false);
            when(sesState.is("rescheduled"));
        }});
        c.start();
        c.run();
        c.shutdown();
        mockery.assertIsSatisfied();
    }
    
    /**
     * tests that calls to rechoke() happen immediately on the nio executor.
     */
    @SuppressWarnings("unchecked")
    public void testImmediateRechoke() throws Exception {
        final NECallable<List<? extends Chokable>> callable = mockery.mock(NECallable.class);
        final ScheduledExecutorService myService = new ScheduledExecutorServiceStub() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        Module m = new AbstractModule() {
            @Override
            public void configure() {
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("nioExecutor")).toInstance(myService);
            }
        };
        Injector inj = LimeTestUtils.createInjector(m);
        ChokerFactory factory = inj.getInstance(ChokerFactory.class);
        final Choker c = factory.getChoker(callable, false);
        mockery.checking(new Expectations() {{
            one(callable).call();
            will(returnValue(Collections.emptyList()));
        }});
        c.rechoke();
        mockery.assertIsSatisfied();
    }
    
    /**
     * tests that getting the number of allowed uploads depends on the 
     * manual setting or the upload speed.
     */
    public void testNumUploads() throws Exception {
        final UploadServices uServices = mockery.mock(UploadServices.class);

        Module m = new AbstractModule() {
            @Override
            public void configure() {
                bind(UploadServices.class).toInstance(uServices);
            }
        };
        Injector inj = LimeTestUtils.createInjector(m);
        ChokerFactory factory = inj.getInstance(ChokerFactory.class);
        Choker c = factory.getChoker(null, false);
        
        mockery.checking(new Expectations() {{
            exactly(5).of(uServices).getRequestedUploadSpeed(); // would be 6 if setting were ignored.
            will(onConsecutiveCalls(
                    returnValue(Float.MAX_VALUE),
                    returnValue(100000.0f),
                    returnValue(41000.0f),
                    returnValue(14000.0f),
                    returnValue(8000.0f)));
        }});
        BittorrentSettings.TORRENT_MAX_UPLOADS.setValue(1);
        assertEquals(1,c.getNumUploads());
        BittorrentSettings.TORRENT_MAX_UPLOADS.setValue(0);
        assertEquals(7,c.getNumUploads());
        assertEquals(244, c.getNumUploads()); // Math.sqrt(rate * 0.6f) // copied from bram
        assertEquals(4,c.getNumUploads());
        assertEquals(3,c.getNumUploads());
        assertEquals(2,c.getNumUploads());
        mockery.assertIsSatisfied();
    }
    
    /**
     * tests unchokings for speed and optimistic
     */
    public void testRechokeSpeedOptimistic() throws Exception {
        
        // provide an executor that executes stuff immediately
        final ScheduledExecutorService myService = new ScheduledExecutorServiceStub() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        Module m = new AbstractModule() {
            @Override
            public void configure() {
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("nioExecutor")).toInstance(myService);
            }
        };
        Injector inj = LimeTestUtils.createInjector(m);
        ChokerFactory factory = inj.getInstance(ChokerFactory.class);
        
        // create a bunch of mock Chockables
        final List<Chokable> chokables = new ArrayList<Chokable>(10);
        for (int i = 0; i < 4; i++)
            chokables.add(mockery.mock(Chokable.class));
        NECallable<List<? extends Chokable>> neCallable = new NECallable<List<? extends Chokable>>() {
            public List<? extends Chokable> call() {
                return chokables;
            }
        };
        final Choker c = factory.getChoker(neCallable, false);
        
        // two will get unchoked for speed, one will get unchoked optimistically.
        
        BittorrentSettings.TORRENT_MAX_UPLOADS.setValue(3);
        // each chokable will be queried for interest, supposed interest and speed
        mockery.checking(new Expectations(){{
            
            // chokable 0 is interested, and slow
            Chokable c = chokables.get(0);
            atLeast(1).of(c).isInterested();
            will(returnValue(true));
            atLeast(1).of(c).shouldBeInterested();
            will(returnValue(true));
            atLeast(1).of(c).getMeasuredBandwidth(true, false);
            will(returnValue(0.1f));
            atMost(1).of(c).isChoked(); // may get unchoked optimistically
            will(returnValue(true));
            atMost(1).of(c).unchoke(with(Matchers.any(Integer.class)));
            
            // chokable 1 is not interested at all
            c = chokables.get(1);
            one(c).isInterested();
            will(returnValue(false));
            allowing(c).shouldBeInterested(); // may check if it should be interested
            will(returnValue(false)); // but it wont' be
            never(c).getMeasuredBandwidth(true, false); // not checking its bw
            never(c).unchoke(with(Matchers.any(Integer.class))); // not unchoked
            one(c).choke(); // choked
            never(c).isChoked(); // regardless of previous status
            
            
            // chokable 2 is interested and fast  It will be unchoked
            c = chokables.get(2);
            one(c).isInterested();
            will(returnValue(true));
            one(c).shouldBeInterested();
            will(returnValue(true));
            atLeast(1).of(c).getMeasuredBandwidth(true, false);
            will(returnValue(0.4f));
            never(c).isChoked(); // don't check if it was choked
            one(c).unchoke(with(Matchers.any(Integer.class))); // don't care for round
            
            // chokable 3 is interested and faster than 2.  It will be unchoked the same way
            c = chokables.get(3);
            one(c).isInterested();
            will(returnValue(true));
            one(c).shouldBeInterested();
            will(returnValue(true));
            atLeast(1).of(c).getMeasuredBandwidth(true, false);
            will(returnValue(0.5f));
            never(c).isChoked(); // don't check if it was choked
            one(c).unchoke(with(Matchers.any(Integer.class))); // don't care for round
        }});
        
        c.rechoke();
        
        mockery.assertIsSatisfied();
    }
    
    /** 
     * all chokables are slow, but one gets unchoked nevertheless 
     */
    public void testOptimisticOnly() throws Exception {
        // provide an executor that executes stuff immediately
        final ScheduledExecutorService myService = new ScheduledExecutorServiceStub() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        Module m = new AbstractModule() {
            @Override
            public void configure() {
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("nioExecutor")).toInstance(myService);
            }
        };
        Injector inj = LimeTestUtils.createInjector(m);
        ChokerFactory factory = inj.getInstance(ChokerFactory.class);
        
        // create a bunch of mock Chockables
        final List<Chokable> chokables = new ArrayList<Chokable>(10);
        for (int i = 0; i < 2; i++)
            chokables.add(mockery.mock(Chokable.class));
        
        NECallable<List<? extends Chokable>> neCallable = new NECallable<List<? extends Chokable>>() {
            public List<? extends Chokable> call() {
                return chokables;
            }
        };
        final Choker c = factory.getChoker(neCallable, false);
        
        BittorrentSettings.TORRENT_MIN_UPLOADS.setValue(1);
        mockery.checking(new Expectations(){{
            
            // 0 is not and should not be interested 
            Chokable c = chokables.get(0);
            atLeast(1).of(c).isInterested();
            will(returnValue(false));
            allowing(c).shouldBeInterested();
            will(returnValue(false));
            never(c).getMeasuredBandwidth(true, false);
            never(c).isChoked(); 
            one(c).choke();
            
            // 1 is interested but slow, gets unchoked optimistically
            c = chokables.get(1);
            atLeast(1).of(c).isInterested();
            will(returnValue(true));
            atLeast(1).of(c).shouldBeInterested();
            will(returnValue(true));
            atLeast(1).of(c).getMeasuredBandwidth(true, false);
            will(returnValue(0.1f));
            one(c).isChoked();
            will(returnValue(true));
            one(c).unchoke(with(Matchers.any(Integer.class)));
        }});
        
        c.rechoke();
        
        mockery.assertIsSatisfied(); 
    }
    
    /** 
     * No chokables are interested, but one should be 
     */
    public void testShouldBeInterested() throws Exception {
        // provide an executor that executes stuff immediately
        final ScheduledExecutorService myService = new ScheduledExecutorServiceStub() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
        Module m = new AbstractModule() {
            @Override
            public void configure() {
                bind(ScheduledExecutorService.class).annotatedWith(Names.named("nioExecutor")).toInstance(myService);
            }
        };
        Injector inj = LimeTestUtils.createInjector(m);
        ChokerFactory factory = inj.getInstance(ChokerFactory.class);
        
        // create a bunch of mock Chockables
        final List<Chokable> chokables = new ArrayList<Chokable>(10);
        for (int i = 0; i < 2; i++)
            chokables.add(mockery.mock(Chokable.class));
        
        NECallable<List<? extends Chokable>> neCallable = new NECallable<List<? extends Chokable>>() {
            public List<? extends Chokable> call() {
                return chokables;
            }
        };
        final Choker c = factory.getChoker(neCallable, false);
        
        BittorrentSettings.TORRENT_MIN_UPLOADS.setValue(1);
        mockery.checking(new Expectations(){{
            
            // 0 is not and should not be interested 
            Chokable c = chokables.get(0);
            atLeast(1).of(c).isInterested();
            will(returnValue(false));
            allowing(c).shouldBeInterested();
            will(returnValue(false));
            never(c).getMeasuredBandwidth(true, false);
            never(c).isChoked(); 
            one(c).choke();
            
            // 1 is not interested but should be
            c = chokables.get(1);
            atLeast(1).of(c).isInterested();
            will(returnValue(false));
            atLeast(1).of(c).shouldBeInterested();
            will(returnValue(true));
            never(c).getMeasuredBandwidth(true, false);
            will(returnValue(0.1f));
            one(c).isChoked();
            will(returnValue(true));
            one(c).unchoke(with(Matchers.any(Integer.class)));
        }});
        
        c.rechoke();
        
        mockery.assertIsSatisfied(); 
    }
    
}
