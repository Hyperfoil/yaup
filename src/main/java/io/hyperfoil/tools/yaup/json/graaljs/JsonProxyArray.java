package io.hyperfoil.tools.yaup.json.graaljs;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.ValueConverter;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;

public class JsonProxyArray implements ProxyArray {

   final Json json;
   JsonProxyArray(Json json){
      this.json = json;
   }

   public Json getJson(){return json;}

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
}
