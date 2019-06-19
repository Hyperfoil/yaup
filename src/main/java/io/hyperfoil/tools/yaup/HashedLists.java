package io.hyperfoil.tools.yaup;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Created by wreicher
 */
public class HashedLists<K,V> implements Serializable{
    private LinkedHashMap<K,List<V>> sets;
    private transient List<V> empty;
    private final Function<K,List<V>> supplier;
    public HashedLists(){
        sets = new LinkedHashMap<>();
        empty = Collections.unmodifiableList( new ArrayList<>() );
        supplier = (k)->new ArrayList<V>();
    }

    public void put(K name,V value){
        sets.computeIfAbsent(name, supplier).add(value);
    }
    public void putFirst(K name,V value){
        sets.computeIfAbsent(name, supplier).add(0,value);
    }
    public void putAll(K name,Collection<V> values){
        sets.computeIfAbsent(name, supplier).addAll(values);
    }
    public void putAllFirst(K name,Collection<V> values){
        sets.computeIfAbsent(name, supplier).addAll(0,values);
    }
    public List<V> get(K name){
        if(sets.containsKey(name)){
            return Collections.unmodifiableList(sets.get(name));
        } else {
            return empty;
        }
    }
    public boolean isEmpty(){return sets.isEmpty();}
    public boolean containsKey(K key){
        return sets.containsKey(key);
    }
    public int size(){return sets.size();}

    public Set<K> keys(){
        return Collections.unmodifiableSet(sets.keySet());
    }
    public Collection<List<V>> values(){return sets.values();}

    public void forEach(BiConsumer<K,List<V>> consumer){
        sets.forEach(consumer);
    }
    public Stream<Map.Entry<K, List<V>>> stream(){return sets.entrySet().stream();}

    public void clear(){
        sets.clear();
    }
}
