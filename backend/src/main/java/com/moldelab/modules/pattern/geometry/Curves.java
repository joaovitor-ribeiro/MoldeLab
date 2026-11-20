package com.moldelab.modules.pattern.geometry;

import java.util.ArrayList;
import java.util.List;

/** Geradores de polilinhas (curvas já "achatadas" em pontos densos, em mm). */
public final class Curves {

    private Curves() {}

    public static List<Vec2> line(Vec2 a, Vec2 b) {
        return List.of(a, b);
    }

    public static List<Vec2> cubic(Vec2 p0, Vec2 c1, Vec2 c2, Vec2 p3) {
        return cubic(p0, c1, c2, p3, 32);
    }

    public static List<Vec2> cubic(Vec2 p0, Vec2 c1, Vec2 c2, Vec2 p3, int segments) {
        List<Vec2> pts = new ArrayList<>(segments + 1);
        for (int i = 0; i <= segments; i++) {
            double t = (double) i / segments, u = 1 - t;
            double a = u * u * u, b = 3 * u * u * t, c = 3 * u * t * t, d = t * t * t;
            pts.add(new Vec2(
                    a * p0.x() + b * c1.x() + c * c2.x() + d * p3.x(),
                    a * p0.y() + b * c1.y() + c * c2.y() + d * p3.y()));
        }
        return pts;
    }

    /** Arco de círculo de a0 até a1 (radianos). */
    public static List<Vec2> arc(Vec2 center, double radius, double a0, double a1) {
        int segments = Math.max(8, (int) Math.ceil(Math.abs(a1 - a0) * radius / 4.0)); // ~4 mm por segmento
        List<Vec2> pts = new ArrayList<>(segments + 1);
        for (int i = 0; i <= segments; i++) {
            double a = a0 + (a1 - a0) * i / segments;
            pts.add(new Vec2(center.x() + radius * Math.cos(a), center.y() + radius * Math.sin(a)));
        }
        return pts;
    }

    /** Curva suave (Catmull-Rom → Bézier) passando por todos os pontos. */
    public static List<Vec2> smoothThrough(List<Vec2> points) {
        if (points.size() < 3) return List.copyOf(points);
        List<Vec2> out = new ArrayList<>();
        for (int i = 0; i < points.size() - 1; i++) {
            Vec2 p0 = i == 0 ? points.get(0) : points.get(i - 1);
            Vec2 p1 = points.get(i);
            Vec2 p2 = points.get(i + 1);
            Vec2 p3 = i + 2 < points.size() ? points.get(i + 2) : p2;
            Vec2 c1 = p1.plus(p2.minus(p0).times(1.0 / 6));
            Vec2 c2 = p2.minus(p3.minus(p1).times(1.0 / 6));
            List<Vec2> seg = cubic(p1, c1, c2, p2, 24);
            out.addAll(i == 0 ? seg : seg.subList(1, seg.size()));
        }
        return out;
    }

    public static double length(List<Vec2> polyline) {
        double len = 0;
        for (int i = 1; i < polyline.size(); i++) len += polyline.get(i - 1).distanceTo(polyline.get(i));
        return len;
    }

    /** Ponto a uma distância (mm) ao longo da polilinha, a partir do início. */
    public static Vec2 pointAt(List<Vec2> polyline, double distance) {
        double acc = 0;
        for (int i = 1; i < polyline.size(); i++) {
            Vec2 a = polyline.get(i - 1), b = polyline.get(i);
            double seg = a.distanceTo(b);
            if (acc + seg >= distance && seg > 0) return a.lerp(b, (distance - acc) / seg);
            acc += seg;
        }
        return polyline.getLast();
    }

    /** Direção tangente (unitária) na distância informada. */
    public static Vec2 tangentAt(List<Vec2> polyline, double distance) {
        double acc = 0;
        for (int i = 1; i < polyline.size(); i++) {
            Vec2 a = polyline.get(i - 1), b = polyline.get(i);
            double seg = a.distanceTo(b);
            if (acc + seg >= distance && seg > 0) return b.minus(a).normalized();
            acc += seg;
        }
        int n = polyline.size();
        return polyline.get(n - 1).minus(polyline.get(n - 2)).normalized();
    }

    public static List<Vec2> reversed(List<Vec2> pts) {
        return new ArrayList<>(pts).reversed();
    }

    public static List<Vec2> mirrorX(List<Vec2> pts) {
        return pts.stream().map(Vec2::mirrorX).toList();
    }
}
