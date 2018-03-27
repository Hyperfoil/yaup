package perf.yaup.xml;

import perf.yaup.StringUtil;
import perf.yaup.file.FileUtility;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static perf.yaup.file.FileUtility.ADD_OPERATION;
import static perf.yaup.file.FileUtility.DELETE_OPERATION;
import static perf.yaup.file.FileUtility.SET_OPERATION;

public class XmlOperation {

    private static final String NAMESPACE_PATTERN = "starts-with(namespace::*[name()=\"%s\"],\"%s\")";
    private static final String XMLNS_PATTERN = "@xmlns:?(?<prefix>[^=\\s]*)\\s*=\\s*['\"]?(?<namespace>[^\\s'\\\"\\]]+)['\"]?";
    private static final String XPATH_ATTRIBUTE_CRITERIA_PATTERN="^\\s*@(?<name>[^\\s=\\]]+)\\s*";


    public static enum Operation {None(""),Add(ADD_OPERATION),Set(SET_OPERATION),Delete(DELETE_OPERATION);
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

    public static XmlOperation parse(String input){
        String patternString = String.format("(?<operation>%s|%s|%s)", StringUtil.escapeRegex(ADD_OPERATION), StringUtil.escapeRegex(DELETE_OPERATION), StringUtil.escapeRegex(SET_OPERATION));
        Pattern operationPattern = Pattern.compile(patternString);

        String path;
        Operation operation=Operation.None;
        String value=null;

        Matcher m = operationPattern.matcher(input);
        if(m.find()){
            path = input.substring(0,m.start()).trim();
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
        this.value = StringUtil.removeQuotes(value);
    }

    public boolean isRead(){return Operation.None.equals(this.operation);}
    public boolean isSet(){return Operation.Set.equals(this.operation);}
    public boolean isAdd(){return Operation.Add.equals(this.operation);}
    public boolean isDelete(){return Operation.Delete.equals(this.operation);}

    public Operation getOperation() {
        return operation;
    }

    public String getPath(){return path;}
    public boolean hasValue(){return value!=null;}
    public String getValue(){
        return value==null ? "" : value;
    }
    public String apply(Xml xml){
        StringBuilder sb = new StringBuilder();
        String xpath = path;
        if(xpath!=null && !xpath.isEmpty()) {
            List<Xml> found = xml.getAll(replaceXmlnsAttribute(xpath));
            if(found.isEmpty()){
                int lastIndex = lastPathIndex(xpath);
                String lastFragment = null;
                if(lastIndex>-1){
                    lastFragment = xpath.substring(lastIndex);
                    xpath = xpath.substring(0,lastIndex);
                    while(xpath.endsWith("/")){
                        xpath=xpath.substring(0,xpath.length()-1);
                    }
                }
                if(lastFragment!=null){//see if we can create it
                    boolean canSet = true;
                    if(lastFragment.startsWith(Xml.ATTRIBUTE_KEY)){
                        operation = Operation.Add;
                        value =
                            lastFragment +
                            " = "+
                            value;

                    }else{
                        int criteriaIndex = lastFragment.indexOf("[");
                        String tagName = lastFragment;

                        HashMap<String,String> newAttributes = new HashMap<>();
                        if(criteriaIndex>-1){
                            tagName = lastFragment.substring(0,criteriaIndex);
                            String criteria = lastFragment.substring(tagName.length()+1,lastFragment.lastIndexOf("]")).trim();
                            //lastFragment = criteria;
                            Matcher attributeMatcher = Pattern.compile(XPATH_ATTRIBUTE_CRITERIA_PATTERN).matcher(criteria);
                            Matcher separatorMatcher = Pattern.compile("^\\s*AND|and\\s+",Pattern.DOTALL).matcher(criteria);
                            //todo strip lastFragment until all that is left is [] so we know we can replace it
                            while(canSet && !criteria.isEmpty()){
                                canSet=false;
                                separatorMatcher.reset(criteria);
                                if(criteria.startsWith("AND") || criteria.startsWith("and")){
                                    criteria = criteria.substring(3).trim();
                                }
                                attributeMatcher.reset(criteria);
                                if(attributeMatcher.find()){
                                    canSet=true;
                                    String name = attributeMatcher.group("name");
                                    String value = "";
                                    criteria = criteria.substring(attributeMatcher.end());
                                    if(criteria.startsWith("=")){
                                        criteria = criteria.substring(1);
                                        value = StringUtil.findNotQuoted(criteria," ]");
                                        criteria = criteria.substring(value.length()).trim();
                                        value = StringUtil.removeQuotes(value);
                                    }
                                    newAttributes.put(name,value);
                                }
                            }

                        }else{

                        }
                        if(canSet) {
                            StringBuilder newTagSb = new StringBuilder();
                            newTagSb.append("<");
                            newTagSb.append(tagName);
                            if (!newAttributes.isEmpty()) {

                                for (String key : newAttributes.keySet()) {
                                    String attributeValue = newAttributes.get(key);
                                    newTagSb.append(" ");
                                    newTagSb.append(key);

                                    newTagSb.append("=");
                                    newTagSb.append(StringUtil.quote(attributeValue));
                                }
                            }
                            newTagSb.append(">");
                            operation = Operation.Add;

                            String newTagValue = newTagSb.toString();

                            value = newTagSb.toString() + value + "</"+tagName+">";
                        }else{
                            //sadness, cannot overcome the 0 found
                        }

                        //oh boy, we are looking for /tag[a big mess]
                    }
                    if(canSet) {
                        found = xml.getAll(replaceXmlnsAttribute(xpath));
                    }
                }
            }
            if (!found.isEmpty()) {
                if(Operation.None.equals(operation)){
                    for(Xml entry : found){
                        if(sb.length()>0){
                            sb.append(System.lineSeparator());
                        }
                        sb.append(entry.toString());
                    }
                }else {
                    found.forEach(xmlEntry -> {
                        xmlEntry.modify(operation.getValue() + getValue());
                    });
                }
            }
        }else{
            xml.modify(operation.getValue() + getValue());
        }
        return sb.toString();
    }
    public static String replaceXmlnsAttribute(String pattern){
        String rtrn = pattern;
        Matcher xmlnsPattern = Pattern.compile(XMLNS_PATTERN).matcher(pattern);

        while(xmlnsPattern.find()){
            rtrn = rtrn.replace(
                    xmlnsPattern.group(),
                    String.format(
                            NAMESPACE_PATTERN,
                            xmlnsPattern.group("prefix"),
                            xmlnsPattern.group("namespace")
                    )
            );
        }
        return rtrn;
    }
    public static int lastPathIndex(String path) {
        int rtrn = -1;
        if (path.contains("/")) {
            rtrn = path.length();
            boolean stop = false;
            char quoteChar = '"';
            boolean inQuote = false;
            boolean inCtriteria = false;
            while (rtrn > 0 && !stop) {
                rtrn--;
                switch (path.charAt(rtrn)) {
                    case '\'':
                    case '"':
                        if (!inQuote) {
                            inQuote = true;
                            quoteChar = path.charAt(rtrn);
                        } else {
                            if (quoteChar == path.charAt(rtrn)) {//potentially end of quote
                                if (rtrn > 0 && '\\' == path.charAt(rtrn - 1)) {//it's escaped, not end
                                    rtrn--;
                                } else {
                                    inQuote = false;
                                }
                            }
                        }
                        break;
                    case ']':
                        if (!inQuote) {
                            inCtriteria = true;
                        }
                        break;
                    case '[':
                        if (!inQuote && inCtriteria) {
                            inCtriteria = false;
                        }
                        break;
                    case '/':
                        if (!inQuote && !inCtriteria) {
                            stop = true;
                            rtrn++;//back track because we don't want to capture the slash
                        }
                        break;
                }
            }
        }
        return rtrn;
    }
    public static String lastPathFragment(String path){
        String rtrn = "";
        int i = lastPathIndex(path);

        if(i<0){
            i=0;
        }
        rtrn = path.substring(i);

        return rtrn;
    }

}
