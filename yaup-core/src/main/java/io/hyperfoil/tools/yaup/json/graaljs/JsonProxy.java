package io.hyperfoil.tools.yaup.json.graaljs;

import io.hyperfoil.tools.yaup.json.Json;
import org.graalvm.polyglot.proxy.Proxy;

public class JsonProxy {


   public static Proxy create(Json json){
      if(json == null){
         return null;
      }else if (json.isArray()){
         return new JsonProxyArray(json);
      }else{
         return new JsonProxyObject(json);
      }
   }
}
