
package com.limegroup.gnutella.metadata.audio.writer;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.vorbiscomment.VorbisCommentTag;

import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 *  Creates a valid OggVorbis comment tag for an undescribed
 *  OGG file is written to.
 */
public class OGGDataEditor extends AudioDataEditor {
    @Override
    protected Tag updateTag(Tag tag, AudioFile audioFile) throws FieldDataInvalidException {
        super.updateTag(tag, audioFile);
        //TODO: copyrights in Ogg files aren't working properlly. Need to fix up
        //  JAudioTagger a bit still then will readd this in
//        if( audioData.getLicense() == null ) 
//            tag.set(tag.createTagField(TagFieldKey.COPYRIGHT, ""));
//        else
//            tag.set(tag.createTagField(TagFieldKey.COPYRIGHT, audioData.getLicense()));
        return tag;
    }
    
    @Override
    protected Tag createTag(AudioFile audioFile) {
        if( audioFile.getTag() == null )
            return new VorbisCommentTag();
        return audioFile.getTag();
    }

    @Override
    protected boolean isValidFileType(String fileName) {
        return LimeXMLUtils.isOGGFile(fileName);
    }
}
	
	
