package pt.ulisboa.tecnico.cnv.raytracer.shapes;

import pt.ulisboa.tecnico.cnv.raytracer.Log;
import pt.ulisboa.tecnico.cnv.raytracer.Point;
import pt.ulisboa.tecnico.cnv.raytracer.Ray;
import pt.ulisboa.tecnico.cnv.raytracer.RayHit;
import pt.ulisboa.tecnico.cnv.raytracer.Vector;

public class Cylinder extends Shape {
    private Point base;
    private Vector axis;
    private double radius;

    public Cylinder(Point base, Vector axis, double radius) {
        this.base = base;
        this.axis = axis;
        this.radius = radius;

        Log.warn("Cylinder shape is not supported. This shape will be ignored.");
    }

    public RayHit intersect(Ray ray) {
        return null;
    }
}
