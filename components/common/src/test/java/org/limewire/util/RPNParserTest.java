package org.limewire.util;

import java.util.Properties;

import org.limewire.util.RPNParser.StringLookup;

import junit.framework.Test;

public class RPNParserTest extends BaseTestCase {

    public RPNParserTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(RPNParserTest.class);
    }    
    
    public void testArithmetic() throws Exception {
        RPNParser p = new RPNParser("1","2","<");
        // 1 < 2
        assertTrue(p.evaluate());

        p = new RPNParser("1","2",">");
        assertFalse(p.evaluate());

        // 0 == 0
        p = new RPNParser("0","0","==");
        assertTrue(p.evaluate());
    }
    
    public void testOr() throws Exception {
        // true or true is true
        RPNParser p = new RPNParser("true","true","OR");
        assertTrue(p.evaluate());
        
        // true or false is true
        p = new RPNParser("true","false","OR");
        assertTrue(p.evaluate());
        
        // false or true is true
        p = new RPNParser("false","true","OR");
        assertTrue(p.evaluate());
        
        // false or false is false
        p = new RPNParser("false","false","OR");
        assertFalse(p.evaluate());
    }
    
    public void testAnd() throws Exception {
        // true and true is true
        RPNParser p = new RPNParser("true","true","AND");
        assertTrue(p.evaluate());
        
        // true and false is false
        p = new RPNParser("true","false","AND");
        assertFalse(p.evaluate());
        
        // false and true is true
        p = new RPNParser("false","true","AND");
        assertFalse(p.evaluate());
        
        // false and false is false
        p = new RPNParser("false","false","AND");
        assertFalse(p.evaluate());
    }
    
    public void testNot() throws Exception {
        
        // not true is false
        RPNParser p = new RPNParser("true","NOT");
        assertFalse(p.evaluate());
        
        // not false is true
        p = new RPNParser("false","NOT");
        assertTrue(p.evaluate());
    }
    
    public void testInvalid() throws Exception {
        RPNParser p;
        try {
            p = new RPNParser("asdf","NOT");
            p.evaluate();
            fail("invalid operand");
        } catch (IllegalArgumentException expected){}
        
        try {
            p = new RPNParser("3","N");
            p.evaluate();
            fail("invalid operation");
        } catch (IllegalArgumentException expected){}
        
        try {
            p = new RPNParser("3","1","NOT");
            p.evaluate();
            fail("logic on number");
        } catch (IllegalArgumentException expected){}
        
        try {
            p = new RPNParser("true","false","<");
            p.evaluate();
            fail("arithmetic on boolean");
        } catch (IllegalArgumentException expected){}
        
        try {
            p = new RPNParser("false","OR");
            p.evaluate();
            fail("not enough operands");
        } catch (IllegalArgumentException expected){}
        
        try {
            p = new RPNParser("true","true","NOT");
            p.evaluate();
            fail("too many operands");
        } catch (IllegalArgumentException expected){}
    }
    
    public void testComposite() throws Exception {
        // ((5 > 4) && (4 < 3)) || ( 5 > 0 && false) || (0 == 0)
        RPNParser p = new RPNParser("5","4",">","4","3","<","AND","5","0",">","false","AND","OR","0","0","==","OR");
        
        assertTrue(p.evaluate());
    }
    
    public void testPropsLookup() throws Exception {
        MyLookup p = new MyLookup();
        p.setProperty("x","1");
        p.setProperty("y", "2");
        p.setProperty("yes", "true");
        p.setProperty("no", "false");
        p.setProperty("maybe","whatever");
        
        RPNParser r = new RPNParser("maybe","whatever","==");
        
        assertTrue(r.evaluate(p));
        
        // x < y ?
        r = new RPNParser("x","y","<");
        assertTrue(r.evaluate(p));
        
        // x > 1 ?
        r = new RPNParser("x","1",">");
        assertFalse(r.evaluate(p));
        
        // y == 2 ?
        r = new RPNParser("y","2","==");
        assertTrue(r.evaluate(p));
        
        // x > 0 == yes?
        r = new RPNParser("x","0",">","yes","==");
        assertTrue(r.evaluate(p));
        
        // yes == !no?
        r = new RPNParser("yes","no","NOT","==");
        assertTrue(r.evaluate(p));
        
        // !yes ?
        r = new RPNParser("yes","NOT");
        assertFalse(r.evaluate(p));
    }
    
    public void testContains() throws Exception {
        RPNParser p = new RPNParser("badger","adge","CONTAINS");
        assertTrue(p.evaluate());
        p = new RPNParser("adge","badger","CONTAINS");
        assertFalse(p.evaluate());
    }
    
    public void testMatches() throws Exception {
        RPNParser p = new RPNParser("[a-c]a.*","badger","MATCHES");
        assertTrue(p.evaluate());
        p = new RPNParser(".*er.","badger","MATCHES");
        assertFalse(p.evaluate());
        try {
            p = new RPNParser(false,"[a-c]a.*","badger","MATCHES");
            p.evaluate();
            fail("experimental was allowed");
        } catch (IllegalArgumentException expected){}
    }

    private static class MyLookup extends Properties implements StringLookup {
        public String lookup(String key) {
            return getProperty(key);
        }
    }
}
