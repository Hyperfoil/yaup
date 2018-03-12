package perf.yaup.xml.pojo;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlPath {

    private static final String PARENT_PREFIX = "./";
    private static final String RELATIVE_PREFIX = "./";
    private static final String DEEP_SEARCH = "//";
    private static final String TAG_SEPARATOR = "/";
    private static final String ATTRIBUTE_PREFIX = "@";
    private static final String CRITERIA_LIST_PREFIX = "[";
    private static final String CRITERIA_LIST_SUFFIX = "]";

    private static final String DOTS = "(?<dots>\\.{0,2})";
    private static final String SLASHES = "(?<slashes>/{0,2})";

    private static final Pattern TAG_NAME_PATTERN = Pattern.compile(("^"+DOTS+SLASHES+"\\s*(?<name>[^/\\[\\]@\\s=]+)\\s*"));
    private static final Pattern ATTRIBUTE_PATH_PATTERN = Pattern.compile("^"+DOTS+SLASHES+"\\s*@(?<name>[^/\\[\\]\\s=]+)\\s*");
    private static final Pattern CRITERIA_SEPARATOR_PATTERN = Pattern.compile("^\\s*(?:and|AND)?\\s*");
    private static final Pattern CRITERIA_ATTRIBUTE_PATTERN = Pattern.compile("^\\s*@(?<name>[^\\]\\s=]+)\\s*=\\s*['\"](?<value>[^\"]+)['\"]\\s*");
    private static final Pattern CRITERIA_VALUE_PATTERN = Pattern.compile("^\\s*=\\s*\"(?<value>[^\"]+)\"\\s*");

    private static enum Scope {Global("//"),Absolute("/"),Relative("");

        private String prefix;
        private Scope(String prefix){
            this.prefix = prefix;
        }
        public String getPrefix(){return prefix;}
    }

    private static enum Method {
        Undefined(""),Equals("="),StartsWith("^"),EndsWith("$"),Contains("~");

        private String operator;
        private Method(String operator){
            this.operator = operator;
        }

        public String getOperator(){return operator;}
    }
    public static enum Type {Undefined,Start,Tag,Attribute}

    public int size(){
        int rtrn = 1;//for this
        XmlPath target = this;
        while(target.hasNext()){
            target = target.getNext();
            rtrn++;
        }
        return rtrn;
    }

    public XmlPath getTail(){
        XmlPath rtrn = this;
        while(rtrn.hasNext()){
            rtrn = rtrn.getNext();
        }
        return rtrn;
    }

    public static XmlPath parse(String path){
        return parse(path,new AtomicInteger(0));
    }
    public static XmlPath parse(String path,AtomicInteger index){
        XmlPath rtrn = new XmlPath(Type.Start);
        XmlPath xp = rtrn;

        //valid paths do not alwasy start with tag separator (relative, absolute, deep scan)
        int previousPathIndex=index.get()-1;
        while(previousPathIndex<index.get() && index.get() < path.length()){
            previousPathIndex = index.get();
            Matcher m;

            m = TAG_NAME_PATTERN.matcher(path.substring(index.get()));

            if (m.find(0)) {
                index.addAndGet(m.end());
                boolean deep = m.group("slashes").length() == DEEP_SEARCH.length();
                String name = m.group("name");


                XmlPath nextSegment = new XmlPath(Type.Tag);
                nextSegment.setDeepScan(deep);
                nextSegment.setName(name);

                if (path.startsWith(CRITERIA_LIST_PREFIX, index.get())) {
                    int prevIndex = index.get();
                    index.incrementAndGet();
                    while (!path.startsWith(CRITERIA_LIST_SUFFIX, index.get()) && prevIndex < index.get()) {
                        prevIndex = index.get();

                        if ((m = CRITERIA_SEPARATOR_PATTERN.matcher(path.substring(index.get()))).find(0)) {
                            index.addAndGet(m.end());
                        }

                        m = CRITERIA_ATTRIBUTE_PATTERN.matcher(path.substring(index.get()));
                        if (m.find(0)) {
                            String attributeName = m.group("name");
                            String attributeValue = m.group("value");

                            XmlPath attributePath = new XmlPath(Type.Attribute);
                            attributePath.isFirst = true;
                            attributePath.setChild(true);
                            attributePath.setName(attributeName);
                            attributePath.setValue(attributeValue);

                            //TODO support multiple critera
                            attributePath.setMethod(Method.Equals);

                            nextSegment.addChild(attributePath);
                            index.addAndGet(m.end());
                        } else {
                            //if(path.startsWith(TAG_SEPARATOR,index.get())) {

                            XmlPath criteriaPath = parse(path, index);
                            criteriaPath.setChild(true);
                            if ((m = CRITERIA_VALUE_PATTERN.matcher(path.substring(index.get()))).find(0)) {
                                String criteriaValue = m.group("value");
                                XmlPath criteriaTail = criteriaPath.getTail();
                                criteriaTail.setValue(criteriaValue);
                                index.addAndGet(m.end());
                            }
                            if (criteriaPath.size() > 1) {//more than just a start
                                nextSegment.addChild(criteriaPath);
                            }
                            //}
                        }
                    }
                    if (path.startsWith(CRITERIA_LIST_SUFFIX, index.get())) {
                        index.incrementAndGet();
                    } else {
                        //WTF?
                    }
                }
                xp.setNext(nextSegment);
                xp = nextSegment;


            } else {
                m = ATTRIBUTE_PATH_PATTERN.matcher(path.substring(index.get()));
                if(m.find(0)){

                    boolean deep = m.group("slashes").length() == DEEP_SEARCH.length();
                    String name = m.group("name");

                    XmlPath nextSegment = new XmlPath(Type.Attribute);
                    nextSegment.setName(name);

                    xp.setNext(nextSegment);

                    xp = nextSegment;

                    index.addAndGet(m.end());
                }else{
                    //WTF

                }
            }
        }

        return rtrn;
    }

    public static void main(String[] args) {
        Xml xml2 = Xml.parseFile("/tmp/jboss-eap-7.1/standalone/configuration/standalone-full.xml");

        //System.out.println(xml2.documentString(4));

        XmlPath path = XmlPath.parse("/server/profile/subsystem[@xmlns=\"urn:jboss:domain:webservices:2.0\"]");//

        System.out.println(path.toString());

        List<Xml> matches = path.getMatches(xml2);
        matches.forEach(xml21 -> {
            System.out.println(xml21.documentString(0));
        });

    }

    private Scope scope = Scope.Relative;
    private String name;
    private String value;
    private Method method = Method.Equals;//default criteria
    private Type type = Type.Undefined;
    private List<XmlPath> children;
    private XmlPath next;
    private XmlPath prev;
    private boolean isFirst = false;
    private boolean isChild = false;

    public XmlPath(Type type){
        this.type = type;
        this.children = new ArrayList<>();
    }


    private void setNext(XmlPath next){
        this.next = next;
        next.prev = this;
        if(Type.Start.equals(this.getType())){
            next.isFirst=true;
        }
    }

    private void setChild(boolean isChild){
        XmlPath target = this;
        while(target!=null){
            target.isChild = isChild;
            target = target.getNext();
        }
    }
    public boolean isChild(){return isChild;}
    public boolean isFirst(){return isFirst;}
    public boolean hasPrevious(){return prev!=null;}
    public XmlPath getPrevious(){return prev;}
    public boolean hasNext() {return next!=null;}
    public XmlPath getNext() {
        return next;
    }

    private void setDeepScan(boolean scan){
        if(scan){
            this.scope = Scope.Global;
        }
    }
    private void setName(String name){
        this.name = name;
    }
    private void setValue(String value){
        this.value = value;
    }
    private void setMethod(Method method){
        if(this.method.equals(Method.Undefined)) {
            this.method = method;
        }
    }
    private void addChild(XmlPath child){
        this.children.add(child);
    }

    public List<XmlPath> getChildren(){return Collections.unmodifiableList(children);}
    public boolean isGlobal(){return scope.equals(Scope.Global);}
    public boolean isRelative(){return scope.equals(Scope.Relative);}
    public boolean isAbsoulte(){return scope.equals(Scope.Absolute);}
    public Type getType(){return type;}
    public String getName(){return name;}
    public boolean hasValue(){return value!=null;}
    public String getValue(){return value;}
    public Method getCriteria(){return method;}

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        append(sb,true);
        return sb.toString();
    }
    public String toString(boolean recursive){
        StringBuilder sb = new StringBuilder();
        append(sb,recursive);
        return sb.toString();
    }



    public List<Xml> getMatches(Xml xml){

        List<Xml> toMatch = new ArrayList<>();
        List<Xml> tmp;
        List<Xml> matches = new ArrayList<>();
        matches.add(xml);
        XmlPath target = this;
        do {
            tmp = toMatch;
            toMatch = matches;
            matches = tmp;
            matches.clear();
            target.addMatches(toMatch, matches);


        }while(!matches.isEmpty() && (target=target.getNext())!=null);

        return matches;
    }
    public void addMatches(List<Xml> toCheck, List<Xml> matches){

        {
            for (Xml xml : toCheck) {
                if (Type.Start.equals(getType())){
                    if(xml.isDocument()){
                        matches.addAll(xml.getChildren());
                    }else{
                        matches.add(xml);
                        //matches.addAll(xml.getChildren());
                    }
                }
                if (getType().equals(Type.Attribute)) {
                    boolean rtrn = true;
                    Xml attributeXml = xml.getAttributes().get(getName());
                    if (attributeXml == null) {
                        rtrn = false;
                    } else {
                        String xmlPathValue = getValue();
                        String attributeValue = attributeXml.getValue();
                        if (xmlPathValue != null) {
                            switch (getCriteria()) {
                                case Equals:
                                    rtrn = rtrn && attributeValue.equals(getValue());
                                    break;
                                case StartsWith:
                                    rtrn = rtrn && attributeValue.startsWith(getValue());
                                    break;
                                case EndsWith:
                                    rtrn = rtrn && attributeValue.endsWith(getValue());
                                    break;
                                case Contains:
                                    rtrn = rtrn && attributeValue.contains(getValue());
                                    break;
                                default:
                                    rtrn = false;
                            }
                        } else {//expect an empty value
                            //it just needs to have the attribute so we are ok
                        }
                    }
                    if(rtrn){
                        matches.add(attributeXml);
                    }

                } else if (getType().equals(Type.Tag)) {

                    //TODO handle absolute, relative, global matching
                    if (isFirst() && !isChild()) {

                        boolean rtrn = true;
                        rtrn = rtrn && getName().equals(xml.getName());
                        if(hasValue()){
                            //TODO use criteria to match it

                            rtrn = rtrn && getValue().equals(xml.getValue());
                        }
                        if (rtrn && !getChildren().isEmpty()) {
                            List<XmlPath> pathChildren = getChildren();
                            for (int i = 0; i < pathChildren.size() && rtrn; i++) {
                                XmlPath pathChild = pathChildren.get(i);
                                List<Xml> matchers = pathChild.getMatches(xml);//new LinkedList<>();
                                //pathChild.addMatches(Collections.singletonList(xml), matches);
                                rtrn = rtrn && !matchers.isEmpty();
                            }
                        }
                        if (rtrn) {
                            matches.add(xml);
                        }
                    } else {

                        for (Xml child : xml.getChildren()) {
                            boolean rtrn = true;
                            rtrn = rtrn && getName().equals(child.getName());
                            if(hasValue()){

                                rtrn = rtrn && getValue().equals(child.getValue());
                            }

                            if (rtrn && !getChildren().isEmpty()) {
                                List<XmlPath> pathChildren = getChildren();
                                for (int i = 0; i < pathChildren.size() && rtrn; i++) {
                                    XmlPath pathChild = pathChildren.get(i);
                                    List<Xml> matchers = pathChild.getMatches(child);
                                    //pathChild.addMatches(Collections.singletonList(child), matchers);
                                    rtrn = rtrn && !matchers.isEmpty();
                                }
                            }
                            if (rtrn) {
                                matches.add(child);
                            }
                        }
                    }
                }
            }
        }

    }
    public boolean matches(perf.yaup.xml.Xml xml){

        boolean rtrn = true;
        if(getType().equals(Type.Attribute)){
            String targetValue = getValue();

            perf.yaup.xml.Xml attributeXml = xml.optAttribute(getName()).orElse(perf.yaup.xml.Xml.EMPTY);
            String attributeValue =attributeXml.getValue();

            if(targetValue!=null) {
                switch (getCriteria()) {
                    case Equals:
                        rtrn = rtrn && attributeValue.equals(getValue());
                        break;
                    case StartsWith:
                        rtrn = rtrn && attributeValue.startsWith(getValue());
                        break;
                    case EndsWith:
                        rtrn = rtrn && attributeValue.endsWith(getValue());
                        break;
                    case Contains:
                        rtrn = rtrn && attributeValue.contains(getValue());
                        break;
                    default:

                        rtrn = false;
                }
            }else{
                //attributes exists
                rtrn = rtrn && !attributeValue.isEmpty();
            }
        }else if (getType().equals(Type.Tag)){
            //Tag type doesn't support criteria other than equals
            rtrn = rtrn && getName().equals(xml.getName());

            if(!getChildren().isEmpty()){
                List<XmlPath> children = getChildren();
                for(int i=0; i<children.size(); i++){
                    XmlPath child = children.get(i);

                    rtrn = rtrn && child.matches(xml);
                }
            }
        }else{
            //wtf
        }
        return rtrn;
    }

    private void append(StringBuilder sb,boolean recursive){
        sb.append(":"+getType()+":");
        if(getType().equals(Type.Attribute)){
            if(!isFirst()){
                sb.append(TAG_SEPARATOR);
            }
            sb.append(ATTRIBUTE_PREFIX);
        }
        if (Type.Tag.equals(getType())) {
            if(isGlobal()){
                sb.append(DEEP_SEARCH);
            }else if (isAbsoulte()) {
                sb.append(TAG_SEPARATOR);
            }else if (isRelative()){
                if(isFirst){
                    sb.append("./");
                }else {
                    sb.append(TAG_SEPARATOR);
                }
            }
        }
        if(getType().equals(Type.Start)){
            //sb.append("START");
        }else {
            sb.append(getName());
        }
//        if(getType().equals(Type.Attribute) && getCriteria()!= Method.Undefined){
//
//            if(hasValue()) {
//                sb.append(getCriteria().getOperator());
//                sb.append("\"");
//                sb.append(getValue());
//                sb.append("\"");
//            }
//        }
        if(!getChildren().isEmpty()){
            sb.append(CRITERIA_LIST_PREFIX);
            List<XmlPath> children = getChildren();
            for(int i=0; i<children.size(); i++){
                XmlPath child = children.get(i);
                if(i>0) {
                    sb.append(" and ");
                }
                child.append(sb,true);
            }
            sb.append(CRITERIA_LIST_SUFFIX);
        }
        if(hasValue()){

            sb.append(getCriteria().getOperator());
            sb.append("\"");
            sb.append(getValue());
            sb.append("\"");
        }

        if(hasNext() && recursive){
            getNext().append(sb,recursive);
        }
    }
}
