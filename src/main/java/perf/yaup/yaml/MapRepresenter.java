package perf.yaup.yaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MapRepresenter extends Representer implements Defer {

    private final Map<Class,Function<Object,Object>> encoders = new LinkedHashMap<>();

    @Override
    public Object deferAs(Object obj, Class<?> clazz) {
        Object rtrn = null;
        if(clazz!=null && encoders.containsKey(clazz)){
            rtrn = encoders.get(clazz).apply(obj);
        }else{
            rtrn = encoders.keySet().stream().filter(e -> e.isInstance(obj)).map(e -> encoders.get(e).apply(obj)).findFirst().orElse(null);
        }
        return rtrn;
    }

    @Override
    public Object defer(Object obj) {
        return deferAs(obj,null);
    }

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
    public class ObjectWrapper<T> implements Represent {
        private final Function<T,Object> encoder;
        public ObjectWrapper(Function<T,Object> encoder){this.encoder = encoder;}

        @Override
        public Node representData(Object data) {
            try{
                Object encoded = encoder.apply((T)data);
                if(encoded instanceof Map){
                    return representMapping(Tag.MAP,(Map)encoded,DumperOptions.FlowStyle.BLOCK);
                }else if (encoded instanceof List){
                    return representSequence(Tag.SEQ,(List)encoded,DumperOptions.FlowStyle.BLOCK);
                }else if (encoded instanceof String){
                    return representScalar(Tag.STR,(String)encoded);
                }else if (encoded instanceof Integer || encoded instanceof Long){
                    return representScalar(Tag.INT,encoded.toString());
                }else if (encoded instanceof Float || encoded instanceof Double){
                    return representScalar(Tag.FLOAT,encoded.toString());
                }else if (encoded instanceof Boolean){
                    return representScalar(Tag.BOOL,encoded.toString());
                }
            }catch (ClassCastException e){
                //defaults to throw YAMLException if encoding fails
            }
            throw new YAMLException("Encoding failed for "+data);
        }
    }
    public void addMapping(Class<?> clazz,Mapping mapping){
        encoders.putIfAbsent(clazz,(o)->mapping.getMap(o));
        if(mapping instanceof WithDefer){
            ((WithDefer)mapping).setDefer(this);
        }
        addRepresent(clazz,new MapWrapper(mapping));
    }
    public <T> void addEncoding(Class<T> clazz,Function<T,Object> encoder){
        if(encoder instanceof WithDefer){
            ((WithDefer)encoder).setDefer(this);
        }
        encoders.putIfAbsent(clazz,(o)->encoder.apply((T)o));
        addRepresent(clazz,new ObjectWrapper<T>(encoder));
    }

    protected void addRepresent(Class<?> clazz,Represent represent){
        representers.put(clazz,represent);
    }
}
