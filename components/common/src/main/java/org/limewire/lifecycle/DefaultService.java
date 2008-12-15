package org.limewire.lifecycle;

public class DefaultService implements Service {
    public void start() {}

    public void stop() {}

    public void initialize() {}

    public String getServiceName() {
        return getClass().getSimpleName();
    }
}
