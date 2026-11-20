package com.moldelab.modules.pattern.domain;

import java.util.List;

/**
 * Parâmetro de estilo de um modelo (comprimento, evasê, decote...). Unidade exibida ao usuário
 * em `unit`; quando `unit` = "cm" o valor é convertido para mm pelo drafter.
 */
public record ParamSpec(
        String key,
        String label,
        String unit,
        double min,
        double max,
        double defaultValue,
        double step,
        String help,
        List<Option> options
) {
    public record Option(double value, String label) {}

    public static ParamSpec number(String key, String label, String unit, double min, double max, double def, double step, String help) {
        return new ParamSpec(key, label, unit, min, max, def, step, help, List.of());
    }

    public static ParamSpec choice(String key, String label, double def, String help, Option... options) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (Option o : options) { min = Math.min(min, o.value()); max = Math.max(max, o.value()); }
        return new ParamSpec(key, label, "", min, max, def, 0, help, List.of(options));
    }

    public double clamp(double v) {
        if (Double.isNaN(v)) return defaultValue;
        if (!options.isEmpty()) {
            return options.stream().map(Option::value)
                    .min((a, b) -> Double.compare(Math.abs(a - v), Math.abs(b - v))).orElse(defaultValue);
        }
        return Math.max(min, Math.min(max, v));
    }
}
