package com.limegroup.bittorrent.handshaking.piecestrategy;

import java.util.List;
import java.util.Random;

import junit.framework.Assert;
import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.BitFieldSet;
import org.limewire.collection.BitSet;
import org.limewire.util.BaseTestCase;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMetaInfo;

public class RandomGapStrategyTest extends BaseTestCase {

    public RandomGapStrategyTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(RandomGapStrategyTest.class);
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


        context.checking(new Expectations() {
            {
                allowing(btMetaInfo).getNumBlocks();
                will(returnValue(numBlocks));
                allowing(btMetaInfo).getPiece(5);
                will(returnValue(new BTInterval(1, 100, 5)));
                allowing(btMetaInfo).getPiece(6);
                will(returnValue(new BTInterval(101, 200, 6)));
                allowing(btMetaInfo).getPiece(7);
                will(returnValue(new BTInterval(201, 300, 7)));
            }
        });

        RandomGapStrategy randomGapStrategy = new RandomGapStrategy(btMetaInfo, derandomizer);
        BitSet availableBlocks = new BitSet(numBlocks);
        availableBlocks.flip(1, 8);
        BitSet neededBlocks = new BitSet(numBlocks);
        neededBlocks.flip(4, 9);
        List<BTInterval> nextPieces = randomGapStrategy.getNextPieces(new BitFieldSet(
                availableBlocks, numBlocks), new BitFieldSet(neededBlocks, numBlocks));
        Assert.assertEquals(3, nextPieces.size());
        BTInterval btInterval1 = nextPieces.get(0);
        Assert.assertNotNull(btInterval1);
        Assert.assertEquals(new BTInterval(1, 100, 5), btInterval1);
        
        BTInterval btInterval2 = nextPieces.get(1);
        Assert.assertNotNull(btInterval2);
        Assert.assertEquals(new BTInterval(101, 200, 6), btInterval2);
        
        BTInterval btInterval3 = nextPieces.get(2);
        Assert.assertNotNull(btInterval3);
        Assert.assertEquals(new BTInterval(201, 300, 7), btInterval3);
    }

}
