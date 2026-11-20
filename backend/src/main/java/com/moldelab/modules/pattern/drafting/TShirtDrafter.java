package com.moldelab.modules.pattern.drafting;

import com.moldelab.modules.measurements.api.MeasurementKey;
import com.moldelab.modules.pattern.domain.DraftInput;
import com.moldelab.modules.pattern.domain.Edge;
import com.moldelab.modules.pattern.domain.EdgeKind;
import com.moldelab.modules.pattern.domain.GarmentDrafter;
import com.moldelab.modules.pattern.domain.GarmentType;
import com.moldelab.modules.pattern.domain.ParamSpec;
import com.moldelab.modules.pattern.domain.PatternPiece;
import com.moldelab.modules.pattern.domain.PieceBuilder;
import com.moldelab.modules.pattern.geometry.Curves;
import com.moldelab.modules.pattern.geometry.Vec2;

import java.util.ArrayList;
import java.util.List;

import static com.moldelab.modules.measurements.api.MeasurementKey.*;

/**
 * Camiseta básica de malha (vira blusa ou vestido-camiseta pelo comprimento).
 * Frente e costas: meias peças na dobra. Manga: cabeça calculada para ter o mesmo
 * comprimento das cavas (busca binária na altura da cabeça). Ribana do decote: 85% do decote.
 *
 * Origem: ponto alto do ombro (encontro ombro/pescoço) na altura y=0, centro (dobra) em x=0.
 */
public final class TShirtDrafter implements GarmentDrafter {

    static final double SHOULDER_DROP_MM = 45;
    static final double BACK_NECK_DEPTH_MM = 20;
    static final double SLEEVE_BICEP_EASE_MM = 40;
    static final double SLEEVE_CAP_EASE_MM = 5;
    static final double RIB_RATIO = 0.85;
    static final double RIB_WIDTH_MM = 20;

    @Override public GarmentType type() { return GarmentType.CAMISETA; }

    @Override
    public List<MeasurementKey> requiredMeasurements() {
        return List.of(BUST, WAIST, HIP, NECK, SHOULDER_WIDTH, BACK_WAIST_LENGTH, ARM_LENGTH, UPPER_ARM, WRIST, WAIST_TO_HIP);
    }

    @Override
    public List<ParamSpec> params() {
        return List.of(
                ParamSpec.number("ease", "Folga no busto", "cm", 0, 24, 6, 0.5, "Folga total no contorno. Malha justa: 0–4; básica: 6–10; ampla: 14+."),
                ParamSpec.number("lengthBelowWaist", "Comprimento abaixo da cintura", "cm", -8, 90, 20, 0.5, "Negativo = cropped. 45+ vira vestido."),
                ParamSpec.number("sleeveLength", "Comprimento da manga", "cm", 0, 80, 20, 0.5, "Medido do alto do ombro. 0 = regata (sem manga)."),
                ParamSpec.number("frontNeckDepth", "Profundidade do decote", "cm", 4, 25, 8, 0.5, "Do ponto alto do ombro até o fundo do decote da frente."),
                ParamSpec.number("neckWidening", "Alargar decote", "cm", 0, 8, 1, 0.5, "Quanto o decote abre em direção ao ombro."),
                ParamSpec.number("waistShaping", "Acinturado", "%", 0, 100, 20, 5, "0 = reta; 100 = segue a cintura."),
                ParamSpec.number("hemFlare", "Evasê na barra", "cm", 0, 30, 0, 0.5, "Abertura extra na lateral da barra (bom para vestido)."));
    }

    @Override
    public List<PatternPiece> draft(DraftInput in) {
        Body b = new Body(in);
        boolean sleeveless = in.param("sleeveLength") < 1;

        Half front = half(b, in, true, sleeveless);
        Half back = half(b, in, false, sleeveless);

        List<PatternPiece> pieces = new ArrayList<>();
        pieces.add(front.piece);
        pieces.add(back.piece);
        if (!sleeveless) pieces.add(sleeve(in, front.armholeLength + back.armholeLength, b));
        pieces.add(rib(in, 2 * (front.neckLength + back.neckLength)));
        return pieces;
    }

    private record Half(PatternPiece piece, double armholeLength, double neckLength) {}

    /** Medidas derivadas comuns a frente e costas (mm). */
    private static final class Body {
        final double bustQ, waistX, hipX, ease, neckW, shoulderX, armholeDepth, waistY, hipY, hemY, hemFlare;

        Body(DraftInput in) {
            ease = in.paramMm("ease");
            double bust = in.mm(BUST);
            bustQ = (bust + ease) / 4;
            double shaping = in.param("waistShaping") / 100.0;
            double waistTarget = (in.mm(WAIST) + ease) / 4;
            waistX = bustQ + shaping * (waistTarget - bustQ);
            double hipTarget = (in.mm(HIP) + ease) / 4;
            hipX = Math.max(hipTarget, waistX);
            neckW = in.mm(NECK) / 6 + 5 + in.paramMm("neckWidening");
            shoulderX = Math.max(in.mm(SHOULDER_WIDTH) / 2, neckW + 60);
            armholeDepth = bust / 6 + 45 + ease * 0.25;
            waistY = in.mm(BACK_WAIST_LENGTH);
            hipY = waistY + in.mm(WAIST_TO_HIP);
            double hem = waistY + in.paramMm("lengthBelowWaist");
            if (hem < armholeDepth + 80) {
                in.warn("Comprimento muito curto; a barra foi ajustada para 8 cm abaixo da cava.");
                hem = armholeDepth + 80;
            }
            hemY = hem;
            hemFlare = in.paramMm("hemFlare");
            if (in.mm(SHOULDER_WIDTH) / 2 < neckW + 60) {
                in.warn("Largura dos ombros parece pequena para o pescoço; ombro ajustado para 6 cm.");
            }
        }
    }

    private Half half(Body b, DraftInput in, boolean front, boolean sleeveless) {
        double neckDepth = front ? Math.max(in.paramMm("frontNeckDepth"), BACK_NECK_DEPTH_MM + 10) : BACK_NECK_DEPTH_MM;
        double sx = front ? b.shoulderX - 5 : b.shoulderX;

        Vec2 neckPoint = Vec2.of(b.neckW, 0);
        Vec2 shoulderTip = Vec2.of(sx, SHOULDER_DROP_MM);
        Vec2 underarm = Vec2.of(b.bustQ, b.armholeDepth);

        double scoop = front ? 25 : 10;
        double drop = b.armholeDepth - SHOULDER_DROP_MM;
        List<Vec2> armhole = Curves.cubic(shoulderTip,
                Vec2.of(sx - scoop * 0.4, SHOULDER_DROP_MM + drop * 0.5),
                Vec2.of(sx - scoop + (b.bustQ - sx + scoop) * 0.25, b.armholeDepth),
                underarm);

        // lateral passando por cintura e quadril (se a barra estiver abaixo deles)
        List<Vec2> sidePts = new ArrayList<>();
        sidePts.add(underarm);
        if (b.hemY > b.waistY + 30) sidePts.add(Vec2.of(b.waistX, b.waistY));
        double hemX;
        if (b.hemY > b.hipY + 30) {
            sidePts.add(Vec2.of(b.hipX, b.hipY));
            hemX = b.hipX + b.hemFlare;
        } else if (b.hemY > b.waistY) {
            double t = (b.hemY - b.waistY) / (b.hipY - b.waistY);
            hemX = b.waistX + (b.hipX - b.waistX) * t + b.hemFlare;
        } else {
            double t = (b.hemY - b.armholeDepth) / (b.waistY - b.armholeDepth);
            hemX = b.bustQ + (b.waistX - b.bustQ) * t + b.hemFlare;
        }
        Vec2 hemSide = Vec2.of(hemX, b.hemY);
        sidePts.add(hemSide);
        List<Vec2> side = Curves.smoothThrough(sidePts);

        Vec2 hemCenter = Vec2.of(0, b.hemY);
        Vec2 neckCenter = Vec2.of(0, neckDepth);
        List<Vec2> neckline = Curves.cubic(neckCenter, Vec2.of(b.neckW * 0.55, neckDepth),
                Vec2.of(b.neckW, neckDepth * 0.45), neckPoint);

        String prefix = front ? "frente" : "costas";
        PieceBuilder pb = PieceBuilder.piece(prefix, front ? "Frente" : "Costas")
                .cut(1, false)
                .edge(Edge.seam("ombro", Curves.line(neckPoint, shoulderTip)))
                .edge(new Edge("cava", sleeveless ? EdgeKind.FINISH : EdgeKind.SEAM, armhole))
                .edge(Edge.seam("lateral", side))
                .edge(Edge.hem("barra", Curves.line(hemSide, hemCenter)))
                .edge(Edge.fold(front ? "centro-frente" : "centro-costas", Curves.line(hemCenter, neckCenter)))
                .edge(Edge.finish("decote", neckline));
        double armLen = Curves.length(armhole);
        if (!sleeveless) {
            if (front) pb.notch("cava", armLen * 0.55);
            else pb.notch("cava", armLen * 0.55).notch("cava", armLen * 0.55 + 8);
        }
        if (b.hemY > b.waistY + 30) pb.notch("lateral", Curves.length(Curves.smoothThrough(sidePts.subList(0, 2))));
        return new Half(pb.build(in.seams()), armLen, Curves.length(neckline));
    }

    private PatternPiece sleeve(DraftInput in, double armholeTotal, Body b) {
        double bicep = in.mm(UPPER_ARM) + SLEEVE_BICEP_EASE_MM + b.ease * 0.25;
        double target = armholeTotal + SLEEVE_CAP_EASE_MM;
        double hw = bicep / 2;
        if (capLength(hw, 1) >= target) {
            hw = target * 0.45;
            in.warn("Contorno do braço grande para a cava: a largura da manga foi reduzida. Considere aumentar a folga.");
        }
        double lo = 1, hi = 400;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2;
            if (capLength(hw, mid) < target) lo = mid; else hi = mid;
        }
        double capH = (lo + hi) / 2;

        double armLen = in.mm(ARM_LENGTH);
        double sl = in.paramMm("sleeveLength");
        if (sl < capH + 30) {
            in.warn("Manga muito curta para a cabeça calculada; ajustada para " + Math.round((capH + 30) / 10) + " cm.");
            sl = capH + 30;
        }
        double wristW = in.mm(WRIST) + 50;
        double t = Math.max(0, Math.min(1, (sl - capH) / Math.max(1, armLen - capH)));
        double hemHalf = (hw * 2 + (wristW - hw * 2) * t) / 2;

        List<Vec2> cap = capCurve(hw, capH);
        Vec2 rightUnder = Vec2.of(hw, capH), leftUnder = Vec2.of(-hw, capH);
        Vec2 rightHem = Vec2.of(hemHalf, sl), leftHem = Vec2.of(-hemHalf, sl);
        double halfCap = Curves.length(cap) / 2;
        return PieceBuilder.piece("manga", "Manga")
                .cut(2, true)
                .edge(Edge.seam("cabeça", cap))
                .edge(Edge.seam("costura-manga-frente", Curves.line(rightUnder, rightHem)))
                .edge(Edge.hem("barra-manga", Curves.line(rightHem, leftHem)))
                .edge(Edge.seam("costura-manga-costas", Curves.line(leftHem, leftUnder)))
                .notch("cabeça", halfCap)               // encontra a costura do ombro
                .notch("cabeça", halfCap * 1.55)        // frente: 1 pique
                .notch("cabeça", halfCap * 0.45)        // costas: 2 piques
                .notch("cabeça", halfCap * 0.45 - 8)
                .grainline(Vec2.of(0, capH + 15), Vec2.of(0, Math.max(capH + 40, sl - 15)))
                .build(in.seams());
    }

    static List<Vec2> capCurve(double hw, double h) {
        List<Vec2> left = Curves.cubic(Vec2.of(-hw, h), Vec2.of(-hw + hw * 0.4, h), Vec2.of(-hw * 0.4, 0), Vec2.of(0, 0));
        List<Vec2> right = Curves.cubic(Vec2.of(0, 0), Vec2.of(hw * 0.4, 0), Vec2.of(hw - hw * 0.4, h), Vec2.of(hw, h));
        List<Vec2> all = new ArrayList<>(left);
        all.addAll(right.subList(1, right.size()));
        return all;
    }

    static double capLength(double hw, double h) {
        return Curves.length(capCurve(hw, h));
    }

    private PatternPiece rib(DraftInput in, double neckTotal) {
        double len = neckTotal * RIB_RATIO;
        double h = RIB_WIDTH_MM * 2;
        Vec2 a = Vec2.of(0, 0), b = Vec2.of(len, 0), c = Vec2.of(len, h), d = Vec2.of(0, h);
        return PieceBuilder.piece("ribana", "Ribana do decote")
                .material("Ribana")
                .cut(1, false)
                .edge(Edge.finish("ribana-borda", Curves.line(a, b)))
                .edge(Edge.seam("ribana-emenda-direita", Curves.line(b, c)))
                .edge(Edge.finish("ribana-borda-dobrada", Curves.line(c, d)))
                .edge(Edge.seam("ribana-emenda-esquerda", Curves.line(d, a)))
                .internalLine(List.of(Vec2.of(0, RIB_WIDTH_MM), Vec2.of(len, RIB_WIDTH_MM)))
                .grainline(Vec2.of(len * 0.5 - 10, h * 0.25), Vec2.of(len * 0.5 - 10, h * 0.75))
                .notch("ribana-borda", len / 2)
                .build(in.seams());
    }
}
