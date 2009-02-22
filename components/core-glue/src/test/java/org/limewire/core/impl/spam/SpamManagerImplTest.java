package org.limewire.core.impl.spam;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SpamServices;
import com.limegroup.gnutella.spam.SpamManager;

public class SpamManagerImplTest extends BaseTestCase {

    public SpamManagerImplTest(String name) {
        super(name);
    }

    public void testMarkAsSpam() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final RemoteFileDescAdapter remoteFileDescAdapter1 = context
                .mock(RemoteFileDescAdapter.class);
        final RemoteFileDescAdapter remoteFileDescAdapter2 = context
                .mock(RemoteFileDescAdapter.class);
        final RemoteFileDesc remoteFileDesc1 = context.mock(RemoteFileDesc.class);
        final RemoteFileDesc remoteFileDesc2 = context.mock(RemoteFileDesc.class);

        final SpamManager spamManager = context.mock(SpamManager.class);
        final SpamServices spamServices = context.mock(SpamServices.class);

        final List<SearchResult> searchResults = new ArrayList<SearchResult>();
        searchResults.add(remoteFileDescAdapter1);
        searchResults.add(remoteFileDescAdapter2);

        final List<RemoteFileDesc> remoteFileDescs = new ArrayList<RemoteFileDesc>();
        remoteFileDescs.add(remoteFileDesc1);
        remoteFileDescs.add(remoteFileDesc2);

        context.checking(new Expectations() {
            {
                one(spamManager).handleUserMarkedSpam(
                        with(new RemoteFileDescMatcher(remoteFileDescs)));
                allowing(remoteFileDescAdapter1).getRfd();
                will(returnValue(remoteFileDesc1));
                allowing(remoteFileDescAdapter2).getRfd();
                will(returnValue(remoteFileDesc2));
            }
        });

        SpamManagerImpl spamManagerImpl = new SpamManagerImpl(spamManager, spamServices);
        spamManagerImpl.handleUserMarkedSpam(searchResults);
        context.assertIsSatisfied();
    }

    public void testMarkAsGood() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final RemoteFileDescAdapter remoteFileDescAdapter1 = context
                .mock(RemoteFileDescAdapter.class);
        final RemoteFileDescAdapter remoteFileDescAdapter2 = context
                .mock(RemoteFileDescAdapter.class);
        final RemoteFileDesc remoteFileDesc1 = context.mock(RemoteFileDesc.class);
        final RemoteFileDesc remoteFileDesc2 = context.mock(RemoteFileDesc.class);

        final SpamManager spamManager = context.mock(SpamManager.class);
        final SpamServices spamServices = context.mock(SpamServices.class);

        final List<SearchResult> searchResults = new ArrayList<SearchResult>();
        searchResults.add(remoteFileDescAdapter1);
        searchResults.add(remoteFileDescAdapter2);

        final List<RemoteFileDesc> remoteFileDescs = new ArrayList<RemoteFileDesc>();
        remoteFileDescs.add(remoteFileDesc1);
        remoteFileDescs.add(remoteFileDesc2);

        context.checking(new Expectations() {
            {
                one(spamManager).handleUserMarkedGood(
                        with(new RemoteFileDescMatcher(remoteFileDescs)));
                allowing(remoteFileDescAdapter1).getRfd();
                will(returnValue(remoteFileDesc1));
                allowing(remoteFileDescAdapter2).getRfd();
                will(returnValue(remoteFileDesc2));
            }
        });

        SpamManagerImpl spamManagerImpl = new SpamManagerImpl(spamManager, spamServices);
        spamManagerImpl.handleUserMarkedGood(searchResults);
        context.assertIsSatisfied();
    }

    public void testClearFilterData() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final SpamManager spamManager = context.mock(SpamManager.class);
        final SpamServices spamServices = context.mock(SpamServices.class);
        context.checking(new Expectations() {
            {
                one(spamManager).clearFilterData();
            }
        });

        SpamManagerImpl spamManagerImpl = new SpamManagerImpl(spamManager, spamServices);
        spamManagerImpl.clearFilterData();
        context.assertIsSatisfied();
    }

    public void testReloadIPFilter() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final SpamManager spamManager = context.mock(SpamManager.class);
        final SpamServices spamServices = context.mock(SpamServices.class);
        context.checking(new Expectations() {
            {
                one(spamServices).reloadIPFilter();
            }
        });

        SpamManagerImpl spamManagerImpl = new SpamManagerImpl(spamManager, spamServices);
        spamManagerImpl.reloadIPFilter();
        context.assertIsSatisfied();
    }

    public void testAdjustSpamFilters() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final SpamManager spamManager = context.mock(SpamManager.class);
        final SpamServices spamServices = context.mock(SpamServices.class);
        context.checking(new Expectations() {
            {
                one(spamServices).adjustSpamFilters();
            }
        });

        SpamManagerImpl spamManagerImpl = new SpamManagerImpl(spamManager, spamServices);
        spamManagerImpl.adjustSpamFilters();
        context.assertIsSatisfied();
    }

    /**
     * Makes sure that the method called with the RemoteFileDesc array is
     * equivalent to the expectedRemoteFileDesc Array.
     */
    private final class RemoteFileDescMatcher extends BaseMatcher<RemoteFileDesc[]> {
        private final List<RemoteFileDesc> expectedRemoteFileDescs;

        private RemoteFileDescMatcher(List<RemoteFileDesc> remoteFileDescs) {
            this.expectedRemoteFileDescs = remoteFileDescs;
        }

        @Override
        public void describeTo(Description description) {

        }

        @Override
        public boolean matches(Object item) {
            if (!(item instanceof RemoteFileDesc[])) {
                return false;
            }

            RemoteFileDesc[] remoteFileDescArray = (RemoteFileDesc[]) item;

            if (remoteFileDescArray.length != expectedRemoteFileDescs.size()) {
                return false;
            }

            for (RemoteFileDesc remoteFileDesc : remoteFileDescArray) {
                if (!expectedRemoteFileDescs.contains(remoteFileDesc)) {
                    return false;
                }
            }
            return true;
        }
    }
}
