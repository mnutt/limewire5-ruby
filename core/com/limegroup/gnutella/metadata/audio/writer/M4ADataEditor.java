package com.limegroup.gnutella.metadata.audio.writer;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.mp4.Mp4Tag;

import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * Returns the Mp4 tag if it exists, or creates a new mp4 tag if
 * it doesn't already exist in the file. 
 */
public class M4ADataEditor extends AudioDataEditor {
    @Override
    protected Tag createTag(AudioFile audioFile) {
        if( audioFile.getTag() == null )
            return new Mp4Tag();
        return audioFile.getTag();
    }

    @Override
    protected boolean isValidFileType(String fileName) {
        return LimeXMLUtils.isM4AFile(fileName);
    }
}
