package com.moldelab.modules.pattern.domain;

import com.moldelab.modules.pattern.geometry.Bounds;
import com.moldelab.modules.pattern.geometry.Curves;
import com.moldelab.modules.pattern.geometry.Polygons;
import com.moldelab.modules.pattern.geometry.Vec2;

import java.util.ArrayList;
import java.util.List;

/** Monta uma {@link PatternPiece} a partir das arestas da linha de costura. */
public final class PieceBuilder {

    private final String id;
    private final String name;
    private String code = "?";
    private String material = "Tecido principal";
    private int cutQuantity = 1;
    private boolean mirrored = false;
    private final List<Edge> edges = new ArrayList<>();
    private final List<NotchSpec> notchSpecs = new ArrayList<>();
    private final List<List<Vec2>> internalLines = new ArrayList<>();
    private PatternPiece.Segment grainline;

    private record NotchSpec(String edgeName, double distanceMm, boolean fromEnd) {}

    private PieceBuilder(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static PieceBuilder piece(String id, String name) {
        return new PieceBuilder(id, name);
    }

    public PieceBuilder code(String code) { this.code = code; return this; }
    public PieceBuilder material(String material) { this.material = material; return this; }
    public PieceBuilder cut(int quantity, boolean mirrored) { this.cutQuantity = quantity; this.mirrored = mirrored; return this; }
    public PieceBuilder edge(Edge e) { edges.add(e); return this; }
    public PieceBuilder internalLine(List<Vec2> line) { internalLines.add(List.copyOf(line)); return this; }
    public PieceBuilder grainline(Vec2 from, Vec2 to) { this.grainline = new PatternPiece.Segment(from, to); return this; }

    /** Pique na aresta `edgeName`, a `distanceMm` do início da aresta. */
    public PieceBuilder notch(String edgeName, double distanceMm) {
        notchSpecs.add(new NotchSpec(edgeName, distanceMm, false));
        return this;
    }

    /** Pique na aresta `edgeName`, a `distanceMm` do FIM da aresta. */
    public PieceBuilder notchFromEnd(String edgeName, double distanceMm) {
        notchSpecs.add(new NotchSpec(edgeName, distanceMm, true));
        return this;
    }

    public PatternPiece build(SeamSettings settings) {
        validateContinuity();
        List<Vec2> seam = SeamAllowance.seamPolygon(edges);
        List<Vec2> cut = SeamAllowance.cutLine(edges, settings);
        double orientation = Math.signum(Polygons.signedArea(seam));

        List<List<Vec2>> folds = edges.stream().filter(e -> e.kind() == EdgeKind.FOLD).map(Edge::points).toList();
        boolean onFold = !folds.isEmpty();

        List<PatternPiece.Segment> notches = new ArrayList<>();
        for (NotchSpec ns : notchSpecs) {
            Edge e = edges.stream().filter(x -> x.name().equals(ns.edgeName())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Aresta inexistente para pique: " + ns.edgeName()));
            List<Vec2> pts = ns.fromEnd() ? Curves.reversed(e.points()) : e.points();
            double d = Math.min(ns.distanceMm(), Curves.length(pts));
            Vec2 at = Curves.pointAt(pts, d);
            Vec2 tangent = Curves.tangentAt(pts, d);
            if (ns.fromEnd()) tangent = tangent.times(-1);
            Vec2 outward = tangent.perp().times(orientation);
            double allowance = settings.allowanceFor(e.kind());
            Vec2 onCut = at.plus(outward.times(allowance));
            notches.add(new PatternPiece.Segment(onCut, onCut.minus(outward.times(Math.min(6, allowance + 4)))));
        }

        Bounds b = Polygons.bounds(cut);
        PatternPiece.Segment grain = grainline != null ? grainline : defaultGrainline(seam, b);

        List<PatternPiece.EdgeInfo> infos = edges.stream()
                .map(e -> new PatternPiece.EdgeInfo(e.name(), e.kind(), round1(e.length()))).toList();

        return new PatternPiece(id, code, name, material, cutQuantity, onFold, mirrored,
                seam, cut, folds, grain, notches, List.copyOf(internalLines), infos, labelPole(seam), b);
    }

    private void validateContinuity() {
        if (edges.size() < 3) throw new IllegalStateException("Peça " + id + " precisa de >= 3 arestas");
        for (int i = 0; i < edges.size(); i++) {
            Edge a = edges.get(i), b = edges.get((i + 1) % edges.size());
            if (a.end().distanceTo(b.start()) > 0.01) {
                throw new IllegalStateException("Peça " + id + ": aresta '" + a.name() + "' não encontra '" + b.name()
                        + "' (" + a.end() + " vs " + b.start() + ")");
            }
        }
    }

    /** Fio vertical passando pelo ponto do rótulo, ocupando 60% do trecho interno da peça nessa vertical. */
    private static PatternPiece.Segment defaultGrainline(List<Vec2> seam, Bounds b) {
        Vec2 pole = labelPole(seam);
        // desloca o fio para o lado do rótulo (sem sair da peça) para não passar por cima do texto
        Vec2 c = pole;
        for (double dx : new double[]{-Math.min(45, b.width() * 0.22), Math.min(45, b.width() * 0.22)}) {
            Vec2 shifted = new Vec2(pole.x() + dx, pole.y());
            if (Polygons.contains(seam, shifted)) { c = shifted; break; }
        }
        double step = Math.max(1, b.height() / 400);
        double top = c.y(), bottom = c.y();
        while (top - step > b.minY() && Polygons.contains(seam, new Vec2(c.x(), top - step))) top -= step;
        while (bottom + step < b.maxY() && Polygons.contains(seam, new Vec2(c.x(), bottom + step))) bottom += step;
        double mid = (top + bottom) / 2, half = (bottom - top) * 0.3;
        return new PatternPiece.Segment(new Vec2(c.x(), mid - half), new Vec2(c.x(), mid + half));
    }

    /** Ponto interno mais distante das bordas (busca em grade) — bom para rótulos. */
    static Vec2 labelPole(List<Vec2> poly) {
        Bounds b = Polygons.bounds(poly);
        Vec2 best = new Vec2((b.minX() + b.maxX()) / 2, (b.minY() + b.maxY()) / 2);
        double bestDist = -1;
        int steps = 24;
        for (int i = 1; i < steps; i++) {
            for (int j = 1; j < steps; j++) {
                Vec2 p = new Vec2(b.minX() + b.width() * i / steps, b.minY() + b.height() * j / steps);
                if (!Polygons.contains(poly, p)) continue;
                double d = distanceToPolygon(poly, p);
                if (d > bestDist) { bestDist = d; best = p; }
            }
        }
        return best;
    }

    private static double distanceToPolygon(List<Vec2> poly, Vec2 p) {
        double min = Double.POSITIVE_INFINITY;
        for (int i = 0; i < poly.size(); i++) {
            Vec2 a = poly.get(i), c = poly.get((i + 1) % poly.size());
            Vec2 ac = c.minus(a);
            double len2 = ac.dot(ac);
            double t = len2 == 0 ? 0 : Math.max(0, Math.min(1, p.minus(a).dot(ac) / len2));
            min = Math.min(min, p.distanceTo(a.plus(ac.times(t))));
        }
        return min;
    }

    private static double round1(double v) { return Math.round(v * 10) / 10.0; }
}
