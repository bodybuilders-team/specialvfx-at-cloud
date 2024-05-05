package pt.ulisboa.tecnico.cnv.raytracer.pigments;

import pt.ulisboa.tecnico.cnv.raytracer.Point;

import java.awt.Color;


public interface Pigment {
    public Color getColor(Point p);
}
