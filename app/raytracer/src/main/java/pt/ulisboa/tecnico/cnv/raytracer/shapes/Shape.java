package pt.ulisboa.tecnico.cnv.raytracer.shapes;

import pt.ulisboa.tecnico.cnv.raytracer.*;
import pt.ulisboa.tecnico.cnv.raytracer.pigments.Finish;
import pt.ulisboa.tecnico.cnv.raytracer.pigments.Pigment;

import java.awt.Color;


public abstract class Shape {
    public Pigment pigment;
    public Finish finish;

    public final void setMaterial(Pigment pigment, Finish finish) {
        this.pigment = pigment;
        this.finish = finish;
    }

    public abstract RayHit intersect(Ray ray);

    public boolean contains(Point p) {
        return false;
    }

    public final Color getColor(Point p) {
        return pigment.getColor(p);
    }
}
