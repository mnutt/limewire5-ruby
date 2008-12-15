package org.limewire.promotion;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.promotion.exceptions.PromotionException;
import org.limewire.security.certificate.CertificateVerifier;
import org.limewire.security.certificate.CipherProvider;
import org.limewire.security.certificate.KeyStoreProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PromotionBinderFactoryImpl implements PromotionBinderFactory {
    
    private final static Log LOG = LogFactory.getLog(PromotionBinderFactoryImpl.class);

    private CipherProvider cipherProvider;

    private KeyStoreProvider keyStore;

    private CertificateVerifier certificateVerifier;

    @Inject
    public PromotionBinderFactoryImpl(CipherProvider cipherProvider, KeyStoreProvider keyStore,
            CertificateVerifier certificateVerifier) {
        this.cipherProvider = cipherProvider;
        this.keyStore = keyStore;
        this.certificateVerifier = certificateVerifier;
    }

    public PromotionBinder newBinder(InputStream in) {
        if (in == null) {
            return null;
        }
        PromotionBinder binder = new PromotionBinder(cipherProvider, keyStore, certificateVerifier);
        try {
            binder.initialize(IOUtils.readFully(in));
            return binder;
        } catch (PromotionException e) {
            LOG.error(e);           // Don't be verbose
        } catch (IOException e) {   // about this error
            LOG.error(e);           // But note it
        }
        return null;
    }    

}
