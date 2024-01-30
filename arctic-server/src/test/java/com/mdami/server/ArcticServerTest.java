package com.mdami.server;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ArcticServerTest {
    ArcticServer arcticServer;

    @Before
    public void setup() {
        String configFilePath;
        try{
            configFilePath = getClass().getClassLoader().getResource("serverConfig.properties").getPath();
            int windowsFile = configFilePath.indexOf(':');
            if (windowsFile != -1) {
                configFilePath = configFilePath.substring(windowsFile+1);
            }

            try {
                arcticServer = new ArcticServer(configFilePath);
                ExecutorService service = Executors.newSingleThreadExecutor();
                service.submit(arcticServer);
            } catch (IOException e){
                fail("Could not set up server");
            }
        } catch (NullPointerException e){
            fail("Config file can not be found");
        }
    }

    @Test
    public void shouldHandleMultipleClients() {
        try {
            Socket socket1 = new Socket("127.0.0.1", 4633);
            Socket socket2 = new Socket("127.0.0.1", 4633);
            Socket socket3 = new Socket("127.0.0.1", 4633);
            assertTrue(true);
        } catch (IOException e){
            fail("IOException thrown");
        }
    }
}
