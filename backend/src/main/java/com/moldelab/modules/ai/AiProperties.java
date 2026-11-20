package com.moldelab.modules.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param enabled  liga/desliga a leitura de imagens por IA (também exige ANTHROPIC_API_KEY)
 * @param model    id do modelo Claude
 * @param timeoutSeconds timeout por requisição
 */
@ConfigurationProperties(prefix = "moldelab.ai")
public record AiProperties(Boolean enabled, String model, Integer timeoutSeconds) {

    public AiProperties {
        if (enabled == null) enabled = true;
        if (model == null || model.isBlank()) model = "claude-opus-5-5";
        if (timeoutSeconds == null) timeoutSeconds = 90;
    }
}
