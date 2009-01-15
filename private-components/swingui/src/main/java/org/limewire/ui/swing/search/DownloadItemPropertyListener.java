/**
 * 
 */
package org.limewire.ui.swing.search;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;

public class DownloadItemPropertyListener implements PropertyChangeListener {
    private final VisualSearchResult vsr;

    public DownloadItemPropertyListener(VisualSearchResult vsr) {
        this.vsr = vsr;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if ("state".equals(evt.getPropertyName())) {
            DownloadState state = (DownloadState) evt.getNewValue();
            switch (state) {
            case CANCELLED:
                vsr.setDownloadState(BasicDownloadState.NOT_STARTED);
                break;
            case DONE:
                vsr.setDownloadState(BasicDownloadState.DOWNLOADED);
                break;
            case ERROR:
            case CONNECTING:
            case LOCAL_QUEUED:
            case PAUSED:
            case REMOTE_QUEUED:
            case STALLED:
            case FINISHING:
            case DOWNLOADING:
                vsr.setDownloadState(BasicDownloadState.DOWNLOADING);
                break;
            }
        }
    }
}