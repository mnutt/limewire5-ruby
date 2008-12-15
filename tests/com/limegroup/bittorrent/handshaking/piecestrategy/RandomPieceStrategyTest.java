package com.limegroup.bittorrent.handshaking.piecestrategy;

import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.BitFieldSet;
import org.limewire.collection.BitSet;
import org.limewire.util.BaseTestCase;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMetaInfo;

public class RandomPieceStrategyTest extends BaseTestCase {

    public RandomPieceStrategyTest(String name) {
        super(name);

    }

    public void testGetNextPieces() {
        Mockery context = new Mockery();

        final BTMetaInfo btMetaInfo = context.mock(BTMetaInfo.class);
        final Random derandomizer = new Random() {
            private float[] values = { .5f, .2f, .8f, .5f, .2f, .8f };

            private int index = 0;

            @Override
            public float nextFloat() {
                return values[index++];
            }

        };

        final int numBlocks = 10;

        final int pieceIndex1 = 5;

        context.checking(new Expectations() {
            {
                allowing(btMetaInfo).getPiece(pieceIndex1);
                will(returnValue(new BTInterval(1, 100, pieceIndex1)));
            }
        });

        RandomPieceStrategy randomPieceStrategy = new RandomPieceStrategy(btMetaInfo, derandomizer);
        BitSet availableBlocks = new BitSet(numBlocks);
        availableBlocks.flip(1, 8);
        BitSet neededBlocks = new BitSet(numBlocks);
        neededBlocks.flip(4, 9);
        List<BTInterval> nextPieces = randomPieceStrategy.getNextPieces(new BitFieldSet(
                availableBlocks, numBlocks), new BitFieldSet(neededBlocks, numBlocks));
        Assert.assertEquals(1, nextPieces.size());
        BTInterval btInterval1 = nextPieces.get(0);
        Assert.assertNotNull(btInterval1);
        Assert.assertEquals(new BTInterval(1, 100, 5), btInterval1);
    }

}
