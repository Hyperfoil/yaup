package perf.yaup;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by wreicher
 */
public class Counters<T> implements Serializable{

    private ConcurrentHashMap<T, LongAdder> counts;
    private LongAdder sum;
    public Counters() {
        counts = new ConcurrentHashMap<>();sum = new LongAdder();
    }
    public void add(T t) {
        add(t,1);
    }
    public void add(T t,long amount) {
        counts.computeIfAbsent(t,(v)->new LongAdder());
        counts.get(t).add(amount);
        sum.add(amount);
    }
    public void clear(){
        sum.reset();
        counts.clear();
    }

    public void forEach(Consumer<T> consumer){
        counts.keySet().forEach(consumer);
    }
    public void forEach(BiConsumer<T,Long> consumer){
        counts.forEach((t,a)->{
            consumer.accept(t,a.longValue());
        });
    }

    public boolean contains(T t) {
        return counts.containsKey(t);
    }

    public long count(T t) {
        if(contains(t)) {
            return counts.get(t).longValue();
        } else {
            return 0;
        }
    }
    public boolean isEmpty(){return counts.isEmpty();}
    public long sum(){return sum.longValue();}
    public int size(){return counts.size();}
    public List<T> entries(){
        return Arrays.asList(((T[]) counts.keySet().toArray()));

    }
}
