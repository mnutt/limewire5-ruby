package org.limewire.ui.swing.upload.table;

import java.awt.Font;

import org.jdesktop.application.Resource;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.properties.AbstractPropertiableFileDialog;
import org.limewire.ui.swing.properties.DialogParam;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PropertiableHeadings;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class UploadItemPropertiesFactory implements PropertiesFactory<UploadItem> {
    private final PropertiableHeadings propertiableHeadings;
    private final DialogParam dialogParam;
    
    @Inject
    public UploadItemPropertiesFactory(PropertiableHeadings propertiableHeadings, DialogParam dialogParam) {
        this.propertiableHeadings = propertiableHeadings;
        this.dialogParam = dialogParam;
    }

    public Properties<UploadItem> newProperties() {
        return new UploadItemProperties(propertiableHeadings, dialogParam);
    }

    private static class UploadItemProperties extends AbstractPropertiableFileDialog implements Properties<UploadItem>{
        private @Resource Font smallFont;
        private @Resource Font mediumFont;
        private @Resource Font largeFont;
        
        private UploadItemProperties(PropertiableHeadings propertiableHeadings, DialogParam dialogParam) {
            super(propertiableHeadings, dialogParam);
            GuiUtils.assignResources(this);
        }

        @Override
        protected Font getSmallFont() {
            return smallFont;
        }
        
        @Override
        protected Font getLargeFont() {
            return largeFont;
        }

        @Override
        protected Font getMediumFont() {
            return mediumFont;
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
