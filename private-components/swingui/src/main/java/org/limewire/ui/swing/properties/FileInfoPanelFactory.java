package org.limewire.ui.swing.properties;

import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;

/**
 * Constructs a FileInfoPanel with the given assisted parameters.
 */
public interface FileInfoPanelFactory {

    public FileInfoPanel createFileInfoPanel(PropertiableFile propertiableFile, FileInfoType type);
}
