package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayConfig.*;
import junit.framework.TestCase;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.MockVisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRuleImpl;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayConfig;
import org.limewire.ui.swing.search.resultpanel.list.ListViewRowHeightRule.RowDisplayResult;

public class ListViewRowHeightRuleTest extends TestCase {

    private ListViewRowHeightRuleImpl rule;
    private MockVisualSearchResult mock;
    
    @Override
    protected void setUp() throws Exception {
        rule = new ListViewRowHeightRuleImpl();
        mock = new MockVisualSearchResult("Foo");
    }

    public void testHeadingOnly() {
        assertStuff(BasicDownloadState.DOWNLOADING, "bar", HeadingOnly, true, true);
        assertStuff(BasicDownloadState.DOWNLOADED, "bar", HeadingOnly, true, true);
        assertStuff(BasicDownloadState.LIBRARY, "bar", HeadingOnly, true, true);
    }
    
    public void testHeadingOnlyWhenSubheadingAndMetadataIsEmptyOrNull() {
        assertStuff(BasicDownloadState.DOWNLOADING, "", HeadingOnly, true, true);
        assertStuff(BasicDownloadState.DOWNLOADING, null, HeadingOnly, true, true);
        assertStuff(BasicDownloadState.DOWNLOADED, "", HeadingOnly, true, true);
        assertStuff(BasicDownloadState.DOWNLOADED, null, HeadingOnly, true, true);
        assertStuff(BasicDownloadState.LIBRARY, "", HeadingOnly, true, true);
        assertStuff(BasicDownloadState.LIBRARY, null, HeadingOnly, true, true);
        assertStuff(BasicDownloadState.NOT_STARTED, "", HeadingOnly, true, true);
        assertStuff(BasicDownloadState.NOT_STARTED, null, HeadingOnly, true, true);
    }
    
    public void testHeadingOnlyForSpam() {
        mock.setSpam(true);
        assertStuff(BasicDownloadState.NOT_STARTED, "bar", HeadingOnly, true, true);
    }
    
    public void testHeadingAndMetadataOnly() {
        mock.setHeading("bar");
        mock.getProperties().put(FilePropertyKey.NAME, "foo");
        assertStuff(BasicDownloadState.NOT_STARTED, "foo", HeadingAndMetadata, true, false);
    }

    private void assertStuff(BasicDownloadState state, String search, RowDisplayConfig config, boolean subHeadingEmpty, boolean metadataNull) {
        mock.setDownloadState(state);
        rule.initializeWithSearch(search);
        RowDisplayResult result = rule.getDisplayResult(mock);
        assertEquals(config, result.getConfig());
        assertEquals(subHeadingEmpty , result.getSubheading() == null || "".equals(result.getSubheading()));
        assertEquals(metadataNull, result.getMetadata() == null);
    }
    
    public void testHeadingAndSubHeading() {
        mock.setDownloadState(BasicDownloadState.NOT_STARTED);
        mock.setSubHeading("bar");
        assertStuff(BasicDownloadState.NOT_STARTED, "foo", HeadingAndSubheading, false, true);
        
        mock.setHeading("bar");
        mock.setSubHeading("foo");
        assertStuff(BasicDownloadState.NOT_STARTED, "foo", HeadingAndSubheading, false, true);
        assertStuff(BasicDownloadState.NOT_STARTED, "baz", HeadingAndSubheading, false, true);
    }
    
    public void testHeadingNotWrappedInHTMLButSubheadingIs() {
        mock.setDownloadState(BasicDownloadState.NOT_STARTED);
        mock.setSubHeading("bar");
        mock.setDownloadState(BasicDownloadState.NOT_STARTED);
        rule.initializeWithSearch("foo");
        RowDisplayResult result = rule.getDisplayResult(mock);
        //Only subheading is explicitly wrapped in HTML tags by the listviewrowheightrule.
        //The heading is wrapped in tags elsewhere later in the processing chain but subheading
        //is more simple and is wrapped here.
        assertEquals("<b>Foo</b>", result.getHeading());
        assertEquals("<html>bar</html>", result.getSubheading());
    }

    public void testHeadingSubHeadingAndMetadata() {
        mock.setHeading("bar");
        mock.setSubHeading("baz");
        mock.getProperties().put(FilePropertyKey.NAME, "foo");
        assertStuff(BasicDownloadState.NOT_STARTED, "foo", HeadingSubHeadingAndMetadata, false, false);
    }
}
