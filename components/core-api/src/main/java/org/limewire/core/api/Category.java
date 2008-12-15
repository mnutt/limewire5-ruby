package org.limewire.core.api;

import java.util.Arrays;
import java.util.List;

import org.limewire.i18n.I18nMarker;


public enum Category {

    AUDIO(I18nMarker.marktr("Audio")),
    VIDEO(I18nMarker.marktr("Video")),
    IMAGE(I18nMarker.marktr("Images")),
    DOCUMENT(I18nMarker.marktr("Documents")),
    PROGRAM(I18nMarker.marktr("Programs")),
    OTHER(I18nMarker.marktr("Other")),
    
    ;
    
    private final String name;
    
    Category(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    public static List<Category> getCategoriesInOrder() {
        return Arrays.asList(AUDIO, VIDEO, IMAGE, DOCUMENT, PROGRAM, OTHER);
    }
}