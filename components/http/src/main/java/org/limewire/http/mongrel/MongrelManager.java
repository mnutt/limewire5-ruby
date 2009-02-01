package org.limewire.http.mongrel;

public interface MongrelManager {
    void start();
    void stop();
    boolean isAsyncStop();
    String getServiceName();
    String getStatus();
    void setStatus(String status);
    void restart();
    void loadMongrel();
    void mapPort();
}
