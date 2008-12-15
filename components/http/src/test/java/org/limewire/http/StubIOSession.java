/**
 * 
 */
package org.limewire.http;

import java.util.concurrent.Executor;

import org.limewire.http.reactor.HttpIOSession;
import org.limewire.nio.AbstractNBSocket;

public class StubIOSession extends HttpIOSession {      

    boolean shutdown;

    public StubIOSession(AbstractNBSocket socket) {
        this(socket, new Executor() {
            public void execute(Runnable command) {
                command.run();
            }
        });
    }
    
    public StubIOSession(AbstractNBSocket socket, Executor executor) {
        super(socket, executor);
    }

    @Override
    public boolean isClosed() {
        return super.isClosed() || isShutdown();
    }
    
    public boolean isShutdown() {
        return shutdown;
    }
    
    @Override
    public void shutdown() {
        this.shutdown = true;
    }
    
}