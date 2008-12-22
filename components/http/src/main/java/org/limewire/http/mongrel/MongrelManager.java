package org.limewire.http.mongrel;

public interface MongrelManager {
    void start();
    void stop();
    boolean isAsyncStop();
    String getServiceName();
}
