package io.hyperfoil.tools.yaup.json;

import io.vertx.core.json.JsonObject;
import io.vertx.json.schema.*;
public class JsonValidator {


   private Json schemaJson;
   private Validator validator;
   public JsonValidator(Json schema) {
      this.schemaJson = schema;
      JsonObject object = new JsonObject(schemaJson.toString());
      JsonSchema jsonSchema = JsonSchema.of(object);
      validator = Validator.create(jsonSchema, new JsonSchemaOptions().setDraft(Draft.DRAFT202012)/*.setDraft(Draft.DRAFT7)*/.setBaseUri("https://hyperfoil.io"));
   }

   public Json validate(Json input) {
      Json rtrn = new Json();
      if(input==null){
         return rtrn;
      }
      try {
         OutputUnit outputUnit = validator.validate(new JsonObject(input.toString()));
         if (!outputUnit.getValid()) {
            outputUnit.getErrors().forEach(e -> rtrn.add(Json.fromString(e.toString())));
         }
      }catch(SchemaException se){
         rtrn.add(se.getMessage());
      }
      return rtrn;
   }

   public Json getSchema() {
      return schemaJson;
   }
}
