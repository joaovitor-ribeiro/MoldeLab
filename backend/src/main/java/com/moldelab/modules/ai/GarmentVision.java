package com.moldelab.modules.ai;

/** Porta para análise de imagem de roupa. Implementação real usa Claude; testes usam fakes. */
public interface GarmentVision {

    boolean available();

    String model();

    /**
     * @param image     bytes da imagem (PNG/JPEG/WEBP/GIF já validados)
     * @param mediaType ex.: image/png
     * @param hint      texto opcional do usuário ("é uma saia midi de linho")
     */
    GarmentAnalysis analyze(byte[] image, String mediaType, String hint);
}
