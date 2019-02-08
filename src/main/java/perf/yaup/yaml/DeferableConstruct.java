package perf.yaup.yaml;

import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.function.Predicate;

public abstract class DeferableConstruct extends AbstractConstruct {

    public Predicate<NodeTuple> keyFilter(String key){
        return (nodeTuple) -> key.equals(((ScalarNode)nodeTuple.getKeyNode()).getValue());
    }

    private OverloadConstructor overloadConstructor;
    void setOverloadConstructor(OverloadConstructor overloadConstructor){
        this.overloadConstructor = overloadConstructor;
    }
    public final Object defer(Node node){
        return overloadConstructor.constructObject(node);
    }
    public final Object deferAs(Node node,Tag tag){
        return overloadConstructor.retryAs(node,tag);
    }
}
