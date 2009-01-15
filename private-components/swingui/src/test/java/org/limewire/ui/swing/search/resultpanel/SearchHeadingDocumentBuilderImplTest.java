package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.search.resultpanel.SearchHeadingDocumentBuilderImpl.DOCUMENT_END;
import junit.framework.TestCase;

import org.limewire.ui.swing.search.model.BasicDownloadState;

public class SearchHeadingDocumentBuilderImplTest extends TestCase {
    private static final String NIGHT_LIFE = "Night life";
    private SearchHeadingDocumentBuilderImpl bldr;
    private SearchHeading heading;
    
    public void setUp() {
        bldr = new SearchHeadingDocumentBuilderImpl();
        heading = new SearchHeading() {
            @Override
            public String getText() {
                return NIGHT_LIFE;
            }

            @Override
            public String getText(String adjoiningFragment) {
                return NIGHT_LIFE;
            }
        };
    }

    public void testNonMouseOverHeadingDocument() {
        String expected = 
             bldr.documentStartHTML + "<span class=\"title\"><a href=\"#download\">Night life</a></span>" + DOCUMENT_END;
        assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.NOT_STARTED, false));
    }
    
    public void testMouseOverHeadingDocument() {
        String expected = 
            bldr.documentStartHTML + "<span class=\"title\"><a href=\"#download\">Night life</a></span>" + DOCUMENT_END;
        assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.NOT_STARTED, false));
    }
    
    public void testDownloadingHeadingDocument() {
        String expected = 
            bldr.documentStartHTML + "<a href=\"#downloading\">Downloading</a> <span class=\"title\">Night life</span>..." + DOCUMENT_END;
        assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.DOWNLOADING, false));

    }
    
    public void testFileInLibraryHeadingDocument() {
        String expected = 
            bldr.documentStartHTML + "<span class=\"title\">Night life</span> is in <a href=\"#library\">My Library</a>." + DOCUMENT_END;
        assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.LIBRARY, false));
        assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.DOWNLOADED, false));
    }

    public void testHandleSpam() {
        String expected = 
            bldr.documentStartHTML + "<span class=\"title\">Night life</span> is marked as spam." + DOCUMENT_END;
       assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.NOT_STARTED, true));
       assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.DOWNLOADING, true));
       assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.DOWNLOADED, true));
       assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.LIBRARY, true));
    }
}
