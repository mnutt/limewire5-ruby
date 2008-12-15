package com.limegroup.gnutella.browser;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import com.limegroup.gnutella.util.LimeWireUtils;


/**
 *  Handle a pure HTTP request
 */
public class HTTPHandler {

    private Socket       _socket; 
	private OutputStream _ostream;

	/**
     *  Create and execute the handler without a new thread.
     */
	public static HTTPHandler createPage( Socket socket, String content ) {

    	HTTPHandler handler = new HTTPHandler(socket);
		handler.handlePage(content);
		return(handler);
	}

    public HTTPHandler( Socket socket) {
        _socket      = socket;
    }

    /**
     *  Return a precreated page
     */
    public void handlePage(String page) {

		// Setup streams 
		setupIO();
		uploadPage(page);
	}


	private void setupIO() {
		try {
			_ostream  = _socket.getOutputStream();
		} catch (IOException e) {
		}
	}
    /**
     *  Echo back a page.
     */
	public void uploadPage(String page) {
        int             length  = page.length();
		byte[]          content;

        try {
			writeHeader(length, getMimeType(".html"));
			content = page.getBytes();
            _ostream.write(content);

        } catch( IOException e ) {
		}

		try {
		    _ostream.close();
        } catch( IOException e ) {
		}
    }

	/** 
	 *  Setup the few mime-types currently required.
	 */
	private String getMimeType(String filename) {
		if ( filename.endsWith(".gif") )
			return "image/gif";
		else if ( filename.endsWith(".img") )
			return "image/gif";
		else if ( filename.endsWith(".js") )
			return "application/x-javascript";
        else if ( filename.endsWith(".css") )
            return "text/css";
        return "text/html"; 
	}

	/**
	 *  Write a simple header
	 */
	private void writeHeader(int length, String mimeType) throws IOException {
		String str;
		str = "HTTP/1.1 200 OK \r\n";
		_ostream.write(str.getBytes());
		str = "Server: "+LimeWireUtils.getVendor()+"\r\n";
		_ostream.write(str.getBytes());
		str = "Content-type:" + mimeType + "\r\n";
		_ostream.write(str.getBytes());
		str = "Content-length:"+ length + "\r\n";
		_ostream.write(str.getBytes());
		
		str = "\r\n";
		_ostream.write(str.getBytes());
		
	}

	/**
	 * Write out a 404 error.
	 */
	public void writeError() {
		try {
			/* Sends a 404 File Not Found message */
			String str;
			str = "HTTP/1.1 404 Not Found\r\n";
			_ostream.write(str.getBytes());
			/**
			str = "Server: " + CommonUtils.getVendor() + "\r\n";
			_ostream.write(str.getBytes());
			str = "Content-Type: text/plain\r\n";
			_ostream.write(str.getBytes());
			str = "Content-Length: " + 0 + "\r\n";
			_ostream.write(str.getBytes());
			str = "\r\n";
			_ostream.write(str.getBytes());
			*/
			_ostream.flush();

        } catch( IOException e ) {
		}

		try {
		    _ostream.close();
        } catch( IOException e ) {
		}
	}
}
