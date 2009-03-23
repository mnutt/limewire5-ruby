package org.limewire.core.impl.magnet;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.browser.MagnetOptions;

public class MagnetLinkImplTest extends BaseTestCase {

    private Mockery context;

    private MagnetOptions magnetOptions;

    public MagnetLinkImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        magnetOptions = context.mock(MagnetOptions.class);
    }

    public void testIsDownloadable() {

        MagnetLinkImpl magnetLinkImpl = new MagnetLinkImpl(magnetOptions);

        context.checking(new Expectations() {
            {
                one(magnetOptions).isDownloadable();
                will(returnValue(true));
            }
        });

        assertTrue(magnetLinkImpl.isDownloadable());

        context.checking(new Expectations() {
            {
                one(magnetOptions).isDownloadable();
                will(returnValue(false));
            }
        });

        assertFalse(magnetLinkImpl.isDownloadable());
        context.assertIsSatisfied();
    }

    public void testIsKeywordTopicOnly() {

        MagnetLinkImpl magnetLinkImpl = new MagnetLinkImpl(magnetOptions);

        context.checking(new Expectations() {
            {
                one(magnetOptions).isKeywordTopicOnly();
                will(returnValue(true));
            }
        });

        assertTrue(magnetLinkImpl.isKeywordTopicOnly());

        context.checking(new Expectations() {
            {
                one(magnetOptions).isKeywordTopicOnly();
                will(returnValue(false));
            }
        });

        assertFalse(magnetLinkImpl.isKeywordTopicOnly());
        context.assertIsSatisfied();
    }

    public void testGetKeywordTopic() {

        MagnetLinkImpl magnetLinkImpl = new MagnetLinkImpl(magnetOptions);

        context.checking(new Expectations() {
            {
                one(magnetOptions).getQueryString();
                will(returnValue("123"));
            }
        });

        assertEquals("123", magnetLinkImpl.getQueryString());
        context.assertIsSatisfied();
    }
    
    public void testGetMagnetOptions() {

        MagnetLinkImpl magnetLinkImpl = new MagnetLinkImpl(magnetOptions);
        assertEquals(magnetOptions, magnetLinkImpl.getMagnetOptions());
        context.assertIsSatisfied();
    }
}
