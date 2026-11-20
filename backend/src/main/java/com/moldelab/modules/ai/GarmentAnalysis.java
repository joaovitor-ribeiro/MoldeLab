package com.moldelab.modules.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** Saída estruturada pedida ao Claude ao analisar a imagem de uma roupa. */
public record GarmentAnalysis(
        @JsonPropertyDescription("Modelo do catálogo mais próximo da roupa da imagem, ou NAO_SUPORTADO.")
        AnalyzedType garmentType,
        @JsonPropertyDescription("Confiança de 0 a 1 de que o modelo escolhido representa bem a roupa.")
        double confidence,
        @JsonPropertyDescription("Descrição curta da roupa em português (tecido aparente, caimento, detalhes).")
        String description,
        @JsonPropertyDescription("Valores estimados para os parâmetros do modelo escolhido, usando as chaves e unidades do catálogo.")
        List<ParamGuess> params,
        @JsonPropertyDescription("Observações em português: detalhes da imagem que o molde básico NÃO reproduz (bolsos, babados, golas...) e sugestões de tecido.")
        List<String> notes
) {
    public enum AnalyzedType { SAIA_RETA, SAIA_GODE, CAMISETA, NAO_SUPORTADO }

    public record ParamGuess(
            @JsonPropertyDescription("Chave do parâmetro exatamente como no catálogo.") String key,
            @JsonPropertyDescription("Valor na unidade do catálogo.") double value) {}
}
