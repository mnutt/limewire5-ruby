package org.limewire.util;

import java.util.LinkedList;
import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * A Hamcrest IsInstanceOf style matcher that stores the matches in
 *  an ordered list. 
 */
public class MatchAndCopy<X> extends BaseMatcher<X> {

    private final List<X> matches = new LinkedList<X>();
    private Class<?> theClass;
    
    public MatchAndCopy(Class<?> theClass) {
        this.theClass = theClass;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(Object item) {
        if (theClass.isInstance(item)) {
            matches.add((X)item);
            return true;
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
    }
   
    public List<X> getMatches() {
        return matches;
    }
    
    public X getLastMatch() {
        return matches.get(matches.size()-1);
    }
}