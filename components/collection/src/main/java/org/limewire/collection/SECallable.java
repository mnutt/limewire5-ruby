package org.limewire.collection;

import java.util.concurrent.Callable;

/**
 * Defines the interface to be a <code>Callable</code> that throws
 * a single exception.
 */
public interface SECallable<Ret, Throw extends Exception> extends Callable<Ret> {
	Ret call() throws Throw;
}
