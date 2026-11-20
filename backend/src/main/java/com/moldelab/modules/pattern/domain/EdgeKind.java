package com.moldelab.modules.pattern.domain;

public enum EdgeKind {
    /** Costura comum: recebe margem de costura padrão. */
    SEAM,
    /** Barra: recebe margem de barra. */
    HEM,
    /** Dobra do tecido: margem zero, peça é cortada com o tecido dobrado nessa linha. */
    FOLD,
    /** Borda que recebe acabamento (decote com ribana, cava com viés): margem de acabamento. */
    FINISH
}
