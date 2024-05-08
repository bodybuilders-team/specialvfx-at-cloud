package pt.ulisboa.tecnico.cnv.imageproc;

import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;

public class ImageProcessingRequest {
    private long id;
    @Getter
    private BufferedImage image;
    public long bblCount;
    public long instructionCount;
    @Setter
    private boolean completed = false;
    @Setter
    private long opTime;

    public ImageProcessingRequest(long id, BufferedImage image) {
        this.id = id;
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
                ", opTime=" + opTime + "ns" +
                '}';
    }
}
