package io.hyperfoil.tools.yaup;

import org.json.JSONArray;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 */
public class Indexer<T> implements Serializable{


    private AtomicInteger counter = new AtomicInteger(0);

    private HashMap<Integer,T> values;
    private HashMap<T,Integer> seen;

    private T defaultValue;
    public Indexer(){
        this(null);
    }
    public Indexer(T defaultValue){
        values = new HashMap<>();
        seen = new HashMap<>();
        this.defaultValue = defaultValue;
    }
    public int get(T value){
        if(seen.containsKey(value)){
            return seen.get(value);
        }else{
            return -1;
        }
    }
    public T get(int index){
        if(values.containsKey(index)){
            return values.get(index);
        }else{
            return defaultValue;
        }
    }
    public int add(T value){
        if(seen.containsKey(value)){
            return seen.get(value);
        }
        synchronized (this){//synchronize on seen to avoid duplicates
            if(seen.containsKey(value)){
                return seen.get(value);
            }
            int newIndex = counter.getAndIncrement();
            seen.put(value,newIndex);
            values.put(newIndex,value);
            return newIndex;
        }
    }

    public List<T> getIndexedList(){
        ArrayList<T> rtrn = new ArrayList<>();
        int size = values.size();
        for(int i=0; i<size; i++){
            rtrn.add(values.get(i));
        }
        return Collections.unmodifiableList(rtrn);
    }
    public boolean contains(T value){
        return seen.containsKey(value);
    }
    public int size(){
        return values.size();
    }
    public Set<Integer> getIndexSet() { return Collections.unmodifiableSet(values.keySet()); }

    public void writeObject(ObjectOutputStream stream)
            throws IOException {
        stream.writeObject(defaultValue);
        stream.writeObject(getIndexedList());
    }
    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        defaultValue = (T)stream.readObject();
        List<T> indexedList = (List<T>) stream.readObject();
        for(int i=0; i<indexedList.size(); i++){
            T value = indexedList.get(i);
            this.values.put(i,value);
            this.seen.put(value,i);
        }
        counter.set(indexedList.size());
    }
    private void readObjectNoData()
            throws ObjectStreamException {

    }

    public static <T> Indexer<T> fromList(List<T> list){
        Indexer<T> rtrn = new Indexer<T>();
        for(int i=0; i<list.size(); i++){
            rtrn.add(list.get(i));
        }
        return rtrn;
    }
    public static Indexer<String> fromJSONArray(JSONArray list){
        Indexer<String> rtrn = new Indexer<>();
        for(int i=0; i<list.length(); i++){
            rtrn.add(list.getString(i));
        }
        return rtrn;
    }

    public void forEach(Consumer<T> consumer){
        seen.keySet().forEach(consumer);
    }
    public void forEach(BiConsumer<Integer,T> consumer){
        values.entrySet().forEach((entry)-> consumer.accept(entry.getKey(),entry.getValue()));
    }

}
