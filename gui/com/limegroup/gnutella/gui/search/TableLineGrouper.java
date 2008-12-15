package com.limegroup.gnutella.gui.search;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.limewire.collection.ApproximateMatcher;
import org.limewire.collection.Comparators;

/** 
 * Used by TableLineModel to quickly find similar SearchResults.  This takes
 * advantage of the fact that two SearchResults' are similar only if their file
 * sizes are similar.  A typical TableLineGrouper is a set of SearchResults', 
 * {a1,..., an}.
 */
final public class TableLineGrouper {        
    /** Maps sizes to lists of index/line pairs.  This list is needed in case
     *  there are multiple files with same size.  (Performance is severely degraded
     *  in this case.) */
    private SortedMap<Long, List<SearchResult>> map=
        new TreeMap<Long, List<SearchResult>>(Comparators.longComparator());

    /** Used to compare all filenames in this.  Ignores case, whitespace.  */
    final private ApproximateMatcher matcher;
    
    public TableLineGrouper() {
        this.matcher=new ApproximateMatcher(120);
        this.matcher.setIgnoreCase(true);
        this.matcher.setIgnoreWhitespace(true);
        this.matcher.setCompareBackwards(true);
    }

    /** Returns true if empty, i.e., cleared. */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void clear() {
        map.clear();
    }

    /** Returns an j s.t. list[j].match(line), 
     *  or -1 if no such i but the caller should keep searching
     *      (because no line in list is close in size)
     *  or -2 if no such i but the caller shouldn't keep searching
     *      (because no line in list is close in size)
     *  @requires elements of list are GrouperPair */
    private int matchHelper(List<SearchResult> list, SearchResult line) {
        assert list!=null : "Trying to match null list";
        assert line!=null : "Trying to match null line";
        for (int j=0; j<list.size(); j++) {
            SearchResult line2 = list.get(j);
            int match=line2.match(line, matcher);
            if (match==0)          //matches
                return j;
            else if (match==2)     //non-similar sizes
                return -2;
        }
        return -1;
    }

    /** Returns a line G in this s.t. G.similar(line), or null of no such line
     *  exists. */
    public SearchResult match(SearchResult line) {
        //OK. This line has no hash...use old algorithm...
        Long key=Long.valueOf(line.getSize());
        //1. Search forward for results with similar (but possibly larger) sizes
        Iterator<List<SearchResult>> iter=map.tailMap(key).values().iterator();
        while (iter.hasNext()) {
            List<SearchResult> lines = iter.next();            
            int ret=matchHelper(lines, line);
            if (ret>=0)
                return lines.get(ret);
            else if (ret==-2)
                break;            
        }
        //2. Search backwards for results with similar (but possibly smaller)
        //   sizes.  Note that we do this in a different way from (1) because 
        //   sorted maps provide no way of iterating through values backwards.
        SortedMap<Long, List<SearchResult>> map=this.map.headMap(key);
        while (true) {
            Long key2;
            try {
                key2 = map.lastKey();
            } catch (NoSuchElementException e) {
                break;
            }

            List<SearchResult> lines = map.get(key2);
            int ret=matchHelper(lines, line);
            if (ret>=0)
                return lines.get(ret);
            else if (ret==-2)
                break;              

            map=map.headMap(key2);
        }
        //3. No luck?  
        return null;
    }

    /**
     * Adds line to this.  Generally there should be no lines similar to line
     * in this, i.e., this.match(line)==null, but this isn't strictly required.
     *     @requires line not in this
     *     @modifies this 
     */
    public void add(SearchResult line) {
        assert line!=null : "Attempting to add null line";
        Long key=Long.valueOf(line.getSize());
        List<SearchResult> lines = map.get(key);
        if (lines==null) {
            lines=new LinkedList<SearchResult>();
            map.put(key, lines);
        }
        
        lines.add(line);        
    }

    /*
    public static void main(String[] args) {
        System.out.println("Starting.");
        //1. Basic tests
        TableLineGrouper grouper=new TableLineGrouper();
        TableLine line1=new TableLine("file.mp3", new Integer(100),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        TableLine line2=new TableLine("file.mp3", new Integer(99),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        TableLine line3=new TableLine("file.mp3", new Integer(101),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        TableLine line4=new TableLine("different.mp3", new Integer(100),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        Assert.that(grouper.match(line1)==null);
        grouper.add(line1);
        Assert.that(grouper.match(line1)==line1);
        Assert.that(grouper.match(line2)==line1);
        Assert.that(grouper.match(line3)==line1);
        Assert.that(grouper.match(line4)==null);        
        TableLine line5=new TableLine("file.mp3", new Integer(1000000),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        TableLine line6=new TableLine("file.mp3", new Integer(1001000),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        TableLine line7=new TableLine("file.mp3", new Integer(999000),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        TableLine line8=new TableLine("file.mp3", new Integer(10000000),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        Assert.that(grouper.match(line5)==null);
        grouper.add(line5);
        grouper.add(line8);
        Assert.that(grouper.match(line6)==line5);
        Assert.that(grouper.match(line7)==line5);     
        System.out.println("Done.");   
    }
    */
    
    /*  
        //Old unit test not adapted to new interface.
      
        //2. Tests whether match searches forward and backward correctly
        grouper=new TableLineGrouper();
        TableLine line0=new TableLine("zero.mp3", new Integer(98),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        line1=new TableLine("first.mp3", new Integer(99),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        line2=new TableLine("second.mp3", new Integer(100),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        line3=new TableLine("third.mp3", new Integer(101),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        line4=new TableLine("fourth.mp3", new Integer(102),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        Assert.that(grouper.add(line0)==0);
        Assert.that(grouper.add(line1)==1);
        Assert.that(grouper.add(line2)==2);
        Assert.that(grouper.add(line3)==3);
        Assert.that(grouper.add(line4)==4);
        TableLine line0c=new TableLine("zero.mp3", new Integer(100),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        TableLine line1c=new TableLine("first.mp3", new Integer(100),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));        
        TableLine line3c=new TableLine("third.mp3", new Integer(100),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        TableLine line4c=new TableLine("fourth.mp3", new Integer(100),
            new Integer(0), new Integer(0), "host", new Integer(6346),
            new byte[16], new Integer(0));
        Assert.that(grouper.match(line1c)==1);
        Assert.that(grouper.match(line3c)==3);
        Assert.that(grouper.match(line0c)==0);
        Assert.that(grouper.match(line4c)==4);
    }
    */
}
