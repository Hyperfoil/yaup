package io.hyperfoil.tools.yaup.xml.pojo;

import io.hyperfoil.tools.yaup.HashedLists;
import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.file.FileUtility;
import io.hyperfoil.tools.yaup.json.Json;
import io.hyperfoil.tools.yaup.xml.XmlOperation;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;

import static io.hyperfoil.tools.yaup.xml.XmlOperation.Operation;
import static io.hyperfoil.tools.yaup.xml.XmlOperation.Operation.Add;

public class Xml {

    public static final String JSON_ATTRIBUTE_PREFIX = "@";
    public static final String JSON_VALUE_KEY = "text()";

    public static final int INLINE_LENGTH = 120;

    public static final Xml INVALID = new Xml(Type.Invalid,null,"","");

    public static final String COMMENT_PREFIX = "<!--";
    public static final String END_TAG_PREFIX = "</";
    public static final String START_TAG_PREFIX = "<";

    public static final String COMMENT_SUFFIX = "-->";
    public static final String CLOSE_TAG_SUFFIX = ">";
    public static final String EMPTY_TAG_SUFFIX = "/>";

    public static final String ATTRIBUTE_PREFIX="@";
    public static final String ATTRIBUTE_VALUE_PREFIX="=";
    public static final String ATTRIBUTE_WRAPPER="\"";

    public static Xml parseFile(String path){
        Xml rtrn = null;
        try(InputStream stream = FileUtility.getInputStream(path)){
          rtrn = parse(stream,path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(rtrn == null){
            rtrn = new Xml(Type.Document,null,"xml");
        }
        return rtrn;
    }
    public static Xml parse(String content){
        return parse(new ByteArrayInputStream(content.getBytes()));
    }
    public static Xml parse(InputStream stream){
        return parse(stream,"InputStream");
    }
    public static Xml parse(InputStream stream,String streamName){
        Xml rtrn = new Xml(Type.Document,null,"xml");//document
        Stack<Xml> parentStack = new Stack<>();
        parentStack.push(rtrn);
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

        try {
            XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(stream);
            while (xmlEventReader.hasNext()) {
                XMLEvent xmlEvent = xmlEventReader.nextEvent();
                if(xmlEvent.isStartDocument()){

                    StartDocument startDocumentEvent = (StartDocument)xmlEvent;
                    if(startDocumentEvent.getVersion()!=null){
                        rtrn.setAttribute(new Xml(Type.Attribute,rtrn,"version",startDocumentEvent.getVersion()));
                    }
                    if(startDocumentEvent.encodingSet()){
                        rtrn.setAttribute(new Xml(Type.Attribute,rtrn,"encoding",startDocumentEvent.getCharacterEncodingScheme()));
                    }
                    if(startDocumentEvent.getSystemId()!=null){
                        rtrn.setAttribute(new Xml(Type.Attribute,rtrn,""));
                    }
                    if(startDocumentEvent.standaloneSet()){
                        rtrn.setAttribute(new Xml(Type.Attribute,rtrn,"standalone",startDocumentEvent.isStandalone()?"yes":"no"));
                    }
                }

                if(xmlEvent.isStartElement()){

                    StartElement startElement = xmlEvent.asStartElement();

                    String newElementName = startElement.getName().getLocalPart();
                    if(!startElement.getName().getPrefix().isEmpty()){
                        newElementName = startElement.getName().getPrefix()+":"+newElementName;
                    }
                    Xml newElement = new Xml(Type.Tag,parentStack.peek(),newElementName);

                    for(Iterator<Namespace> namespaceIterator = startElement.getNamespaces(); namespaceIterator.hasNext();){
                        Namespace namespace = namespaceIterator.next();
                        String namespaceName = namespace.getName().getLocalPart();
                        String namespacePrefix = namespace.getPrefix();
                        if(namespacePrefix.isEmpty()){
                            namespacePrefix="xmlns";
                        }else{
                            namespacePrefix="xmlns:"+namespacePrefix;
                        }
                        String namespaceValue = namespace.getNamespaceURI();
                        Xml newAttribute = new Xml(Type.Attribute,newElement,namespacePrefix,namespaceValue);
                        newElement.setAttribute(newAttribute);
                    }


                    for(Iterator attributeIterator = startElement.getAttributes(); attributeIterator.hasNext();){
                        Attribute attribute = (Attribute)attributeIterator.next();
                        String attributeName = attribute.getName().getLocalPart();
                        String attributePrefix = attribute.getName().getPrefix();
                        if(attributePrefix!=null && !attributePrefix.isEmpty()){
                            attributeName = attribute.getName().getPrefix()+":"+attributeName;
                        }
                        Xml newAttribute = new Xml(Type.Attribute,newElement,attributeName);

                        newAttribute.setValue(attribute.getValue());
                        newElement.setAttribute(newAttribute);
                    }
                    parentStack.peek().addChild(newElement);

                    parentStack.push(newElement);
                }else if(xmlEvent.isCharacters()){
                    String value = xmlEvent.asCharacters().getData().trim();

                    if(!value.isEmpty()) {
                        Xml textElement = new Xml(Type.Text, parentStack.peek(),"#text");
                        textElement.setValue(xmlEvent.asCharacters().getData());
                        parentStack.peek().addChild(textElement);
                    }


                }else if (xmlEvent.isEndElement()){

                    parentStack.pop();
                }else if (xmlEvent.isEndDocument()){

                }
            }
        }catch (XMLStreamException e) {
            System.out.println("XMLSException "+e.getMessage()+" for "+streamName);
            parentStack.forEach(entry-> System.out.println(entry.getName()+" "+entry.getType()+" "+entry.getChildren().size()));
            //e.printStackTrace();
        }

        return rtrn;
    }

    public enum Type {Invalid,Document,Tag,Attribute,Comment,Text}

    private String name;
    private String value;
    private Type type;
    private Map<String,Xml> attributes;
    private ArrayList<Xml> allChildren;
    private ArrayList<Xml> tagChildren;
    private ArrayList<Xml> textChildren;
    private HashedLists<String,Xml> namedTagChildren;
    private Xml parent;
    int tagIndex=-1;
    int namedIndex =-1;

    private Xml(Type type, Xml parent, String name){
        this(type,parent, name,null);
    }
    private Xml(Type type,Xml parent, String name, String value){
        this.type = type;
        this.parent = parent;
        this.name=name;
        this.value=value;
        this.attributes = new LinkedHashMap<>();
        this.allChildren = new ArrayList<>();
        this.tagChildren = new ArrayList<>();
        this.namedTagChildren = new HashedLists<>();

        this.textChildren = new ArrayList<>();
    }
    public Xml get(String search) {
        //detect simple attribute or child search and use attribute / firstChild
        if (!search.contains("/") && !search.contains("[") && search.startsWith("@")) {
            //just an attribute
            return attribute(search.substring(1));
        } else if ( !search.contains("@") && search.lastIndexOf("/")<=0 && !search.contains("[")) {
            //just a tag
            return firstChild(search);
        } else {
            List<Xml> list = getAll(search);
            return list.isEmpty() ? new Xml(Type.Invalid,null,"") : list.get(0);
        }
    }
    public List<Xml> getAll(String search){
        if(!search.contains("@") && !search.contains("/") && !search.contains("[")){
            return Collections.unmodifiableList(namedTagChildren.get(search));
        }else {
            XmlPath path = XmlPath.parse(search);
            List<Xml> rtrn = path.getMatches(this);
            return rtrn;
        }
    }


    public void addChild(Xml child){
        if(!child.exists()){
            return;
        }
        if(child.isAttribute()){
            setAttribute(child);
        }else{
            if(child.isDocument()){
                if(child.tagChildren.size()>0){
                    child = child.tagChildren.get(0);
                }
            }
            allChildren.add(child);
            child.parent = this;
            if (child.getType().equals(Type.Text)) {
                textChildren.add(child);
                addValue(child.getValue());
            } else if (child.getType().equals(Type.Tag)) {
                int tagIndex = tagChildren.size();
                tagChildren.add(child);
                int namedIndex = namedTagChildren.get(child.getName()).size();
                namedTagChildren.put(child.getName(), child);
                child.setIndexes(tagIndex, namedIndex);
            }
        }
    }
    public void setAttribute(Xml child){
        if(!child.exists()){
            return;
        }
        int index = attributes.size();
        Xml previous = attributes.put(child.getName(),child);
        child.parent=this;
        child.type = Type.Attribute;

    }
    public boolean hasAttribute(String name){
        return attributes.containsKey(name);
    }
    public void removeAttribute(String name){
        if(hasAttribute(name)){
            attributes.remove(name);
        }
    }
    public void removeChild(Xml child){
        allChildren.remove(child);
        if(child.isText()){
            resetTextValue();
        }else if (child.isTag()){
            tagChildren.remove(child);
        }else if (child.isAttribute()){
            removeAttribute(child.getName());
        }
    }
    public void removeChild(int index){
        allChildren.remove(index);
    }

    private void setIndexes(int tagIndex,int namedIndex){
        this.tagIndex = tagIndex;
        this.namedIndex = namedIndex;
    }

    int tagIndex(){return tagIndex;}
    int namedIndex(){return namedIndex;}
    private Type getType(){return type;}
    public String getName(){return name;}
    public String getValue(){
        return hasValue()?value:"";
    }
    Xml getValueXml(){
        //TODO should this be detached from parent so it cannot attempt to modify?
        return new Xml(Type.Text,null,"#text",getValue());
    }
    public boolean hasValue(){return value!=null;}
    private void addValue(String value){
        if(this.value==null){
            this.value="";
        }
        this.value += value;
    }
    public void setValue(String value){
        this.value = value;
    }
    public Xml parent(String tagName){
        Xml rtrn = new Xml(Type.Invalid,null,tagName);
        Xml target = this.parent;
        while(!rtrn.exists() && target!=null){
            if(tagName.equals(target.getName())){
                rtrn = target;
            }
        }
        return rtrn;
    }
    public Xml parent(){return parent;}
    private void clearText(){
        Iterator<Xml> childIter = allChildren.iterator();
        while(childIter.hasNext()){
            Xml child = childIter.next();
            if(child.isText()){
                childIter.remove();
            }
        }
        value = "";
    }
    private void resetTextValue(){
        value = "";
        allChildren.forEach(child->{
            if(child.isText()){
                addValue(child.getValue());
            }
        });
    }
    private void clear(){
        tagChildren.clear();
        allChildren.clear();
        value = "";
    }
    public List<Xml> getChildren(){
        return Collections.unmodifiableList(tagChildren);
    }
    public Xml firstChild(String tagName){
        if(isDocument() && !tagChildren.isEmpty() && !tagName.equals(tagChildren.get(0).getName())){
            return tagChildren.get(0).firstChild(tagName);
        }
        Xml rtrn = new Xml(Type.Invalid,this,tagName);
        if(!exists()){
            return rtrn;
        }else{
            for(int i=0; i<tagChildren.size() && !rtrn.exists();i++){
                Xml child = tagChildren.get(i);
                if(tagName.equals(child.getName())){
                    rtrn = child;
                    break;
                }
            }
        }
        return rtrn;
    }
    public static Object convertString(String input){
        if(input.matches("-?\\d{1,18}")){
            return Long.parseLong(input);
        }else if (input.matches("-?\\d*(?:\\.\\d+)?")){
            return Double.parseDouble(input);
        }
        return input;
    }
    private List<Xml> allChildren(){
        return Collections.unmodifiableList(allChildren);
    }
    public Xml attribute(String name){
        if(isDocument() && !tagChildren.isEmpty() ){
            return tagChildren.get(0).attribute(name);
        }

        if(!exists()){
            return new Xml(Type.Invalid,this,name);
        }
        return attributes.containsKey(name)
                ? attributes.get(name)
                : new Xml(Type.Invalid,this,name);
    }
    public Map<String,Xml> getAttributes(){
        return Collections.unmodifiableMap(attributes);
    }

    public Json toJson(){
       return toJson(JSON_ATTRIBUTE_PREFIX,JSON_VALUE_KEY,true);
    }
    public Json toJson(String attributePrefix,String valueKey,boolean convertNumbers){
        Json rtrn = new Json();

        Stack<Json> jsonTodo = new Stack<>();
        Stack<Xml> xmlTodo = new Stack<>();

        jsonTodo.push(rtrn);
        xmlTodo.push(this);

        while(!jsonTodo.isEmpty()){
            Json json = jsonTodo.pop();
            Xml xml = xmlTodo.pop();

            xml.getAttributes().forEach((name,valueXml)->{
                json.set(attributePrefix+name,convertNumbers ? Xml.convertString(valueXml.getValue()) : valueXml.getValue());
            });
            if(xml.hasValue()){
                json.set(valueKey,convertNumbers ? Xml.convertString(xml.getValue()) : xml.getValue());
            }
            xml.getChildren().forEach(childXml->{
                //Json(false) to prevent Json.add from treating it as an array for subsequent adds
                Json child = new Json(false);
                
                json.add(childXml.getName(),child);
                jsonTodo.push(child);
                xmlTodo.push(childXml);
            });
        }

        return rtrn;
    }

    public boolean exists(){return !Type.Invalid.equals(getType());}
    public boolean isDocument(){
        return getType().equals(Type.Document);
    }
    public boolean isAttribute(){
        return getType().equals(Type.Attribute);
    }
    public boolean isTag(){
        return getType().equals(Type.Tag);
    }
    public boolean isText(){
        return getType().equals(Type.Text);
    }

    public void attributeWalk(Consumer<Xml> toWalk){
        attributes.values().forEach(toWalk);
        for(Xml child : getChildren()){
            child.attributeWalk(toWalk);
        }
    }
    public void childWalk(Consumer<Xml> toWalk){
        toWalk.accept(this);
        for(Xml child : allChildren()){
            child.childWalk(toWalk);
        }
    }

    public String apply(String xmlOperation){
        XmlOperation toApply =XmlOperation.parse(xmlOperation);
        return apply(toApply);
    }
    public String apply(XmlOperation xmlOperation){
        if(!exists()){
            return null;
        }

        StringBuilder rtrn = new StringBuilder();

        XmlPath xmlPath = XmlPath.parse(xmlOperation.getPath());

        List<Xml> found = Collections.EMPTY_LIST;

        Operation opp = xmlOperation.getOperation();
        String value = xmlOperation.getValue();

        found = xmlPath.getMatches(this);
        if(found.isEmpty()){
            if(xmlOperation.isAdd() || xmlOperation.isSet()){
                xmlPath = xmlPath.copy();
                XmlPath tail = xmlPath.getTail();
                tail.drop();
                if(XmlPath.Type.Attribute.equals(tail.getType())){
                    //change to add and re-try only if tail doesn't have children
                    if(!tail.hasChildren() && xmlOperation.isSet()){
                        opp = Add;
                        value = ATTRIBUTE_PREFIX+tail.getName()+Xml.ATTRIBUTE_VALUE_PREFIX+xmlOperation.getValue();
                        found = xmlPath.getMatches(this);
                    }
                }else if (XmlPath.Type.Tag.equals(tail.getType())){
                    if(xmlOperation.isSet()){
                        opp = Add;
                        StringBuilder sb = new StringBuilder();
                        sb.append("<");
                        sb.append(tail.getName());
                        if(tail.hasChildren()){
                            for(XmlPath child : tail.getChildren()){
                                if(XmlPath.Type.Start.equals(child.getType())){
                                    child = child.getNext();
                                }
                                if(XmlPath.Type.Attribute.equals(child.getType())){
                                    sb.append(" ");
                                    sb.append(child.getName());
                                    sb.append(Xml.ATTRIBUTE_VALUE_PREFIX);
                                    sb.append(StringUtil.quote(child.getValue()));
                                }else{
                                    System.out.println("cannot build ADD out of child "+child);
                                    sb.delete(0,sb.length());
                                    break;
                                }
                            }
                        }
                        sb.append(">");
                        if(sb.length()>1){
                            sb.append(value);
                            sb.append("</");
                            sb.append(tail.getName());
                            sb.append(">");
                            value = sb.toString();
                        }
                        found = xmlPath.getMatches(this);
                    }
                }else{
                    System.out.println("unsupported type = "+tail.getType());
                }
            }
        }
        String finalValue = value.trim();
        if(!found.isEmpty()){
            switch (opp){
                case None:
                    found.forEach(x->{
                        if(rtrn.length()>0){
                            rtrn.append(System.lineSeparator());
                        }
                        rtrn.append(x.getValue());
                    });
                    break;
                case Set:

                    found.forEach(x->{
                        if(x.isAttribute()){
                            x.setValue(finalValue);
                        }else if (x.isTag()){
                            if(finalValue.startsWith(START_TAG_PREFIX)){
                                Xml toSet = Xml.parse(finalValue);
                                x.clear();
                                x.addChild(toSet);
                            }else{
                                x.clearText();
                                Xml toSet = new Xml(Type.Text,x,"#text",finalValue);
                                x.addChild(toSet);
                            }
                        }else if (x.isText()){
                            x.setValue(finalValue);
                            if(x.parent()!=null){
                                x.parent().resetTextValue();
                            }
                        }else{
                            System.out.println("Unsupported XMl type: cannot SET "+x+" to "+finalValue);
                        }
                    });
                    break;
                case Add:
                    found.forEach(x->{
                        if(x.isAttribute()){
                            x.setValue(x.getValue()+finalValue);
                        }else if (x.isTag()){
                            Xml toAdd;
                            if(finalValue.startsWith(START_TAG_PREFIX)){
                                toAdd = Xml.parse(finalValue);
                                x.addChild(toAdd);
                            }else if (finalValue.startsWith(ATTRIBUTE_PREFIX)) {
                                String attrName = finalValue.substring(1);
                                String attrValue = "";
                                if(attrName.contains(ATTRIBUTE_VALUE_PREFIX)){
                                    int index = attrName.indexOf(ATTRIBUTE_VALUE_PREFIX);
                                    if(attrName.length()>index+1) {
                                        attrValue = attrName.substring(index + 1).trim();
                                    }
                                    attrName = attrName.substring(0,index).trim();

                                }
                                toAdd = new Xml(Type.Attribute,x,attrName,StringUtil.removeQuotes(attrValue));
                                x.setAttribute(toAdd);
                            }else{
                                toAdd = new Xml(Type.Text,x,"#text",finalValue);
                                x.addChild(toAdd);
                            }

                        }else{
                            System.out.println("Unsupported XMl type: cannot ADD "+x+" to "+finalValue);
                        }
                    });
                    break;
                case Delete:
                    found.forEach(x->{
                        if(x.hasParent()){
                            if(x.isAttribute()){
                                x.parent().removeAttribute(x.getName());
                            }else if (x.isTag()){
                                x.parent().removeChild(x);
                            }else if (x.isText()){
                                x.parent().removeChild(x);
                            }else {
                                System.out.println("Unsupported XMl type: cannot DELETE "+x);
                            }
                        }
                    });
                    break;
                default:
                    System.out.println("unsupported opp "+opp+" on "+xmlPath.toString());
            }
        }
        return rtrn.toString();
    }

    private boolean hasParent() {return parent!=null;}

    @Override
    public String toString(){
        return isDocument() ? "?xml" : (getName()+":"+value);
    }

    public String documentString(){
        return documentString(4);
    }
    public String documentString(int indent){
        return documentString(indent,true);
    }
    public String documentString(int indent,boolean includeDocument){
        StringBuilder rtrn = new StringBuilder();
        append(rtrn,0,indent,includeDocument);
        return rtrn.toString();
    }
    private void pad(StringBuilder sb,int amount){
        if(amount>0){
            sb.append(String.format("%"+amount+"s",""));
        }
    }
    private void append(StringBuilder sb,int indent,int increment,boolean includeDocument){
        if(!type.equals(Type.Document)) {
            pad(sb,indent);
        }
        switch (type){
            case Attribute:
                sb.append(getValue());
                break;
            case Text:
                sb.append(getValue());
                break;
            case Document:
                if(includeDocument) {
                    sb.append(START_TAG_PREFIX);
                    sb.append("?");
                    sb.append(getName());
                    for (Xml attribute : getAttributes().values()) {
                        sb.append(" ");
                        sb.append(attribute.getName());
                        sb.append(ATTRIBUTE_VALUE_PREFIX);
                        sb.append(ATTRIBUTE_WRAPPER);
                        sb.append(attribute.getValue());
                        sb.append(ATTRIBUTE_WRAPPER);
                    }
                    sb.append("?");
                    sb.append(CLOSE_TAG_SUFFIX);
                    if (increment > 0) {
                        sb.append(System.lineSeparator());
                    }
                    for (Xml child : getChildren()) {
                        child.append(sb, indent, increment, includeDocument);
                    }
                }
                break;
            case Tag:
                sb.append(START_TAG_PREFIX);
                sb.append(getName());
                for (Xml attribute : getAttributes().values()){
                    sb.append(" ");
                    sb.append(attribute.getName());
                    sb.append(ATTRIBUTE_VALUE_PREFIX);
                    sb.append(ATTRIBUTE_WRAPPER);
                    sb.append(attribute.getValue());
                    sb.append(ATTRIBUTE_WRAPPER);
                }
                if(allChildren().isEmpty() && getValue().isEmpty()){
                    sb.append(EMPTY_TAG_SUFFIX);
                }else{
                    sb.append(CLOSE_TAG_SUFFIX);
                    if(tagChildren.isEmpty() && allChildren.stream().mapToInt(x->x.getValue().length()).sum() < INLINE_LENGTH){//only text, maybe one line?
                        for (Xml child : allChildren()) {
                            sb.append(child.getValue());
                        }
                        sb.append(END_TAG_PREFIX);
                        sb.append(getName());
                        sb.append(CLOSE_TAG_SUFFIX);
                    }else {
                        for (Xml child : allChildren()) {
                            if (increment > 0) {
                                sb.append(System.lineSeparator());
                            }
                            child.append(sb, indent + increment, increment, includeDocument);
                        }
                        if(increment>0){
                            sb.append(System.lineSeparator());
                        }
                        pad(sb,indent);
                        sb.append(END_TAG_PREFIX);
                        sb.append(getName());
                        sb.append(CLOSE_TAG_SUFFIX);

                    }
                }
                break;
            case Comment:
                sb.append(COMMENT_PREFIX);
                sb.append(getValue());
                sb.append(COMMENT_SUFFIX);
                break;
            default:

        }
    }



}



