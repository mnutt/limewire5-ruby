package org.limewire.inject;

import java.util.Collection;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Stage;
import com.google.inject.spi.Message;

public class LazyBinderTest extends BaseTestCase {
    
    private static int ANNOTATED_CONSTRUCTED;
    private static int PLAIN_CONSTRUCTED;

    public LazyBinderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LazyBinderTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        ANNOTATED_CONSTRUCTED = 0;
        PLAIN_CONSTRUCTED = 0;
    }
    
    public void testLazyBinderAnnotated() {
        assertEquals(0, ANNOTATED_CONSTRUCTED);
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                install(new LimeWireInjectModule());
                bind(Foo.class).toProvider(LazyBinder.newLazyProvider(Foo.class, AnnotatedFoo.class));
            }
        });
        assertEquals(0, ANNOTATED_CONSTRUCTED);
        Provider<Foo> pFoo = injector.getProvider(Foo.class);
        assertEquals(0, ANNOTATED_CONSTRUCTED);
        Foo foo = injector.getInstance(Foo.class);
        assertEquals(0, ANNOTATED_CONSTRUCTED);
        
        assertSame(foo, pFoo.get());
        assertEquals(0, ANNOTATED_CONSTRUCTED);
        
        assertSame(foo, injector.getInstance(Foo.class));
        assertEquals(0, ANNOTATED_CONSTRUCTED);
        
        foo.bar();
        assertEquals(1, ANNOTATED_CONSTRUCTED);
        
        foo.baz();
        assertEquals(1, ANNOTATED_CONSTRUCTED);
    }
    
    public void testLazyBinderBoundToClass() {
        assertEquals(0, PLAIN_CONSTRUCTED);
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                install(new LimeWireInjectModule());
                bind(PlainFoo.class).in(LazySingleton.class);
                bind(Foo.class).toProvider(LazyBinder.newLazyProvider(Foo.class, PlainFoo.class));
            }
        });
        assertEquals(0, PLAIN_CONSTRUCTED);
        Provider<Foo> pFoo = injector.getProvider(Foo.class);
        assertEquals(0, PLAIN_CONSTRUCTED);
        Foo foo = injector.getInstance(Foo.class);
        assertEquals(0, PLAIN_CONSTRUCTED);
        
        assertSame(foo, pFoo.get());
        assertEquals(0, PLAIN_CONSTRUCTED);
        
        assertSame(foo, injector.getInstance(Foo.class));
        assertEquals(0, PLAIN_CONSTRUCTED);
        
        foo.bar();
        assertEquals(1, PLAIN_CONSTRUCTED);
        
        foo.baz();
        assertEquals(1, PLAIN_CONSTRUCTED);
    }
    
    public void testLazyBinderBoundToScope() {
        assertEquals(0, PLAIN_CONSTRUCTED);
        Injector injector = Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                install(new LimeWireInjectModule());
                bind(PlainFoo.class).in(MoreScopes.LAZY_SINGLETON);
                bind(Foo.class).toProvider(LazyBinder.newLazyProvider(Foo.class, PlainFoo.class));
            }
        });
        assertEquals(0, PLAIN_CONSTRUCTED);
        Provider<Foo> pFoo = injector.getProvider(Foo.class);
        assertEquals(0, PLAIN_CONSTRUCTED);
        Foo foo = injector.getInstance(Foo.class);
        assertEquals(0, PLAIN_CONSTRUCTED);
        
        assertSame(foo, pFoo.get());
        assertEquals(0, PLAIN_CONSTRUCTED);
        
        assertSame(foo, injector.getInstance(Foo.class));
        assertEquals(0, PLAIN_CONSTRUCTED);
        
        foo.bar();
        assertEquals(1, PLAIN_CONSTRUCTED);
        
        foo.baz();
        assertEquals(1, PLAIN_CONSTRUCTED);
    }
    
    public void testFailsOnNoScope() {
        try {
            Guice.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Foo.class).toProvider(
                            LazyBinder.newLazyProvider(Foo.class, PlainFoo.class));
                }
            });
        } catch (CreationException ce) {
            Collection<Message> messages = ce.getErrorMessages();
            assertEquals(1, messages.size());
            Message message = messages.iterator().next();
            assertEquals("Class: class org.limewire.inject.LazyBinderTest$PlainFoo must be in scope @Singleton or @LazySingleton", 
                        message.getCause().getMessage());
        }
    }
    
    private interface Foo {
        void bar();
        void baz();
    }
    
    @LazySingleton
    private static class AnnotatedFoo implements Foo {
        public AnnotatedFoo() {
            ANNOTATED_CONSTRUCTED++;
        }
        
        @Override public void bar() {}
        @Override public void baz() {}
    }
    
    private static class PlainFoo implements Foo {
        public PlainFoo() {
            PLAIN_CONSTRUCTED++;
        }
        
        @Override public void bar() {}
        @Override public void baz() {}
    }

}
