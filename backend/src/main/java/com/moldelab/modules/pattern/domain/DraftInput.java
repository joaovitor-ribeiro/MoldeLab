package com.moldelab.modules.pattern.domain;

import com.moldelab.modules.measurements.api.MeasurementKey;
import com.moldelab.modules.measurements.api.Measurements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Entrada já validada para um drafter: medidas (cm→mm), parâmetros (já limitados) e margens. */
public final class DraftInput {

    private final Measurements measurements;
    private final Map<String, Double> params;
    private final Map<String, ParamSpec> specs;
    private final SeamSettings seams;
    private final List<String> warnings = new ArrayList<>();

    public DraftInput(Measurements measurements, Map<String, Double> params, Map<String, ParamSpec> specs, SeamSettings seams) {
        this.measurements = measurements;
        this.params = params;
        this.specs = specs;
        this.seams = seams;
    }

    /** Medida corporal em mm. */
    public double mm(MeasurementKey k) { return measurements.mm(k); }

    public boolean has(MeasurementKey k) { return measurements.getCm(k).isPresent(); }

    /** Parâmetro no valor bruto (unidade da spec). */
    public double param(String key) {
        Double v = params.get(key);
        ParamSpec spec = specs.get(key);
        if (spec == null) throw new IllegalArgumentException("Parâmetro desconhecido: " + key);
        return v == null ? spec.defaultValue() : spec.clamp(v);
    }

    /** Parâmetro em cm convertido para mm. */
    public double paramMm(String key) { return param(key) * 10.0; }

    public SeamSettings seams() { return seams; }

    public void warn(String message) { warnings.add(message); }

    public List<String> warnings() { return List.copyOf(warnings); }
}
