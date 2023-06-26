
# Remote Hotswap

This mod/gradle plugin allows for hotswapping of a mod on a forge/fabric instance remotely,
this means you don't have to host the server within your IDE or session.

## How to use

1. place jar as mod on forge/fabric
2. edit `mchotswap.properties` in the minecraft folder

```properties

apiKey=changeme!
port=25401

```

3. add task to gradle:

```groovy

tasks.register("hotswapUpload", UploadToDevServer) {
    inputFile = tasks.remapJar.outputs.files.singleFile
    modid = "modid"
    serverUrl = "http://localhost:25401"
    serverKey = "changeme!"
}

hotswapUpload.dependsOn remapJar

```

4. profit