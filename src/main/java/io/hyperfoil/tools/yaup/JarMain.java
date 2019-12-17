package io.hyperfoil.tools.yaup;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.JsonValidator;
import io.hyperfoil.tools.yaup.xml.pojo.XmlComparison;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JarMain {

    public static void main(String[] vargs) {
        List<String> args = new ArrayList<>(Arrays.asList(vargs));
        if(!args.isEmpty()){
            String tool = args.remove(0);
            switch (tool){
                case "xml-diff":
                    XmlComparison.main(args.toArray(new String[]{}));
                    break;
                case "json-schema":
                    if(args.size() != 2){
                        System.out.printf("json-schema expects <schema-path> <json-path>");
                        System.exit(1);
                    }else{
                        Json schemaJson = Json.fromFile(args.get(0));
                        Json dataJson = Json.fromFile(args.get(1));

                        JsonValidator validator = new JsonValidator(schemaJson);

                        Json errors = validator.validate(dataJson);
                        System.out.println(errors.toString());
                    }
            }
        }
    }
}
