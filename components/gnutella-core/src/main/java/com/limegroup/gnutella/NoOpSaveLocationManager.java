package com.limegroup.gnutella;

import java.io.File;

import org.limewire.core.api.download.SaveLocationManager;

public class NoOpSaveLocationManager implements SaveLocationManager {

    public boolean isSaveLocationTaken(File candidateFile) {
        return false;
    }

}
