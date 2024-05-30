package pt.ulisboa.tecnico.cnv.mss.imageprocessor;

import pt.ulisboa.tecnico.cnv.mss.RequestMetric;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;


@DynamoDbBean
public class ImageProcessorRequestMetric extends RequestMetric {
    private long numPixels;
    private long width;
    private long height;

    public long getNumPixels() {
        return numPixels;
    }

    public void setNumPixels(final long numPixels) {
        this.numPixels = numPixels;
    }

    public long getWidth() {
        return width;
    }

    public void setWidth(final long width) {
        this.width = width;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(final long height) {
        this.height = height;
    }
}
