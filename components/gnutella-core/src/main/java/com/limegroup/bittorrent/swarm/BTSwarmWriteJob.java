package com.limegroup.bittorrent.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.limewire.swarm.SwarmContent;
import org.limewire.swarm.SwarmWriteJob;
import org.limewire.swarm.SwarmWriteJobControl;

import org.limewire.util.Objects;
import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.disk.TorrentDiskManager;

public class BTSwarmWriteJob implements SwarmWriteJob {

    public static final int DEFAULT_BUFFER_SIZE = 16 * 1024;

    private final List<BTInterval> pieces;

    private final TorrentDiskManager torrentDiskManager;

    private final SwarmWriteJobControl callback;

    private ByteBuffer byteBuffer;

    private long piecePosition;

    private long pieceLow;

    private int pieceIndex = 0;

    private final int maxBufferSize;

    public BTSwarmWriteJob(List<BTInterval> pieces, TorrentDiskManager torrentDiskManager,
            SwarmWriteJobControl callback) {
        this(pieces, torrentDiskManager, callback, DEFAULT_BUFFER_SIZE);
    }

    public BTSwarmWriteJob(List<BTInterval> pieces, TorrentDiskManager torrentDiskManager,
            SwarmWriteJobControl callback, int maxBufferSize) {
        this.pieces = Objects.nonNull(pieces, "pieces");
        assert pieces.size() > 0;
        assert maxBufferSize > 0;
        this.torrentDiskManager = Objects.nonNull(torrentDiskManager, "torrentDiskManager");
        this.callback = Objects.nonNull(callback, "callback");
        this.byteBuffer = null;
        this.piecePosition = pieces.get(0).get32BitLow();
        this.pieceLow = pieces.get(0).get32BitLow();
        this.maxBufferSize = maxBufferSize;
    }

    @Override
    public void cancel() {
        // nothing to cancel, we will be relying on the torrent disk manager to
        // do the actual writing
        // This job will just be used to aggregate the data we need
    }

    @Override
    public long write(SwarmContent content) throws IOException {
        BTInterval piece = pieces.get(pieceIndex);
        if (byteBuffer == null) {
            int bufferSize = maxBufferSize;
            if (bufferSize + piecePosition > piece.getHigh()) {
                bufferSize = (int) (piece.get32BitHigh() - piecePosition) + 1;
            }

            if (bufferSize == 0) {
                return 0;
            }

            byteBuffer = ByteBuffer.allocate(bufferSize);
        }

        long read = content.read(byteBuffer);
        if (read == -1) {
            throw new IOException("End of stream reached before expected.");
        }

        piecePosition += read;
        callback.resume();

        if (byteBuffer.remaining() == 0) {
            // piece is done reading
            final byte[] data = byteBuffer.array();
            byteBuffer = null;
            final int pieceId = piece.getId();

            final long pieceLow = this.pieceLow;
            final long pieceHigh = pieceLow + data.length - 1;

            torrentDiskManager.writeBlock(new BTNECallable(pieceId, pieceLow, pieceHigh, data));

            this.pieceLow = pieceHigh + 1;
            if (piecePosition >= piece.getHigh()) {
                if (pieceIndex + 1 == pieces.size()) {
                    return read;
                }
                this.pieceLow = pieces.get(++pieceIndex).getLow();
                this.piecePosition = this.pieceLow;
            }
        }
        return read;
    }

}
