## Image Processing

This project contains functionality to blur and enhance images.

### How to build

1. Make sure your `JAVA_HOME` environment variable is set to Java 11+ distribution
2. Run `mvn clean package`

### How to run locally

To run BlurImage locally in CLI, execute this command:

```
java -cp target/imageproc-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.imageproc.BlurImageHandler <input-file> <output-file>
```

To run EnhanceImage locally in CLI, execute this command:

```
java -cp target/imageproc-1.0.0-SNAPSHOT-jar-with-dependencies.jar pt.ulisboa.tecnico.cnv.imageproc.EnhanceImageHandler <input-file> <output-file>
```

The input file should be an image. You can find some example images in the `resources` folder.
