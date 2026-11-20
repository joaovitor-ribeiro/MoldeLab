package com.moldelab.modules.pattern.domain;

import com.moldelab.modules.pattern.geometry.Polygons;
import com.moldelab.modules.pattern.geometry.Vec2;

import java.util.ArrayList;
import java.util.List;

/**
 * Gera a linha de corte (contorno externo) a partir das arestas da linha de costura,
 * aplicando uma margem diferente por aresta (costura, barra, dobra=0...).
 *
 * Algoritmo: cada aresta é deslocada para fora (miter por vértice); nos cantos entre arestas,
 * as retas dos segmentos vizinhos são estendidas até se cruzarem. Se o cruzamento ficar longe
 * demais (canto muito agudo), o canto é chanfrado.
 */
public final class SeamAllowance {

    private SeamAllowance() {}

    public static List<Vec2> cutLine(List<Edge> edges, SeamSettings settings) {
        List<Vec2> seamPolygon = seamPolygon(edges);
        double orientation = Math.signum(Polygons.signedArea(seamPolygon));
        if (orientation == 0) throw new IllegalArgumentException("Peça degenerada (área zero)");

        int n = edges.size();
        List<List<Vec2>> offsets = new ArrayList<>(n);
        double maxAllowance = 0;
        for (Edge e : edges) {
            double d = settings.allowanceFor(e.kind());
            maxAllowance = Math.max(maxAllowance, d);
            offsets.add(offsetPolyline(e.points(), d, orientation));
        }

        List<Vec2> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Vec2> cur = offsets.get(i);
            List<Vec2> next = offsets.get((i + 1) % n);
            // corpo da aresta atual (sem o último ponto, que vira canto)
            if (i == 0) out.add(cornerPoint(offsets.get(n - 1), cur, edges.get(i).start(), maxAllowance).getLast());
            for (int k = 1; k < cur.size() - 1; k++) out.add(cur.get(k));
            out.addAll(cornerPoint(cur, next, edges.get(i).end(), maxAllowance));
        }
        // o primeiro canto foi adicionado duas vezes (início e fim) — remover duplicatas
        return Polygons.dedupe(out, 1e-6);
    }

    /** Polígono da linha de costura (pontos de todas as arestas, sem repetir junções). */
    public static List<Vec2> seamPolygon(List<Edge> edges) {
        List<Vec2> pts = new ArrayList<>();
        for (Edge e : edges) {
            List<Vec2> p = e.points();
            for (int k = 0; k < p.size() - 1; k++) pts.add(p.get(k));
        }
        return Polygons.dedupe(pts, 1e-6);
    }

    static List<Vec2> offsetPolyline(List<Vec2> pts, double d, double orientation) {
        List<Vec2> clean = Polygons.dedupe(pts, 1e-6);
        if (clean.size() < 2) clean = List.of(pts.getFirst(), pts.getLast());
        if (d == 0) return clean;
        int m = clean.size();
        List<Vec2> out = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            Vec2 nPrev = i > 0 ? normal(clean.get(i - 1), clean.get(i), orientation) : null;
            Vec2 nNext = i < m - 1 ? normal(clean.get(i), clean.get(i + 1), orientation) : null;
            Vec2 offset;
            if (nPrev == null) offset = nNext.times(d);
            else if (nNext == null) offset = nPrev.times(d);
            else {
                double denom = 1 + nPrev.dot(nNext);
                offset = denom < 0.2 ? nNext.times(d) : nPrev.plus(nNext).times(d / denom);
            }
            out.add(clean.get(i).plus(offset));
        }
        return out;
    }

    private static Vec2 normal(Vec2 a, Vec2 b, double orientation) {
        Vec2 dir = b.minus(a).normalized();
        return dir.perp().times(orientation);
    }

    /** Pontos do canto entre o fim da aresta `a` e o início da `b`. */
    private static List<Vec2> cornerPoint(List<Vec2> a, List<Vec2> b, Vec2 seamCorner, double maxAllowance) {
        Vec2 aEnd = a.getLast(), aPrev = a.get(a.size() - 2);
        Vec2 bStart = b.getFirst(), bNext = b.get(1);
        if (aEnd.distanceTo(bStart) < 1e-6) return List.of(aEnd);
        Vec2 x = Polygons.lineIntersection(aEnd, aEnd.minus(aPrev), bStart, bNext.minus(bStart));
        double limit = Math.max(3 * maxAllowance, 15);
        if (x != null && x.distanceTo(seamCorner) <= limit) return List.of(x);
        return List.of(aEnd, bStart); // chanfro
    }
}
