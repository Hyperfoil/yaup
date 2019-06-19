package io.hyperfoil.tools.yaup;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple class that takes an input object and generates String.
 * Useful to turn java hashCodes into a shorter sequence for json
 */
public class IdGenerator<T> {

    private static final int RADIX = 36;

    private HashMap<T,String> seen;
    private AtomicInteger counter;


    public IdGenerator(){
        this(null);
    }
    public IdGenerator(String previous){
        this.seen = new HashMap<>();
        if(previous!=null){
            counter = new AtomicInteger(Integer.parseInt(previous,RADIX));
        }else{
            counter = new AtomicInteger(0);
        }
    }

    public String getId(T value){
        if(seen.containsKey(value)){
            return seen.get(value);
        }else{
            synchronized (this){
                if(seen.containsKey(value)){
                    return seen.get(value);
                }
                int val = counter.getAndIncrement();
                String rtrn = Integer.toString(val,RADIX);
                seen.put(value,rtrn);
                return rtrn;
            }
        }
    }
    public int count(){
        return counter.get();
    }
    protected void addSeen(T value,String key){
        int val = Integer.valueOf(key,RADIX);
        seen.put(value,key);
        if(val >= counter.get()){
            counter.set(val+1);
        }
    }
}
