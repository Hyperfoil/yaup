package io.hyperfoil.tools.yaup.json.graaljs;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.ValueConverter;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class JsonProxyObject implements ProxyObject {

   public static class InstanceCheck implements ProxyExecutable {

      @Override
      public Object execute(Value...args){
         if(args.length<1){
            return false;
         }else{
            Value obj = args[0];
            return obj.isProxyObject() && obj.asProxyObject() instanceof JsonProxyObject || ValueConverter.convert(obj) instanceof Json;
         }
      }
   }

   private Json json;
//   private Map<String,Value> methods; //this code is unused
   JsonProxyObject(Json json){
      this.json = json;
      //methods is a hack around https://github.com/graalvm/graaljs/issues/45
      //use js.experimental-foreign-object-prototype in jsEval
//      this.methods = new HashMap<>();
//      if(json !=null){
//         if(json.isArray()){ //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/
//            methods.put("push",Value.asValue((Consumer<Object>) o -> json.add(o instanceof Value ? ValueConverter.convert((Value)o) : o)));
//            // concat,
//            methods.put("concat",Value.asValue((Function<Object,Object>) o -> {
//               if(o instanceof Value){
//                  o = ValueConverter.convert((Value)o);
//               }
//               Json rtrn = new Json(true);
//               //TODO add the entires
//               return new JsonProxyObject(rtrn);
//            }));
//         }
//      }
   }

   public Json getJson(){return json;}

   @Override
   public Object getMember(String key) {
      Object rtrn = json.isArray() && key.matches("\\d+") ? json.get(Integer.parseInt(key)) : json.get(key);
      if (rtrn instanceof Json){
         rtrn =  JsonProxy.create((Json)rtrn);
      }else if (rtrn instanceof BigDecimal){
         BigDecimal bigDecimal = (BigDecimal)rtrn;
         if(bigDecimal.scale()>=0){
            return bigDecimal.doubleValue();
         }else{
            return bigDecimal.longValue();
         }
      }
      return rtrn;
   }

   @Override
   public Object getMemberKeys() {
      return json.keys().stream().map(o->o.toString()).toArray();
   }

   @Override
   public boolean hasMember(String key) {
      if(json.isArray() && key.matches("\\d+")){
         return json.has(Integer.parseInt(key));
      }else {
         return json.has(key);
      }
   }

   @Override
   public void putMember(String key, Value value) {
      if(json.isArray() && key.matches("\\d+")){
         json.set(Integer.parseInt(key),ValueConverter.convert(value));
      }else {
         json.set(key, ValueConverter.convert(value));
      }
   }

   @Override
   public boolean removeMember(String key) {
      boolean rtrn = json.has(key);
      if(json.isArray() && key.matches("\\d+")){
         Integer intKey =Integer.parseInt(key);
         rtrn = json.has(intKey);
         json.remove(intKey);
      }else {
         json.remove(key);
      }
      return rtrn;
   }

   @Override
   public String toString(){return json.toString(0);}
}
