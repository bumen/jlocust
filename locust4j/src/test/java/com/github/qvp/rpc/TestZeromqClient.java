package com.github.qvp.rpc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import com.github.qvp.message.Message;

/**
 * @author myzhan
 */
public class TestZeromqClient {

    @Test
    @Ignore
    public void TestPingPong() throws Exception {
        // randomized the port to avoid conflicts
        int masterPort = ThreadLocalRandom.current().nextInt(1000) + 1024;

        TestServer server = new TestServer("0.0.0.0", masterPort);
        server.start();

        BaseSocket client = new Server("0.0.0.0", masterPort);
        Map<String, Object> data = new HashMap<>();
        data.put("hello", "world");

        client.send(new Message("test", data, "node"));
        Message message = client.recv();

        assertEquals("test", message.getType());
        assertEquals("node", message.getNodeID());
        assertEquals(data, message.getData());

        Thread.sleep(100);
        server.stop();
        client.close();
    }
}
