package org.limewire.core.api;

public interface URN extends Comparable<URN> {

    public boolean equals(Object obj);
    
    public int hashCode();
    
    public String toString();
}
