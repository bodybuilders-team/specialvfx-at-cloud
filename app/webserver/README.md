## Web Server

This project contains a web server that exposes functionality of the Ray Tracing and Image Processing projects.

### How to build

1. Make sure your `JAVA_HOME` environment variable is set to Java 11+ distribution
2. Run `mvn clean package`

### How to run locally

To run Web Server locally, execute this command:

```
java -cp target/webserver-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.webserver.WebServer
```
