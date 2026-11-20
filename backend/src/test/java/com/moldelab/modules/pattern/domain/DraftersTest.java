package com.moldelab.modules.pattern.domain;

import com.moldelab.modules.measurements.api.MeasurementKey;
import com.moldelab.modules.measurements.api.Measurements;
import com.moldelab.modules.measurements.api.StandardSizes;
import com.moldelab.modules.pattern.geometry.Polygons;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class DraftersTest {

    private final PatternEngine engine = new PatternEngine(new GarmentCatalog());

    static Measurements size(String id) {
        return StandardSizes.all().stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow().measurements();
    }

    static Stream<Arguments> allSizesAllGarments() {
        List<Arguments> args = new ArrayList<>();
        for (var s : StandardSizes.all()) for (GarmentType t : GarmentType.values()) args.add(Arguments.of(s.id(), t));
        return args.stream();
    }

    @ParameterizedTest(name = "{1} tamanho {0}")
    @MethodSource("allSizesAllGarments")
    void todaPecaEhGeometricamenteValida(String sizeId, GarmentType type) {
        Pattern p = engine.draft(type, size(sizeId), Map.of(), SeamSettings.DEFAULT, 1500);
        assertThat(p.pieces()).isNotEmpty();
        for (PatternPiece piece : p.pieces()) {
            double seamArea = Polygons.area(piece.seamLine());
            double cutArea = Polygons.area(piece.cutLine());
            assertThat(seamArea).as(piece.name() + " área").isGreaterThan(1000);
            assertThat(cutArea).as(piece.name() + " corte > costura").isGreaterThan(seamArea);
            assertThat(Polygons.contains(piece.seamLine(), piece.labelAnchor())).as(piece.name() + " rótulo dentro").isTrue();
            assertThat(piece.code()).matches("[A-Z]");
            assertThat(Polygons.contains(piece.seamLine(), piece.grainline().from())).as(piece.name() + " fio dentro (início)").isTrue();
            assertThat(Polygons.contains(piece.seamLine(), piece.grainline().to())).as(piece.name() + " fio dentro (fim)").isTrue();
        }
        assertThat(p.fabric()).allSatisfy(f -> assertThat(f.lengthMm()).isPositive());
    }

    @Test
    void rnMol03_saiaRetaLarguraNoQuadrilSegueMedida() {
        Measurements m = size("F40");
        Pattern p = engine.draft(GarmentType.SAIA_RETA, m, Map.of("flare", 0.0, "hipEase", 4.0), SeamSettings.DEFAULT, 1500);
        PatternPiece front = p.pieces().get(0), back = p.pieces().get(1);
        double expectedFront = (m.hip() * 10 + 40) / 4 + 5;
        double expectedBack = (m.hip() * 10 + 40) / 4 - 5;
        assertThat(Polygons.bounds(front.seamLine()).maxX()).isCloseTo(expectedFront, within(0.5));
        assertThat(Polygons.bounds(back.seamLine()).maxX()).isCloseTo(expectedBack, within(0.5));
        // frente na dobra, costas em par
        assertThat(front.cutOnFold()).isTrue();
        assertThat(back.cutQuantity()).isEqualTo(2);
        // cintura (sem pences) = cintura/4 + folga ± equilíbrio
        double waistEdge = front.edges().stream().filter(e -> e.name().equals("cintura")).findFirst().orElseThrow().lengthMm();
        double dartsAndWaist = (m.waist() * 10) / 4 + 2.5 + 5;
        assertThat(waistEdge).isGreaterThan(dartsAndWaist); // inclui a abertura das pences
        assertThat(front.internalLines()).isNotEmpty();
    }

    @Test
    void rnMol04_godeCinturaFechaNaMedida() {
        Measurements m = size("F38");
        for (double f : new double[]{0.25, 0.5, 0.75, 1.0}) {
            Pattern p = engine.draft(GarmentType.SAIA_GODE, m, Map.of("fraction", f), SeamSettings.DEFAULT, 1500);
            PatternPiece panel = p.pieces().get(0);
            double halfWaist = panel.edges().stream().filter(e -> e.name().equals("cintura")).findFirst().orElseThrow().lengthMm();
            // meia peça na dobra × 2 (dobra) × 2 peças (frente/costas)
            assertThat(halfWaist * 4).as("fração " + f).isCloseTo(m.waist() * 10 + 10, within(2.0));
        }
    }

    @Test
    void rnMol05_cabecaDaMangaTemComprimentoDasCavas() {
        Pattern p = engine.draft(GarmentType.CAMISETA, size("F42"), Map.of(), SeamSettings.DEFAULT, 1500);
        double cavas = p.pieces().stream().filter(pc -> pc.id().equals("frente") || pc.id().equals("costas"))
                .flatMap(pc -> pc.edges().stream()).filter(e -> e.name().equals("cava")).mapToDouble(PatternPiece.EdgeInfo::lengthMm).sum();
        double cap = p.pieces().stream().filter(pc -> pc.id().equals("manga")).findFirst().orElseThrow()
                .edges().stream().filter(e -> e.name().equals("cabeça")).findFirst().orElseThrow().lengthMm();
        assertThat(cap).isCloseTo(cavas + 5, within(1.0));
    }

    @Test
    void rnMol06_ribanaTem85PorCentoDoDecote() {
        Pattern p = engine.draft(GarmentType.CAMISETA, size("MM"), Map.of(), SeamSettings.DEFAULT, 1500);
        double decote = 2 * p.pieces().stream().filter(pc -> pc.id().equals("frente") || pc.id().equals("costas"))
                .flatMap(pc -> pc.edges().stream()).filter(e -> e.name().equals("decote")).mapToDouble(PatternPiece.EdgeInfo::lengthMm).sum();
        PatternPiece rib = p.pieces().stream().filter(pc -> pc.id().equals("ribana")).findFirst().orElseThrow();
        double ribLen = rib.edges().stream().filter(e -> e.name().equals("ribana-borda")).findFirst().orElseThrow().lengthMm();
        assertThat(ribLen).isCloseTo(decote * 0.85, within(1.0));
        assertThat(rib.material()).isEqualTo("Ribana");
    }

    @Test
    void regataNaoGeraManga() {
        Pattern p = engine.draft(GarmentType.CAMISETA, size("F38"), Map.of("sleeveLength", 0.0), SeamSettings.DEFAULT, 1500);
        assertThat(p.pieces()).noneMatch(pc -> pc.id().equals("manga"));
    }

    @Test
    void rnMol07_medidasAusentesSaoListadas() {
        Measurements onlyWaist = Measurements.fromMap(Map.of(MeasurementKey.WAIST, 70.0));
        assertThatThrownBy(() -> engine.draft(GarmentType.SAIA_RETA, onlyWaist, Map.of(), SeamSettings.DEFAULT, 1500))
                .isInstanceOf(PatternEngine.InvalidMeasurementsException.class)
                .satisfies(e -> assertThat(((PatternEngine.InvalidMeasurementsException) e).missing())
                        .containsExactly(MeasurementKey.HIP, MeasurementKey.WAIST_TO_HIP));
    }

    @Test
    void rnMol08_medidaImplausivelEhRejeitada() {
        Measurements m = Measurements.fromMap(Map.of(MeasurementKey.WAIST, 7.0)); // 7 cm: digitou errado
        assertThatThrownBy(() -> engine.draft(GarmentType.SAIA_GODE, m, Map.of(), SeamSettings.DEFAULT, 1500))
                .isInstanceOf(PatternEngine.InvalidMeasurementsException.class)
                .satisfies(e -> assertThat(((PatternEngine.InvalidMeasurementsException) e).outOfRange()).containsExactly(MeasurementKey.WAIST));
    }

    @Test
    void rnMol09_parametroForaDoLimiteEhAjustadoComAviso() {
        Pattern p = engine.draft(GarmentType.SAIA_RETA, size("F38"), Map.of("length", 500.0, "inventado", 1.0), SeamSettings.DEFAULT, 1500);
        assertThat(p.params().get("length")).isEqualTo(120.0);
        assertThat(p.warnings()).anyMatch(w -> w.contains("Comprimento")).anyMatch(w -> w.contains("inventado"));
    }

    @Test
    void draftEhDeterministico() {
        Pattern a = engine.draft(GarmentType.CAMISETA, size("F40"), Map.of(), SeamSettings.DEFAULT, 1500);
        Pattern b = engine.draft(GarmentType.CAMISETA, size("F40"), Map.of(), SeamSettings.DEFAULT, 1500);
        assertThat(a).isEqualTo(b);
    }
}
