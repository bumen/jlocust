package com.github.qvp.message;

import java.io.IOException;
import java.util.Map;

import org.msgpack.core.MessageBufferPacker;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

/**
 * @author vrajat
 */
public class Message {

    private final String type;
    private final Map<String, Object> data;
    private final String nodeID;

    public Message(String type, Map<String, Object> data, String nodeID) {
        this.type = type;
        this.data = data;
        this.nodeID = nodeID;
    }

    public static Message build(byte[] bytes) {
        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(bytes);

        InputVisitor visitor = new InputVisitor(unpacker);

        return visitor.readMessage();
    }

    public String getType() {
        return this.type;
    }

    public Map<String, Object> getData() {
        return this.data;
    }

    public String getNodeID() {
        return this.nodeID;
    }

    public byte[] getBytes() throws IOException {
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        Visitor visitor = new Visitor(packer);
        // a message contains three fields, (type & data & nodeID)
        packer.packArrayHeader(3);
        packer.packString(this.type);
        if (this.data != null) {
            packer.packMapHeader(this.data.size());
            for (Map.Entry<String, Object> entry : this.data.entrySet()) {
                packer.packString(entry.getKey());
                visitor.visit(entry.getValue());
            }
        } else {
            packer.packNil();
        }
        packer.packString(this.nodeID);
        byte[] bytes = packer.toByteArray();
        packer.close();
        return bytes;
    }

    @Override
    public String toString() {
        return String.format("%s-%s-%s", nodeID, type, data);
    }



}