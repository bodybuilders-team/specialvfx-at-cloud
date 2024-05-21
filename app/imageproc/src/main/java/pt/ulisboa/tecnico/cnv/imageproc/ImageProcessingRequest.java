package pt.ulisboa.tecnico.cnv.imageproc;

import lombok.Getter;
import pt.ulisboa.tecnico.cnv.javassist.Request;

import java.awt.image.BufferedImage;

/**
 * An Image Processing request in the system containing an image to be processed.
 */
@Getter
public class ImageProcessingRequest extends Request {
    private final BufferedImage image;

    public ImageProcessingRequest(BufferedImage image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return "ImageProcessingRequest{" +
                "id=" + getId() +
                ", imageSize=" + image.getWidth() + "x" + image.getHeight() +
                ", bblCount=" + getBblCount() +
                ", instructionCount=" + getInstructionCount() +
                ", completed=" + isCompleted() +
                '}';
    }
}

