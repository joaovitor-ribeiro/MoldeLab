package com.moldelab.modules.ai;

import com.moldelab.modules.pattern.domain.GarmentCatalog;
import com.moldelab.modules.pattern.domain.GarmentDrafter;
import com.moldelab.modules.pattern.domain.GarmentType;
import com.moldelab.modules.pattern.domain.ParamSpec;
import com.moldelab.shared.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);
    static final long MAX_BYTES = 8L * 1024 * 1024;

    public record AiStatus(boolean enabled, String model) {}

    public record GarmentSuggestion(GarmentType garmentType, boolean supported, double confidence, String description,
                                    Map<String, Double> params, List<String> notes) {}

    private final GarmentVision vision;
    private final GarmentCatalog catalog;

    public AiController(GarmentVision vision, GarmentCatalog catalog) {
        this.vision = vision;
        this.catalog = catalog;
    }

    @GetMapping("/status")
    public AiStatus status() {
        return new AiStatus(vision.available(), vision.available() ? vision.model() : null);
    }

    @PostMapping(value = "/analyze-garment", consumes = "multipart/form-data")
    public GarmentSuggestion analyze(@RequestParam("image") MultipartFile image,
                                     @RequestParam(value = "hint", required = false) String hint) throws IOException {
        if (!vision.available()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI_DISABLED",
                    "Leitura por IA desativada. Defina ANTHROPIC_API_KEY no backend ou escolha o modelo manualmente.", null);
        }
        if (image.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "Envie uma imagem", null);
        if (image.getSize() > MAX_BYTES) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Imagem acima de 8 MB", null);
        if (hint != null && hint.length() > 500) throw new ApiException(HttpStatus.BAD_REQUEST, "HINT_TOO_LONG", "Dica com no máximo 500 caracteres", null);

        byte[] bytes = image.getBytes();
        String mediaType = sniffMediaType(bytes);
        if (mediaType == null) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_IMAGE", "Use PNG, JPEG, WEBP ou GIF", null);
        }

        GarmentAnalysis a;
        try {
            a = vision.analyze(bytes, mediaType, hint);
        } catch (RuntimeException e) {
            log.warn("Falha na análise de imagem: {}", e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI_FAILED",
                    "Não foi possível analisar a imagem agora. Tente de novo ou escolha o modelo manualmente.", null);
        }
        return toSuggestion(a);
    }

    GarmentSuggestion toSuggestion(GarmentAnalysis a) {
        List<String> notes = new ArrayList<>(a.notes() == null ? List.of() : a.notes());
        if (a.garmentType() == null || a.garmentType() == GarmentAnalysis.AnalyzedType.NAO_SUPORTADO) {
            return new GarmentSuggestion(null, false, clamp01(a.confidence()), a.description(), Map.of(), notes);
        }
        GarmentType type = GarmentType.valueOf(a.garmentType().name());
        GarmentDrafter drafter = catalog.find(type).orElseThrow();
        Map<String, ParamSpec> specs = new LinkedHashMap<>();
        drafter.params().forEach(p -> specs.put(p.key(), p));
        Map<String, Double> params = new LinkedHashMap<>();
        if (a.params() != null) {
            for (GarmentAnalysis.ParamGuess g : a.params()) {
                ParamSpec spec = specs.get(g.key());
                if (spec == null) continue; // a IA inventou uma chave — ignorar
                params.put(g.key(), spec.clamp(g.value()));
            }
        }
        return new GarmentSuggestion(type, true, clamp01(a.confidence()), a.description(), params, notes);
    }

    /** Detecta o tipo pelos bytes iniciais (não confia no Content-Type do cliente). */
    static String sniffMediaType(byte[] b) {
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "image/png";
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "image/jpeg";
        if (b.length >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') return "image/gif";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";
        return null;
    }

    private static double clamp01(double v) {
        return Double.isNaN(v) ? 0 : Math.max(0, Math.min(1, v));
    }
}
