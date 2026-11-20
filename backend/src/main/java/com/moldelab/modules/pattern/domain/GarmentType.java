package com.moldelab.modules.pattern.domain;

public enum GarmentType {
    SAIA_RETA("Saia reta / evasê", "Saia de cintura com pences, frente na dobra, costas com zíper no centro. Evasê ajustável."),
    SAIA_GODE("Saia godê", "Saia circular (1/4, 1/2, 3/4 ou godê inteiro) com cós."),
    CAMISETA("Camiseta / blusa / vestido-camiseta", "Modelagem básica de malha: frente, costas, manga e ribana. Comprimento livre (vira vestido).");

    private final String label;
    private final String description;

    GarmentType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() { return label; }
    public String description() { return description; }
}
