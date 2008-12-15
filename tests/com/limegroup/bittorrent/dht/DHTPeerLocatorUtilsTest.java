package com.limegroup.bittorrent.dht;

import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.Test;

import org.limewire.io.BadGGEPBlockException;
import org.limewire.io.BadGGEPPropertyException;
import org.limewire.io.GGEP;
import org.limewire.io.InvalidDataException;
import org.limewire.util.BaseTestCase;

import com.limegroup.bittorrent.TorrentLocation;

public class DHTPeerLocatorUtilsTest extends BaseTestCase {

    public DHTPeerLocatorUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(DHTPeerLocatorUtilsTest.class);
    }

    @Override
    public void setUp() throws Exception {
    }

    // tests to see if it encodes the given torrent location properly
    public void testEncode() {
        String originalIP = "127.0.0.1";

        int originalPort = 4444;

        // 92 and 17 is byte representation of int 4444 in little endian byte
        // order
        byte[] originalIpPort = { 127, 0, 0, 1, 92, 17 };

        byte[] originalId = { 1, 2, 3 };

        try {
            TorrentLocation torLoc = new TorrentLocation(InetAddress.getByName(originalIP),
                    originalPort, originalId);
            byte[] encodedTorLoc = DHTPeerLocatorUtils.encode(torLoc);

            GGEP encoding = new GGEP(encodedTorLoc, 0);
            byte[] ipPort = encoding.getBytes(DHTPeerLocatorUtils.BT_PEER_IP_PORT_KEY);
            byte[] id = encoding.getBytes(DHTPeerLocatorUtils.BT_PEER_ID_KEY);

            assertEquals(originalIpPort, ipPort);
            assertEquals(torLoc.getPeerID(), id);

        } catch (UnknownHostException uhe) {
            fail(uhe);
        } catch (IllegalArgumentException iae) {
            fail(iae);
        } catch (BadGGEPBlockException e) {
            fail(e);
        } catch (BadGGEPPropertyException e) {
            fail(e);
        }
    }

    // tests to see if it decodes a GGEP encoded payload properly
    public void testDecode() {
        // 92 and 17 is byte representation of int 4444 in little endian byte
        // order
        byte[] originalIpPort = { 127, 0, 0, 1, 92, 17 };

        String originalIp = "127.0.0.1";

        int originalPort = 4444;

        byte[] originalId = { 1, 2, 3 };

        try {
            TorrentLocation torLoc = new TorrentLocation(InetAddress.getByName(originalIp),
                    originalPort, originalId);

            GGEP encoding = new GGEP();

            encoding.put(DHTPeerLocatorUtils.BT_PEER_IP_PORT_KEY, originalIpPort);
            encoding.put(DHTPeerLocatorUtils.BT_PEER_ID_KEY, originalId);

            byte[] payload = encoding.toByteArray();

            TorrentLocation decodedTorLoc = DHTPeerLocatorUtils.decode(payload);

            assertEquals(torLoc, decodedTorLoc);

        } catch (UnknownHostException uhe) {
            fail(uhe);
        } catch (IllegalArgumentException iae) {
            fail(iae);
        } catch (InvalidDataException ide) {
            fail(ide);
        }
    }

    //Tests to see if encoding and decoding works fine together.
    //Decode should return the torrent location passed into the encode.
    public void testEncodeDecode() {
        String originalIp = "127.0.0.1";

        int originalPort = 4444;

        byte[] originalId = { 1, 2, 3 };

        try {
            TorrentLocation torLoc = new TorrentLocation(InetAddress.getByName(originalIp), originalPort,
                    originalId);

            byte[] payload = DHTPeerLocatorUtils.encode(torLoc);

            TorrentLocation decodedTorLoc = DHTPeerLocatorUtils.decode(payload);

            assertEquals(torLoc, decodedTorLoc);

        } catch (UnknownHostException uhe) {
            fail(uhe);
        } catch (IllegalArgumentException iae) {
            fail(iae);
        } catch (InvalidDataException ide) {
            fail(ide);
        }

    }
}
