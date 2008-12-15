package org.limewire.lifecycle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ThreadExecutor;

import com.google.inject.Singleton;

@Singleton
class ServiceRegistryImpl implements ServiceRegistry {
    
    private static final Log LOG = LogFactory.getLog(ServiceRegistryImpl.class);
    
    private final List<StagedRegisterBuilderImpl> builders
        = new ArrayList<StagedRegisterBuilderImpl>();

    private final Map<Object, List<ServiceHolder>> services
        = new HashMap<Object, List<ServiceHolder>>();
    
    private final List<ServiceHolder> startedServices
        = new ArrayList<ServiceHolder>();
    
    private final List<ServiceRegistryListener> registryListeners
        = new ArrayList<ServiceRegistryListener>();
        
    public void initialize() {
        // Remove builders & assign services.
        for(Iterator<StagedRegisterBuilderImpl> iter = builders.iterator(); iter.hasNext(); ) {
            StagedRegisterBuilderImpl builder = iter.next();
            Object stage = builder.getCustomStage();
            if(stage == null)
                stage = builder.getStage();
            List<ServiceHolder> servicesInStage = services.get(stage);
            if(servicesInStage == null) {
                servicesInStage = new ArrayList<ServiceHolder>();
                services.put(stage, servicesInStage);
            }
            servicesInStage.add(new ServiceHolder(builder.getService()));
            iter.remove();
        }
        
        // Do the actual initialization.
        
        // First go through built-in stages.
        for(ServiceStage stage : getStagesInOrder()) {
            if(services.get(stage) != null) {
                for(ServiceHolder service : services.get(stage)) {
                    service.init();
                }
            }
        }
        // Then go through custom stages.
        for(Map.Entry<Object, List<ServiceHolder>> entry : services.entrySet()) {
            if(entry.getKey().getClass() != ServiceStage.class) {
                if(entry.getValue() != null) {
                    for(ServiceHolder service : entry.getValue()) {
                        service.init();
                    }
                }
            }
        }
    }
    
    public void start(Object stage) {
        initialize();        
        startStage(stage);
    }
    
    public void start() {
        initialize();        
        for(ServiceStage stage : getStagesInOrder()) {
            startStage(stage);            
        }
    }
    
    private void startStage(Object stage) {
        List<ServiceHolder> servicedStages = services.get(stage);
        if(servicedStages != null) {
            for(Iterator<ServiceHolder> iter = servicedStages.iterator(); iter.hasNext(); ) {
                ServiceHolder service = iter.next();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("starting service: " + service.service.service.getClass().getSimpleName());
                }
                service.start();
                startedServices.add(service);
                iter.remove();
            }
            for (ServiceHolder startedService : startedServices) {
                try {
                    startedService.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();  // TODO log, throw?
                }
            }
        }
    }
    
    public void stop() {
        for(int i = startedServices.size()-1; i >= 0; i--) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("stopping service: " + startedServices.get(i).service.service.getClass().getSimpleName());
            }
            startedServices.get(i).stop();
        }
        for(int i = startedServices.size()-1; i >= 0; i--) {
            try {
                startedServices.get(i).join();
            } catch (InterruptedException e) {
                e.printStackTrace();  // TODO log, throw?
            }
            startedServices.remove(i);
        }
    }
    
    public StagedRegisterBuilder register(Service service) {
        StagedRegisterBuilderImpl builder = new StagedRegisterBuilderImpl(service);
        builders.add(builder);
        return builder;
    }
    
    public void addListener(ServiceRegistryListener serviceRegistryListener) {
        registryListeners.add(serviceRegistryListener);
    }

    ServiceStage[] getStagesInOrder() {
        return new ServiceStage[] { ServiceStage.EARLY, ServiceStage.NORMAL, ServiceStage.LATE };
    }
    
    private class ServiceHolder {
        private final AnnotatedService service;
        private boolean initted;
        private boolean started;
        private boolean stopped;
        
        public ServiceHolder(Service service) {
            this.service = new AnnotatedService(service);
        }
        
        void init() {
            if(!initted) {
                initted = true;
                for(ServiceRegistryListener listener : registryListeners) {
                    listener.initializing(service);
                }
                service.initialize();
            }
        }

        void start() {
            if(!started) {
                started = true;
                for(ServiceRegistryListener listener : registryListeners) {
                    listener.starting(service);
                }
                service.start();
            }
        }

        void stop() {
            if(!stopped) {
                stopped = true;
                for(ServiceRegistryListener listener : registryListeners) {
                    listener.stopping(service);
                }
                service.stop();
            }
        }
        
        void join() throws InterruptedException {
            service.join();    
        }

        private class AnnotatedService implements Service {
            private final Service service;
            private Thread serviceExecutor;
            
            AnnotatedService(Service service) {
                this.service = service;
            }
            
            void join() throws InterruptedException {
                if(serviceExecutor != null) {
                    serviceExecutor.join();
                    serviceExecutor = null;
                }
            }

            public void initialize() {
                service.initialize();
            }

            public String getServiceName() {
                return service.getServiceName();
            }

            public void start() {
                if(isAsyncStart()) {
                    serviceExecutor = asyncStart();
                } else {
                    service.start();
                }
            }
            
            private Thread asyncStart() {
                Asynchronous asynchronous = getStartAsynchronous();
                Thread startThread = ThreadExecutor.newManagedThread(new Runnable() {
                    public void run() {
                        // TODO LOG
                        service.start();
                        // TODO LOG
                    }
                }, "ServiceRegistry-start-" + service.getServiceName());
                startThread.setDaemon(asynchronous.daemon());
                startThread = wrapWithWaitingThreadIfNeeded(startThread, asynchronous);
                startThread.start();
                return startThread;
            }

            public void stop() {
                if(isAsyncStop()) {
                    serviceExecutor = asyncStop();
                } else {
                    service.stop();
                }
            }
            
            private Thread asyncStop() {
                Asynchronous asynchronous = getStopAsynchronous();
                Thread stopThread = ThreadExecutor.newManagedThread(new Runnable() {
                    public void run() {
                        // TODO LOG
                        service.stop();
                        // TODO LOG
                    }
                }, "ServiceRegistry-stop-" + service.getServiceName());
                stopThread.setDaemon(asynchronous.daemon());
                stopThread = wrapWithWaitingThreadIfNeeded(stopThread, asynchronous);
                stopThread.start(); 
                return stopThread;
            }
            
            private boolean isAsyncStop() {
                return getAsynchronousAnnotation("stop") != null;                
            }
            
            private boolean isAsyncStart() {
                return getAsynchronousAnnotation("start") != null;                
            }
            
            private Asynchronous getStopAsynchronous() {
                return getAsynchronousAnnotation("stop");
            }
            
            private Asynchronous getStartAsynchronous() {
                return getAsynchronousAnnotation("start");
            }
            
            private Asynchronous getAsynchronousAnnotation(String methodName){
                try {
                    return service.getClass().getMethod(methodName).getAnnotation(Asynchronous.class);
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                }
            }
            
            private Thread wrapWithWaitingThreadIfNeeded(final Thread methodThread, final Asynchronous asynchronous) {
                Thread toReturn;
                if(asynchronous.timeout() > 0) {
                    Thread waitingThread = ThreadExecutor.newManagedThread(new Runnable() {
                        public void run() {
                            // TODO LOG
                            methodThread.start();
                            try {
                                methodThread.join(asynchronous.timeout() * 1000);
                            } catch (InterruptedException ignore) {}
                            // TODO LOG
                        }
                    }, "ServiceRegistry-waiting-thread-" + service.getServiceName());
                    waitingThread.setDaemon(false); // TODO is this necessary?
                    toReturn = waitingThread;
                } else {
                    toReturn = methodThread;
                }
                return toReturn;
            }
        }
    }
}
