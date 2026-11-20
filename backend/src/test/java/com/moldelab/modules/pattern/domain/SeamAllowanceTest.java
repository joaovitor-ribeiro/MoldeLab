package com.moldelab.modules.pattern.domain;

import com.moldelab.modules.pattern.geometry.Bounds;
import com.moldelab.modules.pattern.geometry.Curves;
import com.moldelab.modules.pattern.geometry.Polygons;
import com.moldelab.modules.pattern.geometry.Vec2;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SeamAllowanceTest {

    private static final SeamSettings S = new SeamSettings(10, 30, 5);

    private static List<Edge> square(EdgeKind left) {
        Vec2 a = Vec2.of(0, 0), b = Vec2.of(100, 0), c = Vec2.of(100, 100), d = Vec2.of(0, 100);
        return List.of(
                Edge.seam("top", Curves.line(a, b)),
                Edge.seam("right", Curves.line(b, c)),
                Edge.hem("bottom", Curves.line(c, d)),
                new Edge("left", left, Curves.line(d, a)));
    }

    @Test
    void rnMol01_margemDiferentePorTipoDeAresta() {
        List<Vec2> cut = SeamAllowance.cutLine(square(EdgeKind.SEAM), S);
        Bounds b = Polygons.bounds(cut);
        assertThat(b.minX()).isCloseTo(-10, within(1e-6));
        assertThat(b.minY()).isCloseTo(-10, within(1e-6));
        assertThat(b.maxX()).isCloseTo(110, within(1e-6));
        assertThat(b.maxY()).isCloseTo(130, within(1e-6)); // barra = 30
        assertThat(Polygons.area(cut)).isCloseTo(120 * 140, within(1e-3));
    }

    @Test
    void rnMol02_dobraNaoRecebeMargem() {
        Bounds b = Polygons.bounds(SeamAllowance.cutLine(square(EdgeKind.FOLD), S));
        assertThat(b.minX()).isCloseTo(0, within(1e-6));
        assertThat(b.maxX()).isCloseTo(110, within(1e-6));
    }

    @Test
    void funcionaComOrientacaoInvertida() {
        Vec2 a = Vec2.of(0, 0), b = Vec2.of(0, 100), c = Vec2.of(100, 100), d = Vec2.of(100, 0);
        List<Edge> ccw = List.of(Edge.seam("1", Curves.line(a, b)), Edge.seam("2", Curves.line(b, c)),
                Edge.seam("3", Curves.line(c, d)), Edge.seam("4", Curves.line(d, a)));
        Bounds bounds = Polygons.bounds(SeamAllowance.cutLine(ccw, S));
        assertThat(bounds.width()).isCloseTo(120, within(1e-6));
        assertThat(bounds.height()).isCloseTo(120, within(1e-6));
    }

    @Test
    void curvaRecebeMargemConstante() {
        // semicírculo de raio 100 + diâmetro
        List<Vec2> arc = Curves.arc(Vec2.ZERO, 100, Math.PI, 2 * Math.PI); // de (-100,0) por cima até (100,0)
        List<Edge> edges = List.of(Edge.seam("arco", arc), Edge.seam("base", Curves.line(arc.getLast(), arc.getFirst())));
        List<Vec2> cut = SeamAllowance.cutLine(edges, S);
        // pontos da parte curva ficam a ~110 do centro
        double maxR = cut.stream().filter(p -> p.y() < -20).mapToDouble(p -> p.distanceTo(Vec2.ZERO)).max().orElseThrow();
        double minR = cut.stream().filter(p -> p.y() < -20).mapToDouble(p -> p.distanceTo(Vec2.ZERO)).min().orElseThrow();
        assertThat(maxR).isCloseTo(110, within(0.5));
        assertThat(minR).isCloseTo(110, within(0.5));
    }
}
