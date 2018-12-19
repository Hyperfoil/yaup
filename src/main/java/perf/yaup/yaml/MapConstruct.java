package perf.yaup.yaml;

import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;

public abstract class MapConstruct extends AbstractConstruct {

    private MapConstructor mapConstructor;
    void setMapConstructor(MapConstructor mapConstructor){
        this.mapConstructor = mapConstructor;
    }
    public final Object defer(Node node){
        if(mapConstructor.hasConstruct(node.getTag())){
            return mapConstructor.getConstruct(node.getTag()).construct(node);
        }else{
            throw new YAMLException("cannot defer "+node.getStartMark());
        }
    }
}
