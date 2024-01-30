package com.mdami.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArcticServer implements Runnable, AutoCloseable{
    private final Path storageDirectory;
    private final ExistingFileStrategies existingFileStrategy;
    private final List<ConnectionHandler> connectionHandlers;
    private final ExecutorService executorService;
    private final ServerSocket serverSocket;

    public ArcticServer(String configFilePath) throws IOException {
        Properties config;
        try {
            config = readPropertiesFile(configFilePath);
        } catch (NoSuchFileException e){
            throw new IllegalArgumentException("Config file could not be found", e);
        } catch (IOException e){
            throw new IllegalArgumentException("Config file could not be read", e);
        }

        if (config.getProperty("storageDirectory") == null){
            throw new IllegalArgumentException("Config file must contain key storageDirectory");
        }

        storageDirectory = Paths.get(config.getProperty("storageDirectory"));
        int serverPort = config.getProperty("serverPort") != null ? Integer.parseInt(config.getProperty("serverPort")) : 80;
        existingFileStrategy = config.getProperty("existingFileStrategy") != null ?
                ExistingFileStrategies.valueOf(config.getProperty("existingFileStrategy")) : ExistingFileStrategies.IGNORE;

        connectionHandlers = new ArrayList<>();
        executorService = Executors.newCachedThreadPool();

        try {
            serverSocket = new ServerSocket(serverPort);
            System.out.format("Listening on %s%n", serverPort);
        } catch (IOException e) {
            throw new IOException(String.format("Unable to open a server on port %d", serverPort), e);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                ConnectionHandler handler = new ConnectionHandler(clientSocket, storageDirectory, existingFileStrategy);
                connectionHandlers.add(handler);
                executorService.submit(handler);
            } catch (IOException e){
                System.err.format("Incoming connection closed with error %s: %s", e.getClass(), e.getMessage());
            }
        }
    }

    private static Properties readPropertiesFile(String filePath) throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = Files.newInputStream(Paths.get(filePath));
        properties.load(inputStream);
        inputStream.close();
        return properties;
    }

    @Override
    public void close() {
        connectionHandlers.forEach(ConnectionHandler::close);
        executorService.shutdownNow();
    }
}
