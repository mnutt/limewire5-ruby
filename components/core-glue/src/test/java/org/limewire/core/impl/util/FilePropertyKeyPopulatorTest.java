package org.limewire.core.impl.util;

import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;

import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class FilePropertyKeyPopulatorTest extends BaseTestCase {
    public FilePropertyKeyPopulatorTest(String name) {
        super(name);
    }

    public void testAudioFilePopulation() throws Exception {
        final String fileName = "testFileName.mp3";
        final Long fileSize = new Long(1234);
        final Long creationTime = new Long(5678);
        final String title = "Hello World";
        final String artist = "Me and you";
        final String album = "Testing the waters";
        final String genre = "Rock";
        final String comments = "woah!";
        final Long bitrate = new Long(128);
        final Long seconds = new Long(956);
        final Long year = new Long(1999);
        final Long track = new Long(5);
        final Long quality = new Long(2);

        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        final LimeXMLDocument document = context.mock(LimeXMLDocument.class);

        context.checking(new Expectations() {
            {
                allowing(document).getValue(LimeXMLNames.AUDIO_ALBUM);
                will(returnValue(album.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_ARTIST);
                will(returnValue(artist.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_BITRATE);
                will(returnValue(bitrate.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_COMMENTS);
                will(returnValue(comments.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_GENRE);
                will(returnValue(genre.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_TRACK);
                will(returnValue(track.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_YEAR);
                will(returnValue(year.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_TITLE);
                will(returnValue(title.toString()));
                allowing(document).getValue(LimeXMLNames.AUDIO_SECONDS);
                will(returnValue(seconds.toString()));
            }
        });

        Map<FilePropertyKey, Object> map = new HashMap<FilePropertyKey, Object>();
        FilePropertyKeyPopulator
                .populateProperties(fileName, fileSize, creationTime, map, document);

        assertEquals(artist, map.get(FilePropertyKey.AUTHOR));
        assertEquals(title, map.get(FilePropertyKey.TITLE));
        assertEquals(album, map.get(FilePropertyKey.ALBUM));
        assertEquals(genre, map.get(FilePropertyKey.GENRE));
        assertEquals(track, map.get(FilePropertyKey.TRACK_NUMBER));
        assertEquals(year, map.get(FilePropertyKey.YEAR));
        assertEquals(seconds, map.get(FilePropertyKey.LENGTH));
        assertEquals(bitrate, map.get(FilePropertyKey.BITRATE));
        assertEquals(comments, map.get(FilePropertyKey.DESCRIPTION));
        assertEquals(FileUtils.getFilenameNoExtension(fileName), map.get(FilePropertyKey.NAME));
        assertEquals(fileSize, map.get(FilePropertyKey.FILE_SIZE));
        assertEquals(creationTime, map.get(FilePropertyKey.DATE_CREATED));
        assertEquals(quality, map.get(FilePropertyKey.QUALITY));
        
        context.assertIsSatisfied();
    }
    
    public void testVideoFilePopulation() throws Exception {
        final String fileName = "testFileName.mpg";
        final Long fileSize = new Long(1234);
        final Long creationTime = new Long(5678);
        final String title = "Hello World";
        final String genre = "Rock";
        final String comments = "woah!";
        final String company = "company";
        final String producer = "producer";
        final String rating = "rating";
        final Long height = new Long(240);
        final Long width = new Long(352);
        final Long bitrate = new Long(128);
        final Long seconds = new Long(956);
        final Long year = new Long(1999);
        final Long quality = new Long(2);

        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        final LimeXMLDocument document = context.mock(LimeXMLDocument.class);

        context.checking(new Expectations() {
            {
                allowing(document).getValue(LimeXMLNames.VIDEO_PRODUCER);
                will(returnValue(producer.toString()));
                allowing(document).getValue(LimeXMLNames.VIDEO_STUDIO);
                will(returnValue(company.toString()));
                allowing(document).getValue(LimeXMLNames.VIDEO_RATING);
                will(returnValue(rating.toString()));
                allowing(document).getValue(LimeXMLNames.VIDEO_BITRATE);
                will(returnValue(bitrate.toString()));
                allowing(document).getValue(LimeXMLNames.VIDEO_COMMENTS);
                will(returnValue(comments.toString()));
                allowing(document).getValue(LimeXMLNames.VIDEO_TYPE);
                will(returnValue(genre.toString()));
                allowing(document).getValue(LimeXMLNames.VIDEO_HEIGHT);
                will(returnValue(height.toString()));
                allowing(document).getValue(LimeXMLNames.VIDEO_WIDTH);
                will(returnValue(width.toString()));
                allowing(document).getValue(LimeXMLNames.VIDEO_YEAR);
                will(returnValue(year.toString()));
                allowing(document).getValue(LimeXMLNames.VIDEO_TITLE);
                will(returnValue(title.toString()));
                allowing(document).getValue(LimeXMLNames.VIDEO_LENGTH);
                will(returnValue(seconds.toString()));
            }
        });

        Map<FilePropertyKey, Object> map = new HashMap<FilePropertyKey, Object>();
        FilePropertyKeyPopulator
                .populateProperties(fileName, fileSize, creationTime, map, document);

        assertEquals(rating, map.get(FilePropertyKey.RATING));
        assertEquals(producer, map.get(FilePropertyKey.AUTHOR));
        assertEquals(title, map.get(FilePropertyKey.TITLE));
        assertEquals(company, map.get(FilePropertyKey.COMPANY));
        assertEquals(genre, map.get(FilePropertyKey.GENRE));
        assertEquals(height, map.get(FilePropertyKey.HEIGHT));
        assertEquals(width, map.get(FilePropertyKey.WIDTH));
        assertEquals(year, map.get(FilePropertyKey.YEAR));
        assertEquals(seconds, map.get(FilePropertyKey.LENGTH));
        assertEquals(bitrate, map.get(FilePropertyKey.BITRATE));
        assertEquals(comments, map.get(FilePropertyKey.DESCRIPTION));
        assertEquals(FileUtils.getFilenameNoExtension(fileName), map.get(FilePropertyKey.NAME));
        assertEquals(fileSize, map.get(FilePropertyKey.FILE_SIZE));
        assertEquals(creationTime, map.get(FilePropertyKey.DATE_CREATED));
        assertEquals(quality, map.get(FilePropertyKey.QUALITY));
        
        context.assertIsSatisfied();
    }
    
    public void testImageFilePopulation() throws Exception {
        final String fileName = "testFileName.jpg";
        final Long fileSize = new Long(1234);
        final Long creationTime = new Long(5678);
        final String author = "Hello World";
        final String title = "Rock";
        final String comments = "woah!";

        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        final LimeXMLDocument document = context.mock(LimeXMLDocument.class);

        context.checking(new Expectations() {
            {
                allowing(document).getValue(LimeXMLNames.IMAGE_ARTIST);
                will(returnValue(author.toString()));
                allowing(document).getValue(LimeXMLNames.IMAGE_TITLE);
                will(returnValue(title.toString()));
                allowing(document).getValue(LimeXMLNames.IMAGE_DESCRIPTION);
                will(returnValue(comments.toString()));
            }
        });

        Map<FilePropertyKey, Object> map = new HashMap<FilePropertyKey, Object>();
        FilePropertyKeyPopulator
                .populateProperties(fileName, fileSize, creationTime, map, document);

        assertEquals(author, map.get(FilePropertyKey.AUTHOR));
        assertEquals(title, map.get(FilePropertyKey.TITLE));
        assertEquals(comments, map.get(FilePropertyKey.DESCRIPTION));
        assertEquals(FileUtils.getFilenameNoExtension(fileName), map.get(FilePropertyKey.NAME));
        assertEquals(fileSize, map.get(FilePropertyKey.FILE_SIZE));
        assertEquals(creationTime, map.get(FilePropertyKey.DATE_CREATED));
        
        context.assertIsSatisfied();
    }
    
    public void testDocumentFilePopulation() throws Exception {
        final String fileName = "testFileName.txt";
        final Long fileSize = new Long(1234);
        final Long creationTime = new Long(5678);
        final String author = "Hello World";
        final String title = "Rock";
        final String comments = "woah!";

        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        final LimeXMLDocument document = context.mock(LimeXMLDocument.class);

        context.checking(new Expectations() {
            {
                allowing(document).getValue(LimeXMLNames.DOCUMENT_AUTHOR);
                will(returnValue(author.toString()));
                allowing(document).getValue(LimeXMLNames.DOCUMENT_TITLE);
                will(returnValue(title.toString()));
                allowing(document).getValue(LimeXMLNames.DOCUMENT_TOPIC);
                will(returnValue(comments.toString()));
            }
        });

        Map<FilePropertyKey, Object> map = new HashMap<FilePropertyKey, Object>();
        FilePropertyKeyPopulator
                .populateProperties(fileName, fileSize, creationTime, map, document);

        assertEquals(author, map.get(FilePropertyKey.AUTHOR));
        assertEquals(title, map.get(FilePropertyKey.TITLE));
        assertEquals(comments, map.get(FilePropertyKey.DESCRIPTION));
        assertEquals(FileUtils.getFilenameNoExtension(fileName), map.get(FilePropertyKey.NAME));
        assertEquals(fileSize, map.get(FilePropertyKey.FILE_SIZE));
        assertEquals(creationTime, map.get(FilePropertyKey.DATE_CREATED));
        
        context.assertIsSatisfied();
    }
    
    public void testProgramFilePopulation() throws Exception {
        final String fileName = "testFileName.exe";
        final Long fileSize = new Long(1234);
        final Long creationTime = new Long(5678);
        final String company = "Hello World";
        final String title = "Rock";
        final String platform = "woah!";

        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        final LimeXMLDocument document = context.mock(LimeXMLDocument.class);

        context.checking(new Expectations() {
            {
                allowing(document).getValue(LimeXMLNames.APPLICATION_PUBLISHER);
                will(returnValue(company.toString()));
                allowing(document).getValue(LimeXMLNames.APPLICATION_NAME);
                will(returnValue(title.toString()));
                allowing(document).getValue(LimeXMLNames.APPLICATION_PLATFORM);
                will(returnValue(platform.toString()));
            }
        });

        Map<FilePropertyKey, Object> map = new HashMap<FilePropertyKey, Object>();
        FilePropertyKeyPopulator
                .populateProperties(fileName, fileSize, creationTime, map, document);

        assertEquals(company, map.get(FilePropertyKey.COMPANY));
        assertEquals(title, map.get(FilePropertyKey.TITLE));
        assertEquals(platform, map.get(FilePropertyKey.PLATFORM));
        assertEquals(FileUtils.getFilenameNoExtension(fileName), map.get(FilePropertyKey.NAME));
        assertEquals(fileSize, map.get(FilePropertyKey.FILE_SIZE));
        assertEquals(creationTime, map.get(FilePropertyKey.DATE_CREATED));
        
        context.assertIsSatisfied();
    }

}
