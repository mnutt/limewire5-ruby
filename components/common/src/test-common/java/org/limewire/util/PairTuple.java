package org.limewire.util;

public class PairTuple{
    private final Object first;
    private final Object second;
    
    //constructor
    public PairTuple(Object obj1, Object obj2){
        this.first= obj1;
        this.second=obj2;
    }

    public Object[] getPair(){
        Object[] tuple = new Object[2];
        tuple[0] = first;
        tuple[1] = second;
        return tuple;
    }
    
    public Object getFirst(){
        return first;
    }
    
    public Object getSecond(){
        return second;
    }
}
