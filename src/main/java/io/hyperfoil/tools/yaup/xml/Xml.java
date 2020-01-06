package io.hyperfoil.tools.yaup.xml;

import org.w3c.dom.*;
import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.file.FileUtility;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Created by wreicher
 * A convenience wrapper around XML that encapsulates some of the complexity in dealing with XML
 * Included features are
 *   Xpath searching
 *   pretty printing
 *   adding xml from strings
 *   modifying xml with operations from FileUtility
 *     see Xml#modify(java.lang.String)
 */
public class Xml {

    public static final String ATTRIBUTE_VALUE_KEY = "=";
    public static final String ATTRIBUTE_KEY = "@";
    public static final String TAG_START = "<";
    public static final String TAG_END = ">";

    public static final Xml EMPTY = new Xml(null);

    private static final String XPATH_DELIM = "/";

    private final XmlLoader xmlLoader = new XmlLoader();
    private final XPathFactory xPathFactory = XPathFactory.newInstance();
    private final Node node;

    protected Xml(Node node){
        this.node = node;
    }

    //legacy
    public boolean isEmpty(){return node==null;}

    public boolean exists(){return node!=null;}

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
            return list.isEmpty() ? EMPTY : list.get(0);
        }
    }
    public List<Xml> getAll(String search){
        if(isEmpty()){
            return Collections.emptyList();
        }
        ArrayList<Xml> rtrn = new ArrayList<>();
        XPath xPath = xPathFactory.newXPath();


        try {
            Object resultObj = xPath.compile(search).evaluate(node,XPathConstants.NODESET);
            if(resultObj instanceof NodeList) {
                NodeList nodeList = (NodeList) resultObj;
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    rtrn.add(new Xml(node));
                }
            }else if (resultObj instanceof String){
                rtrn.add(new Xml(node.getOwnerDocument().createTextNode((String)resultObj)));
            }else{
//                System.out.println("resultObject is "+resultObj.getClass());
            }


        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return rtrn;
    }
    public void set(String key,String value){
        if(isEmpty()){
            return;
        }
        if(key.startsWith(ATTRIBUTE_KEY)){
            String attributeName = key.substring(ATTRIBUTE_KEY.length());
            ((Element)node).setAttribute(attributeName,value);
        }else{

        }
    }

    private void trimEmptyText(Node toTrim){

        if(isEmpty()){
            return;
        }
        XPathExpression xpathExp = null;
        try {
            xpathExp = xPathFactory.newXPath().compile(
                    "//text()[normalize-space(.) = '']");
            NodeList emptyTextNodes = (NodeList)
                    xpathExp.evaluate(toTrim, XPathConstants.NODESET);
            // Remove each empty text node from document.
            for (int i = 0; i < emptyTextNodes.getLength(); i++) {
                Node emptyTextNode = emptyTextNodes.item(i);
                emptyTextNode.getParentNode().removeChild(emptyTextNode);
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
    }
    public List<Xml> getChildren(){
        if(isEmpty()){
            return Collections.emptyList();
        }
        List<Xml> rtrn = new LinkedList<>();
        NodeList children = node.getChildNodes();
        for(int i=0; i<children.getLength(); i++){
            rtrn.add(new Xml(children.item(i)));
        }
        return rtrn;
    }

    public Xml firstChild(String tagName){
        Xml rtrn = null;
        if(tagName==null){
            return new Xml(null);
        }
        if(isEmpty()){
            rtrn = new Xml(null);
        }else {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child!=null && tagName.equals(child.getNodeName())) {
                    if (rtrn == null) {
                        rtrn = new Xml(child);
                    }
                }
            }
        }
        return rtrn;
    }
    public Optional<Xml> optChild(String tagName){
        Optional<Xml> rtrn = Optional.empty();
        NodeList children = node.getChildNodes();
        for(int i=0; i<children.getLength(); i++){
            Node child = children.item(i);
            if(tagName.equals(child.getNodeName())){
                rtrn = Optional.of(new Xml(child));
            }
        }
        return rtrn;
    }

    public Xml attribute(String name){
        Xml rtrn = null;
        if(isEmpty()){
            rtrn = new Xml(null);
        }else{
            NamedNodeMap attributes = node.getAttributes();
            Node attributeNode = attributes.getNamedItem(name);
            rtrn = new Xml(attributeNode);
        }
        return rtrn;
    }
    public Optional<Xml> optAttribute(String attributeName){
        Optional<Xml> rtrn = Optional.empty();
        NamedNodeMap attributes = node.getAttributes();
        Node node = attributes.getNamedItem(attributeName);
        if(node!=null){
            rtrn = Optional.of(new Xml(node));
        }
        return rtrn;
    }
    public boolean hasChild(String tagName){
        boolean rtrn = false;
        NodeList children = node.getChildNodes();
        for(int i=0; i<children.getLength();i++){
            Node child = children.item(i);
            if(tagName.equals(child.getNodeName())){
                rtrn = true;
            }
        }
        return rtrn;
    }
    protected void clearChildren(){
        if(isEmpty()){
            return;
        }
        while (node.hasChildNodes()) {
            node.removeChild(node.getFirstChild());
        }
    }

    public void setChild(String value){

        if(isEmpty()){
            return;
        }
        clearChildren();
        addChild(value);
    }
    private void addChild(String value){

        value = "<cld>"+value+"</cld>";
        List<Xml> xmls = xmlLoader.loadXml(value).getChildren();
        for(Xml xml : xmls){
            addChild(xml);
        }
    }
    private void addChild(Xml value){
        Node toImport = node.getOwnerDocument().importNode(value.node, true);
        ((Element)node).appendChild(toImport);
    }
    /**
     * add value to this. Will treat "&lt;...&gt;" as xml
     * @param value string to be added
     */
    public void add(String value){

        if(isEmpty()){
            return;
        }
        switch(node.getNodeType()){
            case Node.ATTRIBUTE_NODE:
                node.setNodeValue(node.getNodeValue()+value);
                break;
            case Node.ELEMENT_NODE:

                if(value.startsWith(TAG_START) && value.endsWith(TAG_END)){
                    addChild(value);
                }else {

                    node.setTextContent(node.getTextContent() + value);
                }
                break;
            default:
                System.out.println("add("+value+") "+node.getNodeType());
        }
    }
    /**
     * set the value of this. Will treat "&lt;...&gt;" as xml
     * @param value set value of xml node
     */
    public void set(String value){

        if(isEmpty()){
            return;
        }
        switch(node.getNodeType()){
            case Node.ATTRIBUTE_NODE:
                node.setNodeValue(value);
                break;
            case Node.ELEMENT_NODE:
                if(value.startsWith(TAG_START) && value.endsWith(TAG_END)){
                    setChild(value);
                }else {
                    node.setTextContent(value);
                }
                break;
            default:
                System.out.println("set("+value+") "+node.getNodeType());
        }
    }

    /**
     * Performs a modification to the associated Xml element. support sytanx:
     * value                 : sets the attribute value or text value depending if this represents an attribute or element
     * &lt;value&gt;&lt;/value&gt;...    : set the attribute value or the children of this (deletes existing values)
     * --                    : delete this from the parent (if possible)
     * ++ value              : add the value to the attribute value or text value depending if this represents an attribute or element
     * ++ &lt;value&gt;&lt;/value&gt;... : add the value to the attribute value or add to the children elements
     * ++ @key=value         : add the attribute with value to this (will replace any existing value)
     * == value              : sets the text value
     * == &lt;value&gt;&lt;/value&gt;... : removes all current children and sets children to &lt;value&gt;&lt;/value&gt;
     *
     * @param value xml element to be modified
     */
    public void modify(String value){

        if(isEmpty()){
            return;
        }
        int opIndex = -1;

        switch(node.getNodeType()){
            case Node.ATTRIBUTE_NODE:
                if(value.startsWith(FileUtility.DELETE_OPERATION)){//--
                    delete();
                }else if (value.startsWith(FileUtility.ADD_OPERATION)){//++ value
                    String newValue = StringUtil.removeQuotes(value.substring(FileUtility.OPERATION_LENGTH).trim());
                    node.setNodeValue(node.getNodeValue()+newValue);

                }else if (value.startsWith(FileUtility.SET_OPERATION)){//== value
                    node.setNodeValue( StringUtil.removeQuotes( value.substring(FileUtility.OPERATION_LENGTH).trim() ) );

                }else{ //value
                    node.setNodeValue( StringUtil.removeQuotes( value ) );
                }
                break;
            case Node.ELEMENT_NODE:

                if(value.startsWith(FileUtility.DELETE_OPERATION)) { //--
                    delete();
                }else if(value.startsWith(FileUtility.ADD_OPERATION)) { //++ ?
                    String toAdd = value.substring(FileUtility.ADD_OPERATION.length()).trim();

                    if (toAdd.startsWith(ATTRIBUTE_KEY)) { //++ @key=value
                        int valueIndex = toAdd.indexOf(ATTRIBUTE_VALUE_KEY);
                        String attributeKey = toAdd.substring(ATTRIBUTE_KEY.length(), valueIndex).trim();
                        String attributeValue = toAdd.substring(valueIndex + ATTRIBUTE_VALUE_KEY.length()).trim();
                        Element elm = (Element) node;
                        elm.setAttribute(attributeKey, StringUtil.removeQuotes(attributeValue));
                    } else {//++ value or //++ <value/><value/>... handled by add
                        add(StringUtil.removeQuotes(toAdd));
                    }
                }else if (value.startsWith(FileUtility.SET_OPERATION)){

                    String toSet = value.substring(FileUtility.SET_OPERATION.length()).trim();

                    set(StringUtil.removeQuotes(toSet)); // supports value and <value/>...
                }else{// value or <value/>...
                    set(StringUtil.removeQuotes(value));
                }
                break;
            default:
                System.out.println("set("+value+") "+node.getNodeType());
        }
    }
    public void delete(){
        if(node ==null){
            return;
        }
        switch (node.getNodeType()){
            case Node.ATTRIBUTE_NODE:
                Element attrParent =((Attr)node).getOwnerElement();
                attrParent.removeAttributeNode(((Attr)node));
                break;
            case Node.ELEMENT_NODE:
                Node nodeParent = node.getParentNode();
                nodeParent.removeChild(node);
                break;
            default:
                System.out.println("unknown type="+node.getNodeType());
        }

    }

    @Override
    public String toString(){
        if(isEmpty()){
            return "";
        }
        switch(node.getNodeType()){
            case Node.ATTRIBUTE_NODE:
                return node.getNodeValue();
            case Node.ELEMENT_NODE:
                return node.getTextContent();
            case Node.TEXT_NODE:
                return node.getNodeValue();
            default:
                return node.getNodeValue();
        }

    }
    public String getName(){return node.getNodeName();}
    public String getValue(){return isEmpty()? "" : node.getNodeValue();}
    public String documentString(){
        return documentString(4);
    }
    public String documentString(int indent){
        Node toPrint = node.cloneNode(true);
        trimEmptyText(toPrint);
        Transformer transformer = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");

            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", Integer.toString(indent));
            DOMSource source = new DOMSource(toPrint);
            StreamResult result = new StreamResult(baos);
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

        return new String(baos.toByteArray());
    }

}
