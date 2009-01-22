package org.limewire.ui.swing.properties;


import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import javax.swing.DefaultComboBoxModel;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.ui.swing.util.PropertiableHeadings;

public abstract class AbstractPropertiableFileDialog extends Dialog {
    private final PropertiableHeadings propertiableHeadings;
    private final PropertyDictionary propertyDictionary;
    
    public AbstractPropertiableFileDialog(DialogParam dialogParam) {
        super(dialogParam);
        this.propertiableHeadings = dialogParam.getPropertiableHeadings();
        this.propertyDictionary = dialogParam.getPropertyDictionary();
    }

    protected void populateCommonFields(final PropertiableFile propertiable) {
        heading.setText(propertiableHeadings.getHeading(propertiable));
        filename.setText(propertiable.getFileName());
        fileSize.setText(propertiableHeadings.getFileSize(propertiable));
        genre.setModel(new DefaultComboBoxModel(getGenres(propertiable)));
        unEditableGenre.setText(propertiable.getPropertyString(FilePropertyKey.GENRE));
        rating.setModel(new DefaultComboBoxModel(getRatings(propertiable)));
        platform.setModel(new DefaultComboBoxModel(getPlatforms(propertiable)));
        populateMetadata(propertiable);
        title.setText(str(propertiable.getProperty(FilePropertyKey.TITLE)));
        artist.setText(str(propertiable.getProperty(FilePropertyKey.AUTHOR)));
        author.setText(str(propertiable.getProperty(FilePropertyKey.AUTHOR)));
        company.setText(str(propertiable.getProperty(FilePropertyKey.COMPANY)));
        album.setText(str(propertiable.getProperty(FilePropertyKey.ALBUM)));
        year.setText(str(propertiable.getProperty(FilePropertyKey.YEAR)));
        track.setText(str(propertiable.getProperty(FilePropertyKey.TRACK_NUMBER)));
        description.setText(str(propertiable.getProperty(FilePropertyKey.DESCRIPTION)));
    }

    private Object[] getPlatforms(final PropertiableFile propertiableSeed) {
        return getSeededList(propertiableSeed.getProperty(FilePropertyKey.PLATFORM), propertyDictionary.getApplicationPlatforms());
    }

    private Object[] getSeededList(Object property, List<String> lwEnumeration) {
        HashSet<String> enumeration = new LinkedHashSet<String>();
        if (property != null) {
            //Add property first so it can appear as the selected item
            enumeration.add(property.toString());
        }
        enumeration.addAll(lwEnumeration);
        return enumeration.toArray();
    }
    
    private Object[] getRatings(final PropertiableFile propertiableSeed) {
        Category category = propertiableSeed.getCategory();
        if (category == Category.VIDEO) {
            return getSeededList(propertiableSeed.getProperty(FilePropertyKey.RATING), propertyDictionary.getVideoRatings());
        }
        return new Object[0];
    }

    private Object[] getGenres(final PropertiableFile propertiableSeed) {
        Object genreProperty = propertiableSeed.getProperty(FilePropertyKey.GENRE);
        switch(propertiableSeed.getCategory()) {
        case AUDIO:
            return getSeededList(genreProperty, propertyDictionary.getAudioGenres());
        case VIDEO:
            return getSeededList(genreProperty, propertyDictionary.getVideoGenres());
        }
        return new Object[0];
    }
}
