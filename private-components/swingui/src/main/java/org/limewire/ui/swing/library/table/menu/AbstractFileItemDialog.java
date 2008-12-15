package org.limewire.ui.swing.library.table.menu;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.ui.swing.properties.AbstractPropertiableFileDialog;
import org.limewire.ui.swing.properties.DialogParam;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PropertiableHeadings;

abstract class AbstractFileItemDialog extends AbstractPropertiableFileDialog {

    private final MagnetLinkFactory magnetLinkFactory;

    public AbstractFileItemDialog(PropertiableHeadings propertiableHeadings,
            MagnetLinkFactory magnetLinkFactory, DialogParam dialogParam) {
        super(propertiableHeadings, dialogParam);
        this.magnetLinkFactory = magnetLinkFactory;
    }

    protected void populateCopyToClipboard(final FileItem propertiable) {
        copyToClipboard.setAction(new AbstractAction(I18n.tr("Copy Link to Clipboard")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                new MagnetLinkCopier().copyLinkToClipBoard(propertiable, magnetLinkFactory);
            }
        });
    }
}
