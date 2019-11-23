package io.hyperfoil.tools.yaup.json;

import org.graalvm.polyglot.Value;

public class ValueConverter {

   public static Object convert(Value value){
      if(value == null || value.isNull()) {
         return "";
      }else if (value.isProxyObject()){
         return value.asProxyObject();
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
      }else if (value.canExecute()){
         return value.toString();
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
      Json json = new Json();
      for(String key : value.getMemberKeys()){
         json.set(key,convert(value.getMember(key)));
      }
      return json;
   }
}
