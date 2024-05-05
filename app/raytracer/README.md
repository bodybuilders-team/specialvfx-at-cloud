## Ray Tracing

This project contains functionality to render 3D scenes with ray tracing.

### How to build

1. Make sure your `JAVA_HOME` environment variable is set to Java 11+ distribution
2. Run `mvn clean package`

### How to run locally

To run RayTracer locally in CLI, execute this command:

```
java -cp target/raytracer-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.raytracer.Main <input-scene-file.txt> <output-file.bmp> 400 300 400 300 0 0 [-tm=<texmap-file.bmp>] [-aa]
```

Arguments in brackets are optional.

Some input scene files (the .txt ones) require you to provide a texture file with the `[-tm=<texmap-file.bmp>]` argument. This texture file should be a valid image.

You can find some input scene files in the `resources` directory.

For more details regarding the arguments just run this command:

```
java -cp target/raytracer-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.raytracer.Main
```
