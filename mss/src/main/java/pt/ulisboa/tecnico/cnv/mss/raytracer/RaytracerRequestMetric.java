package pt.ulisboa.tecnico.cnv.mss.raytracer;

import pt.ulisboa.tecnico.cnv.mss.RequestMetric;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class RaytracerRequestMetric extends RequestMetric {
    private int sceneSize;
    private int scols;
    private int srows;
    private int wcols;
    private int wrows;

    public int getWrows() {
        return wrows;
    }

    public void setWrows(final int wrows) {
        this.wrows = wrows;
    }

    public int getWcols() {
        return wcols;
    }

    public void setWcols(final int wcols) {
        this.wcols = wcols;
    }

    public int getSrows() {
        return srows;
    }

    public void setSrows(final int srows) {
        this.srows = srows;
    }

    public int getScols() {
        return scols;
    }

    public void setScols(final int scols) {
        this.scols = scols;
    }

    public int getSceneSize() {
        return sceneSize;
    }

    public void setSceneSize(final int sceneSize) {
        this.sceneSize = sceneSize;
    }
}
