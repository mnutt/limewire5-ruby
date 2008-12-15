package org.limewire.swarm.http;

import java.io.IOException;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.IOControl;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.limewire.collection.Range;
import org.limewire.io.IOUtils;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

public class SwarmHttpUtils {

    private static final Log LOG = LogFactory.getLog(SwarmHttpUtils.class);

    private SwarmHttpUtils() {
    }

    /**
     * Closes the connection associated with this HttpContext.
     */
    public static void closeConnectionFromContext(HttpContext context) {
        IOControl ioctrl = (IOControl) context.getAttribute(ExecutionContext.HTTP_CONNECTION);
        try {
            ioctrl.shutdown();
        } catch (IOException ignored) {
            LOG.warn("Error shutting down IOControl.", ignored);
        }
    }

    public static Range rangeForContentRange(String headerValue, long contentLength)
            throws IOException {
        try {
            int start = headerValue.indexOf("bytes") + 6; // skip "bytes " or
            // "bytes="
            int slash = headerValue.indexOf('/');

            // if looks like: "bytes */*" or "bytes */10" -- NOT part of the
            // spec
            if (headerValue.substring(start, slash).equals("*")) {
                return Range.createRange(0, contentLength - 1);
            }

            int dash = headerValue.lastIndexOf("-");
            long numBeforeDash = numberFor(headerValue.substring(start, dash));
            long numBeforeSlash = numberFor(headerValue.substring(dash + 1, slash));

            if (numBeforeSlash < numBeforeDash) {
                IOException e = new IOException("invalid range, high (" + numBeforeSlash
                        + ") less than low (" + numBeforeDash + ")");
                LOG.warn(e.getMessage(), e);
                throw e;
            }

            return Range.createRange(numBeforeDash, numBeforeSlash);

            // TODO: Is it necessary to validate the number after the slash
            // matches the fileCoordinator's size (or is '*')?
        } catch (IndexOutOfBoundsException e) {
            IOException ioException = IOUtils.getIOException("Invalid Header: " + headerValue, e);
            LOG.warn(ioException.getMessage(), ioException);
            throw ioException;
        }

    }

    public static long numberFor(String number) throws IOException {
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException nfe) {
            IOException e = IOUtils.getIOException("Invalid number: " + number, nfe);
            LOG.warn(e.getMessage(), e);
            throw e;
        }
    }

    public static Range parseContentRange(HttpResponse response) throws IOException {
        Range actualRange;

        Header contentRange = response.getFirstHeader("Content-Range");
        Header contentLengthHeader = response.getFirstHeader("Content-Length");

        if (contentLengthHeader != null) {
            long contentLength = SwarmHttpUtils.numberFor(contentLengthHeader.getValue());
            if (contentLength < 0) {
                IOException e = new IOException("Invalid content length: " + contentLength);
                LOG.warn(e.getMessage(), e);
                throw e;
            }

            if (contentRange != null) {
                // If a range exists, that's what we want.
                actualRange = SwarmHttpUtils.rangeForContentRange(contentRange.getValue(),
                        contentLength);
            } else {
                // If no range exists, assume 0 -> contentLength
                actualRange = Range.createRange(0, contentLength - 1);
            }
        } else if (contentRange == null) {
            // Fail miserably.
            IOException e = new IOException("No Content Range Found.");
            LOG.warn(e.getMessage(), e);
            throw e;
        } else {
            // Fail miserably.
            IOException e = new IOException("No content length, though content range existed.");
            LOG.warn(e.getMessage(), e);
            throw e;
        }
        return actualRange;
    }

    public static String logRequest(String message, HttpRequest request) {
        String requestLine = request != null ? request.getRequestLine().toString() : "null";
        String headers = request != null ? Arrays.asList(request.getAllHeaders()).toString()
                : "null";
        String log = message + " request : " + requestLine + " headers: " + headers;
        return log;
    }

    public static String logReponse(String message, HttpResponse response) {
        String statusLine = response != null ? response.getStatusLine().toString() : "null";
        String headers = response != null ? Arrays.asList(response.getAllHeaders()).toString()
                : "null";
        String log = message + ": " + statusLine + " headers: " + headers;
        return log;
    }
}
