package pt.ulisboa.tecnico.cnv;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

public class CNVOLSMultipleLinearRegression extends OLSMultipleLinearRegression {

    public CNVOLSMultipleLinearRegression() {
        super();
    }

    public CNVOLSMultipleLinearRegression(double threshold) {
        super(threshold);
    }

    public double predict(double[] x) {
        final var beta = this.calculateBeta();

        double[] newArray = new double[x.length + 1];
        newArray[0] = 1.0;
        System.arraycopy(x, 0, newArray, 1, x.length);

        final var vector = new ArrayRealVector(newArray);

        return vector.dotProduct(beta);
    }

}
