package com.limegroup.gnutella.spam;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.Address;
import org.limewire.io.Connectable;
import org.limewire.io.NetworkInstanceUtils;
import org.limewire.util.FileUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.filters.IPFilter;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.util.QueryUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.XMLStringUtils;

/**
 * This class splits a RemoteFileDesc or a QueryRequest into tokens that will
 * be put into the RatingTable.
 * 
 * Currently, it extracts the following data to build a token:
 * 
 * <ul>
 * <li>keywords from the file name or query string</li>
 * <li>name/value pairs from the XML metadata (if any)</li>
 * <li>file urn (if any)</li>
 * <li>file size</li>
 * <li>address (but not port) of the sender</li>
 * </ul>
 * 
 * The vendor string is no longer used, since it's too easy for spammers
 * to forge.
 */
@Singleton
public class Tokenizer {
	private static final Log LOG = LogFactory.getLog(Tokenizer.class);

	/**
	 * The maximum length of a keyword in chars; keywords longer than this
     * will be truncated. We use chars rather than bytes to avoid corrupting
     * multi-byte chars when truncating
	 */
	private int MAX_KEYWORD_LENGTH = 8;

    private final Provider<IPFilter> ipFilter;
    private final NetworkInstanceUtils networkInstanceUtils;
    
	@Inject
	Tokenizer(Provider<IPFilter> ipFilter,
            NetworkInstanceUtils networkInstanceUtils) {
        this.ipFilter = ipFilter;
        this.networkInstanceUtils = networkInstanceUtils;
	}
    
    void setIPFilter(AddressToken t) {
        t.setIPFilter(ipFilter);
    }
	
	/**
	 * Extracts a set of tokens from a RemoteFileDesc
	 * 
	 * @param desc the RemoteFileDesc that should be tokenized
	 * @return a non-empty set of Tokens
	 */
	public Set<Token> getTokens(RemoteFileDesc desc) {
        Set<Token> set = new HashSet<Token>();
        tokenize(desc, set);
        return set;
    }
    
    /**
     * Extracts a set of tokens from an array of RemoteFileDescs - useful if
     * the user wants to mark multiple RFDs from a TableLine as spam (or not),
     * which should rate each token only once
     * 
     * @param descs the array of RemoteFileDescs that should be tokenized
     * @return a non-empty set of Tokens
     */
    public Set<Token> getTokens(RemoteFileDesc[] descs) {
        Set<Token> set = new HashSet<Token>();
        for(RemoteFileDesc desc : descs)
            tokenize(desc, set);
        return set;
    }

    /**
     * Extracts a set of tokens from a RemoteFileDesc
     * 
     * @param desc the RemoteFileDesc that should be tokenized
     * @param set the set to which the tokens should be added
     */
    private void tokenize(RemoteFileDesc desc, Set<Token> set) {
		if(LOG.isDebugEnabled())
			LOG.debug("Tokenizing " + desc);
        String name = desc.getFileName();
        getKeywordTokens(FileUtils.getFilenameNoExtension(name), set);
        String ext = FileUtils.getFileExtension(name);
        if(!ext.equals(""))
            set.add(new FileExtensionToken(ext));
        LimeXMLDocument xml = desc.getXMLDocument();
        if(xml != null)
		    getKeywordTokens(xml, set);
        URN urn = desc.getSHA1Urn();
		if(urn != null)
			set.add(new UrnToken(urn.toString()));
		set.add(new SizeToken(desc.getSize()));
        set.add(new ApproximateSizeToken(desc.getSize()));
        // Ignore private addresses such as 192.168.x.x
        Address address = desc.getAddress();
        if(address instanceof Connectable) {
            Connectable connectable = (Connectable)address;
            if(!networkInstanceUtils.isPrivateAddress(connectable.getInetAddress()))
                set.add(new AddressToken(connectable.getAddress(), ipFilter));
        }
    }

	/**
	 * Tokenizes a QueryRequest, including the search terms, XML metadata and
     * URN (if any) - we clear the spam ratings of search tokens and ignore
     * them for spam rating purposes for the rest of the session
	 * 
	 * @param qr the QueryRequest that should be tokenized
	 * @return a set of Tokens, may be empty
	 */
	public Set<Token> getTokens(QueryRequest qr) {
		if(LOG.isDebugEnabled())
			LOG.debug("Tokenizing " + qr);
		Set<Token> set = new HashSet<Token>();
        getKeywordTokens(qr.getQuery(), set);
        LimeXMLDocument xml = qr.getRichQuery();
        if(xml != null)
            getKeywordTokens(xml, set);
        Set<URN> urns = qr.getQueryUrns();
        for(URN urn : urns)
            set.add(new UrnToken(urn.toString()));
        return set;
	}

	/**
	 * Extracts KeywordTokens from an XML metadata document
	 * 
	 * @param doc the LimeXMLDocument that should be tokenized
	 * @param set the set to which the tokens should be added
	 */
	private void getKeywordTokens(LimeXMLDocument doc, Set<Token> set) {
        for(Map.Entry<String, String> entry : doc.getNameValueSet()) {
            String name = entry.getKey().toString();
            String value = entry.getValue().toString(); 
            getXMLKeywords(name, value, set);
		}
	}

	/**
	 * Extracts XMLKeywordTokens from the field name and value of an XML
	 * metadata item
	 * 
	 * @param name the field name as a String (eg audios_audio_bitrate)
	 * @param value the value as a String
	 * @param set the set to which the tokens should be added
	 */
	private void getXMLKeywords(String name, String value, Set<Token> set) {
        name = extractSimpleFieldName(name);
        name.toLowerCase(Locale.US);
        value.toLowerCase(Locale.US);
		for(String keyword : QueryUtils.extractKeywords(value, false)) {
            if(keyword.length() > MAX_KEYWORD_LENGTH)
                keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
            set.add(new XMLKeywordToken(name, keyword));
        }
	}

	/**
	 * Extracts the last part of the field name for a canonical field name
	 * (eg audios_audio_bitrate becomes bitrate)
	 * 
	 * @param name the canonical field name
	 * @return the last part of the canonical field name
	 */
	private String extractSimpleFieldName(String name) {
		int idx1 = name.lastIndexOf(XMLStringUtils.DELIMITER);
		int idx2 = name.lastIndexOf(XMLStringUtils.DELIMITER, idx1 - 1);
		return name.substring(idx2 + XMLStringUtils.DELIMITER.length(), idx1);
	}

	/**
	 * Splits a String into keyword tokens using QueryUtils.extractKeywords()
	 * 
	 * @param str the String to tokenize
	 * @param set the set to which the tokens should be added
	 */
	private void getKeywordTokens(String str, Set<Token> set) {
        str.toLowerCase(Locale.US);
        for(String keyword : QueryUtils.extractKeywords(str, false)) {
            if(keyword.length() > MAX_KEYWORD_LENGTH)
                keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
            set.add(new KeywordToken(keyword));
        }
	}
}