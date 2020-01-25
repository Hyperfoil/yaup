package io.hyperfoil.tools.yaup.json;

//import com.networknt.schema.JsonSchema;
//import com.networknt.schema.JsonSchemaFactory;
//import com.networknt.schema.SpecVersion;
//import com.networknt.schema.ValidationMessage;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.util.Set;

public class JsonValidator {

   public static void main(String[] args) {

   }


   private Json schemaJson;
   private com.github.fge.jsonschema.main.JsonValidator validator;
//   private JsonSchema jsonSchema;

   public JsonValidator(Json schema) {
      this.schemaJson = schema;
      this.validator = JsonSchemaFactory.byDefault().getValidator();

//      this.jsonSchema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7).getSchema(Json.toJsonNode(schemaJson));
   }

   public Json validate(Json input) {
      Json rtrn = new Json();
      try {
         ProcessingReport report = validator.validate(Json.toJsonNode(schemaJson),Json.toJsonNode(input),true);
         if(!report.isSuccess()){
            report.forEach(processingMessage -> {
               rtrn.add(Json.fromJsonNode(processingMessage.asJson()));
            });
         }
      } catch (ProcessingException e) {
         e.printStackTrace();
      }
//      Set<ValidationMessage> errors = jsonSchema.validate(Json.toJsonNode(input));
//      errors.forEach(validationMessage -> {
//         Json entry = new Json();
//         entry.set("message", validationMessage.getMessage());
//         entry.set("code", validationMessage.getCode());
//         entry.set("path", validationMessage.getPath());
//         entry.set("arguemnts", new Json(true));
//         for (String arg : validationMessage.getArguments()) {
//            entry.getJson("arguments").add(arg);
//         }
//         entry.set("details", new Json(false));
//         validationMessage.getDetails().forEach((k, v) -> {
//            entry.getJson("details").set(k, v);
//         });
//         rtrn.add(entry);
//      });



      return rtrn;
   }

   public Json getSchema() {
      return schemaJson;
   }
}
