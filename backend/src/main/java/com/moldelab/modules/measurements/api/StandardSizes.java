package com.moldelab.modules.measurements.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabelas de medidas de referência (aproximações de tabelas de mercado brasileiras).
 * Servem como ponto de partida — o usuário deve ajustar com as próprias medidas.
 */
public final class StandardSizes {

    public record StandardSize(String id, String label, String group, Measurements measurements) {}

    private static final List<StandardSize> ALL = build();

    private StandardSizes() {}

    public static List<StandardSize> all() { return ALL; }

    private static List<StandardSize> build() {
        List<StandardSize> list = new ArrayList<>();
        // Feminino 36..48 — busto/cintura/quadril crescem ~4 cm por número
        int[] fem = {36, 38, 40, 42, 44, 46, 48};
        for (int i = 0; i < fem.length; i++) {
            double step = i;
            double big = i >= 5 ? (i - 4) * 2 : 0; // a partir do 46 cresce 6 cm
            list.add(new StandardSize("F" + fem[i], "Feminino " + fem[i], "Feminino", new Measurements(
                    162.0 + step * 0.5,
                    84 + step * 4 + big, 66 + step * 4 + big, 92 + step * 4 + big,
                    33.5 + step * 0.75, 37 + step * 0.75, 40 + step * 0.5,
                    58 + step * 0.5, 25.5 + step * 1.5 + big * 0.5, 15 + step * 0.4,
                    20 + step * 0.25, 58 + step * 0.4, 104 + step * 0.4,
                    52 + step * 2.2 + big * 0.5, 26 + step * 0.5)));
        }
        String[] male = {"P", "M", "G", "GG", "XG"};
        for (int i = 0; i < male.length; i++) {
            double step = i;
            list.add(new StandardSize("M" + male[i], "Masculino " + male[i], "Masculino", new Measurements(
                    172.0 + step * 2,
                    92 + step * 8, 80 + step * 8, 96 + step * 6,
                    38 + step * 2, 43 + step * 2, 44 + step * 1,
                    62 + step * 1, 29 + step * 2.5, 17 + step * 0.5,
                    20 + step * 0.5, 58 + step * 0.8, 106 + step * 1,
                    54 + step * 3, 27 + step * 0.7)));
        }
        return List.copyOf(list);
    }
}
