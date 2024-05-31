package pt.ulisboa.tecnico.cnv.mss.raytracer;

import pt.ulisboa.tecnico.cnv.mss.RequestMetric;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
public class RaytracerRequestMetric extends RequestMetric {
    private int sceneSize;
    private int textMapSize;
    private int scols;
    private int srows;
    private int wcols;
    private int wrows;
    private int coff;
    private int roff;

    public int getRoff() {
        return roff;
    }

    public void setRoff(final int roff) {
        this.roff = roff;
    }

    public int getCoff() {
        return coff;
    }

    public void setCoff(final int coff) {
        this.coff = coff;
    }

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


    public int getTextMapSize() {
        return textMapSize;
    }

    public void setTextMapSize(final int textMapSize) {
        this.textMapSize = textMapSize;
    }

    public int getSceneSize() {
        return sceneSize;
    }

    public void setSceneSize(final int sceneSize) {
        this.sceneSize = sceneSize;
    }
}
