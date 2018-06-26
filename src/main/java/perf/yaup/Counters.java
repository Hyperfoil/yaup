package perf.yaup;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by wreicher
 */
public class Counters<T> implements Serializable{

    private ConcurrentHashMap<T,AtomicInteger> counts;
    private AtomicInteger sum;
    public Counters() {
        counts = new ConcurrentHashMap<>();sum = new AtomicInteger(0);
    }
    public void add(T t) {
        add(t,1);
    }
    public void add(T t,int amount) {
        if(!contains(t)) {
            counts.put(t,new AtomicInteger(0));
        }
        counts.get(t).addAndGet(amount);
        sum.addAndGet(amount);
    }
    public void clear(){
        sum.set(0);
        counts.clear();
    }

    public void forEach(Consumer<T> consumer){
        counts.keySet().forEach(consumer);
    }
    public void forEach(BiConsumer<T,Integer> consumer){
        counts.forEach((t,a)->{
            consumer.accept(t,a.get());
        });
    }

    public boolean contains(T t) {
        return counts.containsKey(t);
    }

    public int count(T t) {
        if(contains(t)) {
            return counts.get(t).get();
        } else {
            return 0;
        }
    }
    public boolean isEmpty(){return counts.isEmpty();}
    public int sum(){return sum.get();}
    public int size(){return counts.size();}
    public List<T> entries(){
        return Arrays.asList(((T[]) counts.keySet().toArray()));

    }
}
