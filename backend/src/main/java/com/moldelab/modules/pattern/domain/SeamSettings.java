package com.moldelab.modules.pattern.domain;

/** Margens em milímetros por tipo de aresta. */
public record SeamSettings(double seamMm, double hemMm, double finishMm) {

    public static final SeamSettings DEFAULT = new SeamSettings(10, 25, 7);

    public SeamSettings {
        if (seamMm < 0 || seamMm > 50 || hemMm < 0 || hemMm > 80 || finishMm < 0 || finishMm > 50) {
            throw new IllegalArgumentException("Margens fora do intervalo permitido");
        }
    }

    public double allowanceFor(EdgeKind kind) {
        return switch (kind) {
            case SEAM -> seamMm;
            case HEM -> hemMm;
            case FINISH -> finishMm;
            case FOLD -> 0;
        };
    }
}
