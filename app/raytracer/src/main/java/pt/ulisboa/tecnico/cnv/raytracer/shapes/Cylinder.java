package pt.ulisboa.tecnico.cnv.raytracer.shapes;

import pt.ulisboa.tecnico.cnv.raytracer.*;

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
