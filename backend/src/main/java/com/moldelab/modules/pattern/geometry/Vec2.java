package com.moldelab.modules.pattern.geometry;

/** Ponto/vetor 2D em milímetros. Convenção: x para a direita, y para BAIXO (como SVG/papel). */
public record Vec2(double x, double y) {

    public static final Vec2 ZERO = new Vec2(0, 0);

    public static Vec2 of(double x, double y) {
        return new Vec2(x, y);
    }

    public Vec2 plus(Vec2 o) { return new Vec2(x + o.x, y + o.y); }
    public Vec2 minus(Vec2 o) { return new Vec2(x - o.x, y - o.y); }
    public Vec2 times(double k) { return new Vec2(x * k, y * k); }
    public double dot(Vec2 o) { return x * o.x + y * o.y; }
    public double cross(Vec2 o) { return x * o.y - y * o.x; }
    public double length() { return Math.hypot(x, y); }
    public double distanceTo(Vec2 o) { return Math.hypot(x - o.x, y - o.y); }

    public Vec2 normalized() {
        double len = length();
        return len < 1e-12 ? ZERO : new Vec2(x / len, y / len);
    }

    /** Perpendicular (y, -x). Para polígonos de área positiva aponta para FORA. */
    public Vec2 perp() { return new Vec2(y, -x); }

    public Vec2 lerp(Vec2 o, double t) { return new Vec2(x + (o.x - x) * t, y + (o.y - y) * t); }

    public Vec2 rotate(double radians) {
        double c = Math.cos(radians), s = Math.sin(radians);
        return new Vec2(x * c - y * s, x * s + y * c);
    }

    public Vec2 mirrorX() { return new Vec2(-x, y); }
}
