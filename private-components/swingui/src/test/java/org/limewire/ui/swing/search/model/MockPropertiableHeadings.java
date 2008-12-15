package org.limewire.ui.swing.search.model;

import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.util.PropertiableHeadings;

public class MockPropertiableHeadings implements PropertiableHeadings {
    public String heading;
    public String subheading;


    public int getHeadingCalledCount;
    @Override
    public String getHeading(PropertiableFile propertiable) {
        getHeadingCalledCount++;
        return heading;
    }

    public int getSubHeadingCalledCount;
    @Override
    public String getSubHeading(PropertiableFile propertiable) {
        getSubHeadingCalledCount++;
        return subheading;
    }
    
    @Override
    public String getFileSize(PropertiableFile propertiable) {
        return null;
    }

    @Override
    public String getLength(PropertiableFile propertiable) {
        return null;
    }

    @Override
    public String getQualityScore(PropertiableFile propertiableFile) {
        return null;
    }
}
