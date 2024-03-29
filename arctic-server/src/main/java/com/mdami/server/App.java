package com.mdami.server;

/**
 * Arctic Server
 *
 */
public class App
{
    public static void main( String[] args ) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar arctic-server-1.0-SNAPSHOT <configFilePath>");
            return;
        }

        String configFilePath = args[0];
        try (ArcticServer arcticServer = new ArcticServer(configFilePath)){
            arcticServer.run();
        } catch (Exception e) {
            System.err.println("Arctic server failed: " + e.getMessage());
            e.printStackTrace();
        }

    }
}
