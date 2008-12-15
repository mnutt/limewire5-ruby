package com.limegroup.gnutella.metadata.audio.reader;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagFieldKey;

import com.limegroup.gnutella.metadata.audio.AudioMetaData;

public class OGGMetaData extends AudioDataReader {

    @Override
    protected void readTag(AudioMetaData audioData, AudioFile audioFile, Tag tag) {
        if(tag != null ) {
            super.readTag(audioData, audioFile, tag);
            audioData.setLicense(tag.getFirst(TagFieldKey.COPYRIGHT));
        }
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "ogg" };
    }
}
