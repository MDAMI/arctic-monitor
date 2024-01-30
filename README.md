# Arctic Monitor
This repository contains two programs, a client and server. 

The client monitors a specified directory for new Java
properties files. When a new file is detected, it will read the file, filter out any keys that do not match a regex
pattern specified in the config and send the remaining keys to the server program.

The server listens and waits for files to be sent to it by client programs. When it receives a file, it reconstructs
the file and writes it to a specified storage directory.

## Usage
Ensure you have [Maven](https://maven.apache.org/download.cgi) properly installed and configured on your system.
Build the programs using 

```
mvn clean package
```

This will build both the client and server programs. You can also build each program individually by running the 
command in the corresponding directory. The jar files will be found in the target directory generated for each module

To run the programs use the command:
```shell
java -jar arctic-<server|client>-1.0-SNAPSHOT <configFilePath>
```

## Configuration
Examples of configuration files have been provided in ```./serverConfig.properties``` and ```./clientConfig.
properties```
### Client
| Key                | Required | Value                                                                                                                                             |
|--------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| monitoredDirectory | true     | The path to the directory to be monitored                                                                                                         |
| keyFilterPattern   | true     | The regex pattern that will be used to filter the keys. <br/>The key is required but the value can be blank. This will result in no keys matching |
| serverAddress      | false    | The address of the server that the files will be sent to. <br/>Defaults to the loopback address if not provided.                                  |
| serverPort         | false    | The port of the server that the files will be sent to. <br/>Defaults to 80 if not provided.                                                       |
| sendEmptyFiles     | false    | Whether the client will send any files that have had all of the keys filtered out.                                                                |

### Server
| Key                  | Required | Value                                                                                                                                                                                                                                                                                                                                                    |
|----------------------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| storageDirectory     | true     | The path to the directory the received files should be stored in. The directory must already exist.                                                                                                                                                                                                                                                      |
| serverPort           | false    | The port the server will listen on. Defaults to 80 if not provided.                                                                                                                                                                                                                                                                                      |
| existingFileStrategy | false    | How the server will handle cases where a file sent by a client already exists in the storageDirectory. Possible values are: <br/>```IGNORE```: Do not write the new file to disk.<br/>```OVERWRITE```: Overwrite the existing file.<br/>```RENAME```: Rename the new file to include the current time.<br/>Will default to ```IGNORE``` if not provided. |
