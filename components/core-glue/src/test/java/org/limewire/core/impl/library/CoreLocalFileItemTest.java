package org.limewire.core.impl.library;

import java.io.File;
import java.sql.Date;
import java.util.Collections;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.friend.api.FileMetaData;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.library.LocalFileDetailsFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class CoreLocalFileItemTest extends TestCase {
    private Mockery context;

    private CoreLocalFileItem coreLocalFileItem;

    private FileDesc fileDesc;

    private LocalFileDetailsFactory detailsFactory;

    private CreationTimeCache creationTimeCache;

    private LimeXMLDocument document;

    private File file;

    @Override
    protected void setUp() throws Exception {
        context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        fileDesc = context.mock(FileDesc.class);
        detailsFactory = context.mock(LocalFileDetailsFactory.class);
        creationTimeCache = context.mock(CreationTimeCache.class);
        document = context.mock(LimeXMLDocument.class);
        file = new File("test.txt");
        context.checking(new Expectations() {{
            allowing(fileDesc).getFile();
            will(returnValue(file));
        }});
        coreLocalFileItem = new CoreLocalFileItem(fileDesc, detailsFactory, creationTimeCache);
    }

    public void testGetCreationTime() {
        final long creationTime = 123;
        context.checking(new Expectations() {
            {
                one(fileDesc).getSHA1Urn();
                will(returnValue(URN.INVALID));
                one(creationTimeCache).getCreationTimeAsLong(URN.INVALID);
                will(returnValue(creationTime));
            }
        });
        assertEquals(creationTime, coreLocalFileItem.getCreationTime());
        context.assertIsSatisfied();
    }

    public void testGetFile() {
        assertEquals(file, coreLocalFileItem.getFile());
        context.assertIsSatisfied();
    }

    public void testGetLastModifiedTime() {
        final long lastModified = 3;
        context.checking(new Expectations() {
            {
                one(fileDesc).lastModified();
                will(returnValue(lastModified));
            }
        });
        assertEquals(lastModified, coreLocalFileItem.getLastModifiedTime());
        context.assertIsSatisfied();
    }

    public void testGetName() {
        context.checking(new Expectations() {
            {
                one(fileDesc).getFileName();
                will(returnValue("test.txt"));
            }
        });
        assertEquals("test", coreLocalFileItem.getName());
        context.assertIsSatisfied();
    }

    public void testGetSize() {
        final long size = 1234;
        context.checking(new Expectations() {
            {
                one(fileDesc).getFileSize();
                will(returnValue(size));
            }
        });
        assertEquals(size, coreLocalFileItem.getSize());
        context.assertIsSatisfied();
    }

    public void testGetNumHits() {
        final int numHits = 1234;
        context.checking(new Expectations() {
            {
                one(fileDesc).getHitCount();
                will(returnValue(numHits));
            }
        });
        assertEquals(numHits, coreLocalFileItem.getNumHits());
        context.assertIsSatisfied();
    }

    public void testGetNumUploads() {
        final int numUploads = 1234;
        context.checking(new Expectations() {
            {
                one(fileDesc).getCompletedUploads();
                will(returnValue(numUploads));
            }
        });
        assertEquals(numUploads, coreLocalFileItem.getNumUploads());
        context.assertIsSatisfied();
    }

    public void testGetNumUploadAttempts() {
        final int numUploads = 1234;
        context.checking(new Expectations() {
            {
                one(fileDesc).getAttemptedUploads();
                will(returnValue(numUploads));
            }
        });
        assertEquals(numUploads, coreLocalFileItem.getNumUploadAttempts());
        context.assertIsSatisfied();
    }

    public void testGetCategory() {
        assertEquals(Category.DOCUMENT, coreLocalFileItem.getCategory());
        context.assertIsSatisfied();
    }

    public void testGetProperty() throws Exception {
        final String author = "Hello World";
        final String title = "Rock";
        final String comments = "woah!";
        final URN urn1 = URN.createSHA1Urn("urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

        context.checking(new Expectations() {{
                allowing(fileDesc).getXMLDocument();
                will(returnValue(document));
                exactly(2).of(document).getValue(LimeXMLNames.DOCUMENT_AUTHOR);
                will(returnValue(author.toString()));
                exactly(2).of(document).getValue(LimeXMLNames.DOCUMENT_TITLE);
                will(returnValue(title.toString()));
                exactly(2).of(document).getValue(LimeXMLNames.DOCUMENT_TOPIC);
                will(returnValue(comments.toString()));
                exactly(2).of(fileDesc).getFileName();
                will(returnValue(file.getName()));
                exactly(2).of(fileDesc).getFileSize();
                will(returnValue(1234L));
                exactly(2).of(fileDesc).getSHA1Urn();
                will(returnValue(urn1));
                exactly(2).of(creationTimeCache).getCreationTimeAsLong(urn1);
                will(returnValue(5678L));
        }});

        assertEquals(author, coreLocalFileItem.getProperty(FilePropertyKey.AUTHOR));
        assertEquals(title, coreLocalFileItem.getProperty(FilePropertyKey.TITLE));
        assertEquals(comments, coreLocalFileItem.getProperty(FilePropertyKey.DESCRIPTION));

        assertEquals(author, coreLocalFileItem.getPropertyString(FilePropertyKey.AUTHOR));
        assertEquals(title, coreLocalFileItem.getPropertyString(FilePropertyKey.TITLE));
        assertEquals(comments, coreLocalFileItem.getPropertyString(FilePropertyKey.DESCRIPTION));
    }

    public void testToMetadata() throws Exception {
        final String urn1String = "urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        final URN urn1 = URN.createSHA1Urn(urn1String);
        final long fileSize = 1234L;
        final long creationTime = 5678L;
        final int fileIndex = 5;
        context.checking(new Expectations() {
            {
                allowing(fileDesc).getIndex();
                will(returnValue(fileIndex));
                allowing(fileDesc).getFileName();
                will(returnValue(file.getName()));
                allowing(fileDesc).getFileSize();
                will(returnValue(fileSize));
                allowing(fileDesc).getSHA1Urn();
                will(returnValue(urn1));
                allowing(creationTimeCache).getCreationTimeAsLong(urn1);
                will(returnValue(creationTime));
                allowing(fileDesc).getUrns();
                will(returnValue(Collections.singleton(urn1)));
            }
        });
        FileMetaData fileMetaData = coreLocalFileItem.toMetadata();
        assertEquals(fileSize, fileMetaData.getSize());
        assertEquals(fileIndex, fileMetaData.getIndex());
        assertEquals(new Date(creationTime), fileMetaData.getCreateTime());
        assertEquals(1, fileMetaData.getUrns().size());
        assertEquals(urn1String, fileMetaData.getUrns().iterator().next());

        context.assertIsSatisfied();

    }

    public void testGetFileName() {
        context.checking(new Expectations() {
            {
                one(fileDesc).getFileName();
                will(returnValue(file.getName()));
            }
        });
        assertEquals(file.getName(), coreLocalFileItem.getFileName());
        context.assertIsSatisfied();
    }

    public void testIsShareable() throws Exception {
        final URN urn = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ");
        context.checking(new Expectations() {{
            one(fileDesc).isStoreFile();
            will(returnValue(true));
            one(fileDesc).getSHA1Urn();
            will(returnValue(urn));
        }});
        assertFalse(coreLocalFileItem.isShareable());
        context.assertIsSatisfied();

        context.checking(new Expectations() {{
            one(fileDesc).isStoreFile();
            will(returnValue(false));
            one(fileDesc).getSHA1Urn();
            will(returnValue(urn));
        }});
        assertTrue(coreLocalFileItem.isShareable());
        context.assertIsSatisfied();
    }
    
    public void testIncompleteIsSharable() throws Exception {
        final URN urn = URN.createSHA1Urn("urn:sha1:GLIQY64M7FSXBSQEZY37FIM5QQSA2OUJ");
        final IncompleteFileDesc incompleteFileDesc = context.mock(IncompleteFileDesc.class);
        context.checking(new Expectations() {{
            one(incompleteFileDesc).getFile();
            will(returnValue(file));
        }});
        coreLocalFileItem = new CoreLocalFileItem(incompleteFileDesc, detailsFactory, creationTimeCache);
        context.assertIsSatisfied();
        
        context.checking(new Expectations() {{
            one(incompleteFileDesc).isStoreFile();
            will(returnValue(false));
            one(incompleteFileDesc).getSHA1Urn();
            will(returnValue(urn));
        }});        
        assertFalse(coreLocalFileItem.isShareable());
        context.assertIsSatisfied();
    }

    public void testGetUrn() throws Exception {
        final String urn1String = "urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        final URN urn1 = URN.createSHA1Urn(urn1String);
        context.checking(new Expectations() {
            {
                allowing(fileDesc).getSHA1Urn();
                will(returnValue(urn1));
            }
        });
        assertEquals(urn1, coreLocalFileItem.getUrn());
        context.assertIsSatisfied();
    }

    public void testIsIncomplete() {
        assertFalse(coreLocalFileItem.isIncomplete());

        final IncompleteFileDesc incompleteFileDesc = context.mock(IncompleteFileDesc.class);
        context.checking(new Expectations() {
            {
                one(incompleteFileDesc).getXMLDocument();
                will(returnValue(document));
                allowing(incompleteFileDesc).getFile();
                will(returnValue(file));
            }
        });
        coreLocalFileItem = new CoreLocalFileItem(incompleteFileDesc, detailsFactory,
                creationTimeCache);

        assertTrue(coreLocalFileItem.isIncomplete());
    }

    public void testGetFileDesc() {
        assertEquals(fileDesc, coreLocalFileItem.getFileDesc());
    }

}
