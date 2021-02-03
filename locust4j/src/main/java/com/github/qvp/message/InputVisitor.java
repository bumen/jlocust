package com.github.qvp.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.msgpack.core.MessageFormat;
import org.msgpack.core.MessageUnpacker;

/**
 * @date 2021-02-02
 * @author zhangyuqiang02@playcrab.com
 */
public class InputVisitor {

    private MessageUnpacker unpacker;

    public InputVisitor(MessageUnpacker unpacker) {
        this.unpacker = unpacker;
    }


    public Message readMessage() {
        try {
            int arrayHeader = unpacker.unpackArrayHeader();

            String type = unpacker.unpackString();

            Map<String, Object> data;
            if (unpacker.getNextFormat() == MessageFormat.NIL) {
                unpacker.unpackNil();
                data = null;
            } else {
                data = (Map<String, Object>) readData();
            }

            String id;
            if (unpacker.getNextFormat() == MessageFormat.NIL) {
                unpacker.unpackNil();
                id = null;
            } else {
                id = unpacker.unpackString();
            }

            unpacker.close();

            return new Message(type, data, id);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Object readData() throws IOException {
        // unpack value
        MessageFormat fmt = unpacker.getNextFormat();
        if (fmt == MessageFormat.NIL) {
            return null;
        }

        Object value;

        switch (fmt.getValueType()) {
        case BOOLEAN:
            value = unpacker.unpackBoolean();
            break;
        case FLOAT:
            if (fmt == MessageFormat.FLOAT64) {
                value = (float) unpacker.unpackDouble();
            } else {
                value = unpacker.unpackFloat();
            }
            break;
        case INTEGER:
            // 都是long value
            value = unpacker.unpackLong();
            break;
        case NIL:
            value = null;
            unpacker.unpackNil();
            break;
        case STRING:
            value = unpacker.unpackString();
            break;
        case MAP:
            int mapSize = unpacker.unpackMapHeader();
            Map data = new HashMap<>(mapSize);
            while (mapSize > 0) {
                Object key = readData();
                Object v = readData();

                if (null != key) {
                    data.put(key, v);
                }
                mapSize--;
            }

            value = data;

            break;
        case ARRAY:
            int size = unpacker.unpackArrayHeader();
            List r = new ArrayList<>(size);
            while (size > 0) {
                r.add(readData());
                size--;
            }

            value = r;
            break;
        default:
            throw new IOException(
                    "Message received unsupported type: " + fmt
                            .getValueType());
        }

        return value;
    }

}
