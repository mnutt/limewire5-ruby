package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.FileUtils;

import com.google.inject.Singleton;
import com.limegroup.gnutella.metadata.audio.reader.AudioDataReader;
import com.limegroup.gnutella.metadata.audio.reader.MP3MetaData;
import com.limegroup.gnutella.metadata.audio.reader.OGGMetaData;
import com.limegroup.gnutella.metadata.audio.reader.WMAMetaData;
import com.limegroup.gnutella.metadata.audio.writer.FlacDataEditor;
import com.limegroup.gnutella.metadata.audio.writer.M4ADataEditor;
import com.limegroup.gnutella.metadata.audio.writer.MP3DataEditor;
import com.limegroup.gnutella.metadata.audio.writer.OGGDataEditor;
import com.limegroup.gnutella.metadata.video.reader.MOVMetaData;
import com.limegroup.gnutella.metadata.video.reader.MPEGMetaData;
import com.limegroup.gnutella.metadata.video.reader.OGMMetaData;
import com.limegroup.gnutella.metadata.video.reader.RIFFMetaData;
import com.limegroup.gnutella.metadata.video.reader.WMVMetaData;
import com.limegroup.gnutella.xml.LimeXMLUtils;

/**
 * Implementation of MetaDataFactory. Returns the appropriate reader/writer for
 * the file type if one exists, null if one does not exist 
 */
@Singleton
public class MetaDataFactoryImpl implements MetaDataFactory {

    private static final Log LOG = LogFactory.getLog(MetaDataFactory.class);
    
    private final ConcurrentMap<String, MetaReader> readerByExtension = new ConcurrentHashMap<String, MetaReader>();
    
    public MetaDataFactoryImpl() {
        registerReader(new WMMetaReader());
        registerReader(new MP3MetaData());
        registerReader(new OGGMetaData());
        registerReader(new AudioDataReader());
        registerReader(new WMAMetaData());
        registerReader(new RIFFMetaData());
        registerReader(new OGMMetaData());
        registerReader(new WMVMetaData());
        registerReader(new MPEGMetaData());
        registerReader(new MOVMetaData());
    }
    
    /**
     * factory method which returns an instance of MetaDataEditor which
     * should be used with the specific file
     * @param name the name of the file to be annotated
     * @return the MetaDataEditor that will do the annotation.  null if the
     * lime xml repository should be used.
     */
    public MetaWriter getEditorForFile(String name) {
        if (LimeXMLUtils.isSupportedAudioFormat(name))
            return getAudioEditorForFile(name);
        //add video types here
        return null;
    }

    @Override
    public void registerReader(MetaReader reader) {
        for (String extension : reader.getSupportedExtensions()) {
            MetaReader existingReader = readerByExtension.put(extension, reader);
            if (existingReader != null) {
		        readerByExtension.put(extension, existingReader);
                throw new IllegalArgumentException("factory: " + existingReader.getClass() + " already resistered for: " + extension);
            }
        }
    }
    
    public MetaData parse(File file) throws IOException {
        try {
            MetaReader reader = getMetaReader(file);
            if (reader != null) {
                return reader.parse(file);
            }
        } catch (OutOfMemoryError e) {
            LOG.warn("Ran out of memory while parsing.",e);
            throw (IOException)new IOException().initCause(e);
        } catch (Exception e) {
            LOG.warn("Exception parsing file.",e);
            throw (IOException)new IOException().initCause(e);
        }
        return null;
    }
    
    /** Creates MetaData for the file, if possible. */  
    public MetaReader getMetaReader(File file) {
        String extension = FileUtils.getFileExtension(file);
        if (!extension.isEmpty()) {
            MetaReader reader = readerByExtension.get(extension.toLowerCase(Locale.US));
            return reader;
        }
        return null;
    }
    
    /**
     * Returns the audio editor for the file if an editor exists
     * for that file type. Returns null if no editor exists
     */
    private MetaWriter getAudioEditorForFile(String name) {
        if (LimeXMLUtils.isMP3File(name))
            return new MP3DataEditor();
        if (LimeXMLUtils.isOGGFile(name))
            return new OGGDataEditor();
        if (LimeXMLUtils.isM4AFile(name))
            return new M4ADataEditor();
        if (LimeXMLUtils.isFLACFile(name))
            return new FlacDataEditor();
        return null;
    }

}
