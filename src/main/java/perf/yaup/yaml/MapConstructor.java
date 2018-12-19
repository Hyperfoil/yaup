package perf.yaup.yaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.*;
import perf.yaup.json.Json;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Constructor to create custom re-tag un-tagged yaml maps
 */
public class MapConstructor extends Constructor{
    public static Json json(Node node){
        Json rtrn = new Json();
        if(node instanceof MappingNode){
            MappingNode mappingNode = (MappingNode)node;
            mappingNode.getValue().forEach(nodeTuple -> {
                String key = ((ScalarNode)nodeTuple.getKeyNode()).getValue();
                Node value = nodeTuple.getValueNode();
                if(value instanceof ScalarNode){
                    rtrn.add(key, ((ScalarNode)value).getValue() );
                }else{
                    rtrn.add(key, json(value) );
                }
            });

        }else if (node instanceof SequenceNode){
            SequenceNode sequenceNode = (SequenceNode)node;
            sequenceNode.getValue().forEach(value->{
                if(value instanceof ScalarNode){
                    rtrn.add( ((ScalarNode)value).getValue() );
                }else{
                    rtrn.add( json(value) );
                }
            });
        }else if (node instanceof ScalarNode){
            ScalarNode scalarNode = (ScalarNode)node;
            System.out.println("cannot return scalar as json "+((ScalarNode)node).getValue());

        }


        return rtrn;
    }
    public static Set<String> keys(MappingNode node){
        return node.getValue().stream().map(nodeTuple -> ((ScalarNode)nodeTuple.getKeyNode()).getValue()).collect(Collectors.toSet());
    }

    private class MapOverride extends AbstractConstruct{

        private final Construct parentConstruct;
        public MapOverride(Construct construct){
            this.parentConstruct = construct;
        }
        @Override
        public Object construct(Node node) {
            if(node instanceof MappingNode){
                MappingNode mappingNode = (MappingNode)node;
                Set<String> keys = keys(mappingNode);
                if(keySets.containsKey(keys)){
                    Tag tag = keySets.get(keys);
                    if(hasConstruct(tag)) {
                        return getConstruct(tag).construct(node);
                    }else {
                        //Force reset of org.yaml.snakeyaml.constructor.BaseConstructor.recursiveObjects so we can re-construct with new tagging
                        Mark fakeMark = new Mark("fake",0,0,0,"fake",0);
                        Object throwaway = constructDocument(new ScalarNode(Tag.STR,"throwaway",fakeMark,fakeMark, DumperOptions.ScalarStyle.DOUBLE_QUOTED));
                        node.setTag(tag);
                        return constructObject(node);
                    }
                }else if (!exactMatchOnly){
                    Tag bestMatch = getBestMatch(keys);
                    if(bestMatch!=null){
                        return getConstruct(bestMatch).construct(node);
                    }else{
                        //no close match, just pass it to the parent
                        return parentConstruct.construct(node);
                    }
                }else{
                    //pass it to the parent
                    return parentConstruct.construct(node);
                }
            }
            throw new YAMLException("MapOverride requires MappingNode not "+node);
        }
    }


    boolean exactMatchOnly = true;
    SortedMap<Set<String>,Tag> keySets;
    SortedMap<Json,Tag> typeStructures;

    public MapConstructor(){
        this.yamlConstructors.put(Tag.MAP,new MapOverride(this.yamlConstructors.get(Tag.MAP)));
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


    }

    public String dump(){
        return keySets.entrySet().stream().map(entry->{
            return entry.getKey().toString()+" ["+entry.getKey().hashCode()+"] -> "+entry.getValue().toString();
        }).collect(Collectors.joining("\n"));
    }

    @Override
    public Object constructObject(Node node){
        return super.constructObject(node);
    }



    public Tag getBestMatch(Set<String> keys){
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
        if(construct instanceof MapConstruct){
            ((MapConstruct)construct).setMapConstructor(this);
        }
        yamlConstructors.putIfAbsent(tag,construct);
    }
    public boolean addMapKeys(Tag tag,Set<String> expectedKeys){
        boolean added = keySets.put(expectedKeys,tag) == null;
        return added;
    }
    public boolean addMapStructure(Tag tag,Json structure){
        //TODO implement
        return false;
    }

}
