package perf.yaup.xml;

import perf.yaup.StringUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static perf.yaup.file.FileUtility.ADD_OPERATION;
import static perf.yaup.file.FileUtility.DELETE_OPERATION;
import static perf.yaup.file.FileUtility.SET_OPERATION;

public class XmlOperation {

    private static enum Operation {None(""),Add(ADD_OPERATION),Set(SET_OPERATION),Delete(DELETE_OPERATION);
        private String value;
        Operation(String value){
            this.value = value;
        }
        public String getValue(){return value;}
    }
    private static Operation getOperation(String input){
        Operation rtrn = Operation.None;
        switch (input){
            case ADD_OPERATION:
                rtrn = Operation.Add;
                break;
            case DELETE_OPERATION:
                rtrn = Operation.Delete;
                break;
            case SET_OPERATION:
                rtrn = Operation.Set;
                break;
        }
        return rtrn;
    }

    private String path;
    private Operation operation;
    private String value;

    public static void main(String[] args) {
        //Xml xml = new XmlLoader().loadXml(new File("/home/wreicher/code/spec/jEnterpriseClean/modules/orders/src/main/webapp/WEB-INF/web.xml").toPath());
        Xml xml = new XmlLoader().loadXml(new File("/home/wreicher/perfWork/standalone-full.xserver4.mfg-del-ins-driver-did-testing.xml").toPath());

        xml.getAll("/server/profile/subsystem[@xmlns ='urn:jboss:domain:logging:3.0']").forEach(System.out::println);
        System.exit(0);

        List<XmlOperation> toApply = Arrays.asList(
            XmlOperation.parse("/web-app/env-entry[env-entry-name = \"vehicle.service.host\"]/env-entry-value == laptop"),
            XmlOperation.parse("/web-app/env-entry[env-entry-name = \"vehicle.service.port\"]/env-entry-value == 31337")
        );
        boolean modified = toApply.stream().map(op->op.apply(xml)).reduce(Boolean::logicalOr).orElse(false);
        System.out.println("modified = "+modified);
        //System.out.println(xml.documentString(4));
    }


    public static XmlOperation parse(String input){
        String patternString = String.format("(?<operation>%s|%s|%s)", StringUtil.escapeRegex(ADD_OPERATION), StringUtil.escapeRegex(DELETE_OPERATION), StringUtil.escapeRegex(SET_OPERATION));
        Pattern operationPattern = Pattern.compile(patternString);

        String path;
        Operation operation=Operation.None;
        String value=null;

        Matcher m = operationPattern.matcher(input);
        if(m.find()){
            path = input.substring(0,m.start());
            operation = getOperation(m.group("operation"));
            value = input.substring(m.end()).trim();
        }else{
            path = input;
        }
        return new XmlOperation(path,operation,value);
    }
    public XmlOperation(String path,Operation operation,String value){
        this.path = path;
        this.operation = operation;
        this.value = value;
    }

    public boolean hasValue(){return value!=null;}
    public String getValue(){
        return value==null ? "" : value;
    }
    public boolean apply(Xml xml){
        boolean modified = false;
        String xpath = path;
        List<Xml> found = xml.getAll(xpath);
        if(!found.isEmpty()){
            modified=true;
            found.forEach(xmlEntry->{
                xmlEntry.modify(operation.getValue()+getValue());
            });
        }
        return modified;
    }
}
