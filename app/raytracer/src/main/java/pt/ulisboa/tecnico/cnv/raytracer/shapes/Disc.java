package pt.ulisboa.tecnico.cnv.raytracer.shapes;

import pt.ulisboa.tecnico.cnv.raytracer.Log;
import pt.ulisboa.tecnico.cnv.raytracer.Point;
import pt.ulisboa.tecnico.cnv.raytracer.Ray;
import pt.ulisboa.tecnico.cnv.raytracer.RayHit;
import pt.ulisboa.tecnico.cnv.raytracer.Vector;

public class Disc extends Shape {
    private Point center;
    private Vector normal;
    private double radius;

    public Disc(Point center, Vector normal, double radius) {
        this.center = center;
        this.normal = normal;
        this.radius = radius;

        Log.warn("Disc shape is not supported. This shape will be ignored.");
    }

    @Override
    public RayHit intersect(Ray ray) {
        return null;
    }
}
