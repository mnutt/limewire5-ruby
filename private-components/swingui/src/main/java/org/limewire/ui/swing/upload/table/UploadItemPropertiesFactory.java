package org.limewire.ui.swing.upload.table;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.properties.AbstractPropertiableFileDialog;
import org.limewire.ui.swing.properties.DialogParam;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UploadItemPropertiesFactory implements PropertiesFactory<UploadItem> {
    private final DialogParam dialogParam;
    
    @Inject
    public UploadItemPropertiesFactory(DialogParam dialogParam) {
        this.dialogParam = dialogParam;
    }

    public Properties<UploadItem> newProperties() {
        return new UploadItemProperties(dialogParam);
    }

    private static class UploadItemProperties extends AbstractPropertiableFileDialog implements Properties<UploadItem>{
        
        private UploadItemProperties(DialogParam dialogParam) {
            super(dialogParam);
            GuiUtils.assignResources(this);
        }

        @Override
        public void showProperties(final UploadItem propertiable) {
            
            populateCommonFields(propertiable);
            
            showDialog(propertiable.getFileName(), propertiable.getCategory());
        }
      

        @Override
        protected void commit() {
            //no-op... Downloads have no mutable fields
        }
    }
}
