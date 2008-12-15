package org.limewire.core.impl.support;

import java.util.Map;

import org.limewire.core.api.support.LocalClientInfo;

/**
 * Mock implementation of LocalClientInfo.
 */
public class MockLocalClientInfo implements LocalClientInfo {

    @Override
    public void addUserComments(String comments) {
    }

    @Override
    public String getParsedBug() {
        return null;
    }

    @Override
    public Map.Entry[] getPostRequestParams() {
        return null;
    }

    @Override
    public String getShortParamList() {
        return null;
    }

    @Override
    public String toBugReport() {
        return null;
    }

}
