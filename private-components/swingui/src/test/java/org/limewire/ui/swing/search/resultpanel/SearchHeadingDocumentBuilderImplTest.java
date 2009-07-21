package org.limewire.ui.swing.search.resultpanel;

import junit.framework.TestCase;

import org.limewire.ui.swing.search.model.BasicDownloadState;

public class SearchHeadingDocumentBuilderImplTest extends TestCase {
    private static final String NIGHT_LIFE = "Night life";
    private SearchHeadingDocumentBuilderImpl bldr;
    private SearchHeading heading;
    
    @Override
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
             "<span class=\"title\"><a href=\"#download\">Night life</a></span>";
        assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.NOT_STARTED, false));
    }
    
    public void testMouseOverHeadingDocument() {
        String expected = 
            "<span class=\"title\"><a href=\"#download\">Night life</a></span>";
        assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.NOT_STARTED, false));
    }
    
    public void testDownloadingHeadingDocument() {
        String expected = 
            "<a href=\"#downloading\">Downloading</a> <span class=\"title\">Night life</span>...";
        assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.DOWNLOADING, false));

    }
    
    public void testFileInLibraryHeadingDocument() {
        String expected = 
            "<span class=\"title\">Night life</span> is in <a href=\"#library\">Library</a>.";
        assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.LIBRARY, false));
        assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.DOWNLOADED, false));
    }

    public void testHandleSpam() {
        String expected = 
            "<span class=\"title\">Night life</span> is marked as spam.";
       assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.NOT_STARTED, true));
       assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.DOWNLOADING, true));
       assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.DOWNLOADED, true));
       assertEquals(expected, bldr.getHeadingDocument(heading, BasicDownloadState.LIBRARY, true));
    }
}
