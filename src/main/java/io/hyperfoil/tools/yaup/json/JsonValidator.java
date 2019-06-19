package io.hyperfoil.tools.yaup.json;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;

public class JsonValidator {

   private Json schemaJson;
   private Schema schema;
   public JsonValidator(Json schema){
      this.schemaJson = schema;
      this.schema = SchemaLoader.load(Json.toJSONObject(schemaJson));
   }

   public Json validate(Json input){
      Json rtrn = new Json();
      if(input.isArray()){
         //TODO prevent validating an array?
      }
      try{
         schema.validate(Json.toJSONObject(input));
      }catch (ValidationException e){
         rtrn.set("details",Json.fromJSONObject(e.toJSON()));
         rtrn.set("messages",new Json());
         e.getAllMessages().forEach(m->rtrn.getJson("messages").add(m));
      }
      return rtrn == null ? new Json() : rtrn;
   }
   public Json getSchema(){return schemaJson;}


}
