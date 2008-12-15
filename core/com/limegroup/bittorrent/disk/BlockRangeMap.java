/**
 * 
 */
package com.limegroup.bittorrent.disk;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.limewire.collection.BitField;
import org.limewire.collection.BitFieldSet;
import org.limewire.collection.BitSet;
import org.limewire.collection.IntervalSet;

import com.limegroup.bittorrent.BTInterval;

public class BlockRangeMap {

    private final HashMap<Integer, IntervalSet> blocks;

    private final BitSet bitSet;

    private final int numBlocks;

    public BlockRangeMap(int numBlocks) {
        this.numBlocks = numBlocks;
        blocks = new HashMap<Integer, IntervalSet>(numBlocks);
        bitSet = new BitSet(numBlocks);
    }

    public void addInterval(BTInterval in) {
        IntervalSet s = blocks.get(in.getBlockId());
        if (s == null) {
            s = new IntervalSet();
            blocks.put(in.getBlockId(), s);
            bitSet.set(in.getId(), true);
        }
        s.add(in);
    }

    public void removeInterval(BTInterval in) {
        IntervalSet s = blocks.get(in.getBlockId());
        if (s == null)
            return;
        s.delete(in);
        if (s.isEmpty()) {
            blocks.remove(in.getBlockId());
            bitSet.set(in.getBlockId(), false);
        }

    }

    public long byteSize() {
        long ret = 0;
        for (IntervalSet set : blocks.values())
            ret += set.getSize();
        return ret;
    }

    @Override
    public BlockRangeMap clone() {
        BlockRangeMap clone = new BlockRangeMap(blocks.size());
        for (Map.Entry<Integer, IntervalSet> e : blocks.entrySet()) {
            try {
                clone.blocks.put(e.getKey(), e.getValue().clone());
            } catch (CloneNotSupportedException e1) {
                throw new RuntimeException(e1);
            }
        }
        return clone;
    }

    public void clear() {
        blocks.clear();
        bitSet.clear();
    }

    public int size() {
        return blocks.size();
    }

    public boolean containsKey(Object key) {
        return blocks.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return blocks.containsValue(value);
    }

    public IntervalSet get(Integer key) {
        return blocks.get(key);
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    public IntervalSet remove(Integer key) {
        IntervalSet intervalSet = blocks.remove(key);
        bitSet.set(key, false);
        return intervalSet;
    }

    public Collection<IntervalSet> values() {
        return blocks.values();
    }

    public Set<Map.Entry<Integer, IntervalSet>> entrySet() {
        return blocks.entrySet();
    }

    public Set<Integer> keySet() {
        return blocks.keySet();
    }

    public void putAll(Map<? extends Integer, ? extends IntervalSet> t) {
        blocks.putAll(t);

        for (Integer key : t.keySet()) {
            bitSet.set(key, true);
        }
    }

    public BitField getBitField() {
        return new BitFieldSet(bitSet, numBlocks);
    }

}