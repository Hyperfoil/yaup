package io.hyperfoil.tools.yaup.json;

import io.hyperfoil.tools.yaup.json.graaljs.JsException;
import io.hyperfoil.tools.yaup.json.graaljs.JsonProxyArray;
import io.hyperfoil.tools.yaup.json.graaljs.JsonProxyObject;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.SourceSection;
import org.graalvm.polyglot.Value;

public class ValueConverter {

   public static Object convert(Value value){
      if(value == null) {
         return null;
      }else if (value.isNull()){
         // Value api cannot differentiate null and undefined from javascript
         if(value.toString().contains("undefined")){
            return ""; //no return is the same as returning a missing key from a ProxyObject?
         }else{
            return null;
         }
      }else if (value.isProxyObject()){
         Object po = value.asProxyObject();
         if(po instanceof JsonProxyObject) {
            return ((JsonProxyObject) po).getJson();
         }else if (po instanceof JsonProxyArray){
            return ((JsonProxyArray) po).getJson();
         }else {
            return value.asProxyObject();
         }
      }else if (value.isHostObject()){
         return value.asHostObject();
      }else if (value.isBoolean()){
         return value.asBoolean();
      }else if (value.isNumber()){
         double v = value.asDouble();
         if(v == Math.rint(v)){
            return (long)v;
         }else{
            return v;
         }
      }else if (value.isString()){
         return value.asString();
      }else if (value.hasArrayElements()) {
         return convertArray(value);
      }else if (value.canExecute()) {
         return value.toString();
      }else if (value.isException()){
         try{
            value.throwException();
         }catch(RuntimeException e){
            if (e instanceof PolyglotException){
               PolyglotException pe = (PolyglotException) e;
               return new JsException(value.toString(),pe);
            }else{
               //Have not seen this condition occur
               return new JsException(value.toString(),"unknown",e);
            }
         }
         return new JsException(value.asString());
      }else if (value.hasMembers()){

         return convertMapping(value);
      }else{
         //TODO log error wtf is Value?
         return "";
      }
   }
   public static String asString(Object value){

      if(value instanceof Double){
         return String.format("%.8f",value);
      }else{
         return value.toString();
      }
   }

   public static Json convertArray(Value value){
      Json json = new Json();
      for(int i=0; i<value.getArraySize(); i++){
         json.add(convert(value.getArrayElement(i)));
      }
      return json;
   }
   public static Json convertMapping(Value value){
      Json json = new Json(false);
      if(value.getMemberKeys().isEmpty() ){//return a set as a list
         if(value.hasIterator()) {
            json = new Json(true);
            Value iter = value.getIterator();
            while (iter.hasIteratorNextElement()) {
               Value next = iter.getIteratorNextElement();
               json.add(convert(next));
            }
         }
      }else {
         for (String key : value.getMemberKeys()) {
            json.set(key, convert(value.getMember(key)));
         }
      }
      return json;
   }
}
