package com.limegroup.gnutella;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.limewire.util.MediaType;

import com.limegroup.gnutella.messages.QueryRequest;

public class MediaTypeAggregator {

    /** Utility class for aggregating MediaTypes.
     *  This class is not synchronized - it should never be used in a fashion
     *  where synchronization is necessary.  If that changes, add synch.
     */
    public static class Aggregator {
        /** A list of MediaType objects. */
        private List<MediaType> _filters = new LinkedList<MediaType>();
    
        private Aggregator() {}
        /** I don't check for duplicates. */
        public void addFilter(MediaType filter) {
            _filters.add(filter);
        }
    
        /**
         * @return an immutable list of mediatypes of aggregator.
         */
        public List<MediaType> getMediaTypes() {
        	return Collections.unmodifiableList(_filters);
        }        
    
        /** @return true if the Response falls within one of the MediaTypes
         *  this aggregates.
         */
        public boolean allow(final String fName) {
            for(MediaType mt : _filters) {
                if(mt.matches(fName))
                    return true;
            }
            return false;
        }
    }

    /** @return a MediaType.Aggregator to use for your query.  Null is a
     *  possible return value.
     */
    public static MediaTypeAggregator.Aggregator getAggregator(QueryRequest query) {
        if (query.desiresAll())
            return null;
    
        MediaTypeAggregator.Aggregator retAggr = new MediaTypeAggregator.Aggregator();
        if (query.desiresLinuxOSXPrograms())
            retAggr.addFilter(MediaType.getOsxAndLinuxProgramMediaType());
        if (query.desiresWindowsPrograms())
            retAggr.addFilter(MediaType.getWindowsProgramMediaType());
        if (query.desiresDocuments())
            retAggr.addFilter(MediaType.getDocumentMediaType());
        if (query.desiresAudio())
            retAggr.addFilter(MediaType.getAudioMediaType());
        if (query.desiresVideo())
            retAggr.addFilter(MediaType.getVideoMediaType());
        if (query.desiresImages())
            retAggr.addFilter(MediaType.getImageMediaType());
        return retAggr;
    }

}
