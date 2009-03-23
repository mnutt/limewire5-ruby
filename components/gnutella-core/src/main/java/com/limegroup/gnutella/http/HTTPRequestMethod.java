package com.limegroup.gnutella.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Type-safe enum for HTTP request methods, as specified in RFC 2616.
 * The only required methods for HTTP 1.1 compliance are GET and HEAD.
 */
public abstract class HTTPRequestMethod {

	/**
	 * Constant for the name of the HTTP request method, in uppercase.
	 */
	private final String METHOD_NAME;

	/**
	 * Constructor creates a new <tt>HTTPRequestMethod</tt> with the 
	 * specified indentifying string.
	 *
	 * @param methodName the http request method as a string
	 */
	private HTTPRequestMethod(String methodName) {
		// store the method name in upper case to make string
		// comparisons easier
		METHOD_NAME = methodName.toUpperCase(Locale.US);
	}

    public String getMethod() {
        return METHOD_NAME;
    }
    
	/**
	 * Abstract method for writing the HTTP response based on the HTTP
	 * request method.
	 *
	 * @param response the <tt>HTTPMessage</tt> instance that handles
	 *  writing the actual message
	 * @param os the <tt>OutputStream</tt> to write to
	 * @throws <tt>IOException</tt> if there was an IO error writing 
	 *  the response
	 * @throws <tt>NullPointerException</tt> if either the <tt>response</tt>
	 *  or the <tt>os</tt> arguments are <tt>null</tt>
	 */
	public abstract void writeHttpResponse(HTTPMessage response, 
										   OutputStream os) 
		throws IOException;

	/**
	 * Constant for the "GET" request method.
	 */
	public static final HTTPRequestMethod GET = 
		new HTTPRequestMethod("GET") {
			@Override
            public void writeHttpResponse(HTTPMessage response, 
										  OutputStream os) 
			throws IOException {
				if(response == null) {
					throw new NullPointerException
					    ("cannot write null response object");
				} else if(os == null) {
					throw new NullPointerException
					    ("cannot write to null output stream");
				}
				response.writeMessageHeaders(os);
				response.writeMessageBody(os);
				os.flush();
			}
		};

	/**
	 * Constant for the "HEAD" request method.
	 */
	public static final HTTPRequestMethod HEAD = 
		new HTTPRequestMethod("HEAD") {
			@Override
            public void writeHttpResponse(HTTPMessage response, 
										  OutputStream os) 
			    throws IOException {
				if(response == null) {
					throw new NullPointerException
					    ("cannot write null response object");
				} else if(os == null) {
					throw new NullPointerException
					    ("cannot write to null output stream");
				}
				response.writeMessageHeaders(os);
				os.flush();
			}
		};
    
    // overrides Object.toString to report more information
    @Override
    public String toString() {
        return "HTTPRequestMethod: "+METHOD_NAME;
    }
}
