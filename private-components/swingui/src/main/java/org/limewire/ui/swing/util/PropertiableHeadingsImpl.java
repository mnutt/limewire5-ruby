package org.limewire.ui.swing.util;

import java.text.SimpleDateFormat;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class PropertiableHeadingsImpl implements PropertiableHeadings {
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");
    private final IconManager iconManager;
    
    @Inject
    public PropertiableHeadingsImpl(IconManager iconManager) {
        this.iconManager = iconManager;
    }
    
    @Override
    public String getHeading(PropertiableFile propertiable) {
        Object property = propertiable.getProperty(FilePropertyKey.NAME);
        String name = property == null ? "" : property.toString();
        String renderName = "";
        switch (propertiable.getCategory()) {
        case AUDIO:
            String artist = getPropertyString(propertiable, FilePropertyKey.AUTHOR);
            String title = getPropertyString(propertiable, FilePropertyKey.TITLE);
            if (!StringUtils.isEmpty(artist) && !StringUtils.isEmpty(title)) {
                renderName = artist + " - " + title;
            } else {
                renderName = name;
            }
            break;
        case VIDEO:
        case IMAGE:
        case DOCUMENT:
        case PROGRAM:
        case OTHER:
        default:
            renderName = name + "." + getFileExtension(propertiable);
        }
        return renderName.trim();
    }
    
    private String getPropertyString(PropertiableFile propertiable, FilePropertyKey key) {
        Object property = propertiable.getProperty(key);
        return property == null ? null : property.toString();
    }

    private String getFileExtension(PropertiableFile propertiableFile) {
        return FileUtils.getFileExtension(propertiableFile.getFileName());
    }

    @Override
    public String getSubHeading(PropertiableFile propertiable) {
        //TODO: Unit test this class (then refactor)!!! So many conditions :-(
        String subheading = "";

        switch (propertiable.getCategory()) {
        case AUDIO: {
            String albumTitle = getPropertyString(propertiable, FilePropertyKey.ALBUM);
            Long qualityScore = getQualityScoreLong(propertiable);
            Long length = getLengthLong(propertiable);

            boolean insertHyphen = false;
            if (!StringUtils.isEmpty(albumTitle)) {
                subheading += albumTitle;
                insertHyphen = true;
            }

            if (qualityScore != null) {
                if (insertHyphen) {
                    subheading += " - ";
                }
                subheading += GuiUtils.toQualityString(qualityScore);
                String bitRate = getPropertyString(propertiable, FilePropertyKey.BITRATE);
                if (bitRate != null) {
                    subheading += " (" + getPropertyString(propertiable, FilePropertyKey.BITRATE) + ")";
                }
                insertHyphen = true;
            }

            if (length != null) {
                subheading = addLength(subheading, length, insertHyphen);
            } else {
                Long fileSize = getFileSizeLong(propertiable);
                subheading = addFileSize(subheading, fileSize, insertHyphen);
            }
        }
            break;
        case VIDEO: {
            Long qualityScore = getQualityScoreLong(propertiable);
            Long length = getLengthLong(propertiable);
            Long fileSize = getFileSizeLong(propertiable);

            boolean insertHyphen = false;
            if (qualityScore != null) {
                subheading += GuiUtils.toQualityString(qualityScore);
                insertHyphen = true;
            }

            subheading = addLength(subheading, length, insertHyphen);
            
            subheading = addFileSize(subheading, fileSize, insertHyphen);
            
        }
            break;
        case IMAGE: {
            Long fileSize = getFileSizeLong(propertiable);
            
            boolean insertHyphen = false;
            Object time = propertiable.getProperty(FilePropertyKey.DATE_CREATED);
            if (time != null  && time instanceof Long) {
                subheading = DATE_FORMAT.format(new java.util.Date((Long) time));
                insertHyphen = true;
            }
            
            subheading = addFileSize(subheading, fileSize, insertHyphen);
        }
            break;
        case PROGRAM: {
            subheading = getFileSize(propertiable);
        }
            break;
        case DOCUMENT:
        case OTHER:
        default: {
             subheading = iconManager.getMIMEDescription(propertiable);
             subheading = subheading == null ? "" : subheading;
            // TODO add name of program used to open this file, not included in
            // 5.0
            Long fileSize = getFileSizeLong(propertiable);
            subheading = addFileSize(subheading, fileSize, !"".equals(subheading));
        }
        }
        return subheading == null ? "" : subheading;

    }

    private String addLength(String subheading, Long length, boolean insertHyphen) {
        if (length != null) {
            if (insertHyphen) {
                subheading += " - ";
            }
            subheading += CommonUtils.seconds2time(length);
        }
        return subheading;
    }
    
    @Override
    public String getLength(PropertiableFile propertiable) {
        Long length = getLengthLong(propertiable);
        return length != null ? CommonUtils.seconds2time(length) : null;
    }
    
    @Override
    public String getQualityScore(PropertiableFile propertiableFile) {
        Long qualityScore = getQualityScoreLong(propertiableFile);
        return qualityScore != null ? GuiUtils.toQualityString(qualityScore) : null;
    }

    private String addFileSize(String subheading, Long fileSize, boolean insertHyphen) {
        if (fileSize != null) {
            if (insertHyphen) {
                subheading += " - ";
            }
            subheading += GuiUtils.toUnitbytes(fileSize);
        }
        return subheading;
    }

    private Long getLengthLong(PropertiableFile propertiable) {
        return CommonUtils.parseLongNoException(getPropertyString(propertiable, FilePropertyKey.LENGTH));
    }

    private Long getQualityScoreLong(PropertiableFile propertiable) {
        return CommonUtils.parseLongNoException(getPropertyString(propertiable, FilePropertyKey.QUALITY));
    }

    private Long getFileSizeLong(PropertiableFile propertiable) {
        return CommonUtils.parseLongNoException(getPropertyString(propertiable, FilePropertyKey.FILE_SIZE));
    }
    
    @Override
    public String getFileSize(PropertiableFile propertiable) {
        Long fileSize = getFileSizeLong(propertiable);
        if (fileSize != null) {
            return GuiUtils.toUnitbytes(fileSize);
        }
        return "";
    }
}
