package com.limegroup.bittorrent.handshaking.piecestrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.BitField;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.disk.BlockRangeMap;

public class PartialPieceStrategy implements PieceStrategy {
    private static final Log LOG = LogFactory.getLog(PartialPieceStrategy.class);

    private final Set<BTInterval> exclude;

    private final BTMetaInfo btMetaInfo;

    private final BlockRangeMap partialBlocks;

    private final BlockRangeMap pendingRanges;

    private final BlockRangeMap requestedRanges;

    public PartialPieceStrategy(BTMetaInfo btMetaInfo, Set<BTInterval> exclude, boolean endgame,
            BlockRangeMap partialBlocks, BlockRangeMap pendingRanges, BlockRangeMap requestedRanges) {
        this.btMetaInfo = btMetaInfo;
        this.exclude = exclude;
        this.partialBlocks = partialBlocks;
        this.pendingRanges = pendingRanges;
        this.requestedRanges = requestedRanges;
    }

    @Override
    public List<BTInterval> getNextPieces(BitField availableBlocks, BitField neededBlocks) {
        BTInterval nextPiece = assignPartialPieces(availableBlocks, exclude);
        if (nextPiece != null) {
            return Collections.singletonList(nextPiece);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * This method will try to assign any partially completed pieces that are
     * not currently requested.
     * 
     * @param exclude can be null
     */
    private BTInterval assignPartialPieces(BitField availableBlocks, Set<BTInterval> exclude) {

        // prepare a list of partial or requested blocks the remote host has
        Set<Integer> available = null;
        for (int pieceIndex : partialBlocks.keySet()) {
            if (!availableBlocks.get(pieceIndex) || btMetaInfo.isCompleteBlock(pieceIndex, partialBlocks)) {
                continue;
            }
            if (available == null) {
                available = new HashSet<Integer>(partialBlocks.size());
            }
            available.add(pieceIndex);
        }

        if (available == null) {
            return null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("available partial and requested blocks to attempt: " + available);
        }
        
        List<Integer> availableIndices = new ArrayList<Integer>(available);
        Collections.shuffle(availableIndices);

        
        // go through and find a block that we can request something from.
        for (Iterator<Integer> iterator = availableIndices.iterator(); iterator.hasNext();) {
            int block = iterator.next();

            // figure out which parts of the chunks we need.
            IntervalSet needed = new IntervalSet();
            needed = needed.invert(btMetaInfo.getPieceSize(block));

            IntervalSet partial = partialBlocks.get(block);
            IntervalSet pending = pendingRanges.get(block);
            IntervalSet requested = requestedRanges.get(block);

            // get the parts of the block we're missing
            if (partial != null)
                needed.delete(partial);

            // don't request any parts pending write
            if (pending != null)
                needed.delete(pending);

            // exclude any specified intervals
            if (exclude != null) {
                for (Range excluded : exclude) {
                    needed.delete(excluded);
                }
            }
            // try not to request any parts that are already requested
            if (requested != null) {
                needed.delete(requested);
            }

            if (needed.isEmpty()) {
                continue;
            }

            BTInterval ret = new BTInterval(needed.getFirst(), block);
            if (LOG.isDebugEnabled()) {
                LOG.debug("selected partial/requested interval " + ret + " with partial "
                        + partialBlocks.get(ret.getId()) + " requested "
                        + requestedRanges.get(ret.getId()) + " pending "
                        + pendingRanges.get(ret.getId()));
                
            }
            return ret;
        }

        return null;
    }

}
