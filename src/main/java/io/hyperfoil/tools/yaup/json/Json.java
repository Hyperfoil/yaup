package io.hyperfoil.tools.yaup.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;
import io.hyperfoil.tools.yaup.file.FileUtility;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;

/**
 * Created by wreicher
 * A javascript object representation that can be either an Array or Object.
 */
public class Json {

    public static class MapBuilder {
        private final Json json = new Json();

        public MapBuilder set(String key,Object value){
            json.set(key,value);
            return this;
        }
        public MapBuilder add(String key,Object value){
            json.add(key,value);
            return this;
        }
        public Json build(){
            return json;
        }
    }
    public static class ArrayBuilder {
        private final Json json = new Json();

        public ArrayBuilder add(Object obj){
            json.add(obj);
            return this;
        }
        public Json build(){
            return json;
        }
    }
    public static MapBuilder map(){
        return new MapBuilder();
    }
    public static ArrayBuilder array(){
        return new ArrayBuilder();
    }

    public static class HashJson extends Json {

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
                    if("integer".equals(value) && "number".equals(get(key))){
                        //do not add it
                    }else if ("number".equals(value) && "integer".equals(get(key))){
                        super.set(key,value);//replace integer with more general integer
                    }else if (!(get(key) instanceof HashArray)) {
                        HashArray newEntry = new HashArray();
                        newEntry.add(get(key));
                        newEntry.add(value);
                        super.set(key, newEntry);
                    } else {
                        if (get(key) instanceof Json) {
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
    public static class HashArray extends Json {
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

    public static Map<Object,Object> toObjectMap(Json json){
        Map<Object,Object> rtrn = new LinkedHashMap<>();
        json.forEach((k,v)->{
            if(v instanceof Json){
                rtrn.put(k,toObjectMap((Json)v));
            }else{//scalar
                rtrn.put(k,v);
            }
        });
        return rtrn;
    }
    private static Object toObject(Json json){
        if(json.isArray()){
            List<Object> rtrn = new LinkedList<>();
            json.forEach((entry)->{
                if(entry instanceof Json){
                    ((LinkedList) rtrn).add(toObject((Json)entry));
                }else{
                    ((LinkedList) rtrn).add(entry);
                }
            });
            return rtrn;
        }else{
             return toObjectMap(json);
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


    public static interface JsonAction {

        void accept(Json target,String key,Object value);
    }
    public static JsonAction ADD_ACTION = (target,key,value)->{
        if(key == null || key.isEmpty()){
            if( value instanceof Json && !((Json)value).isArray() && !target.isArray()){
                ((Json)value).forEach((k,v)->{
                    target.add(k,v,true);
                });

            }else{
                //TODO how do we handle adding to object without a key?
            }
        }else{
            target.add(key,value,true);
            //TODO test changing from below to code to add wth forceArray
//            if(!target.has(key)) {
//                target.set(key, new Json());
//            }
//            target.getJson(key).add(value);
        }
    };
    public static JsonAction SET_ACTION = (target,key,value)->{
        if(key == null || key.isEmpty()){
            if( value instanceof Json && !((Json)value).isArray() && !target.isArray()){
                target.merge(((Json)value),true);
            }
        }
        target.set(key,value);
    };
    public static JsonAction MERGE_ACTION = (target,key,value)->{
        if(key == null || key.isEmpty()){
            if(!target.isArray()){
                if(value instanceof Json){
                    Json jsonValue = (Json)value;
                    if(!jsonValue.isArray()){
                        target.merge(jsonValue,false);
                    }else{

                    }
                }else{

                }
            }else{

            }
        }else if(!target.has(key)){
            target.set(key,value);
        }else{
            if(target.get(key) instanceof Json){
                Json existing = target.getJson(key);
                if(existing.isArray()){
                    if(value instanceof Json && ((Json)value).isArray()){
                        ((Json)value).forEach(v->existing.add(v));
                    }else{
                        existing.add(value);
                    }
                }else{
                    if(value instanceof Json){
                        Json valueJson = (Json)value;
                        if(valueJson.isArray()) {
                            valueJson.add(existing);
                            target.set(key,valueJson);
                        }else{
                            existing.merge((Json) value, false);
                        }
                    }else{//key already exissts, points to an object, and we want to add some random value into it? this is probably by mistake
                        System.out.println("MERGE_ACTION unexpected value type for existing json object\nkey\n"+key+"\nvalue\n"+value+"\nexisting\n"+existing);
                        existing.add(value);
                    }
                }
            }else{
                Json newArray = new Json(true);
                newArray.add(target.get(key));
                newArray.add(value);
                target.set(key,newArray);
            }
        }
    };

    public static void chainAct(Json target,String prefix,Object value,JsonAction action){
        List<String> chain = toChain(prefix);
        String key = chain.remove(chain.size()-1).replaceAll("\\\\\\.", ".");
        for(String id : chain){
            id = id.replaceAll("\\\\\\.", ".");
            if (!id.isEmpty()) {
                if (!target.has(id)) {
                    synchronized (target) {
                        if (!target.has(id)) {
                            target.set(id, new Json());
                        }
                    }
                }
                if (!(target.get(id) instanceof Json)) {//this should never happen for our use case
                    synchronized (target) {
                        if (!(target.get(id) instanceof Json)) {
                            Object existing = target.get(id);
                            target.set(id, new Json());
                            target.getJson(id).add(existing);
                        }
                    }
                }
                target = target.getJson(id);
            }
        }
        action.accept(target,key,value);
    }
    public static void chainAdd(Json target,String prefix,Object value){
        chainAct(target,prefix,value,ADD_ACTION);
    }
    public static void chainMerge(Json target,String prefix,Object value){
        chainAct(target,prefix,value,MERGE_ACTION);
    }
    public static void chainSet(Json target,String prefix,Object value){
        chainAct(target,prefix,value,SET_ACTION);
    }

    public static Json fromYamlFile(String path){
        try {
            String content = Files.lines(Paths.get(path)).collect(Collectors.joining("\n"));
            return fromYaml(content);
        } catch (IOException e) {
            e.printStackTrace();
            //TODO prpertly handle IOE from fromYamlFile
        }
        return new Json();
    }
    public static Json fromYaml(String yamlContent){
        Json rtrn = new Json();
        Yaml yaml = new Yaml();
        List<Object> loaded = StreamSupport.stream(yaml.loadAll(yamlContent).spliterator(), false)
           .collect(Collectors.toList());
        if(loaded.size() == 1){
            if(loaded.get(0) instanceof Map){
                rtrn = fromMap((Map)loaded.get(0));
            }else if (loaded.get(0) instanceof Collection){
                rtrn = fromCollection((Collection)loaded.get(0));
            }else{
                //TODO how do we handle the error
            }
        }else{
            Json ref = rtrn;
            loaded.forEach(entry->{
                if(entry instanceof Map){
                    ref.add(fromMap((Map)entry));
                }else if (entry instanceof Collection){
                    ref.add(fromCollection((Collection)entry));
                }
            });
        }
        return rtrn;
    }

    public static Json fromMap(Map map){
        Json rtrn = new Json(false);

        map.forEach((key,value)->{
            if(value instanceof Map){
                rtrn.set(key,fromMap((Map)value));
            }else if (value instanceof Collection){
                rtrn.set(key,fromCollection((Collection)value));
            }else{
                rtrn.set(key,value);
            }
        });
        return rtrn;
    }
    public static String prettyPrintWithJackson(JsonNode node, int indent){
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        DefaultPrettyPrinter.Indenter indenter =
           new DefaultIndenter(String.format("%"+indent+"s",""), DefaultIndenter.SYS_LF);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        try {
            String json = mapper.writer(printer).writeValueAsString(node);
            return json;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }
    public static Json fromCollection(Collection collection){
        Json rtrn = new Json(true);
        collection.forEach(value->{
            if(value instanceof Map){
                rtrn.add(fromMap((Map)value));
            }else if (value instanceof Collection){
                rtrn.add(fromCollection((Collection)value));
            }else{
                rtrn.add(value);
            }
        });
        return rtrn;
    }
    public static Json fromJacksonString(String content){
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(content);
            return fromJsonNode(node);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return new Json(false);
    }
    public static Json fromJacksonFile(String path){
        ObjectMapper mapper = new ObjectMapper();
        try(FileReader reader = new FileReader(path)){
            JsonNode node = mapper.readTree(reader);
            return fromJsonNode(node);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Json(false);
    }
    private static Object convertJsonNode(JsonNode node){
        if(node.isObject() || node.isArray()){
            return fromJsonNode(node);
        }else if (node.isDouble() || node.isFloat()){
            return node.doubleValue();
        }else if (node.isLong() || node.isInt()){
            return node.longValue();
        }else if (node.isNull()){
            //TODO how do we handle null node
            return "";
        }else { // string
            return node.textValue();
        }
    }
    public static JsonNode toJsonNode(Json json){
        JsonNodeFactory factory = JsonNodeFactory.instance;
        ContainerNode rtrn;
        if(json.isArray()){
            ArrayNode arrayNode = new ArrayNode(factory);
            rtrn = arrayNode;
            json.forEach(value->{
                if(value instanceof Json){
                    value = toJsonNode((Json)value);
                    arrayNode.add((JsonNode)value);
                }else {
                    ObjectMapper mapper = new ObjectMapper();
                    arrayNode.add(mapper.convertValue(value, JsonNode.class));
                }
            });
        }else{
            ObjectNode objectNode = new ObjectNode(factory);
            rtrn = objectNode;
            json.forEach((key,value)->{
                if(value instanceof Json){
                    objectNode.set(key.toString(),toJsonNode((Json)value));
                }else{
                    ObjectMapper mapper = new ObjectMapper();
                    objectNode.set(key.toString(),mapper.convertValue(value,JsonNode.class));
                }
            });
        }
        return rtrn;
    }
    public static Json fromJsonNode(JsonNode node){
        Json rtrn = new Json();
        if(node.isArray()){
            node.fields().forEachRemaining((entry)->{
                JsonNode value = entry.getValue();
                rtrn.add(convertJsonNode(value));
            });
        }else if (node.isObject()){
            node.fields().forEachRemaining((entry)->{
                JsonNode value = entry.getValue();
                rtrn.set(entry.getKey(),convertJsonNode(value));
            });
        }
        return rtrn;
    }
    public static Json fromFile(String path){
        String content = FileUtility.readFile(path);
        return Json.fromString(content);
    }
    public static Json fromThrowable(Throwable e){
        Json rtrn = new Json();
        fromThrowable(e,rtrn);
        return rtrn;
    }
    public static void fromThrowable(Throwable e,Json target){
        if(e!=null){
            target.set("class",e.getClass().toString());
            if(e.getMessage()!=null){
                target.set("message",e.getMessage());
            }
            if(e.getStackTrace()!=null){
                target.set("stack",new Json());
                Arrays.asList(e.getStackTrace()).forEach(ste->{
                    Json frame = new Json();
                    frame.set("class",ste.getClassName());
                    frame.set("method",ste.getMethodName());
                    frame.set("line",ste.getLineNumber());
                    frame.set("file",ste.getFileName());
                    target.getJson("stack").add(frame);
                });
            }
            if(e.getCause()!=null){
                Json causedBy = new Json();
                target.set("causedBy",causedBy);
                fromThrowable(e.getCause(),causedBy);
            }
        }
    }
    public static Json fromGraalvm(Value value){
        return ValueConverter.convertMapping(value);
    }
    public static Json fromString(String json){
        return fromString(json,null);
    }
    public static Json fromString(String json,Json defaultValue){
        return fromString(json,defaultValue,false);
    }
    public static Json fromString(String json,Json defaultValue,boolean debug){
        Json rtrn = defaultValue;
        if(json == null){
            return rtrn;
        }
        json = json.trim();
        try {
            if (json.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(json);
                rtrn = fromJSONArray(jsonArray);
            } else if (json.startsWith("{")) {
                JSONObject jsonObject = new JSONObject(json);
                rtrn = fromJSONObject(jsonObject);
            } else {
                rtrn = defaultValue;
            }
        }catch(JSONException e){
            if(debug){
                e.printStackTrace();
            }
            //log failed to parse json
        }
        return rtrn;
    }


    public static Optional<Json> optional(String json){
        if(isJsonLike(json)){
            try{
                Json created = Json.fromString(json);
                return Optional.ofNullable(created);
            }catch (RuntimeException e){//RuntimeExceptions from parsing the json
                return Optional.ofNullable(null);
            }
        }else{
            return Optional.ofNullable(null);
        }
    }

    /**
     * A very simple check to see if the string looks like it could be json.
     * Currently just checks that for [...] or {...}
     * @param input string being tested
     * @return true if string being tested appears to be json, otherwise false
     */
    public static boolean isJsonLike(String input){
        if(input == null || input.trim().isEmpty()){
            return false;
        }

        char firstChar = input.trim().charAt(0);
        char lastChar = input.trim().charAt(input.trim().length()-1);
        switch (firstChar){
            case '{':
                return lastChar=='}';
            case '[':
                return lastChar==']';
            default:
                return false;
        }
    }
    public static Json fromJSONArray(JSONArray json){
        Json rtrn = new Json(true);
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
        Json rtrn = new Json(false);

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
                    if(keyValue instanceof JSONObject || keyValue instanceof JSONArray) {
                        Json newJson = new Json(keyValue instanceof JSONArray);
                        currentJson.add(key, newJson);

                        jsonList.add(newJson);
                        objects.add(keyValue);
                    } else if (keyValue == JSONObject.NULL) {
                        currentJson.add(key, null);
                    }else{
                        currentJson.add(key,keyValue);
                    }
                });

            }else if (currentObject instanceof JSONArray){
                JSONArray jsonArray = (JSONArray)currentObject;
                for(int i=0; i<jsonArray.length(); i++){
                    Object arrayEntry = jsonArray.get(i);
                    if(arrayEntry instanceof JSONObject || arrayEntry instanceof JSONArray){
                        Json newJson = new Json(arrayEntry instanceof JSONArray);
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
            boolean isArray = bs.keySet().stream().allMatch(s->s.matches("\\d+"));
            for(String key : bs.keySet()){
                Object value = bs.get(key);
                if(value instanceof Bindings){
                    Json newTarget = new Json();
                    js.set(isArray ? Integer.parseInt(key) : key,newTarget);
                    targets.add(newTarget);
                    todo.add((Bindings)value);
                }else{
                    js.set( isArray ? Integer.parseInt(key) : key,value);
                }
            }
        }
        return rtrn;
    }
    public static Json fromJbossCli(String output){
        boolean wrap = false;
        StringBuilder sb = new StringBuilder();
        Matcher address = Pattern.compile("(?<spacing>\\s*)\\(\"(?<key>[^\"]+)\"\\s+=>\\s+\"(?<value>[^\"]+)\"\\)(?<suffix>[,]?)").matcher("");
        Matcher L = Pattern.compile("(?<spacing>\\s*)\"(?<key>[^\"]+)\"\\s+=>\\s+(?<value>\\d+)L(?<suffix>[,]?)").matcher("");
        //build a Json compliant string line by line
        String previousLine = "";
        for(String line : output.split("\r?\n")) {
            if (address.reset(line).matches()) {
                sb.append(address.group("spacing"));
                sb.append("{\"key\":\"");
                sb.append(address.group("key"));
                sb.append("\",\"value\":\"");
                sb.append(address.group("value"));
                sb.append("\"}");
                sb.append(address.group("suffix"));
            } else if (L.reset(line).matches()){
                sb.append(L.group("spacing"));
                sb.append("\"");
                sb.append(L.group("key"));
                sb.append("\":");
                sb.append(L.group("value"));
                sb.append(L.group("suffix"));
            }else {
              if(line.startsWith("{")){//multiple cli method calls do not have , separation
                  if ( previousLine.endsWith("}")) {
                      sb.append(",");
                      wrap = true;
                  }
              }
              sb.append(line.replaceAll("=> ",":"));
            }
            sb.append(System.lineSeparator());
            previousLine = line;
        }
        String toParse = (wrap?"[":"")+sb.toString()+(wrap?"]":"");
        return Json.fromString(toParse);
    }
    public static Json fromJs(String...js){
        return fromJs(Arrays.asList(js).stream().collect(Collectors.joining("\n")));
    }
    public static Json fromJs(String js) {
        Json rtrn = new Json();
        try (Context context = Context.newBuilder("js").allowAllAccess(true).allowHostAccess(true).build()) {
            context.enter();
            try {
               Value obj = context.eval("js", "const walker = (parent,key,value)=>{\n" +
                   "if( value === undefined){ parent[key]=null }\n" +
                   "else if (value === null){ parent[key]=null }\n" +
                   "else if( Array.isArray(value) ){ value.forEach((entry,index)=>walker(value,index,entry)) }\n" +
                   "else if (typeof value === 'function'){ parent[key]=value.toString() }\n" +
                   "else if (value instanceof Date){ parent[key]=value.toISOString() }\n" +
                   "else if (typeof value === 'object'){ Object.keys(value).forEach(k=>walker(value,k,value[k])) }\n" +
                   "else{ }\n" +
                   "};\n" +
                   "const walk = (obj)=>{\n" +
                   "  if( Array.isArray(obj) ){ obj.forEach((entry,index)=>walker(obj,index,entry)) }\n" +
                   "  else{ Object.keys(obj).forEach(k=>{ walker(obj,k,obj[k]); return obj; }) }\n" +
                   "  return obj;\n" +
                   "};\n" +
                   "walk( (()=>(" + js + "))() )");
                Object converted = ValueConverter.convert(obj);
                if(converted instanceof Json){
                    rtrn = (Json)converted;
                }
            } finally {
                context.leave();
            }
        }catch(IllegalArgumentException e){//com.oracle.truffle.polyglot.PolyglotIllegalArgumentException
            rtrn = fromJsScriptEngine(js);
        }
        return rtrn;
    }
    public static Json fromJsScriptEngine(String js){
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

    /**
     * load a Json object as a TypeStructure json object
     * @param typeStructure the json type structure
     * @return typeStructure converted to a json object that will enforce type structure
     */
    public static Json loadTypeStructure(Json typeStructure){
        Json rtrn;
        if(typeStructure.isArray()){
            rtrn = new HashArray();
        }else{
            rtrn = new HashJson();
        }
        typeStructure.forEach((key,value)->{
            if (value instanceof Json){
                Json valueAsTypeStructure = loadTypeStructure((Json)value);
                rtrn.add(key,valueAsTypeStructure);
            }else{
                rtrn.add(key,value);//trust the value is already a string presentation of the correct values
            }
        });
        return rtrn;
    }

    public static Json typeStructure(Json target) {
        Json rtrn;
        if (target.isArray()) {
            rtrn = new HashArray();
        } else {
            rtrn = new HashJson();
        }
        target.forEach((key, value) -> {
            if (value == null) {
                if (rtrn.has(key)) {
                    rtrn.add(key, "null");
                } else {
                    rtrn.set(key, "null");
                }
            } else if (value instanceof Json) {
                Json valueStructure = typeStructure((Json) value);
                rtrn.add(key, valueStructure);
            } else if (value instanceof Number) {
                if (value instanceof Long || value instanceof Integer) {
                    if (rtrn.has(key) && "number".equals(rtrn.get(key))) {
                        //do not add integer if already a number
                    } else {
                        rtrn.add(key, "integer");
                    }
                } else {
                    if (rtrn.has(key) && "integer".equals(rtrn.get(key))) {
                        rtrn.set(key, "number");
                    } else {
                        rtrn.add(key, "number");
                    }
                }
            } else {
                rtrn.add(key, value.getClass().getSimpleName().toLowerCase());
            }
        });
        return rtrn;
    }

    private static void mergeStructure(Json to,Json from){
        if (to.isArray()) {//going to be a HashArray
            if (from.isArray()) {
                from.values().forEach(to::add);
            } else {
                to.add(from);
            }
        } else {
            if (from.isArray()) {
                return;
            }
            from.forEach((key, value) -> {
                if (!to.has(key)) {
                    to.set(key, value);
                    return;
                }
                Object toValue = to.get(key);
                if (!(toValue instanceof Json)) {//to has a value that is not a Json
                    to.add(key, value);
                    return;
                }
                Json toValueJson = (Json) toValue;
                if (toValueJson.isArray()) {
                    if (value instanceof Json) {
                        Json jsonValue = (Json) value;
                        if (jsonValue.isArray()) {
                            jsonValue.forEach((Consumer<Object>) toValueJson::add);
                        } else {
                            //TODO not sure if we should just add as an entry
                            toValueJson.add(value);
                        }
                    } else {
                        toValueJson.add(value);
                    }
                } else {
                    if (value instanceof Json) {
                        Json fromValueJson = (Json) value;
                        if (fromValueJson.isArray()) {
                            System.out.println("mergeStructure: what to do?\n  to[" + key + "] = " + toValueJson + "\n  from[" + key + "]=" + value);
                        } else {
                            mergeStructure(toValueJson, fromValueJson);
                        }
                    }
                }
            });
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
    private static Configuration yaup = Configuration.builder().jsonProvider(new YaupJsonProvider()).options(Option.SUPPRESS_EXCEPTIONS,Option.DEFAULT_PATH_LEAF_TO_NULL).build();

    public static Object find(Json input,String jsonPath){
        return find(input,jsonPath,null);
    }
    public static Object find(Json input,String jsonPath,Object defaultValue){
        //TODO cache this and make an immutable json immutable?
        ReadContext ctx = JsonPath.parse(input,yaup);
        Object results = null;
        try {
            JsonPath path = JsonPath.compile(jsonPath);
            results = ctx.read(path);
            if ((jsonPath.contains("..") || jsonPath.contains("?(")) && results != null && results instanceof Json) {
                Json resultJson = (Json) results;
                if (resultJson.isArray() && resultJson.size() == 1) {
                    results = resultJson.get(0);
                } else if (resultJson.size() == 0) {
                    results = null;
                }
            }
        } catch (InvalidPathException e){
            //TODO report invalid path or just keep it as "not found"
        }
        return results == null ? defaultValue : results;
    }
//    public static <T> T findT(Json input,String jsonPath,T defaultValue){
//        return null;
//    }

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
    /**
     * Split the keys by non-slash escaped dots (. not \.)
     * @param keys string containing contatenated keys
     * @return List of keys
     */
    public static List<String> toChain(String keys){
        if(!keys.contains(".")){
            ArrayList<String> rtrn = new ArrayList();
            rtrn.add(keys);
            return rtrn;
        }
        return new ArrayList<>(
           Arrays.asList(keys.split("\\.(?<!\\\\\\.)"))
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

    public void merge(Json toMerge){

        merge(toMerge,false);
    }
    public void merge(Json toMerge,boolean override){
        if(toMerge==null){
            return;
        }
        toMerge.forEach((key,value)->{
            if(override || !has(key)) {
                set(key, value);
            }else{
                if(has(key)){
                    if(get(key) instanceof Json){
                        Json keyJson = getJson(key);
                        if(keyJson.isArray()){
                            if(value instanceof Json && ((Json)value).isArray()){
                                ((Json)value).forEach(v->keyJson.add(v));
                            }else{
                                keyJson.add(value);
                            }
                        }else{
                            if(value instanceof Json && !((Json)value).isArray()){
                                keyJson.merge((Json)value,override);
                            }else{
                                //TODO how do we merge an POJO into a Json object
                                keyJson.add(value); //this will create a new key == size of current Json object
                            }
                        }
                    }
                    if(get(key) instanceof Json && value instanceof Json){
                       ((Json)get(key)).merge((Json)value,override);
                    }
                }
            }
        });
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
        if(o == null){
            return "null";
        }else if(o instanceof Json){
            return ((Json)o).toString();
        } else {
            return o.toString().replaceAll("\"", "\\\\\"");
        }
    }

    public void remove(Object key){
        key = box(key);
        if(isArray() && (key instanceof Integer || key instanceof Long)){
            int index = ((Number)key).intValue();
            int size = size();
            for(int i=index; i<size-1; i++){
                set(i,get(i+1));
            }
            data.remove(size-1);
        }else{
            data.remove(key);
        }
    }
    public void set(Object key, Object value){
        key = box(key);
        if(key instanceof Integer || key instanceof Long){
            key = ((Number)key).intValue();
        }else{
            isArray = false;
        }
        checkKeyType(key);
        data.put(key,value);
    }
    private void checkKeyType(Object key){
        if(! (key instanceof Integer || key instanceof Long) ){
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
        key = box(key);
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
        key = box(key);
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
        return data.get(box(key));
    }

    private Object box(Object key){
        if(isArray() && (key instanceof Integer || key instanceof Long)){
            key = ((Number)key).intValue();
        }
        return key;
    }

    public boolean getBoolean(Object key) { return getBoolean(key,false);}
    public boolean getBoolean(Object key,boolean defaultValue){
        key = box(key);
        return has(key) ? data.get(key) instanceof Boolean ? (Boolean)data.get(key) : Boolean.parseBoolean(data.get(key).toString()): defaultValue;
    }
    public Optional<Boolean> optBoolean(Object key){
        return ofNullable(getBoolean(key));
    }

    public String getString(Object key){
        return getString(key,null);
    }
    public String getString(Object key,String defaultValue) {
        key = box(key);
        if (has(key)) {
            Object value = data.get(key);
            return value == null ? null : value.toString();
        } else {
            return defaultValue;
        }
    }
    public Optional<String> optString(Object key){
        return ofNullable(getString(key));
    }

    public Json getJson(Object key){ return getJson(key,null); }
    public Json getJson(Object key,Json defaultValue){
        return has(key) && data.get(key) instanceof Json ? (Json)data.get(key) : defaultValue;
    }
    public Optional<Json> optJson(Object key){
        return ofNullable(getJson(key));
    }

    public long getLong(Object key){
        return getLong(key,0);
    }
    public long getLong(Object key,long defaultValue){
        return has(key) && data.get(key) instanceof Number ? ((Number) data.get(key)).longValue() : defaultValue;
    }
    public Optional<Long> optLong(Object key){
        return ofNullable(has(key) && get(key) instanceof Long ? getLong(key) : null);
    }

    public double getDouble(Object key){
        return getDouble(key,0);
    }
    public double getDouble(Object key,double defaultValue){
        if( has(key) ){
            Object value = get(key);
            if(value instanceof Double){
                return (Double)value;
            }else if (value instanceof Long){
                return 1.0*((Long)value);
            }else if (value instanceof String && ((String) value).matches("-?\\d+\\.?\\d*")){
                return Double.parseDouble((String)value);
            }else{
                return defaultValue;
            }
        }else{
            return defaultValue;
        }
    }
    public Optional<Double> optDouble(Object key){
        return ofNullable(has(key) && get(key) instanceof Double ? getDouble(key) : null);
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
