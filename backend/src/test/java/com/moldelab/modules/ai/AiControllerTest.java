package com.moldelab.modules.ai;

import com.moldelab.modules.pattern.domain.GarmentCatalog;
import com.moldelab.modules.pattern.domain.GarmentType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiControllerTest {

    private final AiController controller = new AiController(new FakeVision(), new GarmentCatalog());

    @Test
    void rnIa01_parametrosInventadosSaoDescartadosEValoresLimitados() {
        GarmentAnalysis a = new GarmentAnalysis(GarmentAnalysis.AnalyzedType.SAIA_GODE, 1.7, "Saia rodada midi",
                List.of(new GarmentAnalysis.ParamGuess("length", 999), new GarmentAnalysis.ParamGuess("bolso", 2),
                        new GarmentAnalysis.ParamGuess("fraction", 0.6)),
                List.of("Tem bolsos laterais"));
        AiController.GarmentSuggestion s = controller.toSuggestion(a);
        assertThat(s.garmentType()).isEqualTo(GarmentType.SAIA_GODE);
        assertThat(s.confidence()).isEqualTo(1.0);
        assertThat(s.params()).containsEntry("length", 120.0).containsEntry("fraction", 0.5).doesNotContainKey("bolso");
    }

    @Test
    void rnIa02_naoSuportadoNaoTemParametros() {
        GarmentAnalysis a = new GarmentAnalysis(GarmentAnalysis.AnalyzedType.NAO_SUPORTADO, 0.9, "Calça jeans",
                List.of(new GarmentAnalysis.ParamGuess("length", 100)), List.of());
        AiController.GarmentSuggestion s = controller.toSuggestion(a);
        assertThat(s.supported()).isFalse();
        assertThat(s.params()).isEmpty();
    }

    @Test
    void rnIa03_tipoDeImagemPelosBytes() {
        assertThat(AiController.sniffMediaType(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A})).isEqualTo("image/png");
        assertThat(AiController.sniffMediaType(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0})).isEqualTo("image/jpeg");
        assertThat(AiController.sniffMediaType("<svg>".getBytes())).isNull();
    }

    @Test
    void promptDescreveCatalogo() {
        var vision = new ClaudeGarmentVision(new AiProperties(false, null, null), new GarmentCatalog());
        assertThat(vision.available()).isFalse();
        assertThat(vision.systemPrompt()).contains("SAIA_GODE").contains("fraction").contains("sleeveLength");
    }

    static class FakeVision implements GarmentVision {
        public boolean available() { return true; }
        public String model() { return "fake"; }
        public GarmentAnalysis analyze(byte[] image, String mediaType, String hint) { throw new UnsupportedOperationException(); }
    }
}
