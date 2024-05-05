package pt.ulisboa.tecnico.cnv.raytracer.pigments;

import pt.ulisboa.tecnico.cnv.raytracer.Point;

import java.awt.Color;

public class SolidPigment implements Pigment {
    public Color color;

    public SolidPigment(Color color) {
        this.color = color;
    }

    public Color getColor(Point p) {
        return color;
    }

    public String toString() {
        return "solid";
    }
}
