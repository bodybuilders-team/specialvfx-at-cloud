package pt.ulisboa.tecnico.cnv.imageproc;

import java.awt.image.BufferedImage;

public class ImageProcessingRequest {
    private long id;
    private BufferedImage image;
    public long bblCount;
    public long instructionCount;
    private boolean completed = false;

    public ImageProcessingRequest(long id, BufferedImage image) {
        this.id = id;
        this.image = image;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public long getBblCount() {
        return this.bblCount;
    }

    public void setBblCount(long bblCount) {
        this.bblCount = bblCount;
    }

    public long getInstructionCount() {
        return instructionCount;
    }

    public void setInstructionCount(long instructionCount) {
        this.instructionCount = instructionCount;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @Override
    public String toString() {
        return "ImageProcessingRequest{" +
                "id=" + id +
                ", bblCount=" + bblCount +
                ", instructionCount=" + instructionCount +
                ", completed=" + completed +
                '}';
    }
}
