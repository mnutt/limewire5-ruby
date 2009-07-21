package org.limewire.core.impl.util;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class FilePropertyKeyPopulatorTest extends BaseTestCase {
    public FilePropertyKeyPopulatorTest(String name) {
        super(name);
    }

    public void testAudioFilePopulation() throws Exception {
        final String title = "Hello World";
        final String artist = "Me and you";
        final String album = "Testing the waters";
        final String genre = "Rock";
        final String comments = "woah!";
        final Long bitrate = new Long(128);
        final Long seconds = new Long(956);
        final Long year = new Long(1999);
        final String track = "5";
        
        final long fileSize = 1234;
        final int quality = 2;

        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
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


        assertEquals(artist, FilePropertyKeyPopulator.get(Category.AUDIO, FilePropertyKey.AUTHOR, document));
        assertEquals(title, FilePropertyKeyPopulator.get(Category.AUDIO, FilePropertyKey.TITLE, document));
        assertEquals(album, FilePropertyKeyPopulator.get(Category.AUDIO, FilePropertyKey.ALBUM, document));
        assertEquals(genre, FilePropertyKeyPopulator.get(Category.AUDIO, FilePropertyKey.GENRE, document));
        assertEquals(track, FilePropertyKeyPopulator.get(Category.AUDIO, FilePropertyKey.TRACK_NUMBER, document));
        assertEquals(year, FilePropertyKeyPopulator.get(Category.AUDIO, FilePropertyKey.YEAR, document));
        assertEquals(seconds, FilePropertyKeyPopulator.get(Category.AUDIO, FilePropertyKey.LENGTH, document));
        assertEquals(bitrate, FilePropertyKeyPopulator.get(Category.AUDIO, FilePropertyKey.BITRATE, document));
        assertEquals(comments, FilePropertyKeyPopulator.get(Category.AUDIO, FilePropertyKey.DESCRIPTION, document));
        assertEquals(quality, FilePropertyKeyPopulator.calculateQuality(Category.AUDIO, "mp3", fileSize, document));

        context.assertIsSatisfied();
    }

    public void testVideoFilePopulation() throws Exception {
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
        
        final long fileSize = 1234;
        final int quality = 2;

        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
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

        assertEquals(rating, FilePropertyKeyPopulator.get(Category.VIDEO, FilePropertyKey.RATING, document));
        assertEquals(producer, FilePropertyKeyPopulator.get(Category.VIDEO, FilePropertyKey.AUTHOR, document));
        assertEquals(title, FilePropertyKeyPopulator.get(Category.VIDEO, FilePropertyKey.TITLE, document));
        assertEquals(company, FilePropertyKeyPopulator.get(Category.VIDEO, FilePropertyKey.COMPANY, document));
        assertEquals(genre, FilePropertyKeyPopulator.get(Category.VIDEO, FilePropertyKey.GENRE, document));
        assertEquals(height, FilePropertyKeyPopulator.get(Category.VIDEO, FilePropertyKey.HEIGHT, document));
        assertEquals(width, FilePropertyKeyPopulator.get(Category.VIDEO, FilePropertyKey.WIDTH, document));
        assertEquals(year, FilePropertyKeyPopulator.get(Category.VIDEO, FilePropertyKey.YEAR, document));
        assertEquals(seconds, FilePropertyKeyPopulator.get(Category.VIDEO, FilePropertyKey.LENGTH, document));
        assertEquals(bitrate, FilePropertyKeyPopulator.get(Category.VIDEO, FilePropertyKey.BITRATE, document));
        assertEquals(comments, FilePropertyKeyPopulator.get(Category.VIDEO, FilePropertyKey.DESCRIPTION, document));
        assertEquals(quality, FilePropertyKeyPopulator.calculateQuality(Category.VIDEO, "mpg", fileSize, document));

        context.assertIsSatisfied();
    }

    public void testImageFilePopulation() throws Exception {
        final String author = "Hello World";
        final String title = "Rock";
        final String comments = "woah!";

        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
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

        assertEquals(author, FilePropertyKeyPopulator.get(Category.IMAGE, FilePropertyKey.AUTHOR, document));
        assertEquals(title, FilePropertyKeyPopulator.get(Category.IMAGE, FilePropertyKey.TITLE, document));
        assertEquals(comments, FilePropertyKeyPopulator.get(Category.IMAGE, FilePropertyKey.DESCRIPTION, document));

        context.assertIsSatisfied();
    }

    public void testDocumentFilePopulation() throws Exception {
        final String author = "Hello World";
        final String title = "Rock";
        final String comments = "woah!";

        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
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

        assertEquals(author, FilePropertyKeyPopulator.get(Category.DOCUMENT, FilePropertyKey.AUTHOR, document));
        assertEquals(title, FilePropertyKeyPopulator.get(Category.DOCUMENT, FilePropertyKey.TITLE, document));
        assertEquals(comments, FilePropertyKeyPopulator.get(Category.DOCUMENT, FilePropertyKey.DESCRIPTION, document));

        context.assertIsSatisfied();
    }

    public void testProgramFilePopulation() throws Exception {
        final String company = "Hello World";
        final String title = "Rock";
        final String platform = "woah!";

        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
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
        
        assertEquals(company, FilePropertyKeyPopulator.get(Category.PROGRAM, FilePropertyKey.COMPANY, document));
        assertEquals(title, FilePropertyKeyPopulator.get(Category.PROGRAM, FilePropertyKey.TITLE, document));
        assertEquals(platform, FilePropertyKeyPopulator.get(Category.PROGRAM, FilePropertyKey.PLATFORM, document));

        context.assertIsSatisfied();
    }

    public void testGetLimeXmlSchemaUri() throws Exception {
        assertEquals(LimeXMLNames.AUDIO_SCHEMA, FilePropertyKeyPopulator.getLimeXmlSchemaUri(Category.AUDIO));
        assertEquals(LimeXMLNames.IMAGE_SCHEMA, FilePropertyKeyPopulator.getLimeXmlSchemaUri(Category.IMAGE));
        assertEquals(LimeXMLNames.DOCUMENT_SCHEMA, FilePropertyKeyPopulator.getLimeXmlSchemaUri(Category.DOCUMENT));
        assertEquals(LimeXMLNames.APPLICATION_SCHEMA, FilePropertyKeyPopulator.getLimeXmlSchemaUri(Category.PROGRAM));
        assertEquals(LimeXMLNames.VIDEO_SCHEMA, FilePropertyKeyPopulator.getLimeXmlSchemaUri(Category.VIDEO));

        try {
            FilePropertyKeyPopulator.getLimeXmlSchemaUri(Category.OTHER);
            fail("Should have thrown and unsupported Operation exception.");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
