package org.limewire.inject;

import java.util.Arrays;

import junit.framework.Test;

import org.limewire.concurrent.AbstractLazySingletonProvider;
import org.limewire.util.BaseTestCase;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.name.Names;

public class AbstractModuleTest extends BaseTestCase {
    
    public AbstractModuleTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AbstractModuleTest.class);
    }
    
    public void testSameInstanceIfSingletonAndCached() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bindAll(Names.named("A"), I3.class, P.class, I2.class, I1.class);
            }
        });
        
        I3 i3 = injector.getInstance(Key.get(I3.class, Names.named("A")));
        I2 i2 = injector.getInstance(Key.get(I2.class, Names.named("A")));
        I1 i1 = injector.getInstance(Key.get(I1.class, Names.named("A")));
        
        assertSame(i3, i2);
        assertSame(i3, i1);
    }
    
    public void testWrapperModuleSingletons() {
        Injector parent = Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                bind(S1.class).to(S1I.class);
            }
        });
        Injector child = Guice.createInjector(Stage.PRODUCTION, Arrays.asList(Modules.providersFrom(parent), new AbstractModule() {
            @Override
            protected void configure() {
                bind(S2.class).to(S2I.class);
                bind(S3I.class);
            }
        }));
        
        assertSame(parent.getInstance(S1.class), child.getInstance(S1.class));
        assertTrue(S1I.created);
        assertTrue(S2I.created);
        assertTrue(S3I.created);
    }

    
    private static interface I1 {}
    private static interface I2 extends I1 {}
    private static interface I3 extends I2 {}
    
    @Singleton
    private static class P extends AbstractLazySingletonProvider<I3> {
        @Override
        protected I3 createObject() {
            return new I3() {};
        }
    }
    
    private static interface S1 {}
    @Singleton
    private static class S1I implements S1 {
        private static boolean created = false;
        S1I () { created = true; }
    }
    private static interface S2 {}
    @Singleton
    private static class S2I implements S2 {
        private static boolean created = false;
        S2I () { created = true; }
    }
    @Singleton
    private static class S3I {
        private static boolean created = false;
        S3I () { created = true; }
    }
    
   
}
