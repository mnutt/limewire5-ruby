package org.limewire.ui.swing.library.table.menu;

import java.awt.Font;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.properties.DialogParam;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PropertiableHeadings;

import com.google.inject.Inject;

public class RemoteFileItemPropertiesFactory implements PropertiesFactory<RemoteFileItem> {
    private final CategoryIconManager categoryIconManager;
    private final PropertiableHeadings propertiableHeadings;
    private final MagnetLinkFactory magnetLinkFactory;
    private final DialogParam dialogParam;
    
    @Inject
    public RemoteFileItemPropertiesFactory(CategoryIconManager categoryIconManager,
            PropertiableHeadings propertiableHeadings, MagnetLinkFactory magnetLinkFactory, DialogParam dialogParam) {
        this.categoryIconManager = categoryIconManager;
        this.propertiableHeadings = propertiableHeadings;
        this.magnetLinkFactory = magnetLinkFactory;
        this.dialogParam = dialogParam;
    }

    @Override
    public Properties<RemoteFileItem> newProperties() {
        return new RemoteFileItemProperties(categoryIconManager, propertiableHeadings, magnetLinkFactory, dialogParam);
    }

    private static class RemoteFileItemProperties extends AbstractFileItemDialog implements Properties<RemoteFileItem> {
        private final CategoryIconManager categoryIconManager;
        private @Resource Font smallFont;
        private @Resource Font mediumFont;
        private @Resource Font largeFont;
        
        public RemoteFileItemProperties(CategoryIconManager categoryIconManager,
                PropertiableHeadings propertiableHeadings, MagnetLinkFactory magnetLinkFactory, DialogParam dialogParam) {
            super(propertiableHeadings, magnetLinkFactory, dialogParam);
            this.categoryIconManager = categoryIconManager;
            GuiUtils.assignResources(this);
            
            title.setEditable(false);
            genre.setEditable(false);
            rating.setEditable(false);
            year.setEditable(false);
            description.setEditable(false);
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
        protected void commit() {
            //no-op.  RemoteFileItems are immutable in their property view
        }

        @Override
        public void showProperties(RemoteFileItem propertiable) {
            icon.setIcon(categoryIconManager.getIcon(propertiable.getCategory()));
            location.setLayout(new MigLayout("", "[]", "[]"));
            location.add(fileLocation);
            populateCommonFields(propertiable);
            populateCopyToClipboard(propertiable);
            for (RemoteHost host : propertiable.getSources()) {
                fileLocation.setText(host.getFriendPresence().getFriend().getRenderName());
            }
            showDialog(propertiable.getFileName(), propertiable.getCategory());
        }
    }
}
