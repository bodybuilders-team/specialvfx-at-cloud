package pt.ulisboa.tecnico.cnv.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.stream.Collectors;

public class Utils {

    private Utils() {
        // hide implicit public constructor
    }

    /**
     * Read an image from a byte array.
     *
     * @param data The byte array
     * @return The image
     * @throws IOException If an error occurs
     */
    public static BufferedImage readImage(final byte[] data) throws IOException {
        // Result syntax: data:image/<format>;base64,<encoded image>
        String result = new String(data).lines().collect(Collectors.joining("\n"));
        String[] resultSplits = result.split(",");

        byte[] decoded = Base64.getDecoder().decode(resultSplits[1]);
        ByteArrayInputStream bais = new ByteArrayInputStream(decoded);

        return ImageIO.read(bais);
    }
}
