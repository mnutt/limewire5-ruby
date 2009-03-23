package org.limewire.statistic;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.limewire.collection.Buffer;
import org.limewire.inspection.Inspectable;

/**
 * Provides a default implementation of the <code>Statistic</code> interface as an
 * abstract class. <code>AbstractStatistic</code> tracks the history for the 
 * given statistic, provides access to the average value, the maximum value 
 * and optionally writes values to disk.
 * <p>
 * Additionally, <code>AbstractStatistic</code> implements {@link Inspectable} and
 * includes a default implementation of {@link #inspect()} to examine the stored
 * statistics.
 * <p>
 *See the Lime Wire Wiki for sample code using the 
 *<a href="http://www.limewire.org/wiki/index.php?title=Org.limewire.statistic">org.limewire.statistic</a>
 *package.
 */
public abstract class AbstractStatistic implements Statistic {

	/**
	 * <tt>IntBuffer</tt> for recording stats data -- initialized to
     * an empty buffer until stats are actually recorded.
	 */
	protected final Buffer<Double> _buffer = new Buffer<Double>(HISTORY_LENGTH);
    
	/**
	 * int for the statistic currently being added to.
	 */
	protected volatile int _current = 0;

	/**
	 * int for the most recently stored statistic. 
	 */
	private volatile int _lastStored = 0;

	/**
	 * Variable for the total number of messages received for this 
	 * statistic.
	 */
	protected volatile double _total = 0;

	/**
	 * The total number of stats recorded.
	 */
	protected volatile int _totalStatsRecorded = 0;

	/**
	 * The maximum value ever recorded for any time period.
	 */
	protected volatile double _max = 0;

	// inherit doc comment
	public double getTotal() {
		return _total;
	}

	// inherit doc comment
	public double getAverage() {
	    if(_totalStatsRecorded == 0) return 0;
		return _total/_totalStatsRecorded;
	}

	// inherit doc comment
	public double getMax() {
		return _max;
	}

	public int getCurrent() {
		return _current;
	}
	
	
	public int getLastStored() {
		return _lastStored;
	}
	
	// inherit doc comment
	public void incrementStat() {
		_current++;
		_total++;		
	}

	// inherit doc comment
	public void addData(int data) {		
		_current += data;
		_total += data;
	}
		
	// inherit doc comment
	public Buffer<Double> getStatHistory() {
		synchronized(_buffer) {
			initializeBuffer();
			return _buffer;
		}
	}
	
	// inherit doc comment
	public void clearData() {
	    _current = 0;
	    _total = 0;
	    _totalStatsRecorded = 0;
	    _max = 0;
	    synchronized(_buffer) {
	        _buffer.clear();
	    }
	}

	// inherit doc comment
	public void storeCurrentStat() {
 		synchronized(_buffer) {
			initializeBuffer();
 			_buffer.addLast((double)_current);
 		}
		if(_current > _max) {
			_max = _current;
		}
		
		_lastStored = _current;
		_current = 0;
		_totalStatsRecorded++;
	}
    
    public void storeStats(Writer writer) throws IOException {
        writer.write(Integer.toString(getCurrent()));
        writer.write("\t");
        writer.write(Double.toString(getTotal()));
        writer.write("\t");
        writer.write(Double.toString(getAverage()));
        writer.write("\t");
        writer.write(Double.toString(getMax()));
        writer.flush();
    }

	/**
	 * Constructs the <tt>IntBuffer</tt> with 0 for all values if it is
	 * not already constructed.
	 */
	protected final void initializeBuffer() {
		if(_buffer.isEmpty()) {
			for(int i=0; i<HISTORY_LENGTH; i++) {
				_buffer.addLast(Double.NaN);
			}
		}
	}
    
	@Override
    public Object inspect() {
        Map<String,Object> ret = new HashMap<String,Object>();
        ret.put("current",_current);
        ret.put("lastStored",_lastStored);
        ret.put("max",Double.doubleToLongBits(_max));
        ret.put("total",Double.doubleToLongBits(_total));
        ret.put("recorded",_totalStatsRecorded);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream daos = new DataOutputStream(baos);
        try {
            for (Double d : _buffer) {
                if (Double.isNaN(d))
                    continue;
                daos.writeDouble(d);
            }
            daos.flush();
            ret.put("buffer",baos.toByteArray());
        } catch (IOException impossible) {
            ret.put("impossible",impossible.getMessage());
        }
        return ret;
    }
}
