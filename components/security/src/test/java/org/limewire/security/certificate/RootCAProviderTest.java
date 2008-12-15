package org.limewire.security.certificate;

import junit.framework.Test;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.limewire.security.LimeWireSecurityModule;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class RootCAProviderTest extends BaseTestCase {
    private RootCAProvider rootCAProvider;

    public RootCAProviderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RootCAProviderTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = Guice.createInjector(new LimeWireSecurityModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(HttpClient.class).to(DefaultHttpClient.class);
            }
        });
        rootCAProvider = injector.getInstance(RootCAProvider.class);
    }

    public void testInjection() {
        assertNotNull(rootCAProvider);
    }
}
