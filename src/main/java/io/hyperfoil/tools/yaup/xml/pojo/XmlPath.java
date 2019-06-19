package io.hyperfoil.tools.yaup.xml.pojo;

import io.hyperfoil.tools.yaup.StringUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * supported:
 * /absolute/path/to/node
 * /node//findInScope
 * /node/@attribute
 * /node/foo[/criteria]
 * /node/foo[/criteria = 'value']
 * /node/foo[/criteria ^ 'value']
 * /node/foo[/criteria $ 'value']
 * /node/text()
 * /node/foo/position()
 * /node/foo[1]
 * /node/foo/[1]
 */
public class XmlPath {

    public static enum Scope {Descendant,Absolute,Relative}

    public static enum Method {
        Undefined('?'),Equals('='),StartsWith('^'),EndsWith('$'),Contains('~'),GreaterThan('>'),LessThan('<');

        private char operator;
        Method(char operator){
            this.operator = operator;
        }

        public char getOperator(){return operator;}
    }
    public enum Type {Undefined,Start,Tag,Attribute,Function,Index}


    private static enum State {Path,Criteria,Function}

    private static XmlPath error(String error){
        return new XmlPath(error);
    }

    public static XmlPath parse(String path){
        int index = 0;
        XmlPath rtrn = new XmlPath(Type.Start);

        Stack<XmlPath> parentStack = new Stack<>();
        parentStack.push(rtrn);

        Stack<State> state = new Stack<>();
        state.push(State.Path);

        int previousLoopIndex = index-1;//offset for initial loop check

        String operators = Arrays.asList(Method.values()).stream().map(m->m.getOperator()+"").collect(Collectors.joining(""));
        operators = StringUtil.escapeRegex(operators);

        Matcher pathMatcher = Pattern.compile("(?<prefix>\\.{0,2}/{0,2})(?<attr>@?)(?<name>[^'\"\\s,/\\(\\[\\]@"+operators+"]+)(?<suffix>[\\(\\[/]?).*").matcher(path);

        Set<Character> methodOperators = new HashSet<>();
        for(int i=0; i<Method.values().length;i++){
            methodOperators.add(Method.values()[i].getOperator());
        }

        while(previousLoopIndex<index && index < path.length()){
            previousLoopIndex = index;
            //attribute or path fragment
            if(path.startsWith(")",index)){
                if(!State.Function.equals(state.peek())){
                    return error("unexpected close function arguments ')' @ "+index+" in "+path);
                }
                parentStack.pop();//remove the previous argument
                state.pop();
                int nonSpaceIndex =StringUtil.indexNotMatching(path,"     ",index+1);
                index = nonSpaceIndex;
            }else if (path.startsWith("]",index)){
                if(!State.Criteria.equals(state.peek())){
                    return error("unexpected close criteria ']' @ "+index+" in "+path);
                }
                parentStack.pop();
                state.pop();
                int nonSpaceIndex =StringUtil.indexNotMatching(path,"     ",index+1);
                index = nonSpaceIndex;
            }else if (path.startsWith(",",index)){
                if(!State.Function.equals(state.peek())){
                    return error("unexpected function argument separator ',' @ "+index+" in "+path);
                }
                parentStack.pop();
                XmlPath newStart = new XmlPath(Type.Start);
                parentStack.peek().addChild(newStart);
                parentStack.push(newStart);

                int nonSpaceIndex =StringUtil.indexNotMatching(path,"     ",index+1);
                index = nonSpaceIndex;
            }else if ( methodOperators.contains(path.charAt(index)) ) {
                char operatorChar = path.charAt(index);

                Method method = Arrays.asList(Method.values()).stream().filter(m->operatorChar==m.getOperator()).findFirst().orElse(Method.Undefined);
                if (Type.Start.equals(parentStack.peek().getType())) {
                    return error("unexpected value comparison = @ " + index + " in " + path);
                }
                parentStack.peek().setMethod(method);

                int nonSpaceIndex = StringUtil.indexNotMatching(path, "     ", index + 1);
                index = nonSpaceIndex;
            }else if (path.startsWith("[",index)){
                //index
                int closeIndex = StringUtil.indexNotMatching(path,"1234567890",index+1);
                String criteria = path.substring(index+1,closeIndex);
                if(!criteria.isEmpty() && criteria.matches("\\d+")){
                    XmlPath childIndex = new XmlPath(Type.Index);
                    childIndex.setMethod(Method.Equals);
                    childIndex.setName(criteria);
                    parentStack.peek().setNext(childIndex);
                    parentStack.push(childIndex);
                    index = closeIndex+1;
                }

            }else if (path.startsWith("'",index) || path.startsWith("\"",index)) {
                if (Type.Start.equals(parentStack.peek().getType())) {
                    return error("unexpected string constant @ " + index + " in " + path);
                }

                char quoteChar = path.charAt(index);
                int closeIndex = index + 1;
                while (closeIndex < path.length() && quoteChar != path.charAt(closeIndex) || (closeIndex > 1 && '/' == path.charAt(closeIndex - 1))) {
                    closeIndex++;

                }

                if (closeIndex == path.length()) {
                    return error("failed to find closing " + quoteChar + " which starts @ " + index + " in " + path);
                }
                String quotedValue = path.substring(index + 1, closeIndex);
                closeIndex++;//to include the trailing quote

                parentStack.peek().setValue(quotedValue);

                int nonSpaceIndex = StringUtil.indexNotMatching(path, "     ", closeIndex);
                index = nonSpaceIndex;
            }else if ( State.Criteria.equals(state.peek()) && "0123456789".contains(""+path.charAt(index)) ){
                int nonDigit = StringUtil.indexNotMatching(path,"1234567890",index);
                String digits = path.substring(index,nonDigit);
                parentStack.peek().setValue(digits);
                index = nonDigit;
                int notSpace = StringUtil.indexNotMatching(path,"   ",index);
                index = notSpace;
            }else if ( State.Criteria.equals(state.peek()) &&
                    (path.startsWith("and",index) || path.startsWith("AND",index)) ) {

                parentStack.pop();//remove previous criteria

                XmlPath newStart = new XmlPath(Type.Start);
                parentStack.peek().addChild(newStart);
                parentStack.push(newStart);

                int nonSpaceIndex = StringUtil.indexNotMatching(path,"     ",index+"and".length());
                index = nonSpaceIndex;
            }else if(pathMatcher.reset(path.substring(index)).matches()){
                String prefix = pathMatcher.group("prefix");
                String attr = pathMatcher.group("attr");
                String name = pathMatcher.group("name");
                String suffix = pathMatcher.group("suffix");

                Type type = Type.Tag;
                Scope scope = Scope.Relative;
                if("@".equals(attr)){
                    type = Type.Attribute;
                }else if ("(".equals(suffix)){
                    type = Type.Function;
                }
                if("/".equals(prefix) ){
                    if( !Type.Start.equals(parentStack.peek().getType()) ) {
                        if(index>0 && '/' == path.charAt(index-1)){// double // means descendant
                            scope = Scope.Descendant;
                        } else {
                            scope = Scope.Absolute;
                        }
                    }
                }else if("//".equals(prefix)){
                    scope = Scope.Descendant;
                }else if("".equals(prefix)) {

                    //relative start
                }else if("./".equals(prefix)){
                    //TODO handle this scope for children of current node
                    return error("unsupported scope=["+prefix+"] @ "+index+" in "+path);
                }else {
                    return error("unsupported scope=["+prefix+"] @ "+index+" in "+path);
                }

                XmlPath nextPath = new XmlPath(type);
                nextPath.setName(name);
                nextPath.setScope(scope);

                parentStack.peek().setNext(nextPath);
                parentStack.pop();
                parentStack.push(nextPath);
                if("[".equals(suffix)){
                    int suffixIndex = index + pathMatcher.end("suffix");
                    int closeIndex = path.indexOf("]",suffixIndex);


                    if(closeIndex>suffixIndex && path.substring(suffixIndex,closeIndex).matches("\\d+")){
                        String criteria = path.substring(suffixIndex,closeIndex);

                        XmlPath childIndex = new XmlPath(Type.Index);
                        //childIndex.setName(parentStack.peek().getName());
                        childIndex.setName(criteria);

                        index+=closeIndex-suffixIndex+"]".length();
                        parentStack.peek().addChild(childIndex);
                        parentStack.pop();
                        parentStack.push(childIndex);

                    }else {
                        XmlPath newStart = new XmlPath(Type.Start);
                        parentStack.peek().addChild(newStart);
                        parentStack.push(newStart);
                        state.push(State.Criteria);
                    }
                }else if ("(".equals(suffix)){

                    if(index+pathMatcher.end("suffix") < path.length() && ')'==path.charAt(index+pathMatcher.end("suffix"))){
                        index++;
                    }else {
                        XmlPath newStart = new XmlPath(Type.Start);
                        parentStack.peek().addChild(newStart);

                        parentStack.push(newStart);
                        state.push(State.Function);
                    }
                }else if (!"/".equals(suffix) && !"".equals(suffix)){
                    return error("unknown path separator ["+suffix+"] @ "+(index+pathMatcher.start("suffix"))+" in "+path);
                }
                int nonSpaceIndex = StringUtil.indexNotMatching(path,"     ",index+pathMatcher.end("suffix"));
                index = nonSpaceIndex;
            }
        }
        if(index < path.length()){
            return error("failed to parse @ "+index+" in "+path);
        }
        return rtrn;
    }
    @Override
    public boolean equals(Object other){
        boolean rtrn = false;
        if(other instanceof XmlPath){
            XmlPath otherPath = (XmlPath)other;
            rtrn =
                this.scope == otherPath.scope &&
                this.getName() == otherPath.getName() &&
                this.getType() == otherPath.getType() &&
                this.getValue() == otherPath.getValue() &&
                this.getMethod() == otherPath.getMethod() &&
                this.isChild() == otherPath.isChild() &&
                this.isFirst == otherPath.isFirst;
        }
        return rtrn;
    }

    private Scope scope = Scope.Relative;
    private String name;
    private String value;
    private Method method = Method.Equals;//default criteria
    private Type type = Type.Undefined;
    private List<XmlPath> children;
    private XmlPath next;
    private XmlPath prev;
    private XmlPath parent;
    private boolean isFirst = false;
    private boolean isCriteria = false;

    public XmlPath(Type type){
        this.type = type;
        this.children = new ArrayList<>();
    }

    private XmlPath(String error){
        this(Type.Undefined);
        this.name="Error: ";
        this.value = error;
    }

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

    public XmlPath copy(){
        if(!isValid()){
            return new XmlPath(this.getValue());
        }
        XmlPath rtrn = new XmlPath(this.getType());
        rtrn.setScope(this.getScope());
        rtrn.setName(this.getName());
        rtrn.setValue(this.getValue());
        rtrn.setMethod(this.getMethod());
        rtrn.isFirst = this.isFirst;
        rtrn.isCriteria = this.isCriteria;

        if(this.hasNext()){
            rtrn.setNext(this.getNext().copy());
        }
        if(!this.getChildren().isEmpty()){
            for(XmlPath child: getChildren()){
                rtrn.addChild(child.copy());
            }
        }
        return rtrn;
    }

    private Scope getScope(){return scope;}


    public void dropNext(){
        if(hasNext()){
            if(this.getNext().hasParent()){
                this.getNext().getParent().children.remove(this.getNext());
            }
            this.next = null;
        }
    }
    public void drop(){
        if(hasPrevious()){
            getPrevious().next=null;
        }
        if(hasParent()){
            getParent().children.remove(this);
        }
    }

    private void setNext(XmlPath next){
        this.next = next;
        next.prev = this;
        if(Type.Start.equals(this.getType())){
            next.isFirst=true;
        }
        if(this.isChild()){
            next.setParent(this.getParent());

        }
    }

    private void setParent(XmlPath parent){
        XmlPath target = this;
        while(target!=null){
            target.isCriteria = true;
            target.parent = parent;
            target = target.getNext();
        }
    }
    public boolean hasParent(){return parent!=null;}
    public XmlPath getParent(){
        return this.parent;
    }
    public boolean isChild(){return isCriteria;}
    public boolean isFirst(){return isFirst;}
    public boolean hasPrevious(){return prev!=null;}
    public XmlPath getPrevious(){return prev;}
    public boolean hasNext() {return next!=null;}
    public XmlPath getNext() {
        return next;
    }
    private void setScope(Scope scope){
        this.scope = scope;
    }
    private void setName(String name){
        this.name = name;
    }
    private void setValue(String value){
        this.value = value;
    }
    private void setMethod(Method method){
        this.method = method;
    }
    private void addChild(XmlPath child){
        this.children.add(child);
        child.setParent(this);
    }

    public boolean hasChildren(){return !children.isEmpty();}
    public List<XmlPath> getChildren(){return Collections.unmodifiableList(children);}

    public boolean isDescendant(){return scope.equals(Scope.Descendant);}
    public boolean isRelative(){return scope.equals(Scope.Relative);}
    public boolean isAbsoulte(){return scope.equals(Scope.Absolute);}

    public boolean isValid(){return !Type.Undefined.equals(getType());}
    public Type getType(){return type;}

    public String getName(){return name;}
    public boolean hasValue(){return value!=null;}
    public String getValue(){return value;}
    public Method getMethod(){return method;}

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

            if(target.isDescendant()){
                for(int i=0; i<toMatch.size();i++){
                    Xml entry = toMatch.get(i);
                    toMatch.addAll(entry.getChildren());
                }
            }
            target.collectMatches(toMatch, matches);

        }while(!matches.isEmpty() && (target=target.getNext())!=null);

        return matches;
    }

    //right now we merge all matches into the same array, this wont' work with index [0] references
    //because it treats all previous matches as being on the same level... maybe we add an index on the current?
    public void collectMatches(List<Xml> toCheck, List<Xml> matches){

        for (Xml xml : toCheck) {
            if (Type.Start.equals(getType())){
                if(xml.isDocument()){
                    matches.addAll(xml.getChildren());
                }else{
                    //not a document so could be the result of previous match?
                    //how do we know when the input path wants to match against a child not this?
                    //TODO when was this necessary?
                    matches.add(xml);
                    //matches.addAll(xml.getChildren());
                }
            }
            if ( Type.Attribute.equals(getType()) ) {
                boolean rtrn = true;
                Xml attributeXml = xml.getAttributes().get(getName());
                if (attributeXml == null) {
                    rtrn = false;
                } else {
                    String xmlPathValue = getValue();
                    String attributeValue = attributeXml.getValue();
                    if(xmlPathValue!=null) {
                        rtrn = rtrn && methodMatch(attributeValue);
                    } else {//expect an empty value
                        //it just needs to have the attribute so we are ok
                    }
                }
                if(rtrn){
                    matches.add(attributeXml);
                }

            } else if (Type.Tag.equals(getType())) {

                if (isFirst() && !isChild()) {

                    boolean rtrn = true;
                    rtrn = rtrn && getName().equals(xml.getName());
                    if(hasValue()){
                        boolean methodMatches = methodMatch(xml.getValue());
                        rtrn = rtrn && methodMatches;
                    }
                    if (rtrn && !getChildren().isEmpty()) {
                        List<XmlPath> pathChildren = getChildren();
                        for (int i = 0; i < pathChildren.size() && rtrn; i++) {
                            XmlPath pathChild = pathChildren.get(i).getNext();//use next to skip the start pathN
                            List<Xml> matchers = pathChild.getMatches(xml);
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
                        if(rtrn && hasValue()){
                            boolean methodMatches = methodMatch(child.getValue());
                            rtrn = rtrn && methodMatches;
                        }
                        if (rtrn && !getChildren().isEmpty()) {
                            List<XmlPath> pathChildren = getChildren();
                            for (int i = 0; i < pathChildren.size() && rtrn; i++) {
                                XmlPath pathChild = pathChildren.get(i);
                                List<Xml> matchers = pathChild.getMatches(child);
                                rtrn = rtrn && !matchers.isEmpty();
                            }
                        }
                        if (rtrn) {
                            matches.add(child);
                        }
                    }
                }
            }else if (Type.Function.equals(getType())){
                //yikes
                if("text".equals(getName())){
                    String xmlValue = xml.getValue();
                    if(hasValue()){
                        switch(getMethod()){
                            case Contains:
                                if(xmlValue.contains(getValue())){
                                    matches.add(xml.getValueXml());
                                }
                                break;
                            case Equals://technically different than Xpath spec which would use contains
                                if(xmlValue.equals(getValue())){
                                    matches.add(xml.getValueXml());
                                }
                                break;
                            case StartsWith:
                                if(xmlValue.startsWith(getValue())){
                                    matches.add(xml.getValueXml());
                                }
                            case EndsWith:
                                if(xmlValue.endsWith(getValue())){
                                    matches.add(xml.getValueXml());
                                }
                                break;
                            default:
                                System.out.println("text does not support method="+getMethod());
                        }
                    }else{
                        matches.add(xml.getValueXml());
                    }
                } else if ("position".equals(getName())){
                    int position = xml.tagIndex();
                    if (getValue().matches("\\d+") ){
                        int valueInt = Integer.parseInt(getValue());
                        switch (getMethod()){
                            case Equals:
                                if(position == valueInt){
                                    matches.add(xml);
                                }
                                break;
                            case GreaterThan:
                                if(position > valueInt){
                                    matches.add(xml);
                                }
                                break;
                            case LessThan:
                                if(position < valueInt){
                                    matches.add(xml);
                                }
                                break;
                        }
                    }
                }
            } else if (Type.Index.equals(getType())){

                int targetIndex = Integer.parseInt(getName());
                if(isChild()){
                    if(targetIndex == xml.namedIndex()){
                        matches.add(xml);
                    }
                }else{
                    //xml is the parent
                    if(xml.getChildren().size()>targetIndex){
                        matches.add(xml.getChildren().get(targetIndex));
                    }
                }
            }
        }
    }

    public boolean methodMatch(String foundValue){
        boolean rtrn = foundValue != null;
        switch (getMethod()) {
            case Equals:
                rtrn = rtrn && foundValue.equals(getValue());
                break;
            case StartsWith:
                rtrn = rtrn && foundValue.startsWith(getValue());
                break;
            case EndsWith:
                rtrn = rtrn && foundValue.endsWith(getValue());
                break;
            case Contains:
                rtrn = rtrn && foundValue.contains(getValue());
                break;
            case Undefined:
            default:
                rtrn = false;
        }
        return rtrn;
    }

    private void append(StringBuilder sb,boolean recursive){
        if(Type.Attribute.equals(getType())){
            sb.append("@");
        }else if (Type.Tag.equals(getType())){
            if(isFirst()){
                if(Scope.Descendant.equals(getScope())){
                    sb.append("//");
                }else{
                    sb.append("/");
                }
            }else{
                if(Scope.Descendant.equals(getScope())){
                    sb.append("//");
                }else{
                    sb.append("/");
                }
            }
        }else if (Type.Function.equals(getType())){
            if(isFirst()){
                if(Scope.Descendant.equals(getScope())){
                    sb.append("//");
                }else{
                    sb.append("/");
                }
            }else{
                if(Scope.Descendant.equals(getScope())){
                    sb.append("//");
                }else{
                    sb.append("/");
                }
            }
        }else if (Type.Index.equals(getType())){
            if(!isChild()) {
                sb.append("/[");
            }
        }
        if(!Type.Start.equals(getType())) {
            sb.append(getName());
        }
        List<XmlPath> children = getChildren();
        String childWrap = Type.Function.equals(getType()) ? "()" : "[]";
        if(!children.isEmpty()){
            sb.append(childWrap.charAt(0));
            for(int i=0; i<children.size(); i++){
                if (i > 0) {
                    sb.append(" and ");
                }
                XmlPath child = children.get(i);
                child.append(sb,recursive);
            }
            sb.append(childWrap.charAt(1));;
        }else if (Type.Function.equals(getType())){
            sb.append("()");//no children but still a function
        }
        if(Type.Index.equals(getType())){
            if(!isChild()){
                sb.append("]");
            }
        }
        if(hasValue()){
            sb.append(" ");
            sb.append(method.getOperator());
            sb.append(" ");
            sb.append(getValue());
        }
//        sb.append("} ");
        if(recursive && hasNext()){
            getNext().append(sb,recursive);
        }
    }
}
