package pt.ulisboa.tecnico.cnv.mss.imageprocessor;

import pt.ulisboa.tecnico.cnv.mss.RequestMetric;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;


@DynamoDbBean
public class ImageProcessorRequestMetric extends RequestMetric {
    private long numPixels;

    public long getNumPixels() {
        return numPixels;
    }

    public void setNumPixels(final long numPixels) {
        this.numPixels = numPixels;
    }
}
