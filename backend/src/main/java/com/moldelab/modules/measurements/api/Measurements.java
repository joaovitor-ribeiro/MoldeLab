package com.moldelab.modules.measurements.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Positive;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Conjunto de medidas corporais em CENTÍMETROS. Todos os campos são opcionais aqui;
 * cada modelo de roupa declara quais medidas exige (ver GarmentDrafter#requiredMeasurements).
 */
public record Measurements(
        @Positive Double height,
        @Positive Double bust,
        @Positive Double waist,
        @Positive Double hip,
        @Positive Double neck,
        @Positive Double shoulderWidth,
        @Positive Double backWaistLength,
        @Positive Double armLength,
        @Positive Double upperArm,
        @Positive Double wrist,
        @Positive Double waistToHip,
        @Positive Double waistToKnee,
        @Positive Double waistToFloor,
        @Positive Double thigh,
        @Positive Double crotchDepth
) {

    public Optional<Double> getCm(MeasurementKey key) {
        Double v = switch (key) {
            case HEIGHT -> height;
            case BUST -> bust;
            case WAIST -> waist;
            case HIP -> hip;
            case NECK -> neck;
            case SHOULDER_WIDTH -> shoulderWidth;
            case BACK_WAIST_LENGTH -> backWaistLength;
            case ARM_LENGTH -> armLength;
            case UPPER_ARM -> upperArm;
            case WRIST -> wrist;
            case WAIST_TO_HIP -> waistToHip;
            case WAIST_TO_KNEE -> waistToKnee;
            case WAIST_TO_FLOOR -> waistToFloor;
            case THIGH -> thigh;
            case CROTCH_DEPTH -> crotchDepth;
        };
        return Optional.ofNullable(v);
    }

    /** Valor em milímetros (lança se ausente — o chamador deve validar antes). */
    public double mm(MeasurementKey key) {
        return getCm(key).map(cm -> cm * 10.0)
                .orElseThrow(() -> new IllegalStateException("Medida ausente: " + key));
    }

    @JsonIgnore
    public Map<MeasurementKey, Double> asMap() {
        Map<MeasurementKey, Double> map = new EnumMap<>(MeasurementKey.class);
        for (MeasurementKey k : MeasurementKey.values()) getCm(k).ifPresent(v -> map.put(k, v));
        return map;
    }

    public static Measurements fromMap(Map<MeasurementKey, Double> m) {
        return new Measurements(
                m.get(MeasurementKey.HEIGHT), m.get(MeasurementKey.BUST), m.get(MeasurementKey.WAIST),
                m.get(MeasurementKey.HIP), m.get(MeasurementKey.NECK), m.get(MeasurementKey.SHOULDER_WIDTH),
                m.get(MeasurementKey.BACK_WAIST_LENGTH), m.get(MeasurementKey.ARM_LENGTH),
                m.get(MeasurementKey.UPPER_ARM), m.get(MeasurementKey.WRIST), m.get(MeasurementKey.WAIST_TO_HIP),
                m.get(MeasurementKey.WAIST_TO_KNEE), m.get(MeasurementKey.WAIST_TO_FLOOR),
                m.get(MeasurementKey.THIGH), m.get(MeasurementKey.CROTCH_DEPTH));
    }
}
