package com.moldelab.modules.pattern.domain;

import com.moldelab.modules.measurements.api.MeasurementKey;

import java.util.List;

/** Estratégia de modelagem plana para um tipo de roupa. Implementações são puras (sem Spring). */
public interface GarmentDrafter {

    GarmentType type();

    List<MeasurementKey> requiredMeasurements();

    List<ParamSpec> params();

    /** Gera as peças. Deve ser determinístico para a mesma entrada. */
    List<PatternPiece> draft(DraftInput input);
}
