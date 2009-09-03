package com.limegroup.gnutella.filters;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.FilterSettings;
import org.limewire.util.Visitor;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.spam.SpamManager;

/**
 * A filter that checks query responses, query replies and individual URNs
 * against a URN blacklist.
 */
@Singleton
class URNFilterImpl implements URNFilter {

    private static final Log LOG = LogFactory.getLog(URNFilterImpl.class);

    private final SpamManager spamManager;
    private final URNBlacklistManager urnBlacklistManager;
    private final ScheduledExecutorService backgroundExecutor;
    private ImmutableSet<String> blacklist = null;

    @Inject
    URNFilterImpl(SpamManager spamManager,
            URNBlacklistManager urnBlacklistManager,
            @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor) {
        this.spamManager = spamManager;
        this.urnBlacklistManager = urnBlacklistManager;
        this.backgroundExecutor = backgroundExecutor;
    }

    /**
     * Reloads the blacklist in a different thread and informs the callback,
     * unless the callback is null.
     */
    @Override
    public void refreshURNs(final LoadCallback callback) {
        LOG.debug("Refreshing URN filter");
        backgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final ImmutableSet.Builder<String> builder =
                    ImmutableSet.builder();
                // 1. Local setting
                for(String s : FilterSettings.FILTERED_URNS_LOCAL.get())
                    builder.add(s);
                // 2. Remote setting
                if(FilterSettings.USE_NETWORK_FILTER.getValue()) {
                    for(String s : FilterSettings.FILTERED_URNS_REMOTE.get())
                        builder.add(s);
                }
                // 3. File
                urnBlacklistManager.loadURNs(new Visitor<String>() {
                    @Override
                    public boolean visit(String s) {
                        builder.add(s);
                        return true;
                    }
                });
                blacklist = builder.build();
                if(LOG.isDebugEnabled())
                    LOG.debug("Filter contains " + blacklist.size() + " URNs");
                if(callback != null)
                    callback.spamFilterLoaded();
            }
        });
    }

    /**
     * Returns false if the message is a query reply with a URN that matches
     * the blacklist; matching query replies are passed to the spam filter.
     * Returns true for all other messages.
     */
    @Override
    public boolean allow(Message m) {
        if(blacklist == null)
            return true;
        if(m instanceof QueryReply) {
            QueryReply q = (QueryReply)m;
            if(isBlacklisted(q)) {
                if(FilterSettings.FILTERED_URNS_ARE_SPAM.getValue())
                    spamManager.handleSpamQueryReply(q);
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if any response in the query reply matches the blacklist.
     * Unlike <code>allow(Message)</code>, matching query replies are not
     * passed to the spam filter.
     */
    @Override
    public boolean isBlacklisted(QueryReply q) {
        if(blacklist == null)
            return false;
        try {
            for(Response r : q.getResultsArray()) {
                for(URN u : r.getUrns()) {
                    if(isBlacklisted(u))
                        return true;
                }
            }
            return false;
        } catch(BadPacketException bpe) {
            return true;
        }
    }

    /**
     * Returns true if the given URN matches the blacklist.
     */
    @Override
    public boolean isBlacklisted(URN urn) {
        if(blacklist == null)
            return false;
        if(blacklist.contains(urn.getNamespaceSpecificString())) {
            if(LOG.isDebugEnabled())
                LOG.debug(urn + " is spam");
            return true;
        }
        return false;
    }

    /**
     * Returns the blacklisted URNs as base32-encoded strings. For testing.
     */
    @Override
    public Set<String> getBlacklist() {
        return blacklist;
    }
}