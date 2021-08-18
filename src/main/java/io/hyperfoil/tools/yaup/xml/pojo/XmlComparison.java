package io.hyperfoil.tools.yaup.xml.pojo;

import org.apache.commons.cli.*;
import io.hyperfoil.tools.yaup.AsciiArt;
import io.hyperfoil.tools.yaup.Sets;
import io.hyperfoil.tools.yaup.StringUtil;
import io.hyperfoil.tools.yaup.linux.Local;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class XmlComparison {

    final static XLogger logger = XLoggerFactory.getXLogger(MethodHandles.lookup().lookupClass());

    public static final XmlComparison domainXml(){
        XmlComparison rtrn = new XmlComparison();
        domainXml(rtrn);
        return rtrn;
    }
    public static final void domainXml(XmlComparison comp){
        standaloneXml(comp);
        comp.addCriteria("/context-param/param-name",0);
        comp.addCriteria("/filter/filter-name",0);
        comp.addCriteria("/filter-mapping/filter-name",0);
        comp.addCriteria("/servlet/servlet-name",0);
        comp.addCriteria("/servlet-mapping/servlet-name",0);
        comp.addCriteria("/env-entry/env-entry-name",0);
    }

    public static final XmlComparison standaloneXml() {
        XmlComparison rtrn = new XmlComparison();
        standaloneXml(rtrn);
        return rtrn;
    }
    public static final void standaloneXml(XmlComparison comp){
        comp.addCriteria("/option/@value",0);
        comp.addCriteria("@name", 0);
        comp.addCriteria("@jndi-name", 0);
        comp.addCriteria("@xmlns", 3);
        comp.addCriteria("@class-name",0);
        comp.addCriteria("@module", 0);
        comp.addCriteria("@category", 0);
    }
    public static final XmlComparison webXml() {
        XmlComparison rtrn = new XmlComparison();
        webXml(rtrn);
        return rtrn;
    }
    public static final void webXml(XmlComparison comp){
        comp.addCriteria("/context-param/param-name",0);
        comp.addCriteria("/filter/filter-name",0);
        comp.addCriteria("/filter-mapping/filter-name",0);
        comp.addCriteria("/servlet/servlet-name",0);
        comp.addCriteria("/servlet-mapping/servlet-name",0);
        comp.addCriteria("/env-entry/env-entry-name",0);
    }
    private static class XmlNamedChildren {
        private List<Xml> allChildren;
        private Map<String,XmlChildList> namedChildren;

        public XmlNamedChildren(){
            allChildren = new ArrayList<>();
            namedChildren = new LinkedHashMap<>();
        }

        public boolean has(String name){
            return namedChildren.containsKey(name);
        }
        public void add(Xml child){
            String name = child.getName();
            if(!namedChildren.containsKey(name)){
                namedChildren.put(name,new XmlChildList(this));
            }
            allChildren.add(child);
            namedChildren.get(name).add(child);
        }
        public Xml getFirst(){return allChildren.get(0);}
        public void remove(Xml child){
            allChildren.remove(child);
        }
        public void remove(String name){
            namedChildren.remove(name);
        }
        public boolean isEmpty(){return allChildren.size()==0;}
        public XmlChildList get(String tagName){
            return namedChildren.get(tagName);
        }
        public int size(){return allChildren.size();}
        public String debug(){
            StringBuffer sb = new StringBuffer();
            for(String name : namedChildren.keySet()){
                sb.append(name+"\n");
                sb.append("  "+namedChildren.get(name).data+"\n");
            }
            return sb.toString();
        }
    }
    private static class XmlChildList {

        private List<Xml> data;
        private XmlNamedChildren namedChildren;
        public XmlChildList(XmlNamedChildren namedChildren){
            this.data = new ArrayList<>();
            this.namedChildren = namedChildren;
        }
        public int size(){return data.size();}
        public void add(Xml xml){data.add(xml);}
        public void remove(Xml xml){
            namedChildren.remove(xml);
            data.remove(xml);
            if(data.isEmpty()){
                namedChildren.remove(xml.getName());
            }
        }
        public void remove(int index){
            if(index < data.size()) {
                Xml toRemove = data.get(index);
                namedChildren.remove(toRemove);
                data.remove(index);
            }
        }
        public Xml get(int index){
            return data.get(index);
        }
    }
    private static Map<String,XmlNamedChildren> buildNamedChildren(Map<String,Xml> xmls){
        LinkedHashMap<String,XmlNamedChildren> rtrn = new LinkedHashMap<>();

        xmls.forEach((name,xml)->{
            XmlNamedChildren namedChildren = new XmlNamedChildren();
            if(xml.exists()) {
                xml.getChildren().forEach(namedChildren::add);
            }
            rtrn.put(name,namedChildren);
        });
        return rtrn;
    }

    public static class Entry {
        private String path;
        private LinkedHashMap<String,String> values;

        public Entry(String path){
            this.path = path;
            this.values = new LinkedHashMap<>();
        }

        private void put(String name,String value){
            values.put(name,value);
        }
        public String getPath(){return path;}
        public Set<String> keys(){return values.keySet();}
        public String value(String key){return values.get(key);}
        public void forEach(BiConsumer<String,String> consumer){
            values.forEach(consumer);
        }
    }


    private LinkedHashMap<String,Xml> rootXmls;

    private LinkedHashMap<XmlPath,Integer> criteria;

    private boolean ordered = false;


    public static void main(String[] args) {
        Options options = new Options();

        options.addOption(Option.builder("C")
            .longOpt("color")
            .hasArg(false)
            .desc("use terminal colored output")
            .build()
        );
        options.addOption(Option.builder("S")
            .longOpt("standalone")
            .hasArg(false)
            .desc("use standalone.xml criteria")
            .build()
        );
        options.addOption(Option.builder("D")
            .longOpt("domain")
            .hasArg(false)
            .desc("use domain.xml criteria")
            .build()
        );
        options.addOption(Option.builder("W")
            .longOpt("web")
            .hasArg(false)
            .desc("use web.xml criteria")
            .build()
        );
        options.addOption(Option.builder("X")
            .argName("criteria=#")
            .hasArg()
            .desc("criteria=edit_distance")
            .valueSeparator()
            .build()
        );

        List<String> toDelete = new LinkedList<>();

        XmlComparison comp = new XmlComparison();

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        String cmdLineSyntax =
            "java -jar " +
            (new File(XmlComparison.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath()
            )).getName() +
            " " +
            "[options] [name=file...]";

        try{
            cmd = parser.parse(options,args);
        } catch (ParseException e) {
            formatter.printHelp(cmdLineSyntax,options);
            System.exit(1);
            return;
        }

        if(cmd.hasOption("standalone")){
            standaloneXml(comp);
        }
        if(cmd.hasOption("domain")){
            domainXml(comp);
        }
        if(cmd.hasOption("web")){
            webXml(comp);
        }
        Properties criteria = cmd.getOptionProperties("X");
        if(!criteria.isEmpty()){
            criteria.forEach((k,v)->{
                comp.addCriteria(k.toString(),Integer.parseInt(v.toString()));
            });
        }
        List<String> paths = cmd.getArgList();
        if(paths.isEmpty()){
            logger.error("Missing file(s)");
            formatter.printHelp(cmdLineSyntax,options);
            System.exit(1);
            return;
        }
        paths.forEach(arg->{
            String name = "";
            String path = "";
            if (arg.contains("=")) {
                if(arg.endsWith("=") || arg.startsWith("=")){
                    //ERROR
                }
                name = arg.substring(0, arg.indexOf("="));
                path = arg.substring(arg.indexOf("=") + 1);
            } else {
                name = ""+comp.xmlCount();
                path = arg;
            }
            if(path.contains(":")){
                if(path.startsWith("http")){
                    try {
                        File tmp = File.createTempFile("xmlComp-", ".xml");
                        URL url = new URL(path);
                        ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
                        FileOutputStream fileOutputStream = new FileOutputStream(tmp.getAbsolutePath());
                        FileChannel fileChannel = fileOutputStream.getChannel();
                        fileOutputStream.getChannel()
                                .transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

                        toDelete.add(tmp.getPath());
                        path = tmp.getPath();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    Local local = new Local();
                    try {
                        File tmp = File.createTempFile("xmlComp-", ".xml");
                        logger.info("downloading " + path + " to " + tmp.getPath());
                        local.download(tmp.getPath(), path);
                        toDelete.add(tmp.getPath());
                        path = tmp.getPath();
                    } catch (IOException e) {
                        e.printStackTrace();
                        formatter.printHelp(cmdLineSyntax, options);
                        System.exit(1);
                    }
                }
            }
            Xml toLoad = Xml.parseFile(path);
            if(toLoad.exists()){
                comp.load(name, Xml.parseFile(path));
            }else{
                logger.error("missing {}", toLoad.getName());
                formatter.printHelp(cmdLineSyntax,options);
                System.exit(1);
            }

        });

        if(comp.criteriaCount() == 0){
            standaloneXml(comp);
        }

        List<Entry> diffs = comp.getDiffs();

        int nameWidth = comp.xmlNames().stream().mapToInt(String::length).max().orElse(1);

        boolean c = cmd.hasOption("color");
        diffs.forEach(entry->{
            logger.info("{}}{}{}",
                c ? AsciiArt.ANSI_LIGHT_BLUE : "",
                entry.getPath(),
                c ? AsciiArt.ANSI_RESET : ""
            );
            entry.keys().forEach(key->{
                String value = entry.value(key);
                logger.info(String.format("  %s%"+nameWidth+"s%s : %s%n",
                    c ? AsciiArt.ANSI_WHITE : "",
                    key,
                    c ? AsciiArt.ANSI_RESET : "",
                    value.contains("\n") ? "\n    "+value.replaceAll("\n","\n    ") : value)
                );
            });
        });

        if(!toDelete.isEmpty()){
            toDelete.forEach(deleteMe->{
                File f = new File(deleteMe);
                if(f.exists()){
                    f.delete();
                }
            });
        }

    }

    public XmlComparison(){

        rootXmls = new LinkedHashMap<>();
        criteria = new LinkedHashMap<>();
    }

    public void setOrdered(){this.ordered = true;}
    public boolean isOrdered(){return  ordered;}

    public void addCriteria(String path,int editDistance){
        criteria.put(XmlPath.parse(path),editDistance);
    }

    public int criteriaCount(){
        return criteria.size();
    }
    public int xmlCount(){
        return rootXmls.size();
    }
    public Set<String> xmlNames(){
        return rootXmls.keySet();
    }

    public void load(String name,Xml xml){
        if(xml.exists()) {
            if (!rootXmls.containsKey(name)) {
                rootXmls.put(name, xml);
            }
        }else{
            logger.error("cannot load "+name+" xml is invalid "+xml);
        }
    }

    public List<Entry> getDiffs(){
        if(rootXmls.size()<=1){
            return Collections.emptyList();
        }
        LinkedList<Entry> rtrn = new LinkedList<>();

        diff("",rtrn,rootXmls);
        return rtrn;
    }

    /**
     *
     * @param path the path of the xmls in xmls
     * @param diffs
     * @param xmls
     */
    private void diff(String path,List<Entry> diffs, Map<String,Xml> xmls){
        if(isEmtpy(xmls)){
            return;
        }
        if(hasNull(xmls)){//if one of the xmls is missing
            logger.error("THIS SHOULD BE CAUGHT BEFORE THIS POINT FOR ALL SUB_CALLS");
             Entry newEntry = new Entry(path);
             xmls.forEach((xmlName,xml)->{
                 newEntry.put(xmlName,xml.documentString());
             });
             diffs.add(newEntry);
        }else{//they each have them, diff the attributes then find the matching children

            diffAttributes(path,diffs,xmls);
            diffValues(path,diffs,xmls);
            if(ordered){

            }else{
                Map<String,XmlNamedChildren> namedChildrenMap = buildNamedChildren(xmls);

                while(0 < namedChildrenMap.entrySet().stream().mapToInt(e->e.getValue().size()).sum()) {
                    Map<String,Xml> toDiff = new LinkedHashMap<>();

                    Xml firstChild = firstChildXml(namedChildrenMap);
                    if(firstChild.exists()){
                        String firstChildName = firstChild.getName();
                        for(String rootName : namedChildrenMap.keySet()){
                            XmlNamedChildren namedChildren = namedChildrenMap.get(rootName);
                            int bestScore = Integer.MAX_VALUE;
                            int bestIndex= -1;
                            Xml matched = Xml.INVALID;

                            if(namedChildren.has(firstChildName)){
                                XmlChildList childList = namedChildren.get(firstChildName);
                                for(int i=0; i<childList.size(); i++){
                                    Xml childEntry = childList.get(i);
                                    int childScore = getScore(firstChild,childEntry);
                                    if(childScore < bestScore){
                                        bestScore = childScore;
                                        bestIndex = i;
                                        matched = childEntry;
                                    }
                                }

                                //at this point we picked the match from childList, remove it
                                if(bestIndex > -1) {
                                    childList.remove(bestIndex);
                                }
                            }
                            //could be putting one that isn't valid
                            toDiff.put(rootName,matched);
                        }
                        //we should now have a fully populated toDiff
                        if(toDiff.size() != namedChildrenMap.size()){
                            logger.error("toDiff "+toDiff.keySet()+"\n  namedChildren="+namedChildrenMap.keySet());
                        }
                        //calculate the new path
                        StringBuffer toAdd = new StringBuffer("/");
                        boolean inAttribute = false;
                        toAdd.append(firstChildName);
                        for(XmlPath xmlPath : criteria.keySet()){
                            int limit = criteria.get(xmlPath);
                            List<Xml> match = xmlPath.getMatches(firstChild);
                            if(match.size()==1){
                                if(inAttribute){
                                    toAdd.append(" AND ");
                                }else{
                                    inAttribute=true;
                                    toAdd.append("[ ");
                                }
                                String xmlPathString = xmlPath.toString();
                                if(xmlPathString.startsWith("/")){
                                    xmlPathString = xmlPathString.substring(1);
                                }
                                if(xmlPathString.startsWith(firstChildName)){
                                    xmlPathString = xmlPathString.substring(firstChildName.length());
                                }
                                if(xmlPathString.startsWith("/")){
                                    xmlPathString = xmlPathString.substring(1);
                                }

                                toAdd.append(xmlPathString);
                                if(match.get(0).exists()){
                                    if(XmlPath.Method.Equals.equals(xmlPath.getMethod())){
                                        if(limit == 0) {
                                            toAdd.append(XmlPath.Method.Equals.getOperator());
                                        }else{
                                            toAdd.append(XmlPath.Method.StartsWith.getOperator());
                                        }
                                    }else{
                                        toAdd.append(xmlPath.getMethod().getOperator());
                                    }
                                    if(xmlPath.hasValue()){
                                        toAdd.append(StringUtil.quote(xmlPath.getValue().substring(0,xmlPath.getValue().length()-limit)));
                                    }else {
                                        toAdd.append(StringUtil.quote(match.get(0).getValue().substring(0,match.get(0).getValue().length()-limit)));
                                    }
                                }

                            }else if (match.size() > 1){
                                logger.error("too many matches by "+xmlPath+"\n  on"+firstChild.toString());
                                //TODO what do we do
                            }else{

                            }
                        }
                        if(inAttribute){
                            toAdd.append(" ]");
                        }
                        String newPath = path+toAdd.toString();
                        if(hasNull(toDiff)) {//if one of the xmls is missing
                            Entry newEntry = new Entry(path);
                            toDiff.forEach((xmlName, xml) -> {
                                newEntry.put(xmlName, xml.documentString());
                            });
                            diffs.add(newEntry);
                        }else {
                            //TODO change from stach recursion to work Q
                            diff(newPath, diffs, toDiff);
                        }
                    }else{
                        logger.error("HOW DID WE GET AN INVALID ENTRY?");
                    }
                    try {
                        TimeUnit.SECONDS.sleep(0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    private int getScore(Xml base, Xml other){
        int rtrn = 0;//a 0 means perfect match
        if(base.exists() && other.exists()){
            for(XmlPath path : criteria.keySet()){
                int limit = criteria.get(path);
                List<Xml> baseMatched = path.getMatches(base);
                List<Xml> otherMatched = path.getMatches(other);
                if(baseMatched.size() != otherMatched.size()){
                    //TODO how to handle different matche length
                    logger.info("different match counts for "+path+
                            "\n  base "+base.documentString(0)+
                            "\n  baseMatched="+baseMatched.size()+
                            "\n  other "+other.documentString(0)+
                            "\n  otherMatched="+baseMatched.size()
                    );
                }else{
                    for(int i=0; i<baseMatched.size(); i++){
                        String baseValue = baseMatched.get(i).getValue();
                        String otherValue = otherMatched.get(i).getValue();
                        int editDistance = StringUtil.editDistance(baseValue,otherValue);
                        if(editDistance > limit){
                            return Integer.MAX_VALUE;
                        }else{
                            //potentially don't add to the score for this
                            rtrn+=editDistance;//if it's below the limit do we still count it as a delta?
                        }
                    }
                }
            }

            Set<String> attributeNames = new HashSet<>();
            attributeNames.addAll(base.getAttributes().keySet());
            attributeNames.addAll(other.getAttributes().keySet());

            for(String attributeName : attributeNames){
                if(base.hasAttribute(attributeName) && other.hasAttribute(attributeName)){
                    String baseValue = base.getAttributes().get(attributeName).getValue();
                    String otherValue = other.getAttributes().get(attributeName).getValue();

                    int editDistance = StringUtil.editDistance(baseValue,otherValue);
                    rtrn+=editDistance;

                }else {// one is missing, what do we add to the score?
                    Xml target = base.hasAttribute(attributeName) ? base : other;
                    String value = target.getAttributes().get(attributeName).getValue();
                    //TODO just a guess at how to handle missing attributes
                    rtrn+=value.length();
                    rtrn+=attributeName.length();
                }
            }
            String baseValue = base.getValue().trim();
            String otherValue = other.getValue().trim();
            int editDistance = StringUtil.editDistance(baseValue,otherValue);
            rtrn+=editDistance;

        }else if (!base.exists() && !other.exists()){
            return 0;//2 Xml that don't exist are a perfect match :)
        }else{
            return Integer.MAX_VALUE;
        }
        return rtrn;
    }
    private Xml firstChildXml(Map<String,XmlNamedChildren> namedChildrenMap){
        Xml rtrn = Xml.INVALID;
        for(String rootName : namedChildrenMap.keySet()){
            XmlNamedChildren namedChildren = namedChildrenMap.get(rootName);
            if(!namedChildren.isEmpty()){
                rtrn = namedChildren.getFirst();
                return rtrn;
            }
        }
        return rtrn;
    }
    private Set<String> tagNames(Xml xml){
        return xml.getChildren().stream().map(Xml::getName).collect(Collectors.toSet());
    }
    private boolean isEmtpy(Map<String,Xml> xmls){
        return xmls.values().stream().map(xml->xml==null || !xml.exists())
                .reduce(Boolean::logicalAnd).orElse(false);
    }
    private Xml firstNotNull(Map<String,Xml> xmls){
        return xmls.values().stream().filter(xml->xml!=null).findAny().orElse(null);
    }
    private boolean hasNull(Map<String,Xml> xmls){
        boolean rtrn = false;
        for(Xml xml : xmls.values()){
            if(xml == null || !xml.exists()){
                rtrn = true;
                return true;
            }
        }
        return rtrn;
    }
    private boolean allSameAttributeValue(String attributeName,Map<String,Xml> xmls){
        String firstKey = firstKey(xmls.keySet());
        String firstValue = xmls.get(firstKey).attribute(attributeName).getValue();
        return xmls.values().stream()
            .map(xml->xml.attribute(attributeName).getValue().equals(firstValue))
            .reduce(Boolean::logicalAnd).orElse(true);
    }
    private void diffValues(String path,List<Entry> diffs,Map<String,Xml> xmls){
        if(xmls.isEmpty()){
            return;
        }
        List<String> values = xmls.values().stream().map(xml->xml.getValue().trim()).collect(Collectors.toList());
        Set<String> uniqueValues = new HashSet<>(values);
        if(uniqueValues.size() > 1){
            Entry newEntry = new Entry(path);//+"/text()"
            for(String key : xmls.keySet()){
                newEntry.put(key,xmls.get(key).getValue().trim());
            }
            diffs.add(newEntry);
        }else{
        }
    }
    private void diffAttributes(String tagPath,List<Entry> diffs,Map<String,Xml> xmls){
        if(xmls.isEmpty()){
            return;
        }
        String firstKey = firstKey(xmls.keySet());
        Set<String> firstAttributes = xmls.get(firstKey).getAttributes().keySet();
        Set<String> allAttributes = xmls.values().stream()
            .flatMap(x->x.getAttributes().keySet().stream())
            .collect(Collectors.toSet());
        Set<String> notInFirst = Sets.unique(allAttributes,firstAttributes);

        firstAttributes.forEach(key->{
            boolean isSame = allSameAttributeValue(key,xmls);
            if(!isSame){
                Entry newEntry = new Entry(tagPath+"/@"+key);
                xmls.forEach((xmlName,xml)->{
                    newEntry.put(xmlName,xml.attribute(key).getValue());
                });
                diffs.add(newEntry);
            }
        });
        notInFirst.forEach(key->{
            Entry newEntry = new Entry(tagPath+"/@"+key);
            xmls.forEach((xmlName,xml)->{
                newEntry.put(xmlName,xml.attribute(key).getValue());
            });
            diffs.add(newEntry);
        });
    }
    private String firstKey(Set<String> set){
        return set.iterator().next();
    }
}
