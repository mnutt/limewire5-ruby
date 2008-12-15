package com.limegroup.gnutella.http;

import java.util.concurrent.ExecutorService;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.HttpParams;
import org.limewire.collection.Cancellable;
import org.limewire.nio.observer.Shutdownable;


/**
 * Something that executes HTTP requests using the http client library.
 */
public interface HttpExecutor {
	/**
	 * Execute the provided <tt>HttpMethod</tt> and notifies the
	 * provided <tt>HttpClientListener</tt> using a default
	 * <tt>ThreadPool</tt>
     *
     * This returns a Shutdownable that can be used to shutdown the execution
     * of requesting all methods, to stop the current processing.
	 */
	public Shutdownable execute(HttpUriRequest method, HttpParams params, HttpClientListener listener);
	
	/**
	 * Execute the provided <tt>HttpMethod</tt> using the provided <tt>ThreadPool</tt>
	 * and notifies the provided <tt>HttpClientListener</tt> on the same thread.
     *
     * This returns a Shutdownable that can be used to shutdown the execution
     * of requesting all methods, to stop the current processing.
	 */
	public Shutdownable execute(HttpUriRequest method,
            HttpParams params,
            HttpClientListener listener,
			ExecutorService executor);
	
	/**
	 * Tries to execute any of the methods until the HttpClientListener
     * instructs the executor to stop processing more, or the Cancellable
     * returns true for isCancelled.
     * 
     * This returns a Shutdownable that can be used to shutdown the execution
     * of requesting all methods, to stop the current processing.
	 */
	public Shutdownable executeAny(HttpClientListener listener,
			ExecutorService executor,
			Iterable<? extends HttpUriRequest> methods,
            HttpParams params,
            Cancellable canceller);
	
	/**
	 * Release any resources held by the provided method.
	 * The users of this class must call this method once they're done
	 * processing their HttpMethod object.
	 */
	public void releaseResources(HttpResponse method);
}
