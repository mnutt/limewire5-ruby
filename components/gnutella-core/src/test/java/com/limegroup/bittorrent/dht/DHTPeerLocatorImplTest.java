package com.limegroup.bittorrent.dht;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.mojito.EntityKey;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTFuture;
import org.limewire.mojito.concurrent.FixedDHTFuture;
import org.limewire.mojito.db.DHTValue;
import org.limewire.mojito.db.DHTValueEntity;
import org.limewire.mojito.db.impl.DHTValueImpl;
import org.limewire.mojito.result.FindValueResult;
import org.limewire.mojito.routing.Contact;
import org.limewire.mojito.routing.Version;
import org.limewire.util.BaseTestCase;

import com.google.inject.util.Providers;
import com.limegroup.bittorrent.ManagedTorrent;
import com.limegroup.bittorrent.TorrentLocation;
import com.limegroup.bittorrent.TorrentManager;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.dht.DHTManager;
import com.limegroup.gnutella.dht.util.KUIDUtils;

public class DHTPeerLocatorImplTest extends BaseTestCase {

    private String HASH1 = "urn:sha1:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    private String HASH2 = "urn:sha1:BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB";

    private byte[] IP;

    private byte[] ID;

    private int PORT = 4444;

    private TorrentLocation torLoc = null;

    private Mockery context;

    private ManagedTorrent managedTorrentOne;

    private ManagedTorrent managedTorrentTwo;

    private DHTManager dhtManager;

    private ApplicationServices applicationServices;

    private NetworkManager networkManager;

    private MojitoDHT dht;

    private Contact contact;

    private DHTPeerLocator dhtPeerLocator;

    private KUID kuidOne;

    private EntityKey eKeyOne;
    
    private KUID kuidTwo;

    private EntityKey eKeyTwo;

    private URN urnOne;

    private URN urnTwo;

    private TorrentManager torrentManager;

    public DHTPeerLocatorImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DHTPeerLocatorImplTest.class);
    }

    @Override
    public void setUp() throws Exception {
        IP = "124.0.0.1".getBytes("US-ASCII");
        ID = "123".getBytes("US-ASCII");
        
        context = new Mockery();
        managedTorrentOne = context.mock(ManagedTorrent.class);
        managedTorrentTwo = context.mock(ManagedTorrent.class);
        dhtManager = context.mock(DHTManager.class);
        applicationServices = context.mock(ApplicationServices.class);
        networkManager = context.mock(NetworkManager.class);
        dht = context.mock(MojitoDHT.class);
        torrentManager = context.mock(TorrentManager.class);

        contact = context.mock(Contact.class);

        try {
            urnOne = URN.createSHA1Urn(HASH1);
            urnTwo = URN.createSHA1Urn(HASH2);
        } catch (IOException ie) {
            fail(ie);
        }

        dhtPeerLocator = new DHTPeerLocatorImpl(Providers.of(dhtManager),
                Providers.of(torrentManager));

        kuidOne = KUIDUtils.toKUID(urnOne);
        eKeyOne = EntityKey.createEntityKey(kuidOne, DHTPeerLocatorUtils.BT_PEER_TRIPLE);
        kuidTwo = KUIDUtils.toKUID(urnTwo);
        eKeyTwo = EntityKey.createEntityKey(kuidTwo, DHTPeerLocatorUtils.BT_PEER_TRIPLE);
    }

    // Tests if locatePeer properly stores the torrents in waiting list if a DHT
    // was not available or did not support bootstrapping. It also test to
    // ensure duplicate torrents do not get stored in the waiting list.
    public void testLocatePeerWhichShouldPutTorrentsInWaitingList() {

        context.checking(new Expectations() {
            {                
                exactly(2).of(torrentManager).getTorrentForURN(with(equal(urnOne)));
                will(returnValue(managedTorrentOne));
                
                one(torrentManager).getTorrentForURN(with(equal(urnTwo)));
                will(returnValue(managedTorrentTwo));
                
                one(dhtManager).get(eKeyOne);
                will(returnValue(null));
                
                one(dhtManager).get(eKeyTwo);
                will(returnValue(null));
            }
        });

        // should store these torrents in the waiting list
        dhtPeerLocator.locatePeer(urnOne);
        dhtPeerLocator.locatePeer(urnTwo);
        // should not store this in waiting list as it should already be stored
        dhtPeerLocator.locatePeer(urnOne);

        context.assertIsSatisfied();
    }

    // Tests if locatePeer properly locates a peer in DHT.
    public void testLocatePeerWhichShouldLocateAPeerInDHT() {

        context.checking(new Expectations() {
            {
                one(networkManager).getAddress();
                will(returnValue(IP));
                one(networkManager).getPort();
                will(returnValue(PORT));
                one(applicationServices).getMyBTGUID();
                will(returnValue(ID));

                exactly(2).of(dht).getLocalNode();
                will(returnValue(contact));

                one(contact).getNodeID();
                will(returnValue(kuidOne));
            }
        });

        byte[] msg = null;

        try {
            torLoc = new TorrentLocation(InetAddress.getByName(new String((networkManager
                    .getAddress()))), networkManager.getPort(), applicationServices.getMyBTGUID());
            msg = DHTPeerLocatorUtils.encode(torLoc);
        } catch (UnknownHostException uhe) {
            fail(uhe);
        } catch (IllegalArgumentException iae) {
            fail(iae);
        }

        final DHTValue dhtValue = new DHTValueImpl(DHTPeerLocatorUtils.BT_PEER_TRIPLE,
                Version.ZERO, msg);

        DHTValueEntity entity = DHTValueEntity.createFromValue(dht, kuidOne, dhtValue);

        Collection<DHTValueEntity> entities = new ArrayList<DHTValueEntity>();
        entities.add(entity);

        Collection<EntityKey> entityKeys = new ArrayList<EntityKey>();
        entityKeys.add(eKeyOne);

        FindValueResult result = new FindValueResult(eKeyOne, null, entities, entityKeys, 123123L, 10);
        // a fake future with successful result to test if a successful search
        // works
        final DHTFuture<FindValueResult> future = new FixedDHTFuture<FindValueResult>(result);

        context.checking(new Expectations() {
            {
                exactly(2).of(torrentManager).getTorrentForURN(with(equal(urnOne)));
                will(returnValue(managedTorrentOne));                
                
                exactly(2).of(dhtManager).get(eKeyOne);
                will(returnValue(future));
                
                atLeast(1).of(managedTorrentOne).addEndpoint(with(equal(torLoc)));
            }
        });

        // Should perform search successfully
        dhtPeerLocator.locatePeer(urnOne);

        context.assertIsSatisfied();
    }
}
