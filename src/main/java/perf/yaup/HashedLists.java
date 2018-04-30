package perf.yaup;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Created by wreicher
 */
public class HashedLists<K,V> implements Serializable{
    private LinkedHashMap<K,List<V>> sets;
    private transient List<V> empty;
    public HashedLists(){
        sets = new LinkedHashMap<>();
        empty = Collections.unmodifiableList( new ArrayList<>() );
    }

    public void put(K name,V value){

        if (!sets.containsKey(name)) {
            synchronized (sets) {
                if (!sets.containsKey(name)) {
                    sets.put(name, new ArrayList<>());
                }
            }
        }

        sets.get(name).add(value);
    }
    public void putFirst(K name,V value){
        if(!sets.containsKey(name)){
            sets.put(name,new ArrayList<>());
        }
        sets.get(name).add(0,value);
    }
    public void putAll(K name,Collection<V> values){
        if(!sets.containsKey(name)){
            sets.put(name,new ArrayList<V>());
        }
        sets.get(name).addAll(values);
    }
    public void putAllFirst(K name,Collection<V> values){
        if(!sets.containsKey(name)){
            sets.put(name,new ArrayList<V>());
        }
        sets.get(name).addAll(0,values);
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
