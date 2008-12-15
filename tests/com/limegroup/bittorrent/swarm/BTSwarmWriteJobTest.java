package com.limegroup.bittorrent.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.swarm.SwarmContent;
import org.limewire.swarm.SwarmWriteJobControl;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.disk.TorrentDiskManager;
import com.limegroup.gnutella.util.LimeTestCase;

public class BTSwarmWriteJobTest extends LimeTestCase {

    public BTSwarmWriteJobTest(String name) {
        super(name);
    }
    
    
    public static Test suite() {
        return buildTestSuite(BTSwarmWriteJobTest.class);
    }

    public void testWriteOnePartialPieces() throws Exception {
        List<BTInterval> pieces = new ArrayList<BTInterval>();
        final BTInterval piece1 = new BTInterval(0, 9, 0);
        pieces.add(piece1);
        Mockery mockery = new Mockery();

        final TorrentDiskManager tdm = mockery.mock(TorrentDiskManager.class);
        final SwarmWriteJobControl callback = mockery.mock(SwarmWriteJobControl.class);

        final ByteBuffer contentBuffer = ByteBuffer.allocate(10);
        final byte[] data1 = { 0, 1, 2, 3, 4 };
        contentBuffer.put(data1);
        contentBuffer.clear();

        mockery.checking(new Expectations() {
            {
                one(tdm).writeBlock(new BTNECallable(0, 0, 4, data1));
                one(callback).resume();
            }
        });

        BTSwarmWriteJob swarmWriteJob = new BTSwarmWriteJob(pieces, tdm, callback, 5);

        SwarmContent content = new SwarmContentTester(contentBuffer);
        swarmWriteJob.write(content);
        mockery.assertIsSatisfied();
    }

    public void testWriteOnePiece() throws Exception {
        List<BTInterval> pieces = new ArrayList<BTInterval>();
        final BTInterval piece1 = new BTInterval(0, 9, 0);
        pieces.add(piece1);
        Mockery mockery = new Mockery();

        final TorrentDiskManager tdm = mockery.mock(TorrentDiskManager.class);
        final SwarmWriteJobControl callback = mockery.mock(SwarmWriteJobControl.class);

        final ByteBuffer contentBuffer = ByteBuffer.allocate(10);
        final byte[] data1 = { 0, 1, 2, 3, 4 };
        final byte[] data2 = { 5, 6, 7, 8, 9 };
        contentBuffer.put(data1);
        contentBuffer.put(data2);
        contentBuffer.clear();

        mockery.checking(new Expectations() {
            {
                one(tdm).writeBlock(new BTNECallable(0, 0, 4, data1));
                one(tdm).writeBlock(new BTNECallable(0, 5, 9, data2));
                atLeast(2).of(callback).resume();
            }
        });

        BTSwarmWriteJob swarmWriteJob = new BTSwarmWriteJob(pieces, tdm, callback, 5);

        SwarmContent content = new SwarmContentTester(contentBuffer);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);

        mockery.assertIsSatisfied();
    }

    public void testWriteTwoPieces() throws Exception {
        List<BTInterval> pieces = new ArrayList<BTInterval>();
        final BTInterval piece1 = new BTInterval(0, 9, 0);
        final BTInterval piece2 = new BTInterval(0, 9, 1);
        pieces.add(piece1);
        pieces.add(piece2);
        Mockery mockery = new Mockery();

        final TorrentDiskManager tdm = mockery.mock(TorrentDiskManager.class);
        final SwarmWriteJobControl callback = mockery.mock(SwarmWriteJobControl.class);

        final ByteBuffer contentBuffer = ByteBuffer.allocate(20);
        final byte[] data1 = { 0, 1, 2, 3, 4 };
        final byte[] data2 = { 5, 6, 7, 8, 9 };
        contentBuffer.put(data1);
        contentBuffer.put(data2);
        contentBuffer.put(data2);
        contentBuffer.put(data1);
        contentBuffer.clear();

        mockery.checking(new Expectations() {
            {
                one(tdm).writeBlock(new BTNECallable(0, 0, 4, data1));
                one(tdm).writeBlock(new BTNECallable(0, 5, 9, data2));
                one(tdm).writeBlock(new BTNECallable(1, 0, 4, data2));
                one(tdm).writeBlock(new BTNECallable(1, 5, 9, data1));
                atLeast(4).of(callback).resume();
            }
        });

        BTSwarmWriteJob swarmWriteJob = new BTSwarmWriteJob(pieces, tdm, callback, 5);

        SwarmContent content = new SwarmContentTester(contentBuffer);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);

        mockery.assertIsSatisfied();
    }

    public void testWriteTwoPartialPieces() throws Exception {
        List<BTInterval> pieces = new ArrayList<BTInterval>();
        final BTInterval piece1 = new BTInterval(3, 6, 0);
        final BTInterval piece2 = new BTInterval(6, 9, 1);
        pieces.add(piece1);
        pieces.add(piece2);
        Mockery mockery = new Mockery();

        final TorrentDiskManager tdm = mockery.mock(TorrentDiskManager.class);
        final SwarmWriteJobControl callback = mockery.mock(SwarmWriteJobControl.class);

        final ByteBuffer contentBuffer = ByteBuffer.allocate(8);
        final byte[] data1 = { 0, 1, 2, 3 };
        final byte[] data2 = { 6, 7, 8, 9 };
        contentBuffer.put(data1);
        contentBuffer.put(data2);
        contentBuffer.clear();

        mockery.checking(new Expectations() {
            {
                one(tdm).writeBlock(new BTNECallable(0, 3, 6, data1));
                one(tdm).writeBlock(new BTNECallable(1, 6, 9, data2));
                atLeast(2).of(callback).resume();
            }
        });

        BTSwarmWriteJob swarmWriteJob = new BTSwarmWriteJob(pieces, tdm, callback, 5);

        SwarmContent content = new SwarmContentTester(contentBuffer);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);
        mockery.assertIsSatisfied();
    }

    public void testWriteThreePieces() throws Exception {
        List<BTInterval> pieces = new ArrayList<BTInterval>();
        final BTInterval piece1 = new BTInterval(0, 9, 0);
        final BTInterval piece2 = new BTInterval(0, 9, 1);
        final BTInterval piece3 = new BTInterval(0, 9, 2);
        pieces.add(piece1);
        pieces.add(piece2);
        pieces.add(piece3);
        Mockery mockery = new Mockery();

        final TorrentDiskManager tdm = mockery.mock(TorrentDiskManager.class);
        final SwarmWriteJobControl callback = mockery.mock(SwarmWriteJobControl.class);

        final ByteBuffer contentBuffer = ByteBuffer.allocate(30);
        final byte[] data1 = { 0, 1, 2, 3, 4 };
        final byte[] data2 = { 5, 6, 7, 8, 9 };
        contentBuffer.put(data1);
        contentBuffer.put(data2);
        contentBuffer.put(data2);
        contentBuffer.put(data1);
        contentBuffer.put(data2);
        contentBuffer.put(data2);
        contentBuffer.clear();

        mockery.checking(new Expectations() {
            {
                one(tdm).writeBlock(new BTNECallable(0, 0, 4, data1));
                one(tdm).writeBlock(new BTNECallable(0, 5, 9, data2));
                one(tdm).writeBlock(new BTNECallable(1, 0, 4, data2));
                one(tdm).writeBlock(new BTNECallable(1, 5, 9, data1));
                one(tdm).writeBlock(new BTNECallable(2, 0, 4, data2));
                one(tdm).writeBlock(new BTNECallable(2, 5, 9, data2));
                atLeast(4).of(callback).resume();
            }
        });

        BTSwarmWriteJob swarmWriteJob = new BTSwarmWriteJob(pieces, tdm, callback, 5);

        SwarmContent content = new SwarmContentTester(contentBuffer);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);

        mockery.assertIsSatisfied();
    }

    public void testWriteThreePartialPieces() throws Exception {
        List<BTInterval> pieces = new ArrayList<BTInterval>();
        final BTInterval piece1 = new BTInterval(1, 9, 0);
        final BTInterval piece2 = new BTInterval(5, 9, 1);
        final BTInterval piece3 = new BTInterval(0, 2, 2);
        pieces.add(piece1);
        pieces.add(piece2);
        pieces.add(piece3);
        Mockery mockery = new Mockery();

        final TorrentDiskManager tdm = mockery.mock(TorrentDiskManager.class);
        final SwarmWriteJobControl callback = mockery.mock(SwarmWriteJobControl.class);

        final ByteBuffer contentBuffer = ByteBuffer.allocate(17);
        final byte[] data1 = { 0, 1, 2, 3, 4 };
        final byte[] data2 = { 5, 6, 7, 8 };
        final byte[] data3 = { 9, 10, 11, 12, 13 };
        final byte[] data4 = { 14, 15, 16 };
        contentBuffer.put(data1);
        contentBuffer.put(data2);
        contentBuffer.put(data3);
        contentBuffer.put(data4);
        contentBuffer.clear();

        mockery.checking(new Expectations() {
            {
                one(tdm).writeBlock(new BTNECallable(0, 1, 5, data1));
                one(tdm).writeBlock(new BTNECallable(0, 6, 9, data2));
                one(tdm).writeBlock(new BTNECallable(1, 5, 9, data3));
                one(tdm).writeBlock(new BTNECallable(2, 0, 2, data4));
                atLeast(4).of(callback).resume();
            }
        });

        BTSwarmWriteJob swarmWriteJob = new BTSwarmWriteJob(pieces, tdm, callback, 5);

        SwarmContent content = new SwarmContentTester(contentBuffer);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);

        mockery.assertIsSatisfied();
    }
    
    public void testWriteThreePartialPiecesWriteCalledExtraTime() throws Exception {
        List<BTInterval> pieces = new ArrayList<BTInterval>();
        final BTInterval piece1 = new BTInterval(1, 9, 0);
        final BTInterval piece2 = new BTInterval(5, 9, 1);
        final BTInterval piece3 = new BTInterval(0, 2, 2);
        pieces.add(piece1);
        pieces.add(piece2);
        pieces.add(piece3);
        Mockery mockery = new Mockery();

        final TorrentDiskManager tdm = mockery.mock(TorrentDiskManager.class);
        final SwarmWriteJobControl callback = mockery.mock(SwarmWriteJobControl.class);

        final ByteBuffer contentBuffer = ByteBuffer.allocate(17);
        final byte[] data1 = { 0, 1, 2, 3, 4 };
        final byte[] data2 = { 5, 6, 7, 8 };
        final byte[] data3 = { 9, 10, 11, 12, 13 };
        final byte[] data4 = { 14, 15, 16 };
        contentBuffer.put(data1);
        contentBuffer.put(data2);
        contentBuffer.put(data3);
        contentBuffer.put(data4);
        contentBuffer.clear();

        mockery.checking(new Expectations() {
            {
                one(tdm).writeBlock(new BTNECallable(0, 1, 5, data1));
                one(tdm).writeBlock(new BTNECallable(0, 6, 9, data2));
                one(tdm).writeBlock(new BTNECallable(1, 5, 9, data3));
                one(tdm).writeBlock(new BTNECallable(2, 0, 2, data4));
                atLeast(4).of(callback).resume();
            }
        });

        BTSwarmWriteJob swarmWriteJob = new BTSwarmWriteJob(pieces, tdm, callback, 5);

        SwarmContent content = new SwarmContentTester(contentBuffer);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);
        swarmWriteJob.write(content);

        mockery.assertIsSatisfied();
    }

    private final class SwarmContentTester implements SwarmContent {
        private final ByteBuffer contentBuffer;

        private SwarmContentTester(ByteBuffer contentBuffer) {
            this.contentBuffer = contentBuffer;
        }

        public int read(ByteBuffer byteBuffer) throws IOException {
            int oldRemaining = byteBuffer.remaining();
            if (contentBuffer.remaining() == 0) {
                return 0;
            }
            int oldLimit = contentBuffer.limit();
            int remainingOffset = contentBuffer.remaining() - byteBuffer.remaining();
            int newLimit = oldLimit - remainingOffset;
            if (newLimit < oldLimit) {
                contentBuffer.limit(newLimit);
            }
            byteBuffer.put(contentBuffer);
            contentBuffer.limit(oldLimit);
            int read = oldRemaining - byteBuffer.remaining();
            return read;
        }
    }
}
