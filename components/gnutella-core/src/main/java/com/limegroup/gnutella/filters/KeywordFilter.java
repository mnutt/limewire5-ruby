package com.limegroup.gnutella.filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

/** 
 * A spam filter that removes certain "bad" keywords. 
 * If <i>any</i> words in a query are in the banned set, the
 * query is disallowed.
 */
public class KeywordFilter implements SpamFilter {
    /** INVARIANT: strings in ban contain only lowercase */
    private List<String> ban=new ArrayList<String>();
    
    /** 
     * @modifies this
     * @effects bans the given phrase.  Capitalization does not matter.
     */
    public void disallow(String phrase) { 
        String canonical = phrase.toLowerCase(Locale.US);
        if (!ban.contains(canonical))
            ban.add(canonical);
    }

    /** 
     * @modifies this
     * @effects bans several well-known "adult" words.
     */
    public void disallowAdult() {
        disallow("adult");
        disallow("anal");
        disallow("anul");
        disallow("ass");
        disallow("boob");
        disallow("blow");
        disallow("bondage");
        disallow("centerfold");
        disallow("cock");
        disallow("cum");
        disallow("cumshot");
        disallow("cunt");
        disallow("dick");
        disallow("dicks");
        disallow("dildo");
        disallow("facial");
        disallow("fuck");
        disallow("gangbang");
        disallow("hentai");
        disallow("horny");
        disallow("incest");
        disallow("jenna");
        disallow("masturbat");
        disallow("milf");
        disallow("nipple");
        disallow("orgasm");
        disallow("pedo");
        disallow("penis");
        disallow("playboy");
        disallow("porn");
        disallow("porno");
        disallow("pussy");
        disallow("rape");
        disallow("sex");
        disallow("slut");
        disallow("squirt");
        disallow("stripper");
        disallow("suck");
        disallow("tits");
        disallow("tittie");
        disallow("titty");
        disallow("twat");
        disallow("underage");
        disallow("vagina");
        disallow("whore");
        disallow("xxx");
    }

    /**
     * Returns list of banned keywords.
     * <p>
     * Package private for testing.
     */
    List<String> getBannedKeywords() {
        return ban;
    }
    
    public boolean allow(Message m) {
        boolean ok=true;
        if (m instanceof QueryRequest) 
            return allow((QueryRequest)m);
        else if (m instanceof QueryReply)
            return allow((QueryReply)m);
        else
            return ok;
    }

    protected boolean allow(QueryRequest qr) {
        //return false iff any of the words in query are in ban
        String query=qr.getQuery();
        return !matches(query);
    }

    boolean allow(QueryReply qr) {
        //if any of the file names in qr contain bad words, the whole
        //thing is disallowed
        try {
            for(Response response : qr.getResultsAsList()) {
                if (matches(response.getName()))
                    return false;
            }
        } catch (BadPacketException e) {
            return false;
        }
        return true;
    }

    /** 
     * Returns true if phrase matches any of the entries in ban.
     */
    protected boolean matches(String phrase) {
        String canonical=phrase.toLowerCase(Locale.US);
        for (int i=0; i<ban.size(); i++) {
            String badWord = ban.get(i);
            if (canonical.indexOf(badWord)!=-1)
                return true;
        }
        return false;
    }
}
