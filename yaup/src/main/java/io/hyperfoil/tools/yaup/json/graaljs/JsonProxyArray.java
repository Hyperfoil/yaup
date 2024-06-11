package io.hyperfoil.tools.yaup.json.graaljs;

import io.hyperfoil.tools.yaup.AsciiArt;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.ValueConverter;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

public class JsonProxyArray implements ProxyArray, ProxyObject {

   final Json json;
   JsonProxyArray(Json json){
      this.json = json;
   }

   public Json getJson(){return json;}

   public void add(Object o){
      this.json.add(o);
   }

   @Override
   public Object get(long index) {
      Object rtrn = null;
      if(index < json.size()){
         rtrn = json.get(index);
         if(rtrn instanceof Json){
            rtrn = ((Json)rtrn).isArray() ? new JsonProxyArray((Json)rtrn) : new JsonProxyObject((Json)rtrn);
         }
      }
      return rtrn;
   }

   @Override
   public void set(long index, Value value) {
      Object converted = ValueConverter.convert(value);
      json.set(index,converted);
   }

   @Override
   public boolean remove(long index) {
      if(index < json.size()){
         json.remove(index);
         return true;
      }
      return false;
   }

   @Override
   public long getSize() {
      return json.size();
   }


   private int count =0;
   @Override
   public Object getMember(String key) {
      switch (key){
         case "push": {
            return (ProxyExecutable) args -> {
               if(args!=null && args.length >0){
                  for(int i=0; i<args.length; i++){
                     Object v = ValueConverter.convert(args[i]);
                     add(v);
                  }
               }
               return null;
            };
         }
      }
      return null;
   }

   @Override
   public Object getMemberKeys() {

      return null;
   }

   @Override
   public boolean hasMember(String key) {
      return "push".equals(key);
   }

   @Override
   public void putMember(String key, Value value) {}

   @Override
   public boolean removeMember(String key) {
      return ProxyObject.super.removeMember(key);
   }
}
