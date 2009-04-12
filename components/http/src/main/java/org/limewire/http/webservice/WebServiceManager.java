package org.limewire.http.webservice;

public interface WebServiceManager {
    void start();
    void stop();
    boolean isAsyncStop();
    String getServiceName();
    String getStatus();
    void setStatus(String status);
    void restart();
    void loadWebService(String railsRoot);
    void mapPort();
}

