package perf.yaup.xml.pojo;

//import com.sun.xml.internal.stream.events.StartDocumentEvent;

import perf.yaup.HashedLists;
import perf.yaup.json.Json;

import javax.xml.stream.*;
import javax.xml.stream.events.*;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;

public class Xml {



    public static final Xml INVALID = new Xml(Type.Invalid,null,"","");

    public static final String COMMENT_PREFIX = "<!--";
    public static final String END_TAG_PREFIX = "</";
    public static final String START_TAG_PREFIX = "<";

    public static final String COMMENT_SUFFIX = "-->";
    public static final String CLOSE_TAG_SUFFIX = ">";
    public static final String EMPTY_TAG_SUFFIX = "/>";

    public static final String ATTRIBUTE_VALUE_PREFIX="=";
    public static final String ATTRIBUTE_WRAPPER="\"";


    public static Xml parseFile(String path){
        Xml rtrn = null;
        try {
            rtrn = parse(new FileInputStream(path));
        } catch (FileNotFoundException e) {
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
            e.printStackTrace();
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
            return attribute(search);
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

        allChildren.add(child);
        child.parent=this;
        if(child.getType().equals(Type.Text)){
            textChildren.add(child);
            addValue(child.getValue());
        }else if (child.getType().equals(Type.Tag)){
            int tagIndex = tagChildren.size();
            tagChildren.add(child);
            int namedIndex = namedTagChildren.get(child.getName()).size();
            namedTagChildren.put(child.getName(),child);
            child.setIndexes(tagIndex,namedIndex);
        }
    }
    public void setAttribute(Xml child){
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
    public List<Xml> getChildren(){
        return Collections.unmodifiableList(tagChildren);
    }
    public Xml firstChild(String tagName){
        Xml rtrn = new Xml(Type.Invalid,this,tagName);
        if(!exists()){
            return rtrn;
        }else{
            for(int i=0; i<tagChildren.size() && !rtrn.exists();i++){
                Xml child = tagChildren.get(i);
                if(tagName.equals(child.getName())){
                    rtrn = child;
                }
            }
        }
        return rtrn;
    }
    private List<Xml> allChildren(){
        return Collections.unmodifiableList(allChildren);
    }
    public Xml attribute(String name){

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
        Json rtrn = new Json();

        Stack<Json> jsonTodo = new Stack<>();
        Stack<Xml> xmlTodo = new Stack<>();

        jsonTodo.push(rtrn);
        xmlTodo.push(this);

        while(!jsonTodo.isEmpty()){
            Json json = jsonTodo.pop();
            Xml xml = xmlTodo.pop();

            xml.getAttributes().forEach((name,valueXml)->{
                json.set("@"+name,valueXml.getValue());
            });
            if(xml.hasValue()){
                json.set("text()",xml.getValue());
            }
            xml.getChildren().forEach(childXml->{
                Json child = new Json();
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
                    for(Xml child : allChildren()){
                        if(increment>0) {
                            sb.append(System.lineSeparator());
                        }
                        child.append(sb,indent+increment,increment, includeDocument);
                    }
//                    if(!getValue().isEmpty()){
//                        if(increment>0) {
//                            sb.append(System.lineSeparator());
//                        }
//                        pad(sb,indent+increment);
//                        sb.append(getValue());
//                    }
                    if(increment>0){
                        sb.append(System.lineSeparator());
                    }
                    pad(sb,indent);
                    sb.append(END_TAG_PREFIX);
                    sb.append(getName());
                    sb.append(CLOSE_TAG_SUFFIX);
                }
                break;
            case Comment:
                sb.append(COMMENT_PREFIX);
                sb.append(getValue());
                sb.append(COMMENT_SUFFIX);
                break;
        }
    }



}



