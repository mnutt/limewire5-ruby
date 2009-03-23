package org.limewire.ui.swing.properties;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.ui.swing.components.CollectionBackedComboBoxModel;
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
        setupComboBox(genre, propertiable.getPropertyString(FilePropertyKey.GENRE), getGenres(propertiable));
        unEditableGenre.setText(propertiable.getPropertyString(FilePropertyKey.GENRE));
        setupComboBox(rating, propertiable.getPropertyString(FilePropertyKey.RATING), getRatings(propertiable));
        setupComboBox(platform, propertiable.getPropertyString(FilePropertyKey.PLATFORM), getPlatforms(propertiable));
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
    
    private void setupComboBox(JComboBox comboBox, String current, List<String> possibles) {
        if(current == null) {
            current = "";
        }
        
        // If any are listed, current is non-empty, and possibles doesn't contain, add it in.
        if(!possibles.contains(current) && !current.equals("") && possibles.size() > 0) {
            possibles = new ArrayList<String>(possibles);            
            possibles.add(0, current);
            possibles = Collections.unmodifiableList(possibles);
        }
        
        ComboBoxModel model = new CollectionBackedComboBoxModel(possibles);
        comboBox.setModel(model);
        comboBox.setSelectedItem(current);
    }

    private List<String> getPlatforms(final PropertiableFile propertiableSeed) {
        switch (propertiableSeed.getCategory()) {
        case PROGRAM:
            return propertyDictionary.getApplicationPlatforms();
        default:
            return Collections.emptyList();
        }
    }

    private List<String> getRatings(final PropertiableFile propertiableSeed) {
        switch (propertiableSeed.getCategory()) {
        case VIDEO:
            return propertyDictionary.getVideoRatings();
        default:
            return Collections.emptyList();
        }
    }

    private List<String> getGenres(final PropertiableFile propertiableSeed) {
        switch (propertiableSeed.getCategory()) {
        case AUDIO:
            return propertyDictionary.getAudioGenres();
        case VIDEO:
            return propertyDictionary.getVideoGenres();
        default:
            return Collections.emptyList();
        }
    }
}
