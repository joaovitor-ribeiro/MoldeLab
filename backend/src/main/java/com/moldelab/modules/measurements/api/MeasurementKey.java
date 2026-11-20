package com.moldelab.modules.measurements.api;

/**
 * Medidas corporais suportadas. Valores de entrada/saída da API em CENTÍMETROS.
 * minCm/maxCm são limites de plausibilidade (validação), não de tamanho de mercado.
 */
public enum MeasurementKey {
    HEIGHT("Altura", "Em pé, descalço(a), do chão ao topo da cabeça.", 100, 220),
    BUST("Busto / tórax", "Contorno na parte mais saliente do busto, fita paralela ao chão.", 60, 180),
    WAIST("Cintura", "Contorno na parte mais fina do tronco (onde dobra para o lado).", 45, 170),
    HIP("Quadril", "Contorno na parte mais larga do quadril/bumbum, fita paralela ao chão.", 65, 190),
    NECK("Pescoço", "Contorno na base do pescoço.", 25, 55),
    SHOULDER_WIDTH("Largura dos ombros", "Pelas costas, da ponta de um ombro à ponta do outro.", 28, 60),
    BACK_WAIST_LENGTH("Comprimento nuca-cintura", "Pelas costas, do osso da nuca (7ª vértebra) até a cintura.", 30, 60),
    ARM_LENGTH("Comprimento do braço", "Da ponta do ombro até o punho, com o braço levemente dobrado.", 40, 80),
    UPPER_ARM("Contorno do braço", "Contorno na parte mais grossa do braço (bíceps), relaxado.", 18, 55),
    WRIST("Punho", "Contorno do punho, sobre o osso.", 12, 25),
    WAIST_TO_HIP("Altura do quadril", "Da cintura até a linha do quadril (parte mais larga), pela lateral.", 12, 32),
    WAIST_TO_KNEE("Cintura ao joelho", "Pela lateral, da cintura até o meio do joelho.", 40, 75),
    WAIST_TO_FLOOR("Cintura ao chão", "Pela lateral, da cintura até o chão, descalço(a).", 80, 130),
    THIGH("Coxa", "Contorno na parte mais grossa da coxa.", 35, 90),
    CROTCH_DEPTH("Altura do gancho", "Sentado(a) numa cadeira reta: da cintura até o assento, pela lateral.", 18, 40);

    private final String label;
    private final String howToMeasure;
    private final double minCm;
    private final double maxCm;

    MeasurementKey(String label, String howToMeasure, double minCm, double maxCm) {
        this.label = label;
        this.howToMeasure = howToMeasure;
        this.minCm = minCm;
        this.maxCm = maxCm;
    }

    public String label() { return label; }
    public String howToMeasure() { return howToMeasure; }
    public double minCm() { return minCm; }
    public double maxCm() { return maxCm; }

    /** Nome em camelCase usado no JSON (ex.: BACK_WAIST_LENGTH → backWaistLength). */
    public String jsonName() {
        String[] parts = name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }
}
