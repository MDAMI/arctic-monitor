package com.mdami.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class ConnectionHandler implements Runnable, AutoCloseable {
    private final Socket socket;
    private final Path storageDirectory;
    private final ExistingFileStrategies existingFileStrategy;

    public ConnectionHandler(Socket socket, Path storageDirectory, ExistingFileStrategies existingFileStrategy){
        this.socket = socket;
        this.storageDirectory = storageDirectory;
        this.existingFileStrategy = existingFileStrategy;
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e){
            System.err.format("Socket did not close correctly. This should not occur. The following cause was given %s: %s%n", e.getClass(), e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            String fileName = in.readUTF();
            Properties properties = (Properties) in.readObject();
            in.close();

            System.out.format("Received file %s from client with properties:%n", fileName);
            properties.forEach((key, val) -> System.out.format("%s=%s%n", key, val));
            System.out.println();

            Path resolvedPath = storageDirectory.resolve(fileName);
            File newFile = resolvedPath.toFile();
            if (newFile.exists() && !existingFileStrategy.equals(ExistingFileStrategies.OVERWRITE)){
                if (existingFileStrategy == ExistingFileStrategies.RENAME){
                    String currentTime = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-MM-DD-AAAAAAAA"));
                    int lastPeriod = fileName.lastIndexOf(".");
                    String newFileName = fileName.substring(0,lastPeriod) + currentTime + fileName.substring(lastPeriod);
                    writeToFile(properties, storageDirectory.resolve(newFileName).toFile());
                }
            } else {
                writeToFile(properties, newFile);
            }


        } catch (IOException e) {
            System.err.format("Could not write to file. The cause is %s: %s%n", e.getClass(), e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Client sent invalid file");
        } finally {
            this.close();
        }
    }

    private void writeToFile(Properties properties, File file) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        properties.store(fileOutputStream, null);
        fileOutputStream.close();
    }
}
