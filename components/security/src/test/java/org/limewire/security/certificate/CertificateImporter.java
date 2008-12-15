package org.limewire.security.certificate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * This class must be run from the root of the limewire repository.
 */
public class CertificateImporter {
    public static void main(String[] args) throws CertificateException, KeyStoreException,
            NoSuchAlgorithmException, IOException {
        String base = "src/main/java/org/limewire/security/certificate/v3certs/";
        // The keystore to add the certificates to
        // FileInputStream ksis = new FileInputStream(base + "keystore");


        // The certificate files, to be added to keystore
        FileInputStream certFile1 = new FileInputStream(new File(base, "cacert.der"));
        FileInputStream certFile2 = new FileInputStream(new File(base, "promocert.der"));

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // Read the 3 certificates into memory
        Certificate cert1 = cf.generateCertificate(certFile1);
        X509Certificate cert2 = (X509Certificate)cf.generateCertificate(certFile2);

        // Read the keystore file, type="jks"
        KeyStore ks = KeyStore.getInstance("jks");

        char[] password = "".toCharArray();
        ks.load(null, password);

        // Add certificates to keystore
        ks.setCertificateEntry("ca.limewire.com", cert1);
        ks.setCertificateEntry("promotion.limewire.com", cert2);

        // Write keystore to file system
        ks.store(new FileOutputStream(new File(base, "limewire.keystore")), password);
    }
}
