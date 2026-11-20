package com.moldelab.modules.export;

import com.moldelab.modules.measurements.api.StandardSizes;
import com.moldelab.modules.pattern.domain.GarmentCatalog;
import com.moldelab.modules.pattern.domain.GarmentType;
import com.moldelab.modules.pattern.domain.Pattern;
import com.moldelab.modules.pattern.domain.PatternEngine;
import com.moldelab.modules.pattern.domain.SeamSettings;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PdfPatternRendererTest {

    private final PatternEngine engine = new PatternEngine(new GarmentCatalog());
    private final PdfPatternRenderer renderer = new PdfPatternRenderer();

    private Pattern pattern(GarmentType t) {
        var m = StandardSizes.all().stream().filter(s -> s.id().equals("F40")).findFirst().orElseThrow().measurements();
        return engine.draft(t, m, Map.of(), SeamSettings.DEFAULT, 1500);
    }

    @ParameterizedTest
    @EnumSource(GarmentType.class)
    void rnPdf01_paginasA4EmEscalaReal(GarmentType t) throws Exception {
        byte[] pdf = renderer.render(pattern(t), new PdfPatternRenderer.Options(PaperSize.A4, "Teste", "Maria", true));
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(2);
            for (PDPage page : doc.getPages()) {
                // A4 = 210 x 297 mm = 595,28 x 841,89 pt
                assertThat((double) page.getMediaBox().getWidth()).isCloseTo(595.28, within(0.1));
                assertThat((double) page.getMediaBox().getHeight()).isCloseTo(841.89, within(0.1));
            }
            String text = new PDFTextStripper().getText(doc);
            assertThat(text).contains("TAMANHO REAL").contains("TESTE 5 x 5 cm").contains("Folha A1");
        }
    }

    @Test
    void rnPdf02_plotterGeraFaixaUnicaComLarguraDoRolo() throws Exception {
        byte[] pdf = renderer.render(pattern(GarmentType.CAMISETA), new PdfPatternRenderer.Options(PaperSize.PLOTTER_914, "Camiseta", null, true));
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2); // capa + faixa
            assertThat((double) doc.getPage(1).getMediaBox().getWidth()).isCloseTo(914 * 72 / 25.4, within(0.1));
        }
    }

    @Test
    void rnPdf03_folhasVaziasNaoSaoImpressas() {
        Pattern p = pattern(GarmentType.SAIA_GODE);
        SheetLayout.Result layout = SheetLayout.forTiles(p.pieces(), 190, 277);
        assertThat(layout.usedPageCount()).isLessThanOrEqualTo(layout.rows() * layout.columns());
        // todas as peças dentro da folha virtual
        for (SheetLayout.Placed pl : layout.placed()) {
            assertThat(pl.placedBounds().minX()).isGreaterThanOrEqualTo(0);
            assertThat(pl.placedBounds().maxX()).isLessThanOrEqualTo(layout.widthMm());
            assertThat(pl.placedBounds().maxY()).isLessThanOrEqualTo(layout.heightMm());
        }
    }

    @Test
    void textoComCaracteresForaDoWinAnsiNaoQuebra() {
        assertThat(PdfPatternRenderer.safe("Saia → godê “linda” — ok")).isEqualTo("Saia ? godê \"linda\" - ok");
    }
}
