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

import static com.moldelab.modules.measurements.api.MeasurementKey.HIP;
import static com.moldelab.modules.measurements.api.MeasurementKey.WAIST;
import static com.moldelab.modules.measurements.api.MeasurementKey.WAIST_TO_HIP;

/**
 * Saia reta com pences (base de modelagem), com evasê opcional na barra.
 * Frente: meia peça na dobra. Costas: par com costura central (zíper). Cós retangular.
 *
 * Regras (docs/modelagem/regras.md): RN-MOL-01..06.
 */
public final class StraightSkirtDrafter implements GarmentDrafter {

    static final double FRONT_BACK_BALANCE_MM = 5;   // frente 5 mm maior que costas (por quarto)
    static final double WAIST_EASE_QUARTER_MM = 2.5; // 1 cm de folga total na cintura
    static final double MAX_SIDE_SHAPING_MM = 30;
    static final double MAX_SINGLE_DART_MM = 30;

    @Override public GarmentType type() { return GarmentType.SAIA_RETA; }

    @Override public List<MeasurementKey> requiredMeasurements() { return List.of(WAIST, HIP, WAIST_TO_HIP); }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                ParamSpec.number("length", "Comprimento", "cm", 30, 120, 55, 0.5, "Da cintura até a barra, medido no centro."),
                ParamSpec.number("flare", "Evasê na barra", "cm", 0, 30, 0, 0.5, "Quanto a barra abre em cada lateral de cada peça. 0 = saia reta (lápis)."),
                ParamSpec.number("hipEase", "Folga no quadril", "cm", 0, 12, 4, 0.5, "Folga total somada ao contorno do quadril."),
                ParamSpec.number("waistbandWidth", "Largura do cós", "cm", 0, 8, 4, 0.5, "0 = sem cós."));
    }

    @Override
    public List<PatternPiece> draft(DraftInput in) {
        double waist = in.mm(WAIST), hip = in.mm(HIP), hipDepth = in.mm(WAIST_TO_HIP);
        double length = in.paramMm("length");
        if (length < hipDepth + 50) {
            in.warn("Comprimento menor que a altura do quadril + 5 cm; ajustado para " + Math.round((hipDepth + 50) / 10) + " cm.");
            length = hipDepth + 50;
        }
        double flare = in.paramMm("flare");
        double hipEase = in.paramMm("hipEase");

        List<PatternPiece> pieces = new ArrayList<>();
        pieces.add(half("frente", "Frente da saia", true, waist, hip, hipDepth, length, flare, hipEase, in));
        pieces.add(half("costas", "Costas da saia", false, waist, hip, hipDepth, length, flare, hipEase, in));
        double band = in.paramMm("waistbandWidth");
        if (band > 0) {
            // ordem no cós (começando pelo centro das costas): lateral, centro frente, lateral
            pieces.add(Waistband.build(waist, band, in.seams(), 0.25, 0.5, 0.75));
        }
        return pieces;
    }

    private PatternPiece half(String id, String name, boolean front, double waist, double hip, double hipDepth,
                              double length, double flare, double hipEase, DraftInput in) {
        double balance = front ? FRONT_BACK_BALANCE_MM : -FRONT_BACK_BALANCE_MM;
        double hipQ = (hip + hipEase) / 4 + balance;
        double waistQ = waist / 4 + WAIST_EASE_QUARTER_MM + balance;
        double excess = hipQ - waistQ;

        double sideShaping = excess <= 0 ? excess : Math.min(excess * 0.45, MAX_SIDE_SHAPING_MM);
        double dartTotal = Math.max(0, excess - sideShaping);
        double rise = sideShaping > 5 ? -10 : -5; // a lateral da cintura sobe para acompanhar o quadril

        Vec2 center = Vec2.of(0, 0);
        Vec2 waistSide = Vec2.of(hipQ - sideShaping, rise);
        Vec2 hipPoint = Vec2.of(hipQ, hipDepth);

        List<Vec2> waistLine = Curves.cubic(center, Vec2.of(waistSide.x() * 0.5, 0),
                Vec2.of(waistSide.x() * 0.85, rise * 0.6), waistSide);

        // lateral: curva até o quadril, depois reta até a barra (inclinada pelo evasê)
        List<Vec2> sideCurve = Curves.cubic(waistSide,
                Vec2.of(waistSide.x() + (hipQ - waistSide.x()) * 0.4, rise + (hipDepth - rise) * 0.3),
                Vec2.of(hipQ, hipDepth * 0.55), hipPoint);
        double below = length - hipDepth;
        double angle = Math.atan2(flare, below);
        Vec2 hemSide = Vec2.of(hipQ + below * Math.sin(angle), hipDepth + below * Math.cos(angle));
        List<Vec2> side = new ArrayList<>(sideCurve);
        side.add(hemSide);

        // barra: perpendicular à lateral na ponta, horizontal no centro
        Vec2 hemCenter = Vec2.of(0, length);
        Vec2 sideDir = hemSide.minus(hipPoint).normalized();
        Vec2 inward = Vec2.of(-sideDir.y(), sideDir.x()).times(-1); // perpendicular apontando para o centro
        if (inward.x() > 0) inward = inward.times(-1);
        double hemReach = hemSide.x() * 0.4;
        List<Vec2> hem = Curves.cubic(hemSide, hemSide.plus(inward.times(hemReach)),
                Vec2.of(hemSide.x() * 0.4, length), hemCenter);

        PieceBuilder pb = PieceBuilder.piece(id, name)
                .edge(Edge.seam("cintura", waistLine))
                .edge(Edge.seam("lateral", side))
                .edge(Edge.hem("barra", hem));
        if (front) {
            pb.cut(1, false).edge(Edge.fold("centro-frente", Curves.line(hemCenter, center)));
        } else {
            pb.cut(2, true).edge(Edge.seam("centro-costas", Curves.line(hemCenter, center)))
                    .notchFromEnd("centro-costas", hipDepth + 20); // fim do zíper
        }
        pb.notch("lateral", Curves.length(sideCurve)); // linha do quadril

        addDarts(pb, waistLine, dartTotal, front ? 90 : 130, Math.min(hipDepth - 30, front ? 90 : 140));
        if (flare > 0 && flare > below * 0.5) in.warn("Evasê muito grande para o comprimento: considere a saia godê.");
        return pb.build(in.seams());
    }

    private static void addDarts(PieceBuilder pb, List<Vec2> waistLine, double dartTotal, double nominalLen, double maxLen) {
        if (dartTotal < 4) return;
        double waistLen = Curves.length(waistLine);
        int count = dartTotal > MAX_SINGLE_DART_MM ? 2 : 1;
        double w = dartTotal / count;
        double[] positions = count == 1 ? new double[]{0.5} : new double[]{0.36, 0.66};
        double len = Math.min(nominalLen, maxLen);
        for (double pos : positions) {
            double c = waistLen * pos;
            Vec2 left = Curves.pointAt(waistLine, c - w / 2);
            Vec2 right = Curves.pointAt(waistLine, c + w / 2);
            Vec2 mid = Curves.pointAt(waistLine, c);
            Vec2 apex = Vec2.of(mid.x(), mid.y() + len * (count == 2 && pos > 0.5 ? 0.85 : 1));
            pb.internalLine(List.of(left, apex, right));
        }
    }
}
