package org.limewire.inject;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Stage;

public class LazySingletonTest extends BaseTestCase {
    
    private static boolean LAZY_ANNOTATED_CONSTRUCTED;
    private static boolean FOO_CONSTRUCTED;

    public LazySingletonTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LazySingletonTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        LAZY_ANNOTATED_CONSTRUCTED = false;
        FOO_CONSTRUCTED = false;
    }
    
    public void testLazyAnnotated() throws Exception {
        assertFalse(LAZY_ANNOTATED_CONSTRUCTED);
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                install(new LimeWireInjectModule());
                bind(LazyAnnotated.class);
            }
        });
        assertFalse(LAZY_ANNOTATED_CONSTRUCTED);
        Provider<?> p = injector.getProvider(LazyAnnotated.class);
        assertFalse(LAZY_ANNOTATED_CONSTRUCTED);
        LazyAnnotated la = injector.getInstance(LazyAnnotated.class);
        assertTrue(LAZY_ANNOTATED_CONSTRUCTED);
        
        assertSame(la, injector.getInstance(LazyAnnotated.class));
        assertSame(la, p.get());
    }
    
    public void testLazyBoundByClassAnnotation() throws Exception {
        assertFalse(FOO_CONSTRUCTED);
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                install(new LimeWireInjectModule());
                bind(Foo.class).in(LazySingleton.class);
            }
        });
        assertFalse(FOO_CONSTRUCTED);
        Provider<?> p = injector.getProvider(Foo.class);
        assertFalse(FOO_CONSTRUCTED);
        Foo foo = injector.getInstance(Foo.class);
        assertTrue(FOO_CONSTRUCTED);
        
        assertSame(foo, injector.getInstance(Foo.class));
        assertSame(foo, p.get());
    }
    
    public void testLazyBoundByScope() throws Exception {
        assertFalse(FOO_CONSTRUCTED);
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                install(new LimeWireInjectModule());
                bind(Foo.class).in(MoreScopes.LAZY_SINGLETON);
            }
        });
        assertFalse(FOO_CONSTRUCTED);
        Provider<?> p = injector.getProvider(Foo.class);
        assertFalse(FOO_CONSTRUCTED);
        Foo foo = injector.getInstance(Foo.class);
        assertTrue(FOO_CONSTRUCTED);
        
        assertSame(foo, injector.getInstance(Foo.class));
        assertSame(foo, p.get());
    }
    
    @LazySingleton
    private static class LazyAnnotated {
        public LazyAnnotated() {
            LAZY_ANNOTATED_CONSTRUCTED = true;
        }
    }
    
    private static class Foo {
        public Foo() {
            FOO_CONSTRUCTED = true;
        }
    }
}
