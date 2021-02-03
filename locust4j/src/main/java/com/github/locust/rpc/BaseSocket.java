package com.github.locust.rpc;

import java.io.IOException;

import org.zeromq.ZMQ;

import com.github.locust.message.Message;

/**
 * RPC Client interface.
 *
 * @author myzhan
 */
public abstract class BaseSocket {
    protected final ZMQ.Context context;

    protected final ZMQ.Socket socket;


    protected BaseSocket(int sock_type) {
        context = ZMQ.context(1);

        socket = context.socket(sock_type);

        // socket.setIdentity("123123".getBytes());
        socket.setTCPKeepAlive(1);
        socket.setTCPKeepAliveIdle(30);
    }

    /**
     * receive message from master
     *
     * @return Message
     * @throws IOException network IO exception
     */
    public Message recv() throws IOException {
        byte[] bytes = this.socket.recv();
        if (bytes == null) {
            return null;
        }

        return Message.build(bytes);
    }

    /**
     * send message to master
     *
     * @param message msgpack message sent to the master
     * @throws IOException network IO exception
     */
    public void send(Message message) throws IOException {
        byte[] bytes = message.getBytes();
        this.socket.send(bytes, 0);
    }


    public void close() {
        socket.close();
        context.close();
    }

}