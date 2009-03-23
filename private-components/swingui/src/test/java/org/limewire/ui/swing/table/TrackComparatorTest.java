package org.limewire.ui.swing.table;

import org.limewire.util.BaseTestCase;

public class TrackComparatorTest extends BaseTestCase{

    public TrackComparatorTest(String name) {
        super(name);
    }
    
    /**
     * Test various situations where at least one of the descriptors
     *  is null or empty.
     */
    public void testNullsAndEmpties() {
        TrackComparator comparator = new TrackComparator();
        
        assertEquals(0, comparator.compare(null, null));
        assertEquals(0, comparator.compare("", null));
        assertEquals(0, comparator.compare(null, ""));
        assertEquals(0, comparator.compare("", ""));
        assertEquals(0, comparator.compare("  \r\r   ", ""));
        assertEquals(0, comparator.compare("", "   \n\n\n\n\r      "));
        assertEquals(0, comparator.compare("   ", "      "));
        assertEquals(0, comparator.compare("  ", null));
        assertEquals(0, comparator.compare(null, "    "));
        assertEquals(0, comparator.compare("             ", null));
        
        assertEquals(-1, comparator.compare("", "410"));
        assertEquals(-1, comparator.compare("  ", "405"));
        assertEquals(-1, comparator.compare(null, "280"));
        
        assertEquals(-1, comparator.compare(null, "95055/10003"));
        assertEquals(-1, comparator.compare("    ", "27960/L6H"));
        assertEquals(-1, comparator.compare("", "10000/DF"));
        
        assertEquals(-1, comparator.compare(null, "(416)"));
        assertEquals(-1, comparator.compare("  ", "(+44)"));
        assertEquals(-1, comparator.compare("", "(+34)"));
        
        assertEquals(1, comparator.compare("880", ""));
        assertEquals(1, comparator.compare("680", "                \t"));
        assertEquals(1, comparator.compare("580", null));
        
        assertEquals(1, comparator.compare("495/395", null));
        assertEquals(1, comparator.compare("90/90", "\t  "));
        assertEquals(1, comparator.compare("290/190", ""));
        
        assertEquals(1, comparator.compare("(905)", null));
        assertEquals(1, comparator.compare("(+39)", "        \t\t\t           "));
        assertEquals(1, comparator.compare("(519)", ""));
    }
    
    /**
     * Tests basic comparisons in two basic numeric track descriptors.
     */
    public void testBasic() {
        TrackComparator comparator = new TrackComparator();
        
        assertEquals(0, comparator.compare("5", "5"));
        assertEquals(0, comparator.compare("-3", "-03"));
        assertEquals(0, comparator.compare("08", "08"));
        assertEquals(0, comparator.compare("00003", "003"));
        assertEquals(0, comparator.compare("    100 ", " 100   "));
        
        assertEquals(1, comparator.compare("10110011111001101", "10002"));
        assertEquals(-1, comparator.compare("10002", "10110011111001000"));
        assertEquals(1, comparator.compare("-1", "-002"));
        assertEquals(1, comparator.compare("500", "-800"));
        
        assertEquals(1, comparator.compare("     10110011111001101        ", " 10002"));
        assertEquals(-1, comparator.compare("10002      ", "10110011111001000         "));
        assertEquals(1, comparator.compare(" -1", "  -002"));
        assertEquals(1, comparator.compare("500  ", "-800"));
    }

    /**
     * Tests comparisons of two X/Y fractional descriptors considering a simplification
     *  where only the numerator is considered.
     */
    public void testFractionalComplex() {
        TrackComparator comparator = new TrackComparator();
        
        assertEquals(0, comparator.compare("5/5", "5/5"));
        assertEquals(0, comparator.compare("-3/1", "-03/1"));
        assertEquals(0, comparator.compare("08/-01", "08/-01k"));
        assertEquals(0, comparator.compare("00003/11e1111", "003/1111e1111111"));
        assertEquals(0, comparator.compare("    100/-8abc ", " 100/ blapheraction   "));
        
        assertEquals(1, comparator.compare("10110011111001101/56", "10002/10002"));
        assertEquals(-1, comparator.compare("10002/99", "1011001111100100/asdsa99"));
        assertEquals(1, comparator.compare("-0/5", "-002/-0"));
        assertEquals(1, comparator.compare("500/1", "-800/1"));
        
        assertEquals(1, comparator.compare("     10110011111001101/   kde        ", " 10002/kruler "));
        assertEquals(-1, comparator.compare("10002/fyrefax      ", "10110011111001000/5tundabyrd         "));
        assertEquals(1, comparator.compare(" -1/*~##^N", "  -002"));
        assertEquals(1, comparator.compare("500/pingüinos  ", "-800/    carrots!"));
    }

    public void testGibberish() {
        TrackComparator comparator = new TrackComparator();
        
        assertEquals(0, comparator.compare("a", "a"));
        assertEquals(0, comparator.compare("bb", "bb"));
        assertEquals(0, comparator.compare("cat", "cat"));
        assertEquals(0, comparator.compare("dog/doggies", "dog/doggies"));
        assertEquals(0, comparator.compare("    cat/kitties ", "    cat/kitties "));
        assertEquals(0, comparator.compare("aaaaaaaa", "aaaaaaaa"));
        
        assertEquals(1, comparator.compare("b", "a"));
        assertEquals(-1, comparator.compare("a", "b"));
        assertEquals("carrots".compareTo("cranapples"), comparator.compare("carrots", "cranapples"));
        assertEquals("patatobugs".compareTo("-1meelyworms"), comparator.compare("patatobugs", "-1meelyworms"));

        assertEquals("   water lillez en big /treeses ".compareTo("  water lillez en big /treeses"), 
                comparator.compare("   water lillez en big /treeses ", "  water lillez en big /treeses"));
        
        assertEquals(0, comparator.compare("randomas$ortment of -characters españoles ",
                        "randomas$ortment of -characters españoles "));
    }
    
    /**
     * Tests mixed comparisons not including those with null or empty.
     * 
     * <p>That is tests mixing between simple, fractional-complex, and 
     *  gibberish descriptors.
     */
    public void testMixed() {
        TrackComparator comparator = new TrackComparator();
                
        assertEquals(-1, comparator.compare("a", "1"));
        assertEquals(1, comparator.compare("1", "a"));
        assertEquals(1, comparator.compare("1", "%"));
        assertEquals(-1, comparator.compare("%", "1"));
        assertEquals(1, comparator.compare("-1", "-one"));
        assertEquals(1, comparator.compare("10/fwacsin", "10\fwacsin"));
        assertEquals(1, comparator.compare("        1/1       ", "          1+1        "));
        assertEquals(-1, comparator.compare("       wan        ", "         -10000000000         "));
        assertEquals(-1, comparator.compare("jibberinish        ", "0"));
        assertEquals(1, comparator.compare("0       ", "jibberinish        "));
        
        assertEquals(0, comparator.compare("1", "1/1"));
        assertEquals(0, comparator.compare("1/1", "1"));
        assertEquals(0, comparator.compare("111111/d", "111111"));
        assertEquals(0, comparator.compare("        -182   ", "      -0000182/2235435346345rdfg345"));
        
        assertEquals(1, comparator.compare("123", "122/122"));
        assertEquals(1, comparator.compare("-1001/1", "       \t       \t\n           -1002"));
        assertEquals(1, comparator.compare("1/2", "0"));
        assertEquals(1, comparator.compare("15", "13/muskrats"));
        
        assertEquals(-1, comparator.compare("111111/d", "111111111"));
        assertEquals(-1, comparator.compare("        -183   ", "      -0000182/2235435346345rdfg345"));
        assertEquals(-1, comparator.compare("        00000/00000   ", "1"));
        assertEquals(-1, comparator.compare("   -1   ", "     00000/00000"));
    }
    
    /**
     * Quick tests of big and small inputs to make sure bad things don't happen.
     */
    public void testEdgeCases() {
        TrackComparator comparator = new TrackComparator();
                
        // Normal
        assertEquals(-1, comparator.compare("12/birre", Long.toString(Long.MAX_VALUE)));
        assertEquals(-1, comparator.compare(Long.toString(Long.MIN_VALUE)+"/1", Long.toString(Long.MAX_VALUE)));
        assertEquals(1, comparator.compare(Long.toString(Long.MAX_VALUE)+"/1", Long.toString(Long.MIN_VALUE)));
        assertEquals(0, comparator.compare(Long.toString(Long.MAX_VALUE), Long.toString(Long.MAX_VALUE)));
        assertEquals(1, comparator.compare(Long.toString(Long.MAX_VALUE), "1"));
        
        // Too big
        assertEquals(-1, comparator.compare(Long.toString(Long.MAX_VALUE)+"99999/toohuuuuge", "0"));
        assertEquals(1, comparator.compare(Long.toString(Long.MAX_VALUE)+"99999", Long.toString(Long.MAX_VALUE)+"99998"));
        assertGreaterThan(0, comparator.compare("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
        		"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
        		"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
        		"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
        		"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
        		"aaaaaaaaaaaaaaaaaaaaaaaprogrammifyingRox!aaaaaaaaaaaaaaaaaaaa" +
        		"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
        		"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
        		"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
        		"aaaaaaaaaaaaaaathis is funaaaaaaaaaaaaa" +
        		"aaaaaaaaaaaaaaaaaaaaaa", "a"));
    }
}

