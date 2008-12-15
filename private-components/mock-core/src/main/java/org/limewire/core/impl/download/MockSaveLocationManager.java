package org.limewire.core.impl.download;

import java.io.File;

import org.limewire.core.api.download.SaveLocationManager;

public class MockSaveLocationManager implements SaveLocationManager {

    @Override
    public boolean isSaveLocationTaken(File candidateFile) {
        return false;
    }

}
