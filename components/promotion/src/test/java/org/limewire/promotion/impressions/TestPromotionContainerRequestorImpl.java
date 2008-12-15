package org.limewire.promotion.impressions;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.HttpParams;
import org.limewire.promotion.AbstractPromotionBinderRequestor;
import org.limewire.promotion.PromotionBinder;
import org.limewire.promotion.PromotionBinderFactory;

/**
 * Instances of this will use either return a valid or invalid
 * {@link PromotionBinder}. This flag {@link #isValid} represents a request
 * that has a Binder or one that does not. Hence if {@link #isValid} is
 * <code>true</code> we will return a non-null binder; otherwise it will be
 * <code>null</code>.
 */
public class TestPromotionContainerRequestorImpl extends AbstractPromotionBinderRequestor {

    private final boolean isValid;

    public TestPromotionContainerRequestorImpl(boolean isValid) {
        super(new PromotionBinderFactory() {
            public PromotionBinder newBinder(InputStream in) {
                //
                // See the contract for this method
                //
                if (in == null) {
                    return null;
                }
                return new PromotionBinder(null, null, null);
            }
        });
        this.isValid = isValid;
    }

    @Override
    protected InputStream makeRequest(HttpPost request, HttpParams params) throws HttpException, IOException {
        return isValid ? new EmptyInputStream() : null;
    }

    @Override
    public void error(Exception e) {
        e.printStackTrace();
    }

    @Override
    public String getUserAgent() {
        return "Limewire/@version@";
    }

    @Override
    protected String alterUrl(String url) {
        //
        // Don't alter it
        //
        return url;
    }

    public void setNetworkTimeout(int timeoutMillis) {
        // Ignore this, because we never hit the network
    }

    /**
     * Empty and always returns <code>-1</code>.
     */
    private final class EmptyInputStream extends InputStream {

        @Override
        public int read() throws IOException {
            return -1;
        }

    }

}
