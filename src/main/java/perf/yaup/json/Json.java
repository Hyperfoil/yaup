package perf.yaup.json;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Created by wreicher
 * A javascript object representation that can be either an Array or Object.
 */
public class Json {

    public static JSONArray toJSONArray(Json json){
        JSONArray rtrn = new JSONArray();
        if(json.isArray()){
            for(int i=0; i<json.size();i++){
                Object toAdd = json.get(i);
                if(toAdd instanceof Json){
                    Json entryJson = (Json)toAdd;
                    if(entryJson.isArray()){
                        toAdd = toJSONArray(entryJson);
                    }else{
                        toAdd = toJSONObject(entryJson);
                    }
                }
                rtrn.put(toAdd);
            }
        }
        return rtrn;
    }
    public static JSONObject toJSONObject(Json json){
        JSONObject rtrn = new JSONObject();

        if(!json.isArray){
            Queue<Json> jsonList = new LinkedList<>();
            jsonList.add(json);
            Queue<Object> objects = new LinkedList<>();
            objects.add(rtrn);
            while(!jsonList.isEmpty()){
                Json currentJson = jsonList.poll();
                Object currentObject = objects.poll();
                if(currentJson.isArray()){
                    JSONArray objectArray = (JSONArray)currentObject;
                    currentJson.forEach(entry->{
                        if(entry instanceof Json){
                            Json entryJson = (Json)entry;
                            if(entryJson.isArray()){
                                JSONArray newJsonArray = new JSONArray();
                                jsonList.add(entryJson);
                                objects.add(newJsonArray);
                                objectArray.put(newJsonArray);
                            }else{
                                JSONObject newJsonObject = new JSONObject();
                                jsonList.add(entryJson);
                                objects.add(newJsonObject);
                                objectArray.put(newJsonObject);
                            }
                        }else{
                            objectArray.put(entry);
                        }

                    });

                }else{
                    JSONObject objectJson = (JSONObject)currentObject;
                    currentJson.forEach((key,value)->{
                        if(value instanceof Json){
                            Json valueJson = (Json)value;
                            if(valueJson.isArray()){
                                JSONArray newJsonArray = new JSONArray();
                                jsonList.add(valueJson);
                                objects.add(newJsonArray);
                                objectJson.put(key.toString(),newJsonArray);
                            }else{
                                JSONObject newJsonObject = new JSONObject();
                                jsonList.add(valueJson);
                                objects.add(newJsonObject);
                                objectJson.put(key.toString(),newJsonObject);

                            }
                        }else{
                            objectJson.put(key.toString(),value);
                        }
                    });
                }
            }

        }
        return rtrn;
    }
    public static Json fromString(String json){
        Json rtrn = null;
        json = json.trim();
        if(json.startsWith("[")){
            JSONArray jsonArray = new JSONArray(json);
            rtrn = fromJSONArray(jsonArray);
        }else if (json.startsWith("{")){
            JSONObject jsonObject = new JSONObject(json);
            rtrn = fromJSONObject(jsonObject);
        }else{
            rtrn = new Json();
        }

        return rtrn;
    }
    public static Json fromJSONArray(JSONArray json){
        Json rtrn = new Json();
        for(int i=0; i<json.length(); i++){
            Object obj = json.get(i);
            if(obj instanceof JSONArray){
                rtrn.add(fromJSONArray((JSONArray)obj));
            }else if (obj instanceof JSONObject){
                rtrn.add(fromJSONObject((JSONObject)obj));
            }else{
                rtrn.add(obj);
            }
        }
        return rtrn;
    }
    public static Json fromJSONObject(JSONObject json){
        Json rtrn = new Json();

        Queue<Json> jsonList = new LinkedList<>();
        jsonList.add(rtrn);
        Queue<Object> objects = new LinkedList<>();
        objects.add(json);

        while(!jsonList.isEmpty()){
            Json currentJson = jsonList.poll();
            Object currentObject = objects.poll();

            if(currentObject instanceof JSONObject){
                JSONObject jsonObject = (JSONObject)currentObject;
                jsonObject.keySet().forEach(key->{
                    Object keyValue = jsonObject.get(key);
                    if(keyValue instanceof JSONObject || keyValue instanceof JSONArray){
                        Json newJson = new Json();
                        currentJson.add(key,newJson);

                        jsonList.add(newJson);
                        objects.add(keyValue);
                    }else{
                        currentJson.add(key,keyValue);
                    }
                });

            }else if (currentObject instanceof JSONArray){
                JSONArray jsonArray = (JSONArray)currentObject;
                for(int i=0; i<jsonArray.length(); i++){
                    Object arrayEntry = jsonArray.get(i);
                    if(arrayEntry instanceof JSONObject || arrayEntry instanceof JSONArray){
                        Json newJson = new Json();
                        currentJson.add(newJson);

                        jsonList.add(newJson);
                        objects.add(arrayEntry);
                    }else{
                        currentJson.add(arrayEntry);
                    }
                }
            }
        }
        //TODO test this
        return rtrn;
    }

    private Map<Object,Object> data;
    private boolean isArray;

    public Json(){
        this(true);
    }
    public Json(boolean isArray){
        this.data = new LinkedHashMap<>();
        this.isArray = isArray;
    }
    public String toString(int indent){
        if(isArray){
            return toJSONArray(this).toString(indent);
        }else {
            return toJSONObject(this).toString(indent);
        }
    }

    @Override
    public int hashCode(){
        return super.hashCode();
    }
    @Override
    public boolean equals(Object obj){
        if(obj !=null && obj instanceof Json){
            Json other = (Json)obj;
            Set<Object> thisKeySet = this.keySet();

            Set<Object> otherKeySet = other.keySet();

            if( thisKeySet.containsAll(otherKeySet) && otherKeySet.containsAll(thisKeySet) ){
                for(Object key : thisKeySet){
                    if(!this.get(key).equals(other.get(key))){
                        return false;
                    }
                }
                return true;
            }else{
                return false;
            }
        }else{
            return false;
        }
    }

    @Override
    public String toString(){
        StringBuilder rtrn = new StringBuilder();
        if(isArray){
            rtrn.append("[");
            for(int i=0; i<data.size(); i++){
                Object value = data.get(i);
                if(i>0){
                    rtrn.append(",");
                    //newline
                }
                rtrn.append(escape(value));
            }
            rtrn.append("]");
        }else{
            rtrn.append("{");
            boolean first = true;
            for(Object key : data.keySet()){
                Object value = data.get(key);
                if(!first){
                    rtrn.append(",");
                }
                first=false;

                rtrn.append("\"");
                rtrn.append(escape(key));
                rtrn.append("\":");
                if(value instanceof String){
                    rtrn.append("\"");
                    rtrn.append(escape(value));
                    rtrn.append("\"");
                }else{
                    rtrn.append(escape(value));
                }
            }
            rtrn.append("}");
        }
        return rtrn.toString();
    }
    private String escape(Object o){
        if(o instanceof Json){
            return ((Json)o).toString();
        }else {
            return o.toString().replaceAll("\"", "\\\\\"");
        }
    }

    public void set(Object key, Object value){
        if(key instanceof Integer){
            key = ((Number)key).intValue();
        }else{
            isArray = false;
        }
        checkKeyType(key);
        data.put(key,value);
    }
    private void checkKeyType(Object key){
        if(! (key instanceof Integer) ){
            isArray = false;
        }else{
            if(isEmpty()){
                isArray = true;
            }
        }
    }
    public int size(){
        return data.size();
    }
    public boolean has(Object key){
        return data.containsKey(key);
    }

    public void add(Object value){
        add(data.size(),value,false);
    }
    public void add(Object key,Object value){ add(key,value,false); }
    public void add(Object key,Object value,boolean forceArray){
        if(value instanceof Integer){
            value = new Long(((Integer) value).longValue());
        }
        checkKeyType(key);
        if(has(key)){
            Object existing = get(key);
            if(existing instanceof Json && ( (Json)existing).isArray()){
                Json existingJson = (Json)existing;
                existingJson.add(value);
            }else{
                Json newArray = new Json();
                newArray.add(existing);
                newArray.add(value);
                data.put(key,newArray);
            }
        } else {
            if(forceArray) {
                Json arry = new Json();
                arry.add(value);
                data.put(key, arry);
            }else{
                data.put(key,value);
            }

        }
    }

    public boolean isEmpty(){return data.size()==0;}
    public boolean isArray(){return isArray;}

    public Object get(Object key){
        return data.get(key);
    }

    public boolean getBoolean(Object key) { return getBoolean(key,false);}
    public boolean getBoolean(Object key,boolean defaultValue){
        return has(key) ? (Boolean)data.get(key) : defaultValue;
    }
    public Optional<Boolean> optBoolean(Object key){
        return Optional.ofNullable(getBoolean(key));
    }

    public String getString(Object key){
        return getString(key,null);
    }
    public String getString(Object key,String defaultValue) {
        return has(key) ? data.get(key).toString() : defaultValue;
    }
    public Optional<String> optString(String key){
        return Optional.ofNullable(getString(key));
    }

    public Json getJson(Object key){ return getJson(key,null); }
    public Json getJson(Object key,Json defaultValue){
        return has(key) ? (Json)data.get(key) : defaultValue;
    }
    public Optional<Json> optJson(Object key){
        return Optional.ofNullable(getJson(key));
    }

    public long getLong(Object key){
        return getLong(key,0);
    }
    public long getLong(Object key,long defaultValue){
        return has(key) && data.get(key) instanceof Number ? ((Number) data.get(key)).longValue() : defaultValue;
    }
    public Optional<Long> optLong(Object key){
        return Optional.ofNullable(has(key) && get(key) instanceof Long ? getLong(key) : null);
    }

    public double getDouble(Object key){
        return getDouble(key,0);
    }
    public double getDouble(Object key,double defaultValue){
        return has(key) ? (Double)data.get(key) : defaultValue;
    }
    public Optional<Double> optDouble(Object key){
        return Optional.ofNullable(has(key) && get(key) instanceof Long ? getDouble(key) : null);
    }

    public Set<Object> keySet(){return data.keySet();}

    public Stream<Map.Entry<Object,Object>> stream(){
        return data.entrySet().stream();
    }
    public void forEach(Consumer<Object> consumer){
        data.values().forEach(consumer);
    }
    public void forEach(BiConsumer<Object,Object> consumer){
        data.entrySet().forEach((entry)->consumer.accept(entry.getKey(),entry.getValue()));
    }
    public Collection<Object> values(){return data.values();}
    public Set<Object> keys(){return data.keySet();}
}
