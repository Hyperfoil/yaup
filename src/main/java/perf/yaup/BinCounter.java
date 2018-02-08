package perf.yaup;

import java.util.*;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by wreicher
 * Creates bins with the specified cutoffs (plus one for overflow) and counts the number of objects that fall into each bin
 */
public class BinCounter {

    private ArrayList<LongAdder> counts = new ArrayList<>();
    private Long bins[];
    public BinCounter(Long...bins){
        if(bins != null && bins.length > 0){
            for(long bin : bins){
                counts.add(new LongAdder());
            }
        }
        counts.add(new LongAdder());//overflow adder
        this.bins = bins;
    }
    public void add(long value){
        int index = findBin(value);
        counts.get(index).increment();
    }
    public long count(Long value){
        int index = findBin(value);
        return counts.get(index).longValue();
    }
    public long getOverflow(Long value){
        return counts.get(bins.length).longValue();
    }

    /**
     * Returns all the bin limits WITHOUT the overflow because it doesn't have a limit
     * (the effective limit is Long.MAX_VALUE but we don't return that)
     * @return
     */
    public List<Long> getBins(){
        List<Long> rtrn = Arrays.asList(bins);
        return rtrn;
    }
    private int findBin(Long value){
        return Arrays.binarySearch(bins,value);
    }
}
