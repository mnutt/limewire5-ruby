package com.limegroup.gnutella.filters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 * This class is not thread safe, since there is currently no need for it.
 */
public class XMLDocFilter extends KeywordFilter {

    private Map<String, List<String>> disallowedFieldValues = new HashMap<String, List<String>>();
    
    private Map<String, List<String>> disallowedExactFieldValues = new HashMap<String, List<String>>();
    
    /**
     * Add disallowed substrings of field values.
     * 
     * @param field fully qualified field name, see {@link LimeXMLNames}.
     * @param values list of values that should not appear as substrings in the value of <code>field</code>
     */
    public void addDisallowedEntries(String field, List<String> values) {
        List<String> list = disallowedFieldValues.get(field);
        if (list == null) {
            list = new ArrayList<String>(values);
            disallowedFieldValues.put(field, list);
        }
        else {
            list.addAll(values);
        }
    }
    
    /**
    * Add disallowed field values.
    * 
    * @param field fully qualified field name, see {@link LimeXMLNames}.
    * @param values list of values that should not be the value of <code>field</code>
    */
    public void addDisallowedExactEntries(String field, List<String> values) {
        List<String> list = disallowedExactFieldValues.get(field);
        if (list == null) {
            list = new ArrayList<String>(values);
            disallowedExactFieldValues.put(field, list);
        }
        else {
            list.addAll(values);
        }
    }
    
    /**
     * Enable the filtering of xml field values that indicate adult content.
     * 
     * Also activates the keyword filtering of adult content in the super class
     * {@link KeywordFilter#disallowAdult()}.
     */
    @Override
    public void disallowAdult() {
        addDisallowedEntries(LimeXMLNames.VIDEO_TYPE, Arrays.asList("adult"));
        addDisallowedEntries(LimeXMLNames.VIDEO_RATING, Arrays.asList("adult"));
        addDisallowedExactEntries(LimeXMLNames.VIDEO_RATING, Arrays.asList("r", "nc-17"));
        super.disallowAdult();
    }
    
    @Override
    boolean allow(QueryReply qr) {
        if (!super.allow(qr)) {
            return false;
        }
        try {
            for (Response resp : qr.getResultsAsList()) {
                LimeXMLDocument doc = resp.getDocument();
                if (doc != null) {
                    if (!allowDoc(doc)) {
                        return false;
                    }
                }
            }
            return true; 
        }
        catch (BadPacketException bpe) {
            return false;
        }
    }
    
    /**
     * Returns true if none of the filters matched. 
     */
    protected boolean allowDoc(LimeXMLDocument doc) {
        for (Entry<String, List<String>> entry : disallowedFieldValues.entrySet()) {
            String values = doc.getValue(entry.getKey());
            if (values != null) {
                values = values.toLowerCase(Locale.US);
                for (String disallowed : entry.getValue()) {
                    if (values.contains(disallowed)) {
                        return false;
                    }
                }
            }
        }
        for (Entry<String, List<String>> entry : disallowedExactFieldValues.entrySet()) {
            String values = doc.getValue(entry.getKey());
            if (values != null) {
                values = values.toLowerCase(Locale.US);
                for (String disallowed : entry.getValue()) {
                    if (values.equals(disallowed)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
