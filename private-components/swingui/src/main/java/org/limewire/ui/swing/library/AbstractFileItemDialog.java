package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.ui.swing.action.BitziLookupAction;
import org.limewire.ui.swing.properties.AbstractPropertiableFileDialog;
import org.limewire.ui.swing.properties.DialogParam;
import org.limewire.ui.swing.util.I18n;

abstract class AbstractFileItemDialog extends AbstractPropertiableFileDialog {

    private final MagnetLinkFactory magnetLinkFactory;

    public AbstractFileItemDialog(DialogParam dialogParam) {
        super(dialogParam);
        this.magnetLinkFactory = dialogParam.getMagnetLinkFactory();
    }

    protected void populateCopyToClipboard(final FileItem propertiable) {
        copyToClipboard.setAction(new AbstractAction(I18n.tr("Copy Link")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new MagnetLinkCopier().copyLinkToClipBoard(propertiable, magnetLinkFactory);
            }
        });
        moreFileInfo.setAction(new BitziLookupAction(propertiable));
    }
}
