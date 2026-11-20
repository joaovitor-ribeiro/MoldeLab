package com.moldelab.modules.pattern.drafting;

import com.moldelab.modules.measurements.api.MeasurementKey;
import com.moldelab.modules.pattern.domain.DraftInput;
import com.moldelab.modules.pattern.domain.Edge;
import com.moldelab.modules.pattern.domain.GarmentDrafter;
import com.moldelab.modules.pattern.domain.GarmentType;
import com.moldelab.modules.pattern.domain.ParamSpec;
import com.moldelab.modules.pattern.domain.PatternPiece;
import com.moldelab.modules.pattern.domain.PieceBuilder;
import com.moldelab.modules.pattern.geometry.Curves;
import com.moldelab.modules.pattern.geometry.Vec2;

import java.util.ArrayList;
import java.util.List;

import static com.moldelab.modules.measurements.api.MeasurementKey.WAIST;

/**
 * Saia godê. A circunferência da cintura (com folga) é uma fração do círculo:
 * raio = (cintura + folga) / (2π · fração). Frente e costas iguais, cada uma cortada na dobra;
 * o zíper vai numa lateral.
 */
public final class CircleSkirtDrafter implements GarmentDrafter {

    static final double WAIST_EASE_MM = 10;
    static final double FOLDED_FABRIC_USABLE_MM = 730; // tecido de 1,50 m dobrado, descontando ourela

    @Override public GarmentType type() { return GarmentType.SAIA_GODE; }

    @Override public List<MeasurementKey> requiredMeasurements() { return List.of(WAIST); }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                ParamSpec.choice("fraction", "Rodado", 1.0, "Quanto do círculo a saia ocupa.",
                        new ParamSpec.Option(0.25, "1/4 godê (pouco rodado)"),
                        new ParamSpec.Option(0.5, "Meio godê"),
                        new ParamSpec.Option(0.75, "3/4 godê"),
                        new ParamSpec.Option(1.0, "Godê inteiro")),
                ParamSpec.number("length", "Comprimento", "cm", 25, 120, 55, 0.5, "Da cintura até a barra."),
                ParamSpec.number("waistbandWidth", "Largura do cós", "cm", 0, 8, 4, 0.5, "0 = sem cós."));
    }

    @Override
    public List<PatternPiece> draft(DraftInput in) {
        double waist = in.mm(WAIST);
        double fraction = in.param("fraction");
        double length = in.paramMm("length");
        double r = (waist + WAIST_EASE_MM) / (2 * Math.PI * fraction);
        double big = r + length;
        double halfPanelAngle = Math.PI * fraction / 2; // cada peça (frente/costas) = π·f; metade na dobra

        double down = Math.PI / 2;           // dobra vertical, apontando para baixo
        double sideAngle = down - halfPanelAngle;
        Vec2 o = Vec2.ZERO;

        List<Vec2> waistArc = Curves.arc(o, r, down, sideAngle);
        Vec2 waistSide = waistArc.getLast();
        Vec2 hemSide = Vec2.of(big * Math.cos(sideAngle), big * Math.sin(sideAngle));
        List<Vec2> hemArc = Curves.arc(o, big, sideAngle, down);
        Vec2 hemFold = hemArc.getLast();
        Vec2 waistFold = waistArc.getFirst();

        double sideLen = length;
        PieceBuilder pb = PieceBuilder.piece("painel", "Frente e costas (godê)")
                .cut(2, false)
                .edge(Edge.seam("cintura", waistArc))
                .edge(Edge.seam("lateral", Curves.line(waistSide, hemSide)))
                .edge(Edge.hem("barra", hemArc))
                .edge(Edge.fold("centro", Curves.line(hemFold, waistFold)))
                .notch("lateral", Math.min(200, sideLen * 0.4)); // referência para o fim do zíper
        double grainX = Math.max(20, big * Math.cos(sideAngle) * 0.25);
        pb.grainline(Vec2.of(grainX, r + length * 0.2), Vec2.of(grainX, r + length * 0.8));

        double pieceWidth = hemSide.x() > 0 ? Math.max(hemSide.x(), 0) : 0;
        double widthNeeded = Math.max(pieceWidth, big * Math.sin(Math.min(halfPanelAngle, Math.PI / 2)));
        if (widthNeeded > FOLDED_FABRIC_USABLE_MM) {
            in.warn("A peça tem " + Math.round(widthNeeded / 10) + " cm de largura e não cabe em tecido de 1,50 m dobrado. "
                    + "Use tecido mais largo, diminua o comprimento ou o rodado, ou corte em mais painéis.");
        }
        if (in.seams().hemMm() > 15) {
            in.warn("Barras curvas (godê) ficam melhores com margem de barra de até 1 cm (bainha estreita).");
        }

        List<PatternPiece> pieces = new ArrayList<>();
        pieces.add(pb.build(in.seams()));
        double band = in.paramMm("waistbandWidth");
        if (band > 0) pieces.add(Waistband.build(waist, band, in.seams(), 0.5));
        return pieces;
    }
}
