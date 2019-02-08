package perf.yaup;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by wreicher
 */
public class HashedSets<K,V> implements Serializable{

    private Map<K,HashSet<V>> sets;
    private Set<V> empty;
    private final boolean linked;
    private final Function<K,HashSet<V>> supplier;
    public HashedSets() {
        this(true);
    }
    public HashedSets(boolean linked){
        sets = new ConcurrentHashMap<>();
        empty = Collections.unmodifiableSet( new HashSet<>() );
        this.linked = linked;
        this.supplier = (k) -> linked ? new LinkedHashSet<V>() : new HashSet<V>();
    }
    public void put(K name, V value){
        sets.computeIfAbsent(name, supplier).add(value);
    }
    public void putAll(K name, Collection<V> values){
        sets.computeIfAbsent(name, supplier).addAll(values);
    }
    public Set<V> get(K key){
        if(sets.containsKey(key)){
            return Collections.unmodifiableSet(sets.get(key));
        } else {
            return empty;
        }
    }
    public boolean has(K key){
        return sets.containsKey(key);
    }
    public Set<K> keys(){
        return Collections.unmodifiableSet(sets.keySet());
    }
    public int size(){return sets.size();}
    public boolean isEmpty(){return sets.isEmpty();}

    public void writeObject(ObjectOutputStream stream)
            throws IOException {
        stream.writeObject(empty);
        stream.writeObject(sets);
    }
    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        empty = (Set<V>)stream.readObject();
        sets = (HashMap<K,HashSet<V>>) stream.readObject();
    }
    private void readObjectNoData()
            throws ObjectStreamException {

    }
    public void clear(){
        sets.clear();
    }
    public void forEach(BiConsumer<K,Set<V>> consumer){
        sets.forEach(consumer);
    }

    public Stream<Map.Entry<K, HashSet<V>>> stream(){return sets.entrySet().stream();}

    public Iterator<Map.Entry<K,HashSet<V>>> iterator(){return sets.entrySet().iterator();}
}
