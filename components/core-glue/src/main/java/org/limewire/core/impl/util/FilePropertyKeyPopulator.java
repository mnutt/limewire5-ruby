package org.limewire.core.impl.util;

import java.util.Map;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.I18NConvert;

import com.limegroup.gnutella.CategoryConverter;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 * Given a lime xml document this class will populate the given map by
 * converting limexml values to the appropriate FilePropertyKey.
 */
public class FilePropertyKeyPopulator {
    public static void populateProperties(String fileName, long fileSize, long creationTime,
            Map<FilePropertyKey, Object> properties, LimeXMLDocument doc) {

        properties.put(FilePropertyKey.NAME, ""); // Make sure name defaults to empty.
        set(properties, FilePropertyKey.NAME, FileUtils.getFilenameNoExtension(fileName));
        set(properties, FilePropertyKey.DATE_CREATED, creationTime);
        set(properties, FilePropertyKey.FILE_SIZE, fileSize);

        String extension = FileUtils.getFileExtension(fileName);
        Category category = CategoryConverter.categoryForExtension(extension);

        if (doc != null) {
            for (FilePropertyKey filePropertyKey : FilePropertyKey.values()) {
                set(properties, doc, category, filePropertyKey);
            }

            Long bitrate, length;
            Integer quality;
            switch(category) {
            case AUDIO:
                bitrate = CommonUtils.parseLongNoException(doc.getValue(LimeXMLNames.AUDIO_BITRATE));
                length = CommonUtils.parseLongNoException(doc.getValue(LimeXMLNames.AUDIO_SECONDS));
                quality = toAudioQualityScore(extension, fileSize, bitrate, length);
                set(properties, FilePropertyKey.QUALITY, quality);
                break;
            case VIDEO:
                bitrate = CommonUtils.parseLongNoException(doc.getValue(LimeXMLNames.VIDEO_BITRATE));
                length = CommonUtils.parseLongNoException(doc.getValue(LimeXMLNames.VIDEO_LENGTH));
                Long height = CommonUtils.parseLongNoException(doc.getValue(LimeXMLNames.VIDEO_HEIGHT));
                Long width = CommonUtils.parseLongNoException(doc.getValue(LimeXMLNames.VIDEO_WIDTH));
                quality = toVideoQualityScore(extension, fileSize, bitrate, length, height, width);
                set(properties, FilePropertyKey.QUALITY, quality);
                break;
            }
        }
    }

    /**
     * Sets the given value for the supplied property key. Nothing is set if the
     * value is empty or null. If the supplied value is a String, it is passed
     * through the I18NConvert.compose method before being set.
     */
    public static void set(Map<FilePropertyKey, Object> map, FilePropertyKey property, Object value) {
        // Insert nothing if value is null|empty.
        if (value != null && !value.toString().isEmpty()) {
            if (value instanceof String) {
                if (FilePropertyKey.isLong(property)) {
                    value = CommonUtils.parseLongNoException((String)value);
                } else {
                    value = I18NConvert.instance().compose((String) value).intern();
                }
            }
            map.put(property, value);
        }
    }

    /**
     * Sets the correct value in the map, retrieving the value from the
     * {@link LimeXMLDocument}. The value retrieved from the document is based
     * on the {@link Category} and {@link FilePropertyKey}.
     */
    public static void set(Map<FilePropertyKey, Object> map, LimeXMLDocument doc, Category category, FilePropertyKey property) {
        String limeXmlName = getLimeXmlName(category, property);
        if (limeXmlName != null) {
            Object value = doc.getValue(limeXmlName);
            set(map, property, value);
        }
    }

    /**
     * TODO use a better analysis to map bit rates and file types to quality,
     * for now using the following articles as a guide for now.
     * 
     * http://www.extremetech.com/article2/0,2845,1560793,00.asp
     * 
     * http://www.cdburner.ca/digital-audio-formats-article/digital-audio-
     * comparison.htm
     * 
     * http://ipod.about.com/od/introductiontoitunes/a/sound_qual_test.htm
     * 
     * Returns 1 of 4 quality scores.
     * 
     * null - unscored 1 - poor 2 - good 3 - excellent
     */
    private static Integer toAudioQualityScore(String fileExtension, Long fileSize, Long bitrate,
            Long length) {
        Integer quality = null;
        if ("wav".equalsIgnoreCase(fileExtension) || "flac".equalsIgnoreCase(fileExtension)) {
            quality = 3;
        } else if (bitrate != null) {
            if ("mp3".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 96) {
                    quality = 1;
                } else if (bitrate < 192) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if ("wma".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 64) {
                    quality = 1;
                } else if (bitrate < 128) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if ("aac".equalsIgnoreCase(fileExtension)
                    || "m4a".equalsIgnoreCase(fileExtension)
                    || "m4b".equalsIgnoreCase(fileExtension)
                    || "m4p".equalsIgnoreCase(fileExtension)
                    || "m4v".equalsIgnoreCase(fileExtension)
                    || "mp4".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 64) {
                    quality = 1;
                } else if (bitrate < 128) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if ("ogg".equalsIgnoreCase(fileExtension)
                    || "ogv".equalsIgnoreCase(fileExtension)
                    || "oga".equalsIgnoreCase(fileExtension)
                    || "ogx".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 48) {
                    quality = 1;
                } else if (bitrate < 96) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if (length != null && length < 30) {
                quality = 1;
            } else if (fileSize != null) {
                if (fileSize < (1 * 1024 * 1024)) {
                    quality = 1;
                } else if (fileSize < (3 * 1024 * 1024)) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            }
        }
        return quality;
    }

    /**
     * TODO use a better analysis to map video attributes to quality for now
     * using the following articles as a guide for now.
     * 
     * Right now the scoring is somewhat arbitrary.
     * 
     * Returns 1 of 4 quality scores.
     * 
     * null - unscored 1 - poor 2 - good 3 - excellent
     */
    private static Integer toVideoQualityScore(String fileExtension, Long fileSize, Long bitrate,
            Long length, Long height, Long width) {
        Integer quality = null;

        if ("mpg".equalsIgnoreCase(fileExtension) && height != null && width != null) {
            if ((height * width) < (352 * 240)) {
                quality = 1;
            } else if ((height * width) < (352 * 480)) {
                quality = 2;
            } else {
                quality = 3;
            }
        } else if (length != null && length < 60) {
            quality = 1;
        } else if (fileSize != null) {
            if (fileSize < (5 * 1024 * 1024)) {
                quality = 1;
            } else if (fileSize < (100 * 1024 * 1024)) {
                quality = 2;
            } else {
                quality = 3;
            }
        }
        return quality;
    }

    /**
     * Returns the lime xml name that maps to the given category and
     * FilePropertyKey. If not mapping exists null is returned.
     */
    public static String getLimeXmlName(Category category, FilePropertyKey filePropertyKey) {
        switch(category) {
        case AUDIO:
            switch (filePropertyKey) {
            case ALBUM:
                return LimeXMLNames.AUDIO_ALBUM;
            case AUTHOR:
                return LimeXMLNames.AUDIO_ARTIST;
            case BITRATE:
                return LimeXMLNames.AUDIO_BITRATE;
            case DESCRIPTION:
                return LimeXMLNames.AUDIO_COMMENTS;
            case GENRE:
                return LimeXMLNames.AUDIO_GENRE;
            case LENGTH:
                return LimeXMLNames.AUDIO_SECONDS;
            case TRACK_NUMBER:
                return LimeXMLNames.AUDIO_TRACK;
            case YEAR:
                return LimeXMLNames.AUDIO_YEAR;
            case TITLE:
                return LimeXMLNames.AUDIO_TITLE;
            }
            break;
        case DOCUMENT:
            switch (filePropertyKey) {
            case AUTHOR:
                return LimeXMLNames.DOCUMENT_AUTHOR;
            case TITLE:
                return LimeXMLNames.DOCUMENT_TITLE;
            case DESCRIPTION:
                return LimeXMLNames.DOCUMENT_TOPIC;
            }
            break;
        case IMAGE:
            switch (filePropertyKey) {
            case AUTHOR:
                return LimeXMLNames.IMAGE_ARTIST;
            case TITLE:
                return LimeXMLNames.IMAGE_TITLE;
            case DESCRIPTION:
                return LimeXMLNames.IMAGE_DESCRIPTION;
            }
            break;
        case PROGRAM:
            switch (filePropertyKey) {
            case COMPANY:
                return LimeXMLNames.APPLICATION_PUBLISHER;
            case PLATFORM:
                return LimeXMLNames.APPLICATION_PLATFORM;
            case TITLE:
                return LimeXMLNames.APPLICATION_NAME;
            }
            break;
        case VIDEO:
            switch (filePropertyKey) {
            case AUTHOR:
                return LimeXMLNames.VIDEO_PRODUCER;
            case BITRATE:
                return LimeXMLNames.VIDEO_BITRATE;
            case DESCRIPTION:
                return LimeXMLNames.VIDEO_COMMENTS;
            case COMPANY:
                return LimeXMLNames.VIDEO_STUDIO;
            case GENRE:
                return LimeXMLNames.VIDEO_TYPE;
            case HEIGHT:
                return LimeXMLNames.VIDEO_HEIGHT;
            case WIDTH:
                return LimeXMLNames.VIDEO_WIDTH;
            case LENGTH:
                return LimeXMLNames.VIDEO_LENGTH;
            case YEAR:
                return LimeXMLNames.VIDEO_YEAR;
            case TITLE:
                return LimeXMLNames.VIDEO_TITLE;
            case RATING:
                return LimeXMLNames.VIDEO_RATING;
            }
            break;
        }
        return null;
    }
}
