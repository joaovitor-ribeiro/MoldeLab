package com.moldelab.modules.pattern.domain;

import com.moldelab.modules.measurements.api.MeasurementKey;
import com.moldelab.modules.measurements.api.Measurements;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orquestra a modelagem sem depender de Spring: valida medidas, limita parâmetros,
 * chama o drafter, nomeia peças (A, B, C...) e estima tecido.
 */
public final class PatternEngine {

    /** Lançada quando faltam medidas ou alguma está fora do plausível. */
    public static final class InvalidMeasurementsException extends RuntimeException {
        private final List<MeasurementKey> missing;
        private final List<MeasurementKey> outOfRange;

        InvalidMeasurementsException(List<MeasurementKey> missing, List<MeasurementKey> outOfRange) {
            super(buildMessage(missing, outOfRange));
            this.missing = missing;
            this.outOfRange = outOfRange;
        }

        public List<MeasurementKey> missing() { return missing; }
        public List<MeasurementKey> outOfRange() { return outOfRange; }

        private static String buildMessage(List<MeasurementKey> missing, List<MeasurementKey> outOfRange) {
            List<String> parts = new ArrayList<>();
            if (!missing.isEmpty()) parts.add("Faltam medidas: " + missing.stream().map(MeasurementKey::label).collect(Collectors.joining(", ")));
            if (!outOfRange.isEmpty()) parts.add("Medidas fora do intervalo plausível: " + outOfRange.stream().map(MeasurementKey::label).collect(Collectors.joining(", ")));
            return String.join(". ", parts);
        }
    }

    private final GarmentCatalog catalog;

    public PatternEngine(GarmentCatalog catalog) {
        this.catalog = catalog;
    }

    public Pattern draft(GarmentType type, Measurements m, Map<String, Double> rawParams, SeamSettings seams, double fabricWidthMm) {
        GarmentDrafter drafter = catalog.find(type)
                .orElseThrow(() -> new IllegalArgumentException("Modelo não suportado: " + type));

        List<MeasurementKey> missing = new ArrayList<>();
        List<MeasurementKey> outOfRange = new ArrayList<>();
        for (MeasurementKey k : drafter.requiredMeasurements()) {
            var v = m.getCm(k);
            if (v.isEmpty()) missing.add(k);
            else if (v.get() < k.minCm() || v.get() > k.maxCm()) outOfRange.add(k);
        }
        if (!missing.isEmpty() || !outOfRange.isEmpty()) throw new InvalidMeasurementsException(missing, outOfRange);

        Map<String, ParamSpec> specs = drafter.params().stream()
                .collect(Collectors.toMap(ParamSpec::key, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<String, Double> params = rawParams == null ? Map.of() : rawParams;
        DraftInput input = new DraftInput(m, params, specs, seams);

        List<String> warnings = new ArrayList<>();
        params.keySet().stream().filter(k -> !specs.containsKey(k))
                .forEach(k -> warnings.add("Parâmetro ignorado (não existe neste modelo): " + k));

        Map<String, Double> used = new LinkedHashMap<>();
        specs.forEach((k, spec) -> {
            double v = input.param(k);
            Double raw = params.get(k);
            if (raw != null && Math.abs(raw - v) > 1e-9) {
                warnings.add(spec.label() + " ajustado(a) para " + v + " " + spec.unit() + " (limite do modelo).");
            }
            used.put(k, v);
        });

        List<PatternPiece> drafted = drafter.draft(input);
        warnings.addAll(input.warnings());

        List<PatternPiece> coded = new ArrayList<>();
        for (int i = 0; i < drafted.size(); i++) {
            PatternPiece p = drafted.get(i);
            String code = String.valueOf((char) ('A' + i));
            coded.add(new PatternPiece(p.id(), code, p.name(), p.material(), p.cutQuantity(), p.cutOnFold(), p.mirrored(),
                    p.seamLine(), p.cutLine(), p.foldLines(), p.grainline(), p.notches(), p.internalLines(),
                    p.edges(), p.labelAnchor(), p.bounds()));
        }

        return new Pattern(type, type.label(), used, seams, m, coded,
                FabricEstimator.estimate(coded, fabricWidthMm), warnings);
    }
}
