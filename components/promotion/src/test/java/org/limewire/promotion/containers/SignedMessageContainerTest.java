package org.limewire.promotion.containers;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

import junit.framework.Test;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.security.certificate.CertificateVerifier;
import org.limewire.security.certificate.CipherProviderImpl;
import org.limewire.security.certificate.KeyStoreProvider;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

public class SignedMessageContainerTest extends BaseTestCase {
    public SignedMessageContainerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(SignedMessageContainerTest.class);
    }

    public void testSetAndSign() throws NoSuchAlgorithmException, IOException {
        SignedMessageContainer message = new SignedMessageContainer();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair keyPair = generator.generateKeyPair();

        message.setAndSignWrappedMessage(new MockMessage(), new CipherProviderImpl(), keyPair
                .getPrivate(), "foo");
    }

    public void testSignAndVerifyCycle() throws NoSuchAlgorithmException, IOException,
            KeyStoreException, CertificateException, BadGGEPPropertyException {
        SignedMessageContainer message = new SignedMessageContainer();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair keyPair = generator.generateKeyPair();

        KeyStore keyStore = KeyStore.getInstance("jks");
        keyStore.load(null);
        keyStore.setCertificateEntry("foo", new MockCertificate(keyPair.getPublic()));

        MockMessage mockMessage = new MockMessage();
        mockMessage.setFoo("before");
        message.setAndSignWrappedMessage(mockMessage, new CipherProviderImpl(), keyPair
                .getPrivate(), "foo");
        // OK, we've put the message in and signed it. Try changing our
        // reference to the mock message. It shouldn't affect
        // anything.

        mockMessage.setFoo("after");
        MessageContainerParser.addParser(new MockMessage());

        MockMessage decodedMessage = (MockMessage) message.getAndVerifyWrappedMessage(
                new CipherProviderImpl(), new MockKeyStoreProvider(keyStore),
                new MockCertificateVerifier());
        assertEquals("before", decodedMessage.getFoo());
    }

    /** Fake certificate, just holds the public key. */
    private class MockCertificate extends Certificate {
        private PublicKey publicKey;

        MockCertificate(PublicKey publicKey) {
            super(publicKey.getAlgorithm());
            this.publicKey = publicKey;
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public PublicKey getPublicKey() {
            return publicKey;
        }

        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void verify(PublicKey key) throws CertificateException, NoSuchAlgorithmException,
                InvalidKeyException, NoSuchProviderException, SignatureException {
            // TODO Auto-generated method stub

        }

        @Override
        public void verify(PublicKey key, String sigProvider) throws CertificateException,
                NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException,
                SignatureException {
            // TODO Auto-generated method stub

        }

    }

    /** Always returns the passed-in keystore. */
    private class MockKeyStoreProvider implements KeyStoreProvider {
        private KeyStore keystore;

        MockKeyStoreProvider(KeyStore keystore) {
            this.keystore = keystore;
        }

        public KeyStore getKeyStore() throws IOException {
            return keystore;
        }

        public void invalidateKeyStore() {
        }

        public boolean isCached() {
            return false;
        }
    }

    /** Always validates certificates. */
    private class MockCertificateVerifier implements CertificateVerifier {

        public boolean isValid(java.security.cert.Certificate certificate) {
            return true;
        }

    }

    public static class MockMessage implements MessageContainer {
        private GGEP payload = new GGEP();

        public byte[] getType() {
            return StringUtils.toUTF8Bytes("MOCK");
        }

        public void setFoo(String foo) {
            payload.put("FOO", foo);
        }

        public String getFoo() throws BadGGEPPropertyException {
            if (!payload.hasKey("FOO"))
                return null;
            return payload.getString("FOO");
        }

        public byte[] encode() {
            payload.put(TYPE_KEY, getType());
            return payload.toByteArray();
        }

        public void decode(GGEP rawGGEP) throws BadGGEPBlockException {
            payload = rawGGEP;
        }
    }
}
