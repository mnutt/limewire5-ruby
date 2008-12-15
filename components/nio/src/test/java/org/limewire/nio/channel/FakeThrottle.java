package org.limewire.nio.channel;

import java.util.ArrayList;
import java.util.List;

import org.limewire.nio.Throttle;
import org.limewire.nio.ThrottleListener;




/** A fake throttle, for testing ThrottleWriter. */
public class FakeThrottle implements Throttle {
    private int available;
    private int interests = 0;
    private boolean didRequest;
    private boolean didRelease;
    
    private List<ThrottleListener> tl = new ArrayList<ThrottleListener>();
    
    public void interest(ThrottleListener writer) {
        if(tl.size() == 0){
            tl.add(interests, writer);
        }//don't want to add the same ThrottleListener twice so check uniqueness
        else{
            boolean unique = false;
            for(int i = 0; i < tl.size() - 1; i++){
                ThrottleListener o = tl.get(i);
                if(o.equals(writer)){
                    unique = false;
                    break;
                }
                else
                    unique = true;
            }
            if(unique){
                tl.add(interests, writer);                
            }
        }        
        interests++;
    }
    
    public int request() {
        didRequest = true;
        int av = available;
        available = 0;
        return av;
    }
    
    public void release(int amount) {
        didRelease = true;
        available += amount;
    }
    
    void setAvailable(int av) { available = av; }
    public void limit(int i){}
    int getAvailable() { return available; }
    int interests() { return interests; }
    public boolean didRequest() { return didRequest; }
    public boolean didRelease() { return didRelease; }
    void clear() { available = 0; interests = 0; didRequest = false; didRelease = false; }

    ThrottleListener getListener(int pos){
        return tl.get(pos);
    }
    public int listeners(){return tl.size();} 
    
    public void setRate(float rate) {}
    public long nextTickTime() {return 0;}
}
    
    