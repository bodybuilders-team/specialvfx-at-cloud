package pt.ulisboa.tecnico.cnv;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;

public class CNVMultipleLinearRegression {
    private RealVector w;

    final long numIterations;
    final double learningRate;

    public CNVMultipleLinearRegression() {
        super();

        this.learningRate = 0.001;
        this.numIterations = 10000;
    }

    public CNVMultipleLinearRegression(long numIterations, double learningRate) {
        super();
        this.numIterations = numIterations;
        this.learningRate = learningRate;
    }

    public double predict(double[] x) {
        double[] newArray = new double[x.length + 1];
        newArray[0] = 1.0;
        System.arraycopy(x, 0, newArray, 1, x.length);

        final var vector = new ArrayRealVector(newArray);

        return vector.dotProduct(this.w);
    }

    public void newSampleData(final double[] y, final double[][] x) {
        double[][] xWithIntercept = new double[x.length][x[0].length + 1];

        for (int i = 0; i < x.length; i++) {
            xWithIntercept[i][0] = 1.0;
            System.arraycopy(x[i], 0, xWithIntercept[i], 1, x[i].length);
        }

        final var xMat = MatrixUtils.createRealMatrix(xWithIntercept);
        final var yMat = MatrixUtils.createColumnRealMatrix(y);

        var w = MatrixUtils.createRealMatrix(xWithIntercept[1].length, 1);

        for (int i = 0; i < numIterations; i++) {
            final var error = xMat.multiply(w).subtract(yMat);

            final var gradient = xMat.transpose().multiply(error).scalarMultiply(1.0 / xWithIntercept.length);
            w = w.subtract(gradient.scalarMultiply(learningRate));
        }

        this.w = w.getColumnVector(0);
    }
}
