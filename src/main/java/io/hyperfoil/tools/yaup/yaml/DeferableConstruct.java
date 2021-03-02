package io.hyperfoil.tools.yaup.yaml;

import org.yaml.snakeyaml.constructor.AbstractConstruct;
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
        if(overloadConstructor == null){
            throw new RuntimeException("DeferableConstruct.defer without setting overloadConstructor @ "+node.getStartMark());
        }
        return overloadConstructor.constructObject(node);
    }
    public final Object deferAs(Node node,Tag tag){
        if(overloadConstructor == null){
            throw new RuntimeException("DeferableConstruct.deferAs without setting overloadConstructor @ "+node.getStartMark());
        }
        return overloadConstructor.retryAs(node,tag);
    }
}
