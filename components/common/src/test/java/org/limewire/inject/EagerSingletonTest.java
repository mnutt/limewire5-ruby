package org.limewire.inject;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Stage;

public class EagerSingletonTest extends BaseTestCase {

    private static boolean EAGER_ANNOTATED_CONSTRUCTED;
    private static boolean FOO_CONSTRUCTED;
    
    public EagerSingletonTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(EagerSingletonTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        EAGER_ANNOTATED_CONSTRUCTED = false;
        FOO_CONSTRUCTED = false;
    }
    
    public void testEagerAnnotated() throws Exception {
        assertFalse(EAGER_ANNOTATED_CONSTRUCTED);
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new AbstractModule() {
            @Override
            protected void configure() {
                install(new LimeWireInjectModule());
                bind(EagerAnnotated.class);
            }
        });
        GuiceUtils.loadEagerSingletons(injector);
        
        assertTrue(EAGER_ANNOTATED_CONSTRUCTED);
        Provider<?> p = injector.getProvider(EagerAnnotated.class);
        assertTrue(EAGER_ANNOTATED_CONSTRUCTED);
        EagerAnnotated la = injector.getInstance(EagerAnnotated.class);
        assertTrue(EAGER_ANNOTATED_CONSTRUCTED);
        
        assertSame(la, injector.getInstance(EagerAnnotated.class));
        assertSame(la, p.get());
    }
    
    public void testEagerBoundByClassAnnotation() throws Exception {
        assertFalse(FOO_CONSTRUCTED);
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new AbstractModule() {
            @Override
            protected void configure() {
                install(new LimeWireInjectModule());
                bind(Foo.class).in(EagerSingleton.class);
            }
        });
        GuiceUtils.loadEagerSingletons(injector);
        
        assertTrue(FOO_CONSTRUCTED);
        Provider<?> p = injector.getProvider(Foo.class);
        assertTrue(FOO_CONSTRUCTED);
        Foo foo = injector.getInstance(Foo.class);
        assertTrue(FOO_CONSTRUCTED);
        
        assertSame(foo, injector.getInstance(Foo.class));
        assertSame(foo, p.get());
    }
    
    public void testEagerBoundByScopeEagerAnnotation() throws Exception {
        assertFalse(FOO_CONSTRUCTED);
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new AbstractModule() {
            @Override
            protected void configure() {
                install(new LimeWireInjectModule());
                bind(Foo.class).asEagerSingleton();
            }
        });
        GuiceUtils.loadEagerSingletons(injector);
        
        assertTrue(FOO_CONSTRUCTED);
        Provider<?> p = injector.getProvider(Foo.class);
        assertTrue(FOO_CONSTRUCTED);
        Foo foo = injector.getInstance(Foo.class);
        assertTrue(FOO_CONSTRUCTED);
        
        assertSame(foo, injector.getInstance(Foo.class));
        assertSame(foo, p.get());
    }
    
    public void testEagerBoundByScope() throws Exception {
        assertFalse(FOO_CONSTRUCTED);
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new AbstractModule() {
            @Override
            protected void configure() {
                install(new LimeWireInjectModule());
                bind(Foo.class).in(MoreScopes.EAGER_SINGLETON);
            }
        });
        GuiceUtils.loadEagerSingletons(injector);
        
        assertTrue(FOO_CONSTRUCTED);
        Provider<?> p = injector.getProvider(Foo.class);
        assertTrue(FOO_CONSTRUCTED);
        Foo foo = injector.getInstance(Foo.class);
        assertTrue(FOO_CONSTRUCTED);
        
        assertSame(foo, injector.getInstance(Foo.class));
        assertSame(foo, p.get());
    }
    
    @EagerSingleton
    private static class EagerAnnotated {
        public EagerAnnotated() {
            EAGER_ANNOTATED_CONSTRUCTED = true;
        }
    }
    
    private static class Foo {
        public Foo() {
            FOO_CONSTRUCTED = true;
        }
    }
}
