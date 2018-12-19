package perf.yaup.yaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import java.util.Map;

public class MapRepresenter extends Representer {


    public class MapWrapper implements Represent {
        private final Mapping mapping;
        MapWrapper(Mapping mapping){
            this.mapping = mapping;
        }
        @Override
        public final Node representData(Object data) {
            Map<Object,Object> map = mapping.getMap(data);
            if(map!=null){
                Node rtrn = representMapping(Tag.MAP,map, DumperOptions.FlowStyle.BLOCK);
                return rtrn;
            }
            throw new RuntimeException("failed to map "+data);
        }
    }
    public void addMapping(Class<?> clazz,Mapping mapping){
        representers.put(clazz,new MapWrapper(mapping));
    }
}
