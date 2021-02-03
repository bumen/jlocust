package com.github.locust.rpc;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import com.github.locust.message.Message;

/**
 * Locust used to support both plain-socket and zeromq.
 * Since Locust 0.8, it removes the plain-socket implementation.
 *
 * Locust4j only supports zeromq.
 *
 * @author myzhan
 */
public class Server extends BaseSocket {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);


    public Server(String host, int port) {
        super(ZMQ.ROUTER);
        this.socket.bind(String.format("tcp://%s:%d", host, port));

        logger.debug("Locust4j is connected to master({}:{})", host, port);
    }

    @Override
    public Message recv() throws IOException {
        String client_id = this.socket.recvStr();
        byte[] bytes = this.socket.recv();
        if (bytes == null) {
            return null;
        }

        return Message.build(bytes);
    }

    @Override
    public void send(Message message) throws IOException {
        byte[] bytes = message.getBytes();
        this.socket.sendMore(message.getNodeID());
        this.socket.send(bytes, ZMQ.DONTWAIT);
    }
}

