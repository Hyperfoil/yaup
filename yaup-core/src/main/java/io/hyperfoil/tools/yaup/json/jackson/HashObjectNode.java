package io.hyperfoil.tools.yaup.json.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HashObjectNode extends ObjectNode {

    public Set<String> seen = new HashSet<>();
    public HashObjectNode(JsonNodeFactory nc) {
        super(nc);
    }
    public HashObjectNode(JsonNodeFactory nc, Map<String, JsonNode> kids) {
        super(nc,kids);
    }

    protected ObjectNode _put(String fieldName, JsonNode value) {
        this._children.put(fieldName, value);
        return this;
    }
    public ObjectNode putNull(String fieldName) {
        this._children.put(fieldName, this.nullNode());
        return this;
    }
}
