package com.mdami.client;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class ArcticClient implements Runnable, AutoCloseable {
    private final Path monitoredDirectory;
    private final Pattern keyFilterPattern;
    private final WatchService watchService;
    private final Boolean sendEmptyFiles;
    private final String serverAddress;
    private final int serverPort;

    public ArcticClient(String configFilePath){
        Properties config;
        try {
            config = readPropertiesFile(configFilePath);
        } catch (NoSuchFileException e){
            throw new IllegalArgumentException("Config file could not be found", e);
        } catch (IOException e){
            throw new IllegalArgumentException("Config file could not be read", e);
        }

        if (config.getProperty("monitoredDirectory") == null){
            throw new IllegalArgumentException("Config file must contain key monitoredDirectory");
        }
        if (config.getProperty("keyFilterPattern") == null){
            throw new IllegalArgumentException("Config file must contain key keyFilterPattern");
        }

        monitoredDirectory = Paths.get(config.getProperty("monitoredDirectory"));
        keyFilterPattern = Pattern.compile(config.getProperty("keyFilterPattern"));
        serverAddress = config.getProperty("serverAddress") != null ? config.getProperty("serverAddress") : "127.0.0.1";
        serverPort = config.getProperty("serverPort") != null ? Integer.parseInt(config.getProperty("serverPort")) : 80;
        sendEmptyFiles = Boolean.parseBoolean(config.getProperty("sendEmptyFiles"));

        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Could not create watch service for directory %s", monitoredDirectory),e);
        }
    }

    @Override
    public void run() {
        try {
            monitoredDirectory.register(watchService, ENTRY_CREATE);
        } catch (NotDirectoryException e){
            throw new IllegalArgumentException(String.format("The provided path %s is not a directory"), e);
        } catch (NoSuchFileException e){
            throw new IllegalArgumentException(String.format("Directory %s does not exist. Monitor can not be registered"), e);
        } catch (IOException e) {
            throw new IllegalStateException("An error occurred while registering the watchService", e);
        }

        System.out.format("Watching directory %s; Connecting to server %s:%s%n", monitoredDirectory, serverAddress, serverPort);
        while (true) {
            try {
                WatchKey watchKey = watchService.take();

                for (WatchEvent<?> event : watchKey.pollEvents()) {
                    if (event.kind() == OVERFLOW) {
                        continue;
                    }

                    String fileName = event.context().toString();
                    Path resolvedFile = monitoredDirectory.resolve(fileName);

                    //Delay to ensure filesystem has released any locks on file
                    TimeUnit.MICROSECONDS.sleep(50);

                    Properties fileProperties;
                    try {
                        fileProperties = readPropertiesFile(resolvedFile.toString());
                    } catch (IOException e) {
                        System.err.format("Property file %s could not be read. Caused by %s: %s%n  Further processing for this file has been halted%n", fileName, e.getClass(), e.getMessage());
                        continue;
                    }

                    applyKeyFilter(fileProperties, keyFilterPattern);
                    try{
                        sendPropertiesFile(fileName, fileProperties);
                        deleteFile(resolvedFile);
                    } catch (ConnectException e) {
                        System.err.format("Unable to connect to server %s:%s%n  Further processing for this file has been halted%n", serverAddress, serverPort);
                    } catch (IOException e) {
                        System.err.format("Error sending file %s. Caused by %s: %s%n  Further processing for this file has been halted%n", fileName, e.getClass(), e.getMessage());
                    }
                }

                boolean valid = watchKey.reset();
                if (!valid) {
                    break;
                }

            } catch (InterruptedException | ClosedWatchServiceException e) {
                throw new IllegalStateException("Error with Watch Key", e);
            }
        }
    }

    public static void applyKeyFilter(Properties properties, Pattern pattern) {
        properties.keySet().removeIf(key -> !pattern.matcher(key.toString()).matches());
    }
    private static void deleteFile(Path resolvedFile) {
        try {
            Files.delete(resolvedFile);
            System.out.format("File %s deleted%n", resolvedFile);
        } catch (IOException e) {
            System.err.format("File %s could not be deleted%n", resolvedFile);
            System.err.println(e.getMessage());
        }
    }

    public void sendPropertiesFile(String fileName, Properties properties) throws IOException{
        if (!sendEmptyFiles && properties.isEmpty()){
            return;
        }

        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeUTF(fileName);
            out.writeObject(properties);
            out.flush();
        }
        System.out.format("File %s sent to server%n", fileName);
    }

    private Properties readPropertiesFile(String filePath) throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = Files.newInputStream(Paths.get(filePath));
        properties.load(inputStream);
        inputStream.close();
        return properties;
    }

    @Override
    public void close() throws Exception {
        watchService.close();
    }
}