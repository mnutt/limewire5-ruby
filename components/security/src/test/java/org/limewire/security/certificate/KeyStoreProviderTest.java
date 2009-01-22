package org.limewire.security.certificate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import junit.framework.Test;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.limewire.inject.Providers;
import org.limewire.util.BaseTestCase;

public class KeyStoreProviderTest extends BaseTestCase {
    private char[] getUnitKeyStorePassword(){
        return "".toCharArray();
    }

    public KeyStoreProviderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(KeyStoreProviderTest.class);
    }

    public void testGetKeyStoreFromNetwork() throws IOException, KeyStoreException {
        KeyStoreProviderImpl ksp = new KeyStoreProviderImpl(Providers.of((HttpClient)new DefaultHttpClient()));
        KeyStore keystore = ksp.getKeyStoreFromNetwork();
        validateKeyStore(keystore);
    }

    private void validateKeyStore(KeyStore keyStore) throws KeyStoreException {
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("ca.limewire.com");
        assertEquals(
                "3B0FAC37D4F1847574FD8FB6F56DAA05B0E3D81D8EE1A4FB2552A91E0A8D29CB2E0EE1FEB13BB547E59F8B127D0D48114828E5C96EF73BB2EBDFF0213D3B67FE7C1A2CC37CC5EF3D5BDD45F2FCE1D37F103DBB75BA7819E857FFA9B8D5D12FF8DC1D36A87B1905301AEBA86FB8E973295944233601DDCEB265B232F404831DA8",
                CertificateTools.encodeBytesToString(certificate.getSignature()));
    }

    /**
     * Loads the key store from the network, saves it to disk, then calls
     * getKeyStoreFromDisk.
     * 
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    public void testGetKeyStoreFromDisk() throws IOException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException {
        File location = File.createTempFile("lwunit", "keystore");
        location.deleteOnExit();

        KeyStoreProviderImpl ksp = new KeyStoreProviderImpl(Providers.of((HttpClient)new DefaultHttpClient()));
        ksp.setKeyStoreLocation(location);
        ksp.setKeyStorePassword(getUnitKeyStorePassword());
        KeyStore keystore = ksp.getKeyStoreFromNetwork();
        keystore.store(new FileOutputStream(location), getUnitKeyStorePassword());

        // Saved to disk, see if it loads!
        validateKeyStore(ksp.getKeyStoreFromDisk());
        location.delete();
    }

    public void testIsCachedAndInvalidateKeyStore() throws IOException, KeyStoreException {
        File location = File.createTempFile("lwunit", "keystore");
        location.delete();
        try {
            assertFalse(location.exists());
            KeyStoreProviderImpl ksp = new KeyStoreProviderImpl(Providers.of((HttpClient)new DefaultHttpClient()));
            ksp.setKeyStoreLocation(location);
            ksp.setKeyStorePassword(getUnitKeyStorePassword());
            assertFalse(ksp.isCached());

            // Make sure caching works
            validateKeyStore(ksp.getKeyStore());
            assertTrue(location.exists());
            assertTrue(ksp.isCached());
            // Make sure invalidation works
            ksp.invalidateKeyStore();
            assertFalse(location.exists());
            assertFalse(ksp.isCached());
            // Make sure in-memory caching works
            validateKeyStore(ksp.getKeyStore());
            assertTrue(location.exists());
            assertTrue(ksp.isCached());
            location.delete();
            assertFalse(location.exists());
            assertTrue(ksp.isCached());
        } finally {
            location.delete();
        }
    }

}
