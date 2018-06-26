package perf.yaup.json;

import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.json.JSONArray;
import org.json.JSONObject;
import perf.yaup.StringUtil;

import javax.script.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by wreicher
 * A javascript object representation that can be either an Array or Object.
 */
public class Json {

    private static class HashJson extends Json {

        public HashJson(){
            super(false);
        }
        @Override
        public void add(Object key,Object value){
            if(value==null){
                return;
            }
            if(has(key) ){
                if(get(key).equals(value)){
                    //do nothing because we already have that value
                }else {
                    if (!(get(key) instanceof HashArray)) {
                        HashArray newEntry = new HashArray();
                        newEntry.add(get(key));
                        newEntry.add(value);
                        super.set(key, newEntry);
                    } else {
                        if (get(key) instanceof Json) {
                            System.out.println("key(" + key + ") is a " + get(key).getClass().getSimpleName());
                        }
                    }
                }
            }else{
                super.add(key,value);
            }
        }
        @Override
        public void add(Object value){}
    }
    private static class HashArray extends Json {
        HashMap<String,Json> seen;
        public HashArray(){
            seen = new HashMap<>();
        }
        @Override
        public void add(Object object){
            if(object == null){
                return;
            }
            String key = object.toString();
            if (object instanceof Json){
                Json jsonObject = (Json)object;
                if(jsonObject.isArray()){

                }else{
                    StringBuilder sb = new StringBuilder();
                    Json.keyId(jsonObject,sb);
                    key = sb.toString();
                }
            }
            if(!seen.containsKey(key)){
                seen.put(key,object instanceof Json ? (Json)object : null);
                super.add(object);
            }else{
                if (object instanceof Json && seen.get(key)!=null){
                    Json.mergeStructure(seen.get(key),(Json)object);
                }
                //log filtering of seen entry?
            }

        }
        @Override
        public void set(Object key,Object value){}
        @Override
        public void add(Object key,Object value){
            add(value);
        }
    }

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
    public static Json fromBindings(Bindings bindings){
        Json rtrn = new Json();

        Queue<Json> targets = new LinkedList<>();
        Queue<Bindings> todo = new LinkedList<>();

        todo.add(bindings);
        targets.add(rtrn);

        while(!todo.isEmpty()){
            Json js = targets.remove();
            Bindings bs = todo.remove();
            boolean isArray = bindings.keySet().stream().allMatch(s->s.matches("\\d+"));

            for(String key : bs.keySet()){

                Object value = bs.get(key);
                if(value instanceof Bindings){
                    Json newTarget = new Json();
                    js.set(key,newTarget);
                    targets.add(newTarget);
                    todo.add((Bindings)value);
                }else{
                    js.set( isArray ? Integer.parseInt(key) : key,value);
                }
            }
        }
        return rtrn;
    }
    public static Json fromJs(String js){
        Json rtrn = new Json();
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        ScriptContext defaultContext = engine.getContext();
        ScriptContext context = new ScriptContext(){
            @Override
            public void setBindings(Bindings bindings, int i) {
                defaultContext.setBindings(bindings,i);
            }

            @Override
            public Bindings getBindings(int i) {
                return defaultContext.getBindings(i);
            }

            @Override
            public void setAttribute(String s, Object o, int i) {
                defaultContext.setAttribute(s,o,i);
            }

            @Override
            public Object getAttribute(String s, int i) {
                if(i!=ENGINE_SCOPE && i!=GLOBAL_SCOPE){
                    return s;
                }
                if(defaultContext.getAttribute(s,i)!=null){
                    return defaultContext.getAttribute(s,i);
                }
                return s;
            }

            @Override
            public Object removeAttribute(String s, int i) {
                return defaultContext.removeAttribute(s,i);
            }

            @Override
            public Object getAttribute(String s) {
                if(defaultContext.getAttribute(s)!=null){
                    return defaultContext.getAttribute(s);
                }

                return s;
            }

            @Override
            public int getAttributesScope(String s) {
                return 0;
            }

            @Override
            public Writer getWriter() {
                return defaultContext.getWriter();
            }

            @Override
            public Writer getErrorWriter() {
                return defaultContext.getErrorWriter();
            }

            @Override
            public void setWriter(Writer writer) {
                defaultContext.setWriter(writer);
            }

            @Override
            public void setErrorWriter(Writer writer) {
                defaultContext.setErrorWriter(writer);
            }

            @Override
            public Reader getReader() {
                return defaultContext.getReader();
            }

            @Override
            public void setReader(Reader reader) {
                defaultContext.setReader(reader);
            }

            @Override
            public List<Integer> getScopes() {
                return defaultContext.getScopes();
            }
        };
        try {
            Object foo = engine.eval("var rtrn = "+js+"; rtrn",context);

            if(foo instanceof ScriptObjectMirror){
                ScriptObjectMirror scriptObjectMirror = (ScriptObjectMirror)foo;
                rtrn = fromBindings(scriptObjectMirror);
            }

        } catch (ScriptException e) {
            e.printStackTrace();
        }

        return rtrn;
    }

    public static Json typeStructure(Json target){
        Json rtrn ;
        if(target.isArray()) {
            rtrn = new HashArray();
        }else{
            rtrn = new HashJson();
        }
        target.forEach((key,value)->{
            if(value instanceof Json){
                Json valueStructure = typeStructure((Json)value);
                rtrn.add(key,valueStructure);
            }else{
                if(value instanceof Number){
                    rtrn.add(key,"Number");
                }else {
                    rtrn.add(key, value.getClass().getSimpleName());
                }
            }
        });
        return rtrn;
    }
    private static void mergeStructure(Json to,Json from){
        if(to.isArray()){//going to be a HashArray
            if(from.isArray()){
                from.values().forEach(to::add);
            }else{
                to.add(from);
            }
        }else{
            if(from.isArray()){

            }else{
                from.forEach((key,value)->{
                    if(to.has(key)){
                        Object toValue = to.get(key);
                        if(toValue instanceof Json){
                            Json toValueJson = (Json)toValue;
                            if(toValueJson.isArray()){
                                if(value instanceof Json){
                                    Json jsonValue = (Json)value;
                                    if(jsonValue.isArray()){
                                        jsonValue.forEach(v->toValueJson.add(v));
                                    }else{
                                        //TODO not sure if we should just add as an entry
                                        toValueJson.add(value);
                                    }
                                }else {
                                    toValueJson.add(value);
                                }
                            }else{
                                if(value instanceof Json){
                                    Json fromValueJson = (Json)value;
                                    if(fromValueJson.isArray()){
                                        System.out.println("mergeStructure: what to do?\n  to["+key+"] = "+toValueJson+"\n  from["+key+"]="+value);
                                    }else{
                                        mergeStructure(toValueJson,fromValueJson);
                                    }
                                }

                            }
                        }else{//to has a value that is not a Json
                            to.add(key,value);
                        }
                    }else{
                        System.out.println("mergeStructure missing to["+key+"]");
                    }
                });
            }
        }

    }
    private static void keyId(Json target,StringBuilder sb){
        sb.append("[ ");
        target.keys().stream().sorted().forEach(key->{
            Object value = target.get(key);
            sb.append(key);
            sb.append(" ");
            if(value instanceof Json){
                keyId((Json)value,sb);
            }
        });
        sb.append("]");
    }

    private LinkedHashMap<Object,Object> data;
    private boolean isArray;

    public static List<String> dotChain(String path){
        return new ArrayList<>(
                Arrays.asList(path.split("\\.(?<!\\\\\\.)"))
                        .stream()
                        .map(s->s.replaceAll("\\\\\\.","."))
                        .collect(Collectors.toList())
        );
    }

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
        return toString().hashCode();
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
    public Json clone(){
        Json rtrn = new Json();
        for(Object key : data.keySet()){
            Object value = data.get(key);
            if(value instanceof Json){
                rtrn.set(key,((Json)value).clone());
            }else{
                rtrn.set(key,value);
            }
        }
        return rtrn;
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
                if(value instanceof String){
                    rtrn.append("\"");
                    rtrn.append(escape(value));
                    rtrn.append("\"");
                }else{
                    rtrn.append(escape(value));
                }
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
        } else {
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

    public Json chain(String...keys){
        return chain(Arrays.asList(keys));
    }
    public Json chain(List<String> keys){
        Json rtrn = this;
        for(int i=0; i<keys.size(); i++){
            String key = keys.get(i);
            if(!key.isEmpty()){
                if(!rtrn.has(key)){
                    rtrn.set(key,new Json());
                }
                if( !(rtrn.get(key) instanceof Json) ){
                    Object existing = rtrn.get(key);
                    rtrn.set(key,new Json());
                    rtrn.getJson(key).add(existing);
                }
                rtrn = rtrn.getJson(key);
            }
        }
        return rtrn;
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
    public Optional<String> optString(Object key){
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
