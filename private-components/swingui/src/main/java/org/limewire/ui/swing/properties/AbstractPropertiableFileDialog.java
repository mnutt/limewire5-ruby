package org.limewire.ui.swing.properties;


import javax.swing.DefaultComboBoxModel;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.util.PropertiableHeadings;

public abstract class AbstractPropertiableFileDialog extends Dialog {
    private final PropertiableHeadings propertiableHeadings;
    
    public AbstractPropertiableFileDialog(DialogParam dialogParam) {
        super(dialogParam);
        this.propertiableHeadings = dialogParam.getPropertiableHeadings();
    }

    protected void populateCommonFields(final PropertiableFile propertiable) {
        heading.setText(propertiableHeadings.getHeading(propertiable));
        filename.setText(propertiable.getFileName());
        fileSize.setText(propertiableHeadings.getFileSize(propertiable));
        genre.setModel(new DefaultComboBoxModel(new Object[]{ propertiable.getProperty(FilePropertyKey.GENRE) }));
        rating.setModel(new DefaultComboBoxModel(new Object[]{ propertiable.getProperty(FilePropertyKey.RATING) }));
        platform.setModel(new DefaultComboBoxModel(new Object[]{ propertiable.getProperty(FilePropertyKey.PLATFORM) }));
        populateMetadata(propertiable);
        title.setText(str(propertiable.getProperty(FilePropertyKey.TITLE)));
        artist.setText(str(propertiable.getProperty(FilePropertyKey.AUTHOR)));
        author.setText(str(propertiable.getProperty(FilePropertyKey.AUTHOR)));
        company.setText(str(propertiable.getProperty(FilePropertyKey.COMPANY)));
        album.setText(str(propertiable.getProperty(FilePropertyKey.ALBUM)));
        year.setText(str(propertiable.getProperty(FilePropertyKey.YEAR)));
        track.setText(str(propertiable.getProperty(FilePropertyKey.TRACK_NUMBER)));
        description.setText(str(propertiable.getProperty(FilePropertyKey.COMMENTS)));
    }
}
