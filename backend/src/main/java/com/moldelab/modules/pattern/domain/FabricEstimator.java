package com.moldelab.modules.pattern.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estimativa de consumo de tecido por material (encaixe em prateleiras, sem rotação — respeita o fio).
 * Supõe tecido dobrado ao meio no comprimento (ourela com ourela): cada corte gera 2 camadas.
 * É uma ESTIMATIVA conservadora (+10%), não um encaixe otimizado.
 */
public final class FabricEstimator {

    public record FabricEstimate(String material, double fabricWidthMm, double lengthMm) {}

    static final double GAP_MM = 10;
    static final double SELVEDGE_MM = 20;

    private FabricEstimator() {}

    public static List<FabricEstimate> estimate(List<PatternPiece> pieces, double fabricWidthMm) {
        Map<String, List<PatternPiece>> byMaterial = new LinkedHashMap<>();
        for (PatternPiece p : pieces) byMaterial.computeIfAbsent(p.material(), k -> new ArrayList<>()).add(p);

        List<FabricEstimate> out = new ArrayList<>();
        byMaterial.forEach((material, list) -> out.add(new FabricEstimate(material, fabricWidthMm, lengthFor(list, fabricWidthMm))));
        return out;
    }

    private static double lengthFor(List<PatternPiece> pieces, double fabricWidthMm) {
        double usableFolded = fabricWidthMm / 2 - SELVEDGE_MM;
        double extra = 0;
        List<double[]> boxes = new ArrayList<>(); // [w, h]
        for (PatternPiece p : pieces) {
            int placements = p.cutOnFold() ? p.cutQuantity() : (int) Math.ceil(p.cutQuantity() / 2.0);
            double w = p.bounds().width() + GAP_MM, h = p.bounds().height() + GAP_MM;
            for (int i = 0; i < placements; i++) {
                if (w > usableFolded) {
                    // não cabe dobrado: corta em camada única, tecido aberto
                    extra += h * (p.cutOnFold() ? 1 : 2);
                } else {
                    boxes.add(new double[]{w, h});
                }
            }
        }
        boxes.sort(Comparator.comparingDouble((double[] b) -> b[1]).reversed());
        double total = 0, rowW = 0, rowH = 0;
        for (double[] b : boxes) {
            if (rowW + b[0] > usableFolded) { total += rowH; rowW = 0; rowH = 0; }
            rowW += b[0];
            rowH = Math.max(rowH, b[1]);
        }
        total += rowH + extra;
        double withMargin = total * 1.10;
        return Math.ceil(withMargin / 50.0) * 50.0; // arredonda para 5 cm
    }
}
