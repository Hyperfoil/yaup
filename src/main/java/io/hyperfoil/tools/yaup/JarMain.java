package io.hyperfoil.tools.yaup;

import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.json.JsonValidator;
import io.hyperfoil.tools.yaup.xml.XmlOperation;
import io.hyperfoil.tools.yaup.xml.pojo.Xml;
import io.hyperfoil.tools.yaup.xml.pojo.XmlComparison;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JarMain {

    public static void main(String[] vargs) {
        List<String> args = new ArrayList<>(Arrays.asList(vargs));
        if(!args.isEmpty()){
            String tool = args.remove(0);
            System.out.println("tool: "+tool);
            for(int i=0; i<args.size(); i++){
                System.out.printf("  arg[%2d]: %s%n",i,args.get(i));
            }
            switch (tool){
                case "xml":
                    if(args.size()<2){
                        System.out.printf("xml expects <operation> <file-path>%n");
                        System.exit(1);
                    }
                    Xml doc = Xml.parseFile(args.get(1));
                    if(doc == null || !doc.exists()){
                        System.out.printf("failed to load %s as xml %n",args.get(1));
                        System.exit(1);
                    }
                    XmlOperation operation = XmlOperation.parse(args.get(0));
                    if(operation == null){
                        System.out.printf("failed to identify xml operation %s%n",args.get(0));
                        System.exit(1);
                    }
                    String response = doc.apply(operation);
                    System.out.printf("response:%n%s",response);
                    //System.out.printf("document:%n%s",doc.documentString());
                    break;
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
