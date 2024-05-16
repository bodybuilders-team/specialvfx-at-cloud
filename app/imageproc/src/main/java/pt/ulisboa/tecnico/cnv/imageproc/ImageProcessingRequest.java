package pt.ulisboa.tecnico.cnv.imageproc;

import lombok.Getter;
import pt.ulisboa.tecnico.cnv.mss.Request;

import java.awt.image.BufferedImage;

/**
 * An Image Processing request in the system containing an image to be processed.
 */
@Getter
public class ImageProcessingRequest extends Request {
    private final BufferedImage image;

    public ImageProcessingRequest(long id, BufferedImage image) {
        super(id);
        this.image = image;
    }

    @Override
    public String toString() {
        return "ImageProcessingRequest{" +
                "id=" + id +
                ", imageSize=" + image.getWidth() + "x" + image.getHeight() +
                ", bblCount=" + bblCount +
                ", instructionCount=" + instructionCount +
                ", completed=" + completed +
                '}';
    }
}

