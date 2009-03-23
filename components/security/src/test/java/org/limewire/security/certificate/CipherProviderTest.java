package org.limewire.security.certificate;

import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;

import junit.framework.Test;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.limewire.security.LimeWireSecurityModule;
import org.limewire.security.certificate.CipherProvider.CipherType;
import org.limewire.security.certificate.CipherProvider.SignatureType;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class CipherProviderTest extends BaseTestCase {
    private CipherProvider cipherProvider;

    public CipherProviderTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(CipherProviderTest.class);
    }

    public void testEncyptDecryptCycleRSA() throws NoSuchAlgorithmException, IOException {
        byte[] plaintext = StringUtils.toAsciiBytes("I'm a test.");

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair keyPair = generator.generateKeyPair();
        System.out.println(keyPair.getPrivate().getAlgorithm());

        byte[] ciphertext = cipherProvider.encrypt(plaintext, keyPair.getPrivate(), CipherType.RSA);
        assertGreaterThan(0, ciphertext.length);
        byte[] plaintext2 = cipherProvider.decrypt(ciphertext, keyPair.getPublic(), CipherType.RSA);
        assertEquals(plaintext, plaintext2);
    }

    public void testEncyptDecryptCycleAES() throws NoSuchAlgorithmException, IOException {
        byte[] plaintext = StringUtils.toAsciiBytes("Lorem ipsum dolor sit amet, consectetuer adipiscing elit. "
                + "Pellentesque posuere, metus nonummy molestie dictum, ligula eros nonummy "
                + "pede, sit amet bibendum risus risus eget turpis. Nam porttitor ultrices "
                + "enim. Quisque ut nibh non tortor vestibulum dapibus. Fusce risus. Morbi "
                + "molestie egestas lacus. Morbi bibendum. Maecenas bibendum, risus at aliquam "
                + "vehicula, pede magna bibendum risus, at viverra sem est eget elit. Donec id "
                + "nisl non lacus semper lobortis. Mauris non nibh. Curabitur non mauris. Maecenas "
                + "sit amet leo placerat enim placerat gravida.");

        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128); // 192 and 256 bits may not be available
        Key key = kgen.generateKey();

        byte[] ciphertext = cipherProvider.encrypt(plaintext, key, CipherType.AES);
        assertGreaterThan(0, ciphertext.length);
        byte[] plaintext2 = cipherProvider.decrypt(ciphertext, key, CipherType.AES);
        assertEquals(plaintext, plaintext2);
    }

    public void testSignVerifyCycleRSA() throws NoSuchAlgorithmException, IOException {
        byte[] plaintext = StringUtils.toAsciiBytes("Lorem ipsum dolor sit amet, consectetuer adipiscing elit. "
                + "Pellentesque posuere, metus nonummy molestie dictum, ligula eros nonummy "
                + "pede, sit amet bibendum risus risus eget turpis. Nam porttitor ultrices "
                + "enim. Quisque ut nibh non tortor vestibulum dapibus. Fusce risus. Morbi "
                + "molestie egestas lacus. Morbi bibendum. Maecenas bibendum, risus at aliquam "
                + "vehicula, pede magna bibendum risus, at viverra sem est eget elit. Donec id "
                + "nisl non lacus semper lobortis. Mauris non nibh. Curabitur non mauris. Maecenas "
                + "sit amet leo placerat enim placerat gravida.");

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair keyPair = generator.generateKeyPair();

        byte[] signature = cipherProvider.sign(plaintext, keyPair.getPrivate(),
                SignatureType.SHA1_WITH_RSA);

        assertGreaterThan(0, signature.length);
        assertTrue(cipherProvider.verify(plaintext, signature, keyPair.getPublic(),
                SignatureType.SHA1_WITH_RSA));

    }

    @Override
    protected void setUp() throws Exception {
        Injector injector = Guice.createInjector(new LimeWireSecurityModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(HttpClient.class).to(DefaultHttpClient.class);
            }
        });
        cipherProvider = injector.getInstance(CipherProvider.class);
    }

}
