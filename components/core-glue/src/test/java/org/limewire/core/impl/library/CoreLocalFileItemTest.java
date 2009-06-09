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
import org.limewire.core.api.friend.client.FileMetaData;
import org.limewire.core.impl.URNImpl;

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
        context.checking(new Expectations() {
            {
                one(fileDesc).getXMLDocument();
                will(returnValue(document));
                allowing(fileDesc).getFile();
                will(returnValue(file));
            }
        });
        coreLocalFileItem = new CoreLocalFileItem(fileDesc, detailsFactory, creationTimeCache);
    }

    public void testGetFriendShareCount() {
        final int friendShareCount = 4;
        context.checking(new Expectations() {
            {
                one(fileDesc).getShareListCount();
                will(returnValue(friendShareCount));
            }
        });
        assertEquals(friendShareCount, coreLocalFileItem.getFriendShareCount());
        context.assertIsSatisfied();
    }

    public void testIsSharedWithGnutella() {
        context.checking(new Expectations() {
            {
                one(fileDesc).isSharedWithGnutella();
                will(returnValue(true));
            }
        });
        assertTrue(coreLocalFileItem.isSharedWithGnutella());

        context.checking(new Expectations() {
            {
                one(fileDesc).isSharedWithGnutella();
                will(returnValue(false));
            }
        });
        assertFalse(coreLocalFileItem.isSharedWithGnutella());
        context.assertIsSatisfied();
    }

    public void testGetCreationTime() {
        final long creationTime = 123;
        context.checking(new Expectations() {
            {
                one(creationTimeCache).getCreationTimeAsLong(null);
                will(returnValue(creationTime));
                one(fileDesc).getSHA1Urn();
                will(returnValue(null));
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

        context.checking(new Expectations() {
            {
                allowing(document).getValue(LimeXMLNames.DOCUMENT_AUTHOR);
                will(returnValue(author.toString()));
                allowing(document).getValue(LimeXMLNames.DOCUMENT_TITLE);
                will(returnValue(title.toString()));
                allowing(document).getValue(LimeXMLNames.DOCUMENT_TOPIC);
                will(returnValue(comments.toString()));
                allowing(fileDesc).getFileName();
                will(returnValue(file.getName()));
                allowing(fileDesc).getFileSize();
                will(returnValue(1234L));
                allowing(fileDesc).getSHA1Urn();
                will(returnValue(urn1));
                allowing(creationTimeCache).getCreationTimeAsLong(urn1);
                will(returnValue(5678L));
            }
        });

        assertEquals(author, coreLocalFileItem.getProperty(FilePropertyKey.AUTHOR));
        assertEquals(title, coreLocalFileItem.getProperty(FilePropertyKey.TITLE));
        assertEquals(comments, coreLocalFileItem.getProperty(FilePropertyKey.DESCRIPTION));

        assertEquals(author, coreLocalFileItem.getPropertyString(FilePropertyKey.AUTHOR));
        assertEquals(title, coreLocalFileItem.getPropertyString(FilePropertyKey.TITLE));
        assertEquals(comments, coreLocalFileItem.getPropertyString(FilePropertyKey.DESCRIPTION));

        coreLocalFileItem.reloadProperties();

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

    public void testIsShareable() {
        context.checking(new Expectations() {
            {
                one(fileDesc).isStoreFile();
                will(returnValue(true));
            }
        });
        assertFalse(coreLocalFileItem.isShareable());

        context.checking(new Expectations() {
            {
                one(fileDesc).isStoreFile();
                will(returnValue(false));
            }
        });
        assertTrue(coreLocalFileItem.isShareable());

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
        context.checking(new Expectations() {
            {
                one(incompleteFileDesc).isStoreFile();
                will(returnValue(false));
            }
        });
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
        assertEquals(new URNImpl(urn1), coreLocalFileItem.getUrn());
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
