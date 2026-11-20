package com.moldelab.modules.pattern.geometry;

import java.util.ArrayList;
import java.util.List;

public final class Polygons {

    private Polygons() {}

    /** Área com sinal (fórmula do laço). Positiva = sentido horário visual na convenção y-para-baixo. */
    public static double signedArea(List<Vec2> poly) {
        double s = 0;
        for (int i = 0; i < poly.size(); i++) {
            Vec2 a = poly.get(i), b = poly.get((i + 1) % poly.size());
            s += a.x() * b.y() - b.x() * a.y();
        }
        return s / 2;
    }

    public static double area(List<Vec2> poly) {
        return Math.abs(signedArea(poly));
    }

    public static Bounds bounds(List<Vec2> pts) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        for (Vec2 p : pts) {
            minX = Math.min(minX, p.x()); minY = Math.min(minY, p.y());
            maxX = Math.max(maxX, p.x()); maxY = Math.max(maxY, p.y());
        }
        return new Bounds(minX, minY, maxX, maxY);
    }

    /** Remove pontos consecutivos duplicados (e o fechamento repetido). */
    public static List<Vec2> dedupe(List<Vec2> pts, double eps) {
        List<Vec2> out = new ArrayList<>();
        for (Vec2 p : pts) {
            if (out.isEmpty() || out.getLast().distanceTo(p) > eps) out.add(p);
        }
        if (out.size() > 1 && out.getFirst().distanceTo(out.getLast()) <= eps) out.removeLast();
        return out;
    }

    /** Ponto dentro do polígono (ray casting). */
    public static boolean contains(List<Vec2> poly, Vec2 p) {
        boolean inside = false;
        for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++) {
            Vec2 a = poly.get(i), b = poly.get(j);
            if ((a.y() > p.y()) != (b.y() > p.y())
                    && p.x() < (b.x() - a.x()) * (p.y() - a.y()) / (b.y() - a.y()) + a.x()) {
                inside = !inside;
            }
        }
        return inside;
    }

    /** Interseção das retas (infinitas) p+t·r e q+u·s. Null se paralelas. */
    public static Vec2 lineIntersection(Vec2 p, Vec2 r, Vec2 q, Vec2 s) {
        double denom = r.cross(s);
        if (Math.abs(denom) < 1e-9) return null;
        double t = q.minus(p).cross(s) / denom;
        return p.plus(r.times(t));
    }
}
