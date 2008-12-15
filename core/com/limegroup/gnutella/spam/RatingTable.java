package com.limegroup.gnutella.spam;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.SearchSettings;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectionPoint;
import org.limewire.io.IOUtils;
import org.limewire.lifecycle.Service;
import org.limewire.util.CommonUtils;
import org.limewire.util.GenericsUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.messages.QueryRequest;
import com.limegroup.gnutella.spam.Token;

@Singleton
public class RatingTable implements Service {
	private static final Log LOG = LogFactory.getLog(RatingTable.class);

	/**
	 * Don't hold more than this many entries in memory or save more than
     * this many entries to disk. The size is a tradeoff - tokens should be
     * discarded when they become irrelevant, but not before.
	 */
	private static final int MAX_SIZE = 5000;
    
    /**
     * Initial size of the rating table - should not be too large as many
     * users probably don't use the spam filter.
     */
    private static final int INITIAL_SIZE = 100;
    
	/**
	 * A map containing a limited number of tokens. We use a map rather than
     * a set so that we can retrieve a stored token by using an equivalent
     * token as a key. This allows us to use a token without rating data to
     * retrieve an equivalent token that has rating data.
     * 
     * The size of the map is limited. Entries are discarded in
     * least-recently-used order when the map is full, on the assumption that
     * the least-recently-used token is the least important to keep.
	 */
	private final Map<Token, Token> tokenMap
        = new LinkedHashMap<Token, Token>(INITIAL_SIZE, 0.75f, true) {
            // This method will be called on every get(), put(), and putAll()
            protected boolean removeEldestEntry(Map.Entry<Token, Token> e) {
                if(size() > MAX_SIZE) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Discarding token " + e.getValue());
                    return true;
                }
                return false;
            }
        };
    
    /**
     * Tokens that the user has searched for during this session (could be
     * keywords, XML metadata, and maybe URNs in the future). They will not
     * contribute to the spam ratings of search results, because spammers
     * often echo the search terms.
     */
    private final HashSet<Token> searchTokens = new HashSet<Token>();
	
	private final Tokenizer tokenizer;
	
	@Inject
	RatingTable(Tokenizer tokenizer) {
	    this.tokenizer = tokenizer;
	}
	
	@Inject
    void register(org.limewire.lifecycle.ServiceRegistry registry) {
        registry.register(this);
    }
	
	public String getServiceName() {
	    return org.limewire.i18n.I18nMarker.marktr("Spam Management");
	}
	
	public void initialize() {
	}
	
	public synchronized void start() {
        load();
    }
	
    public synchronized void stop() {
        save();
    }
    
	/**
	 * Clears the filter data
	 */
	protected synchronized void clear() {
        LOG.debug("Clearing ratings");
		tokenMap.clear();
	}

	/**
	 * Returns the rating for a RemoteFileDesc
	 * 
	 * @param desc the RemoteFileDesc to rate
	 * @return the rating for the RemoteFileDesc
	 */
	protected synchronized float getRating(RemoteFileDesc desc) {
		float rating = getRating(lookup(tokenizer.getTokens(desc)));
		if(LOG.isDebugEnabled())
			LOG.debug(desc + " rated " + rating);
		return rating;
	}

	/**
	 * Returns the rating for a tokenized RemoteFileDesc
	 * 
	 * @param tokens a set of tokens taken from a RemoteFileDesc
	 * @return the rating for the RemoteFileDesc
	 */
    private float getRating(Set<Token> tokens) {
        float rating = 1;
        for(Token t : tokens)
            rating *= 1 - t.getRating();
        return 1 - rating;
    }

	/**
	 * Assigns the given rating to an array of RemoteFileDescs
	 * 
	 * @param descs an array of RemoteFileDescs to be rated
	 * @param rating a rating between 0 (not spam) and 1 (spam)
	 */
	protected synchronized void rate(RemoteFileDesc[] descs, float rating) {
		rateInternal(lookup(tokenizer.getTokens(descs)), rating);
	}

	/**
     * Clears the ratings of the tokens associated with a QueryRequest and
     * ignores them for the rest of the session
     * 
	 * @param qr the QueryRequest to clear
	 */
	protected synchronized void clear(QueryRequest qr) {
        Set<Token> tokens = lookup(tokenizer.getTokens(qr));
        for(Token t : tokens) {
            if(LOG.isDebugEnabled())
                LOG.debug("Clearing search token " + t);
            searchTokens.add(t); // Ignore the token for this session
            tokenMap.remove(t); // Clear the rating for future sessions
        }
	}

	/**
	 * Assigns the given rating to a set of tokens
	 * 
	 * @param tokens a set of tokens to be rated
	 * @param rating a rating between 0 (not spam) and 1 (spam)
	 */
	private void rateInternal(Set<Token> tokens, float rating) {
		for(Token t : tokens) {
            float before = t.getRating();
			t.updateRating(rating);
            float after = t.getRating();
            if(LOG.isDebugEnabled())
                LOG.debug(t + " was rated " + before + ", now rated " + after);
        }
	}

	/**
	 * Replaces each token with an equivalent previously stored token, or
     * stores the token and returns it if no equivalent exists. Tokens that
     * have been searched for during this session are removed.
	 * 
	 * @param tokens a set of tokens to be replaced
	 * @return a set of equivalent tokens, with search tokens removed
	 */
	private Set<Token> lookup(Set<Token> tokens) {
        Set<Token> newTokens = new HashSet<Token>();
		for(Token t : tokens) {
            if(!searchTokens.contains(t))
                newTokens.add(lookup(t));
            else if(LOG.isDebugEnabled())
                LOG.debug("Ignoring search token " + t);
        }
		return newTokens;
	}

	/**
	 * Returns an equivalent previously stored token if any such token exists,
     * otherwise stores the token and returns it
	 * 
	 * @param token the token to look up
	 * @return the same token or a previously stored equivalent
	 */
	private Token lookup(Token token) {
        Token stored = tokenMap.get(token);
        if(stored == null) {
            tokenMap.put(token, token);
            stored = token;
        }
        return stored;
	}

	/**
	 * Loads ratings from disk
	 */
	private void load() {
        tokenMap.clear();
		ObjectInputStream is = null;
		try {
			is = new ObjectInputStream(
                    new BufferedInputStream(
                            new FileInputStream(getSpamDat())));
            List<Token> list
                = GenericsUtils.scanForList(is.readObject(),
                    Token.class, GenericsUtils.ScanMode.REMOVE);
            for(Token t : list) {
                if(t instanceof AddressToken)
                    tokenizer.setIPFilter((AddressToken) t);
                tokenMap.put(t, t);
            }
            if(LOG.isDebugEnabled())
                LOG.debug("Loaded " + tokenMap.size() + " entries");
		} catch(Throwable t) {
		    LOG.debug("Error loading spam ratings: ", t);
		} finally {
            IOUtils.close(is);
		}
	}
    
    /**
     * Saves ratings to disk (called whenever the user marks a search result)
     */
	public void save() {
	    ArrayList<Token> list;
	    synchronized(this) {
	        list = new ArrayList<Token>(tokenMap.size());
	        // The iterator returns the least-recently-used entry first
	        for(Map.Entry<Token,Token> e : tokenMap.entrySet()) {
	            Token t = e.getKey();
	            // Don't save ratings that have default scores
	            if(t.getRating() > 0f)
	                list.add(t);
	        }
	    }
	    ObjectOutputStream oos = null;
	    try {
	        oos = new ObjectOutputStream(
	                new BufferedOutputStream(
	                        new FileOutputStream(getSpamDat())));
	        oos.writeObject(list);
	        oos.flush();
	        if(LOG.isDebugEnabled())
	            LOG.debug("Saved " + list.size() + " entries");
	    } catch (IOException iox) {
	        LOG.debug("Error saving spam ratings: ", iox);
	    } finally {
	        IOUtils.close(oos);
	    }
	}
    
    /**
     * @return the number of tokens in the rating table (for testing)
     */
    protected int size() {
        return tokenMap.size();
    }
    
    /**
     * @return the least-recently-used token in the table (for testing)
     */
    protected Token getLeastRecentlyUsed() {
        for(Map.Entry<Token,Token> e : tokenMap.entrySet())
            return e.getKey();
        return null; // Empty
    }
    
	private static File getSpamDat() {
	    return new File(CommonUtils.getUserSettingsDir(), "spam.dat");
	}
    
	/** Inspectable that returns a hash and rating of the tokens */
	@InspectionPoint("spam rating table token hashes")
	@SuppressWarnings("unused")
	private final Inspectable TOKEN_HASH = new Inspectable() {
	    public Object inspect() {
	        synchronized(RatingTable.this) {
	            Map<String, Object> ret = new HashMap<String, Object>();
	            ret.put("ver",1);
                final float spamThreshold = SearchSettings.FILTER_SPAM_RESULTS.getValue();
	            ByteArrayOutputStream baos = new ByteArrayOutputStream();
	            DataOutputStream daos = new DataOutputStream(baos);
	            try {
	                for (Token t : tokenMap.values()) {
	                    // 8 bytes per entry
	                    float rating = t.getRating();
	                    if (rating < spamThreshold)
	                        break;
	                    daos.writeFloat(rating);
	                    daos.writeInt(t.hashCode());
	                }
	                daos.flush();
	                daos.close();
	                ret.put("dump", baos.toByteArray());
	            } catch (IOException impossible) {
	                ret.put("error", impossible.toString());
	            }
	            return ret;
	        }
	    }
	};
}
