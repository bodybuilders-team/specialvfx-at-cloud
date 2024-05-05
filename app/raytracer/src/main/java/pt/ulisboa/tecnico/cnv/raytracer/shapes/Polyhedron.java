package pt.ulisboa.tecnico.cnv.raytracer.shapes;

import pt.ulisboa.tecnico.cnv.raytracer.*;

import java.util.List;

public class Polyhedron extends Shape {
    private List<Polygon> faces;

    public Polyhedron(List<Polygon> faces) {
        this.faces = faces;

        Log.warn("Polyhedron shape is not supported. This shape will be ignored.");
    }

    @Override
    public RayHit intersect(Ray ray) {
        return null;
    }
}
