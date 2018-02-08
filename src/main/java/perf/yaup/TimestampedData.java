package perf.yaup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by wreicher
 */
public class TimestampedData<T> {

    private TreeSet<Long> timestamps;
    private LinkedHashSet<String> names;
    private HashMap<String,HashMap<Long,T>> data;

    public TimestampedData(){
        data = new HashMap<>();
        names = new LinkedHashSet<>();
        timestamps = new TreeSet<>();
    }

    public void add(String name,long timestamp,T value){
        if(name.equals("[B")){
            System.out.println(name+"->"+timestamp+"="+value);
        }
        names.add(name);
        timestamps.add(timestamp);

        if(!data.containsKey(name)){
            data.put(name,new HashMap<>());
        }

        data.get(name).put(timestamp,value);
    }
    public T get(long timestamp,String name){
        if(data.containsKey(name)){
            HashMap<Long,T> map = data.get(name);
            if(map.containsKey(timestamp)){
                return map.get(timestamp);
            }
        }
        return null;
    }
    public long firstTimestamp(){
        return timestamps.first();
    }
    public long lastTimestamp(){
        return timestamps.last();
    }
    public List<Long> timestamps(){
        return Collections.unmodifiableList(new ArrayList<Long>(timestamps));
    }
    public List<String> names(){
        return Collections.unmodifiableList(new ArrayList<String>(names));
    }
    public long nextTimestamp(long timestamp){
        if(timestamp >= lastTimestamp()){
            return Long.MAX_VALUE;
        }
        return timestamps.higher(timestamp);
    }
    public long prevTimestamp(long timestamp){
        if(timestamp <= firstTimestamp()){
            return Long.MIN_VALUE;
        }
        return timestamps.lower(timestamp);
    }

}
