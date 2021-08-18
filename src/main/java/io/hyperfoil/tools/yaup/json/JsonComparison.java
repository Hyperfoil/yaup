package io.hyperfoil.tools.yaup.json;

import io.hyperfoil.tools.yaup.StringUtil;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;

public class JsonComparison {

   final static XLogger logger = XLoggerFactory.getXLogger(MethodHandles.lookup().lookupClass());

   public static class Entry {
      private String path;
      private LinkedHashMap<String,String> values;

      public Entry(String path){
         this.path = path;
         this.values = new LinkedHashMap<>();
      }

      private void put(String name,String value){
         values.put(name,value);
      }
      public String getPath(){return path;}
      public Set<String> keys(){return values.keySet();}
      public String value(String key){return values.get(key);}
      public void forEach(BiConsumer<String,String> consumer){
         values.forEach(consumer);
      }
   }


   private boolean ordered;
   private LinkedHashMap<String,Json> rootJson;
   private LinkedHashMap<String,Integer> criteria;

   public void addCriteria(String path,int editDistance){
      criteria.put(path,editDistance);
   }
   public int jsonCount(){
      return rootJson.size();
   }
   public Set<String> jsonNames(){
      return rootJson.keySet();
   }
   public void load(String name,Json json){
      rootJson.put(name,json);
   }

   public int criteriaCount(){
      return criteria.size();
   }

   private void diff(String path, List<Entry> diffs, Map<String,Json> jsons){
      if(isEmtpy(jsons)){
         return;
      }
      if(hasNull(jsons)){//if one if the jsons is missing or empty
         logger.error("THIS SHOULD BE CAUGHT BEFORE THIS POINT FOR ALL SUB_CALLS");
         Entry newEntry = new Entry(path);
         jsons.forEach((name,json)->{
            newEntry.put(name,json.toString(2));
         });
      }else{//each json is not empty

         if(allArrays(jsons)){
            if(ordered){
               Set<Object> allKeys = allKeys(jsons);
               allKeys.forEach(key->{
                  String currentPath = path+"["+key+"]";
                  compareKey(currentPath,diffs,key,jsons);
               });
            }else{// if they are not ordered we need to find the best match
               //create a Map<String,Map<Object,Object>> of name : json key:value
               //remove them as they are matched

            }
         }else if (allObjects(jsons)){
            Set<Object> allKeys = allKeys(jsons);
            allKeys.forEach(key->{
               String currentPath = path+"."+key;
               compareKey(currentPath,diffs,key,jsons);
            });

         }else{
            //WTF, how are we comparing non-empty arrays to objects?

         }
      }
   }
   public List<Entry> getDiffs(){
      if(rootJson.size()>1){
         return Collections.emptyList();
      }
      LinkedList<Entry> rtrn = new LinkedList<>();
      diff("$",rtrn,rootJson);
      return rtrn;
   }
   private int getScore(Json base,Json other){
      int rtrn = 0;// 0 means perfect match
      for(String path : criteria.keySet()){
         int limit = criteria.get(path);
         Object baseFound = Json.find(base,path,null);
         Object otherFound = Json.find(other,path,null);
         if(baseFound==null && otherFound==null){
            //ignore it because it didn't match both
         }else if(baseFound==null || otherFound==null) {
            return Integer.MAX_VALUE; // if only one has it then they don't match the criteria
         }else{
            int editDistance = StringUtil.editDistance(baseFound.toString(),otherFound.toString());
            if(editDistance > limit){
               return Integer.MAX_VALUE;
            }else{
               rtrn+=editDistance;
            }
         }
      }

      //at this point we have to compare all the keys
      Set<Object> allKeys = new HashSet<>(base.keys());
      allKeys.addAll(other.keys());
      LongAdder adder = new LongAdder();
      allKeys.forEach(key->{
         Object baseValue = base.get(key);
         Object otherValue = other.get(key);
         if(base == null){
            adder.add(otherValue.toString().length());
         }else if (other == null){
            adder.add(baseValue.toString().length());
         }else{
            //TODO skip keys that reference objects?
            adder.add(StringUtil.editDistance(baseValue.toString(),otherValue.toString()));
         }
      });
      rtrn+=adder.intValue();
      return rtrn;
   }
   private void compareKey(String path, List<Entry> diffs, Object key, Map<String,Json> jsons){
      if(!allHave(key,jsons)){
         Entry newDiff = new Entry(path);
         jsons.forEach((name,json)->{
            newDiff.put(name,json.toString(2));
         });
         diffs.add(newDiff);
      }else{
         if(allJson(key,jsons)){
            Map<String,Json> keyJsons = new HashMap<>();
            jsons.forEach((name,json)-> {
               keyJsons.put(name,json.getJson(key));
            });
            diff(path,diffs,keyJsons);
         }else{//see if they differ
            Map<String,Object> values = new HashMap<>();
            HashSet<Object> uniqueValues = new HashSet<>();
            jsons.forEach((name,json)->{
               Object jsonValue = json.getJson(key);
               values.put(name,jsonValue);
               uniqueValues.add(jsonValue);
            });

            if(uniqueValues.size()>1){
               Entry newDiff = new Entry(path);
               values.forEach((name,value)->{
                  newDiff.put(name,value.toString());
               });
               diffs.add(newDiff);
            }
         }
      }

   }


   private boolean allJson(Object key,Map<String,Json> jsons){
      return jsons.values().stream().filter(json->!(json.get(key) instanceof Json)).findAny().orElse(null) == null;
   }
   private boolean allHave(Object key,Map<String,Json> jsons){
      return jsons.values().stream().filter(json->!json.has(key)).findAny().orElse(null) == null;
   }
   private Set<Object> allKeys(Map<String, Json> jsons){
      Set<Object> rtrn = new LinkedHashSet<>();
      jsons.values().forEach(json->{
         rtrn.addAll(json.keys());
      });
      return rtrn;
   }
   private boolean allArrays(Map<String, Json> jsons){
      return jsons.values().stream().filter(v->!v.isArray()).findAny().orElse(null)==null;
   }
   private boolean allObjects(Map<String, Json> jsons){
      return jsons.values().stream().filter(v->v.isArray()).findAny().orElse(null)==null;
   }

   private boolean isEmtpy(Map<String, Json> jsons){
      return jsons.values().stream().map(json->json==null || json.isEmpty())
         .reduce(Boolean::logicalAnd).orElse(false);
   }
   private boolean hasNull(Map<String, Json> jsons){
      boolean rtrn = false;
      for(Json json: jsons.values()){
         if(json == null || json.isEmpty()){
            rtrn = true;
            return true;
         }
      }
      return rtrn;
   }
}
