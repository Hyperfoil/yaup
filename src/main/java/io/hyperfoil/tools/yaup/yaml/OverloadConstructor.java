package io.hyperfoil.tools.yaup.yaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.*;
import io.hyperfoil.tools.yaup.Sets;
import io.hyperfoil.tools.yaup.json.Json;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Constructor for yaml with untagged types (because tags are ugly)
 */
public class OverloadConstructor extends Constructor{
    public static Object fromScalar(ScalarNode node){
        if(Tag.BOOL.equals(node.getTag())){
            return Boolean.parseBoolean(node.getValue());
        }else if (Tag.INT.equals(node.getTag())){
            return Integer.parseInt(node.getValue());
        }else if (Tag.FLOAT.equals(node.getTag())){
            return Float.parseFloat(node.getValue());
        }else {
            return node.getValue();
        }
    }
    public static Json json(Node node){
        Json rtrn = new Json();
        if(node instanceof MappingNode){
            MappingNode mappingNode = (MappingNode)node;
            mappingNode.getValue().forEach(nodeTuple -> {
                String key = ((ScalarNode)nodeTuple.getKeyNode()).getValue();
                Node value = nodeTuple.getValueNode();
                if(value instanceof ScalarNode){
                    rtrn.add(key, fromScalar((ScalarNode)value) );
                }else{
                    rtrn.add(key, json(value) );
                }
            });
        }else if (node instanceof SequenceNode){
            SequenceNode sequenceNode = (SequenceNode)node;
            sequenceNode.getValue().forEach(value->{
                if(value instanceof ScalarNode){
                    rtrn.add( fromScalar((ScalarNode)value) );
                }else{
                    rtrn.add( json(value) );
                }
            });
        }else if (node instanceof ScalarNode){
            ScalarNode scalarNode = (ScalarNode)node;
        }
        return rtrn;
    }
    public static Set<String> keys(MappingNode node){
        return node.getValue().stream().map(nodeTuple -> ((ScalarNode)nodeTuple.getKeyNode()).getValue()).collect(Collectors.toSet());
    }

    private class StrOverride extends AbstractConstruct{

        private final Construct parentConstruct;
        public StrOverride(Construct construct){this.parentConstruct = construct;}
        @Override
        public Object construct(Node node) {
            if(node instanceof ScalarNode){

                ScalarNode scalarNode = (ScalarNode)node;
                String value = scalarNode.getValue();
                Tag scalarTag = scalarNode.getTag();
                if(Tag.STR.equals(scalarTag)){
                    if(!stringOverrides.isEmpty()){
                        for(String pattern: stringOverrides.keySet()){
                            Tag newTag = stringOverrides.get(pattern);
                            if(value.contains(pattern) || value.matches(pattern)){
                                return retryAs(scalarNode,newTag);
                            }
                        }
                    }
                }
                return parentConstruct.construct(node);
            }
            throw new YAMLException("StrOverride requires ScalarNode not "+node.getClass().getSimpleName()+" "+node.getStartMark());
        }
    }
    private class MapOverride extends AbstractConstruct{

        private final Construct parentConstruct;
        public MapOverride(Construct construct){
            this.parentConstruct = construct;
        }
        @Override
        public Object construct(Node node) {
            Tag targetTag;
            if(node instanceof MappingNode){
                MappingNode mappingNode = (MappingNode)node;
                Set<String> keys = keys(mappingNode);
                if(keySets.containsKey(keys)){
                    Tag tag = keySets.get(keys);
                    if(hasConstruct(tag)) {
                        return getConstruct(tag).construct(node);
                    }else {
                        return retryAs(node,tag);
                    }
                }else if (!exactMatchOnly && (targetTag=getBestMatch(keys))!=null ){
                    return getConstruct(targetTag).construct(node);
                }
                //default map style construction
                LinkedHashMap<Object,Object> rtrn = new LinkedHashMap<>();
                mappingNode.getValue().forEach(nodeTuple -> {
                    Object keyObject = constructObject(nodeTuple.getKeyNode());
                    Node valueNode = nodeTuple.getValueNode();
                    if(!valueTags.isEmpty()){
                        if(keyObject instanceof String){
                            if(valueTags.containsKey(keyObject)){
                                Tag valueTag = valueTags.get(keyObject);
                                valueNode.setTag(valueTag);
                            }
                        }
                    }
                    Object valueObject = constructObject(valueNode);
                    rtrn.put(keyObject,valueObject);
                });
                return rtrn;
                //return parentConstruct.construct(node);

            }
            throw new YAMLException("MapOverride requires MappingNode not "+node.getClass().getSimpleName()+" "+node.getStartMark());
        }
    }


    boolean exactMatchOnly = true;
    Map<String,Tag> valueTags;
    Map<String,Tag> targetTags;
    Map<String,Tag> stringOverrides;
    SortedMap<Set<String>,Tag> keySets;
    SortedMap<Json,Tag> typeStructures;

    public OverloadConstructor(){
        this.yamlConstructors.put(Tag.MAP,new MapOverride(this.yamlConstructors.get(Tag.MAP)));
        this.yamlConstructors.put(Tag.STR,new StrOverride(this.yamlConstructors.get(Tag.STR)));
        this.keySets = new TreeMap<>((a, b)->{
            int rtrn = b.size()-a.size();
            if (rtrn == 0){
                rtrn = b.hashCode()-a.hashCode();
            }
            return rtrn;//longest first
        });
        this.typeStructures = new TreeMap<>((a,b)->{
            int rtrn = b.size()-a.size();
            if (rtrn == 0){
                rtrn = b.hashCode()-a.hashCode();
            }
            return rtrn;//longest first
        });
        this.targetTags = new HashMap<>();
        this.valueTags = new HashMap<>();
        this.stringOverrides = new HashMap<>();

    }

    public boolean isExactMatchOnly(){return exactMatchOnly;}
    public void setExactMatchOnly(boolean exactMatchOnly){
        this.exactMatchOnly = exactMatchOnly;
    }

    public String dump(){
        return keySets.entrySet().stream().map(entry->{
            return entry.getKey().toString()+" ["+entry.getKey().hashCode()+"] -> "+entry.getValue().toString();
        }).collect(Collectors.joining("\n"));
    }

    public Object retryAs(Node node,Tag newTag){
        if(newTag!=null && !newTag.equals(node.getTag())) {
            node.setTag(newTag);
            //Force reset of org.yaml.snakeyaml.constructor.BaseConstructor.recursiveObjects so we can re-construct with new tagging
            Mark fakeMark = new Mark("fake", 0, 0, 0, "fake", 0);
            Object throwaway = constructDocument(new ScalarNode(Tag.STR, "throwaway", fakeMark, fakeMark, DumperOptions.ScalarStyle.DOUBLE_QUOTED));
            return constructObject(node);
        }
        throw new YAMLException("Already tried to load "+newTag.getValue()+" from "+node.getStartMark());
    }

    @Override
    public Object constructObject(Node node){
        return super.constructObject(node);
    }


    /**
     * The best match is the only matching target tag or the longest set of required keys contained by the keys
     * @param keys
     * @return
     */
    public Tag getBestMatch(Set<String> keys){
        Set<String> targetMatch = Sets.getOverlap(keys,targetTags.keySet());
        if(targetMatch.size()==1){
            return targetTags.get(targetMatch.iterator().next());
        }
        Set<String> bestMatch = keySets.keySet().stream().filter(required->{
            return keys.containsAll(required);
        }).findFirst().orElse(null);
        return bestMatch != null ? keySets.get(bestMatch) : null;
    }

    public boolean hasConstruct(Tag tag){
        return yamlConstructors.containsKey(tag);
    }
    public Construct getConstruct(Tag tag){
        return yamlConstructors.get(tag);
    }
    public void addConstruct(Tag tag,Construct construct){
        if(construct instanceof DeferableConstruct){
            ((DeferableConstruct)construct).setOverloadConstructor(this);
        }
        yamlConstructors.putIfAbsent(tag,construct);
    }
    public void addValueTag(Tag tag,String key){
        valueTags.put(key,tag);
    }
    public void addStringTag(Tag tag,String pattern){
        stringOverrides.put(pattern,tag);
    }
    public void addTargetTag(Tag tag,String key){
        targetTags.put(key,tag);
    }

    /**
     * Tells constructor to use tag when a map contains all the requiredKeys.
     * Changing to
     * @param tag
     * @param requiredKeys
     * @return
     */
    public boolean addMapKeys(Tag tag,Set<String> requiredKeys){
        boolean added = keySets.put(requiredKeys,tag) == null;
        return added;
    }

    public boolean addMapStructure(Tag tag,Json structure){
        //TODO implement
        return false;
    }

}
