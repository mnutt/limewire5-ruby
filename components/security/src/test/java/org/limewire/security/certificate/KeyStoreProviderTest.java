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
                "82565A033FC668D2A4F4D2823E0482349E507A4A241CC056BC291EC5CFBB7347CAB8B98612C5395165D7319E2B90A4F5902F048F78447ADA980246AE0BF89AFB29604C45089DE7D1CA31AA791A0F6D19FA38C583CA745C5728EF039CA44462B2F11E4076BC7E72539A33930ECFCAB66DACB79C87790797A129992A5E908DF869",
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
