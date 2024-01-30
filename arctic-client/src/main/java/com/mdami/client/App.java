package com.mdami.client;

/**
 * Arctic Client
 *
 */
public class App
{
    public static void main( String[] args ) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar arctic-client-1.0-SNAPSHOT <configFilePath>");
            return;
        }

        String configFilePath = args[0];
        try (ArcticClient arcticClient = new ArcticClient(configFilePath)){
            arcticClient.run();
        } catch (Exception e) {
            System.err.println("Arctic client failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

