package com.limegroup.gnutella.lws.server;

import java.util.Map;

import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;
import org.limewire.lws.server.LWSDispatcherSupport.ErrorCodes;
import org.limewire.lws.server.LWSDispatcherSupport.Parameters;

import com.limegroup.gnutella.lws.server.LWSManager.AbstractHandler;

/**
     * A {@link LWSManagerCommandResponseHandler} requiring a callback specified by the
     * parameter {@link Parameters#CALLBACK}.
     */
    public abstract class LWSManagerCommandResponseHandlerWithCallback extends AbstractHandler {
        
        protected LWSManagerCommandResponseHandlerWithCallback(String name) {
            super(name);
        }

        public final String handle(final Map<String, String> args) {
            final String callback = args.get(LWSDispatcherSupport.Parameters.CALLBACK);
            if (callback == null) {
                return report(LWSDispatcherSupport.ErrorCodes.MISSING_CALLBACK_PARAMETER);
            }
            return handleRest(args);
        }

        /**
         * Returns the result <b>IN PLAIN TEXT</b>. Override this to provide
         * functionality after the {@link Parameters#CALLBACK} argument has been
         * extracted. This method should <b>NOT</b> wrap the result in the
         * callback, nor should it be called from any other method except this
         * abstract class.
         * 
         * <br/><br/>
         * 
         * Instances of this class
         * must not use {@link #report(String)}, and <b>must</b> only pass back
         * error codes from {@link ErrorCodes}.  To ensure that {@link #report(String)}
         * is implemented to throw a {@link RuntimeException}.
         * 
         * @param args original, untouched arguments
         * @return result <b>IN PLAIN TEXT</b>
         */
        protected abstract String handleRest(Map<String, String> args);
        
        /**
         * Overrides {@link AbstractHandler#report(String)} by simply wrapping
         * the error with the error prefix as defined in {@link LWSServerUtil#wrapError(String)}
         * so that we don't wrap it in a callback.
         */
        public static String report(String error) {
            return LWSServerUtil.wrapError(error);
        }        
    }