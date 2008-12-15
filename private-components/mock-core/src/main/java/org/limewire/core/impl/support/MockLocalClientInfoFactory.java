package org.limewire.core.impl.support;

import org.limewire.core.api.support.LocalClientInfo;
import org.limewire.core.api.support.LocalClientInfoFactory;

public class MockLocalClientInfoFactory implements LocalClientInfoFactory {

    @Override
    public LocalClientInfo createLocalClientInfo(Throwable bug, 
            String threadName, String detail, boolean fatal) {
        return new MockLocalClientInfo();
    }

}
