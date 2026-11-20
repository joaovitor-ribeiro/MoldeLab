package com.moldelab.modules.pattern.geometry;

public record Bounds(double minX, double minY, double maxX, double maxY) {
    public double width() { return maxX - minX; }
    public double height() { return maxY - minY; }
}
