package org.limewire.promotion;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpException;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.limewire.promotion.impressions.UserQueryEvent;
import org.limewire.promotion.impressions.UserQueryEventData;

/**
 * Instances of this class make <code>POST</code> requests to the URL given
 * for containers, and also add the data given in the {@link UserQueryEvent}.
 * <p>
 * The {@link PromotionBinder} that is passed in is created by the injected
 * {@link PromotionBinderFactory}.
 */
public abstract class AbstractPromotionBinderRequestor implements PromotionBinderRequestor {

    private final PromotionBinderFactory binderFactory;

    public AbstractPromotionBinderRequestor(PromotionBinderFactory binderFactory) {
        this.binderFactory = binderFactory;
    }

    /**
     * The main entry point. This will create a <code>POST</code> request to
     * <code>url</code> and include the proper information we want to store.
     */
    public final PromotionBinder request(String url, long id, Set<? extends UserQueryEvent> queries) {
        //
        // This request takes the following parameters
        // - id: bucket ID for the bucket that will be returned
        // - for i=1..n
        // query_<i>: query for the ith UserQueryEvent
        // data_<i>: data for the ith UserQueryEvent
        //

        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2*queries.size() + 1);
        nameValuePairs.add(new BasicNameValuePair("id", String.valueOf(id)));
        int i = 0;
        for (UserQueryEvent e : queries) {
            //
            // The query and data
            //
            UserQueryEventData data = new UserQueryEventData(e);
            String dataStr = new String(new Base64().encode(data.getData()));
            nameValuePairs.add(new BasicNameValuePair("query_" + i, data.getQuery())); 
            nameValuePairs.add(new BasicNameValuePair("data_" + i, dataStr));
            i++;
        }
        HttpPost tmp = null;
        String alteredUrl = alterUrl(url);
        tmp = new HttpPost(alteredUrl);
        final HttpPost request = tmp;
        try {
            request.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        } catch (UnsupportedEncodingException e) {
            error(e);
            return null;
        }
        HttpParams params = new BasicHttpParams();
        request.addHeader("User-Agent", getUserAgent());
        try {
            return binderFactory.newBinder(makeRequest(request, params));
        } catch (HttpException e) {
            error(e);
        } catch (IOException e) {
            error(e);
        }
        return null;
    }
    
    /**
     * Called when an {@link Exception} occurs.
     * 
     * @param e the {@link Exception} that occured.
     */
    protected abstract void error(Exception e);

    /**
     * Returns the <code>User-Agent</code> to send along with the
     * <code>POST</code> request.
     * 
     * @return the <code>User-Agent</code> to send along with the
     *         <code>POST</code> request.
     */
    protected abstract String getUserAgent();

    /**
     * Subclasses should alter the final URL in this method, such as adding
     * version information, etc.
     */
    protected abstract String alterUrl(String url);
    
    /**
     * Called once the {@link PostMethod} <code>request</code> is constructed.
     * The purpose of the callback is so we can pass a {@link PromotionBindder}
     * created from the {@link PromotionBinderFactory} from the passed in bytes
     * we recieve from the request.
     * 
     * @param request <code>POST</code> to send to a server
     * @throws HttpException thrown when a protocol error occurs
     * @throws IOException thrown when a protocol I/O occurs
     * 
     * @return a stream for the HTTP request
     */
    protected abstract InputStream makeRequest(HttpPost request, HttpParams params) throws HttpException, IOException;
    
    public static String encode(String string) {
        try {
            return URLEncoder.encode(string, "8859_1");
        } catch(UnsupportedEncodingException uee) {
            return string;
        }
    }
  
}
