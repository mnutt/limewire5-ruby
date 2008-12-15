package org.limewire.security.certificate;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;

import junit.framework.Test;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.limewire.util.BaseTestCase;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class CertificateVerifierTest extends BaseTestCase {
    Injector injector = null;

    public CertificateVerifierTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(CertificateVerifierTest.class);
    }

    @Override
    public void setUp() throws Exception {
        injector = Guice.createInjector(new LimeWireSecurityCertificateModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(HttpClient.class).to(DefaultHttpClient.class);
            }
        });
    }

    public void testIsValidCertificatePass() throws IOException, KeyStoreException {
        CertificateVerifier cv = injector.getInstance(CertificateVerifier.class);
        KeyStoreProvider ksp = injector.getInstance(KeyStoreProvider.class);
        ksp.invalidateKeyStore();

        KeyStore ks = ksp.getKeyStore();
        Certificate certificate = ks.getCertificate("promotion.limewire.com");
        // We know this is a valid cert, it came from the damn key store...
        assertTrue(cv.isValid(certificate));
    }

    public void testIsValidCertificateFail() throws IOException, KeyStoreException {
        CertificateVerifier cv = injector.getInstance(CertificateVerifier.class);
        KeyStoreProvider ksp = injector.getInstance(KeyStoreProvider.class);
        ksp.invalidateKeyStore();

        KeyStore ks = ksp.getKeyStore();
        Certificate certificate = ks.getCertificate("unsigned.limewire.com");
        // We know this is a valid cert, but it shouldn't be valid since it's not signed...
        assertFalse(cv.isValid(certificate));
    }

}
