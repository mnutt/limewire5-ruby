package org.limewire.lifecycle;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;

public class ServiceSchedulerTest extends BaseTestCase {

    public ServiceSchedulerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ServiceSchedulerTest.class);
    }
    
    public void testScheduleAtFixedRateMock() throws Exception {
        Mockery mock = new Mockery();
        
        final ScheduledExecutorService service = mock.mock(ScheduledExecutorService.class);
        final ServiceRegistry registry = mock.mock(ServiceRegistry.class);
        final StagedRegisterBuilder builder = mock.mock(StagedRegisterBuilder.class);
        final Runnable runnable = mock.mock(Runnable.class);
        ServiceScheduler scheduler = new ServiceSchedulerImpl(registry);
        
        mock.checking(new Expectations() {
            {
                one(registry).register(with(any(Service.class)));
                will(returnValue(builder));
            }
        });
        assertSame(builder, scheduler.scheduleAtFixedRate("Test1", runnable, 1, 2, TimeUnit.NANOSECONDS, service));
        
        mock.assertIsSatisfied();
    }

    public void testScheduleWithFixedDelayMock() throws Exception {
        Mockery mock = new Mockery();
        
        final ScheduledExecutorService service = mock.mock(ScheduledExecutorService.class);
        final ServiceRegistry registry = mock.mock(ServiceRegistry.class);
        final StagedRegisterBuilder builder = mock.mock(StagedRegisterBuilder.class);
        final Runnable runnable = mock.mock(Runnable.class);
        ServiceScheduler scheduler = new ServiceSchedulerImpl(registry);
        
        mock.checking(new Expectations() {
            {
                one(registry).register(with(any(Service.class)));
                will(returnValue(builder));
            }
        });
        assertSame(builder, scheduler.scheduleWithFixedDelay("Test1", runnable, 1, 2, TimeUnit.NANOSECONDS, service));
        
        mock.assertIsSatisfied();
    }
    
    public void testScheduleAtFixedRateActual() throws Exception {
        Mockery mock = new Mockery();
        
        final ServiceRegistry registry = new ServiceRegistryImpl();
        final ScheduledExecutorService service = mock.mock(ScheduledExecutorService.class);
        final Runnable runnable = mock.mock(Runnable.class);
        final ScheduledFuture future = mock.mock(ScheduledFuture.class);
        ServiceScheduler scheduler = new ServiceSchedulerImpl(registry);
        
        scheduler.scheduleAtFixedRate("Test1", runnable, 1, 2, TimeUnit.NANOSECONDS, service);
        mock.checking(new Expectations() {
            {
                one(service).scheduleAtFixedRate(runnable, 1, 2, TimeUnit.NANOSECONDS);
                will(returnValue(future));
            }
        });
        registry.start();
        
        mock.checking(new Expectations() {
            {
                one(future).cancel(false);
            }
        });        
        registry.stop();
        
        
        mock.assertIsSatisfied();
    }

    public void testScheduleWithFixedDelayActual() throws Exception {
        Mockery mock = new Mockery();
        
        final ServiceRegistry registry = new ServiceRegistryImpl();
        final ScheduledExecutorService service = mock.mock(ScheduledExecutorService.class);
        final Runnable runnable = mock.mock(Runnable.class);
        final ScheduledFuture future = mock.mock(ScheduledFuture.class);
        ServiceScheduler scheduler = new ServiceSchedulerImpl(registry);

        scheduler.scheduleWithFixedDelay("Test1", runnable, 1, 2, TimeUnit.NANOSECONDS, service);
        mock.checking(new Expectations() {
            {
                one(service).scheduleWithFixedDelay(runnable, 1, 2, TimeUnit.NANOSECONDS);
                will(returnValue(future));
            }
        });
        registry.start();
        
        mock.checking(new Expectations() {
            {
                one(future).cancel(false);
            }
        });        
        registry.stop();
        
        mock.assertIsSatisfied();
    }

    
}
