package com.moldelab.modules.pattern.drafting;

import com.moldelab.modules.pattern.domain.Edge;
import com.moldelab.modules.pattern.domain.PatternPiece;
import com.moldelab.modules.pattern.domain.PieceBuilder;
import com.moldelab.modules.pattern.domain.SeamSettings;
import com.moldelab.modules.pattern.geometry.Curves;
import com.moldelab.modules.pattern.geometry.Vec2;

import java.util.List;

/** Cós retangular dobrado ao meio, com transpasse para botão/colchete. */
final class Waistband {

    static final double OVERLAP_MM = 30;
    static final double WAIST_EASE_MM = 10;

    private Waistband() {}

    /**
     * @param waistMm    medida da cintura
     * @param widthMm    largura do cós pronto
     * @param sideNotchFractions frações do comprimento (sem transpasse) onde marcar laterais/centros
     */
    static PatternPiece build(double waistMm, double widthMm, SeamSettings seams, double... sideNotchFractions) {
        double len = waistMm + WAIST_EASE_MM + OVERLAP_MM;
        double h = widthMm * 2;
        Vec2 a = Vec2.of(0, 0), b = Vec2.of(len, 0), c = Vec2.of(len, h), d = Vec2.of(0, h);
        PieceBuilder pb = PieceBuilder.piece("cos", "Cós")
                .cut(1, false)
                .edge(Edge.seam("cós-superior", Curves.line(a, b)))
                .edge(Edge.seam("cós-ponta-direita", Curves.line(b, c)))
                .edge(Edge.seam("cós-inferior", Curves.line(c, d)))
                .edge(Edge.seam("cós-ponta-esquerda", Curves.line(d, a)))
                .internalLine(List.of(Vec2.of(0, widthMm), Vec2.of(len, widthMm)))
                .grainline(Vec2.of(len * 0.35, widthMm * 0.5), Vec2.of(len * 0.65, widthMm * 0.5));
        double base = waistMm + WAIST_EASE_MM;
        for (double f : sideNotchFractions) pb.notch("cós-superior", base * f);
        pb.notch("cós-superior", base); // início do transpasse
        return pb.build(seams);
    }
}
