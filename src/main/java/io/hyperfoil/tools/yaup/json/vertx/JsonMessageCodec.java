package io.hyperfoil.tools.yaup.json.vertx;

import io.hyperfoil.tools.yaup.json.Json;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

/**
 * Used with Vertx EventBus to add support for yaup Json 
 */
public class JsonMessageCodec implements MessageCodec<Json, Json> {
    @Override
    public void encodeToWire(Buffer buffer, Json json) {
        String jsonStr = json.toString();
        int length = jsonStr.getBytes().length;
        buffer.appendInt(length);
        buffer.appendString(jsonStr);
    }

    @Override
    public Json decodeFromWire(int i, Buffer buffer) {
        int _pos = i;
        int length = buffer.getInt(_pos);
        _pos+=4;//add lenght of integer
        String jsonStr = buffer.getString(_pos,_pos+length);
        _pos+=length;
        return Json.fromString(jsonStr);
    }

    @Override
    public Json transform(Json json) {
        return null;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public byte systemCodecID() {
        return 0;
    }
}
