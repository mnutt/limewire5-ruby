package org.limewire.ui.swing.library;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.properties.DialogParam;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class RemoteFileItemPropertiesFactory implements PropertiesFactory<RemoteFileItem> {
    private final DialogParam dialogParam;
    
    @Inject
    public RemoteFileItemPropertiesFactory(DialogParam dialogParam) {
        this.dialogParam = dialogParam;
    }

    @Override
    public Properties<RemoteFileItem> newProperties() {
        return new RemoteFileItemProperties(dialogParam);
    }

    private static class RemoteFileItemProperties extends AbstractFileItemDialog implements Properties<RemoteFileItem> {
        private final CategoryIconManager categoryIconManager;
        
        public RemoteFileItemProperties(DialogParam dialogParam) {
            super(dialogParam);
            this.categoryIconManager = dialogParam.getCategoryIconManager();
            GuiUtils.assignResources(this);
            disableEditForAllCommonFields();
        }
        
        @Override
        protected void commit() {
            //no-op.  RemoteFileItems are immutable in their property view
        }

        @Override
        public void showProperties(RemoteFileItem propertiable) {
            icon.setIcon(categoryIconManager.getIcon(propertiable.getCategory()));
            location.setLayout(new MigLayout("", "[]", "[]"));
            location.add(fileLocation, "gapbottom 5");
            populateCommonFields(propertiable);
            populateCopyToClipboard(propertiable);
            fileLocation.setText(propertiable.getSources().get(0).getFriendPresence().getPresenceId());
            showDialog(propertiable.getFileName(), propertiable.getCategory());
        }
    }
}
