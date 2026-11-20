package com.moldelab.modules.pattern.domain;

import com.moldelab.modules.measurements.api.Measurements;

import java.util.List;
import java.util.Map;

/** Resultado completo de uma modelagem. Coordenadas das peças em mm. */
public record Pattern(
        GarmentType garmentType,
        String garmentLabel,
        Map<String, Double> params,
        SeamSettings seams,
        Measurements measurements,
        List<PatternPiece> pieces,
        List<FabricEstimator.FabricEstimate> fabric,
        List<String> warnings
) {}
