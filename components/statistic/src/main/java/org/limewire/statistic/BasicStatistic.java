package org.limewire.statistic;


/**
 * Abstract base class that records basic statistics. 
 * <p>
 * A sample, aka a cycle of data, is all the data collected between calls to 
 * {@link #storeCurrentStat()}. 
 * Therefore, the {@link #getMax()} is the largest sample and the 
 * {@link #getAverage()} is the total size / the number of samples.
 * <p>
 * An example of using <code>BasicStatistic</code>:
 * <pre>
    class Stats extends BasicStatistic{}       
    Statistic s = new Stats();

    s.addData(1);
    s.storeCurrentStat();
    
    s.addData(2);
    s.storeCurrentStat();
    
    s.addData(3);
    s.storeCurrentStat();
    
    for(int i = 0; i < 4; i++)
        s.incrementStat();
    s.storeCurrentStat();
    
    for(int i = 0; i < 5; i++)
        s.incrementStat();
    s.storeCurrentStat();
            
    System.out.println("Total: " + s.getTotal());
    System.out.println("Max: " + s.getMax());
    System.out.println("Average: " + s.getAverage());

    Output:
        Total: 15.0
        Max: 5.0
        Average: 3.0
 * </pre>
 */
public abstract class BasicStatistic extends AbstractStatistic {

	protected BasicStatistic(StatisticAccumulator statisticAccumulator) {
		statisticAccumulator.addBasicStatistic(this);
	}
}
