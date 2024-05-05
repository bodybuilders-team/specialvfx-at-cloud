package pt.ulisboa.tecnico.cnv.raytracer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

public class Main {
    private static final String USAGE = "Usage:\n"+
            "java -cp src raytracer.Main infile outfule scols srows wcols wrows coff roff [-tm=texmap.bmp] [-options]\n"+
            "\n"+
            "    where:\n"+
            "        infile          - input file name\n"+
            "        outfile         - output result file name\n"+
            "        scols           - scene width (in pixels)\n"+
            "        srows           - scene height (in pixels)\n"+
            "        wcols           - window width (in pixels, max scols)\n"+
            "        wrows           - window height (in pixels, max srows)\n"+
            "        coff            - window column offset (in pixels, max: scols - wcols)\n"+
            "        roff            - windows row offset (in pixels, max: srows - wrows)\n"+
            "        -tm=texmap.bmp  - bmp output file name\n"+
//            "        -test     - run in test mode (see below)\n"+
//            "        -noshadow - don't compute shadows\n"+
//            "        -noreflec - don't do reflections\n"+
//            "        -notrans  - don't do transparency\n"+
            "        -aa             - use anti-aliasing (~4x slower)\n"+
            "        -multi          - use multi-threading (good for large, anti-aliased images)";
//            "        -nocap    - cylinders and cones are infinite";

    public static boolean ANTI_ALIAS = false;
    public static boolean MULTI_THREAD = false;
    public static final boolean DEBUG = false;


    private static void printUsage() {
        System.out.println(USAGE);
    }

    public static void main(String[] args) throws IOException {
        if(args.length < 8) {
            printUsage();
            System.exit(0);
        }

        // required arguments
        String inFile = args[0];
        String outFile = args[1];
        int scols = Integer.parseInt(args[2]);
        int srows = Integer.parseInt(args[3]);
        int wcols = Integer.parseInt(args[4]);
        int wrows = Integer.parseInt(args[5]);
        int coff = Integer.parseInt(args[6]);
        int roff = -Integer.parseInt(args[7]);
        byte[] bmptexmap = null;

        // optional arguments
        Optional<String> texmapOptional = Arrays.stream(args).filter(x -> x.startsWith("-tm=")).findFirst();
        if (texmapOptional.isPresent()) {
            // Get the filename after "=".
            String texmapFilename = texmapOptional.get().split("=", 2)[1];
            bmptexmap = Files.readAllBytes(Paths.get(texmapFilename));
            System.out.println("Using texmap: '" + texmapFilename + "'.");
        }
        if (Arrays.stream(args).anyMatch("-aa"::equals)) {
            System.out.println("Anti-aliasing enabled.");
            ANTI_ALIAS = true;
        }
        if (Arrays.stream(args).anyMatch("-multi"::equals)) {
            System.out.println("Multi-threading enabled.");
            MULTI_THREAD = true;
        }

        RayTracer rayTracer = new RayTracer(scols, srows, wcols, wrows, coff, roff);
        rayTracer.readScene(Files.readAllBytes(Paths.get(inFile)), bmptexmap);
        BufferedImage image = rayTracer.draw();
        ImageIO.write(image, "bmp", new File(outFile));
    }
}
