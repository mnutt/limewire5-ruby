
package com.limegroup.gnutella.metadata.audio.reader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v24Frames;

import com.limegroup.gnutella.metadata.audio.AudioMetaData;

/**
 *  Reads MetaData from MP3 files. This extends AudioDataReader which also
 *  handles this format. However, store files need to get checked and parsed
 *  correctly so we do that here. 
 */
public class MP3MetaData extends AudioDataReader {
	
	
    @Override
    protected void readTag(AudioMetaData audioData, AudioFile audioFile, Tag tag) {
        
        MP3File mp3File = ((MP3File)audioFile);
        mp3File.getID3v1Tag();
        
        AbstractID3v2Tag v2Tag = mp3File.getID3v2Tag();
        ID3v1Tag v1Tag = mp3File.getID3v1Tag();

        // check v2 tags first if they exist
        if( v2Tag != null )
            readV2Tag(audioData, v2Tag);

        // check v1 tags next
        if( v1Tag != null )
            readV1Tag(audioData, v1Tag);
    }
    
    /**
     * Reads v1 tags from the mp3. Only writes the field to the AudioData if
     * it has not been filled in by v2 tags
     */
    private void readV1Tag(AudioMetaData audioData, ID3v1Tag tag){
        if( audioData.getTitle() == null || audioData.getTitle().length() == 0)
            audioData.setTitle(tag.getFirstTitle());
        if( audioData.getArtist() == null || audioData.getArtist().length() == 0)
            audioData.setArtist(tag.getFirstArtist());
        if( audioData.getAlbum() == null || audioData.getAlbum().length() == 0)
            audioData.setAlbum(tag.getFirstAlbum());
        if( audioData.getYear() == null || audioData.getYear().length() == 0)
            audioData.setYear(tag.getFirstYear()); 
        if( audioData.getComment() == null || audioData.getComment().length() == 0)
            audioData.setComment(tag.getFirstComment());
        if( audioData.getGenre() == null || audioData.getGenre().length() == 0)
            audioData.setGenre(tag.getFirstGenre()); 
        if( audioData.getTrack() == null || audioData.getTrack().length() == 0) {
            try {
                audioData.setTrack(tag.getFirstTrack());
            }
            catch(UnsupportedOperationException e) {
                // id3v1.0 tags dont have tracks
            }
        }
    }
    
    /**
     * Reads v2 tags from the mp3. 
     */
    private void readV2Tag(AudioMetaData audioData, AbstractID3v2Tag tag){
        audioData.setTitle(tag.getFirstTitle());
        audioData.setArtist(tag.getFirstArtist());
        audioData.setAlbum(tag.getFirstAlbum());
        audioData.setYear(tag.getFirstYear()); 
        audioData.setComment(tag.getFirstComment());
        audioData.setGenre(parseGenre(tag.getFirstGenre()));
        audioData.setTrack(tag.getFirstTrack());
        audioData.setLicense(tag.getFirst(ID3v24Frames.FRAME_ID_COPYRIGHTINFO));
        
        Iterator iter = tag.iterator();
        while(iter.hasNext()) {
            if( audioData.getLicenseType() != null && audioData.getLicenseType().equals(MAGIC_KEY) )
                return;
            Object nextFrame = iter.next();

            if( !(nextFrame instanceof AbstractID3v2Frame)) 
                continue;

            AbstractID3v2Frame o = (AbstractID3v2Frame)nextFrame;           

            if( !(o.getId().equals("TIT2") || o.getId().equals("TALB") || o.getId().equals("TOAL") ||
                    o.getId().equals("TOPE") || o.getId().equals("TPE1") || o.getId().equals("TPE2")
                    || o.getId().equals("TPE3") || o.getId().equals("TPE4")) )
            
            if( o.getBody().getObject("Text") != null )
                checkLWS(audioData, o.getBody().getObject("Text").toString());
            else 
                isRawCheck(audioData, o.getRawContent());
        }
    }
       
    /**
     * Checks a raw content field for the magic String. This is always in UTF-8 encoding so use the 
     * content byte array instead. 
     */
    private void isRawCheck(AudioMetaData audioDatas, byte[] contentBytes) {
        try {
            String content = new String(contentBytes,"UTF-8"); 
            checkLWS(audioDatas, content);
        } catch (UnsupportedEncodingException e) {
        }
    }

    /**
     * If the song is not a LWS already, do a substring search to see if
     * it is a LWS song
     * @param content - ID3 tag to scan for a substring
     */
    private void checkLWS(AudioMetaData audioData, String content) { 
        if( audioData.getLicenseType() == null || !audioData.getLicenseType().equals(MAGIC_KEY))
            if( content.indexOf(MAGIC_KEY) != -1) { 
                audioData.setLicenseType(MAGIC_KEY);
            }
    }
    
    /**
     * Some genres in ID3v2 tags are displaying (XXX) numbers along side the genre.
     * If this exists it hides the number from the user
     */
    private String parseGenre(String genre){
        if( genre == null || genre.length() <= 0) 
            return genre;
        String cleanGenre = genre;
        if( genre.charAt(0) == '(') {
            int startIndex = 0;
            for(int i = 0; i < genre.length(); i++) {
                if( genre.charAt(i) == ')') {
                    startIndex = i + 1;
                }
            }            
            cleanGenre = genre.substring(startIndex);
        }
        return cleanGenre;
        
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "mp3" };
    }
}
