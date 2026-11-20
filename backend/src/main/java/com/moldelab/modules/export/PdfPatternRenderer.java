package com.moldelab.modules.export;

import com.moldelab.modules.measurements.api.MeasurementKey;
import com.moldelab.modules.pattern.domain.FabricEstimator;
import com.moldelab.modules.pattern.domain.Pattern;
import com.moldelab.modules.pattern.domain.PatternPiece;
import com.moldelab.modules.pattern.geometry.Vec2;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.util.Matrix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Gera o PDF do molde em ESCALA REAL (1 mm do molde = 1 mm no papel, se impresso a 100%).
 *
 * Estrutura: página de capa (instruções, quadrado de teste, mapa de montagem, lista de peças,
 * consumo de tecido) + páginas do molde (folhas lado a lado ou uma faixa única de plotter).
 */
public final class PdfPatternRenderer {

    public static final double MARGIN_MM = 10;
    public static final int MAX_PAGES = 300;
    /** Limite de tamanho de página da especificação PDF: 14.400 pt ≈ 5080 mm. */
    static final double MAX_PAGE_MM = 5000;
    static final double TEST_BAND_MM = 90;
    private static final float MM = 72f / 25.4f;

    private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public record Options(PaperSize paper, String title, String profileName, boolean showSeamLine) {}

    public byte[] render(Pattern pattern, Options opt) {
        try (PDDocument doc = new PDDocument()) {
            PDDocumentInformation info = doc.getDocumentInformation();
            info.setTitle(safe(opt.title()));
            info.setCreator("MoldeLab");

            SheetLayout.Result layout;
            double tileW, tileH;
            if (opt.paper().roll()) {
                tileW = opt.paper().widthMm() - 2 * MARGIN_MM;
                layout = SheetLayout.forRoll(pattern.pieces(), tileW);
                tileH = layout.heightMm();
                if (tileH + 2 * MARGIN_MM + TEST_BAND_MM > MAX_PAGE_MM) {
                    throw new IllegalArgumentException("A faixa de plotter teria " + Math.round(tileH / 10)
                            + " cm (máx. " + Math.round(MAX_PAGE_MM / 10) + " cm por arquivo). Use um rolo mais largo ou folhas.");
                }
            } else {
                tileW = opt.paper().widthMm() - 2 * MARGIN_MM;
                tileH = opt.paper().heightMm() - 2 * MARGIN_MM;
                layout = SheetLayout.forTiles(pattern.pieces(), tileW, tileH);
                if (layout.usedPageCount() > MAX_PAGES) {
                    throw new IllegalArgumentException("O molde precisaria de " + layout.usedPageCount()
                            + " páginas. Use um papel maior (A0/plotter).");
                }
            }

            cover(doc, pattern, opt, layout, tileW, tileH);

            if (opt.paper().roll()) {
                // faixa reservada no fim do rolo para o quadrado de teste (não sobrepõe peças)
                PDPage page = new PDPage(new PDRectangle((float) (opt.paper().widthMm() * MM),
                        (float) ((tileH + 2 * MARGIN_MM + TEST_BAND_MM) * MM)));
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    drawTile(cs, page, pattern, opt, layout, 0, 0, tileW, tileH, "Faixa única", false);
                    testSquare(cs, page, MARGIN_MM + 5, MARGIN_MM + tileH + 5);
                }
            } else {
                for (int r = 0; r < layout.rows(); r++) {
                    for (int c = 0; c < layout.columns(); c++) {
                        if (!layout.usedTiles().get(r)[c]) continue;
                        PDPage page = new PDPage(new PDRectangle((float) (opt.paper().widthMm() * MM), (float) (opt.paper().heightMm() * MM)));
                        doc.addPage(page);
                        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                            drawTile(cs, page, pattern, opt, layout, c * tileW, r * tileH, tileW, tileH, tileName(r, c), true);
                        }
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String tileName(int row, int col) {
        return String.valueOf((char) ('A' + (row % 26))) + (col + 1);
    }

    // ---------------------------------------------------------------- capa

    private void cover(PDDocument doc, Pattern pattern, Options opt, SheetLayout.Result layout, double tileW, double tileH) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        float h = page.getMediaBox().getHeight();
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            float x = mm(15);
            float y = h - mm(20);
            text(cs, bold, 18, x, y, opt.title());
            y -= 16;
            text(cs, regular, 10, x, y, pattern.garmentLabel() + (opt.profileName() != null ? "  |  Medidas: " + opt.profileName() : ""));
            y -= 20;

            text(cs, bold, 11, x, y, "Antes de imprimir");
            y -= 13;
            String[] steps = opt.paper().roll() ? new String[]{
                    "1. Envie este PDF para a gráfica/plotter pedindo impressão em TAMANHO REAL (100%).",
                    "2. Confira o quadrado de teste no fim da faixa: deve medir exatamente 5 x 5 cm.",
                    "3. Recorte pela linha contínua (linha de corte). A linha tracejada é a linha de costura."
            } : new String[]{
                    "1. Imprima em TAMANHO REAL / 100% (desative 'Ajustar à página'). Papel: " + opt.paper().name() + ", retrato.",
                    "2. Imprima ESTA capa junto e meça o quadrado de teste ao lado: deve ter exatamente 5 x 5 cm.",
                    "3. Monte as folhas conforme o mapa abaixo. Recorte a margem branca das bordas direita e de baixo",
                    "   de cada folha (na linha cinza) e cole sobre a folha vizinha, completando os losangos.",
                    "4. Recorte as peças pela linha contínua (linha de corte). A tracejada é a linha de costura.",
                    "5. Transfira piques (tracinhos na borda) e pences para o tecido. O fio (seta) fica paralelo à ourela."
            };
            for (String s : steps) { text(cs, regular, 9, x, y, s); y -= 12; }
            y -= 6;

            // Margens
            text(cs, regular, 9, x, y, String.format(Locale.ROOT, "Margens incluídas: costura %.1f cm, barra %.1f cm, acabamento %.1f cm.",
                    pattern.seams().seamMm() / 10, pattern.seams().hemMm() / 10, pattern.seams().finishMm() / 10));
            y -= 18;

            // Peças
            text(cs, bold, 11, x, y, "Peças");
            y -= 13;
            for (PatternPiece p : pattern.pieces()) {
                text(cs, regular, 9, x, y, p.code() + " - " + p.name() + " (" + p.material() + "): " + p.cutInstruction());
                y -= 12;
            }
            y -= 6;

            text(cs, bold, 11, x, y, "Consumo estimado de tecido");
            y -= 13;
            for (FabricEstimator.FabricEstimate f : pattern.fabric()) {
                text(cs, regular, 9, x, y, String.format(Locale.ROOT, "%s: %.2f m (largura %.2f m, tecido dobrado). Estimativa, compre com folga.",
                        f.material(), f.lengthMm() / 1000, f.fabricWidthMm() / 1000));
                y -= 12;
            }
            y -= 6;

            if (!pattern.warnings().isEmpty()) {
                text(cs, bold, 11, x, y, "Avisos");
                y -= 13;
                for (String w : pattern.warnings()) {
                    for (String line : wrap(w, 110)) { text(cs, regular, 9, x, y, line); y -= 12; }
                }
                y -= 6;
            }

            // Medidas usadas
            text(cs, bold, 11, x, y, "Medidas usadas (cm)");
            y -= 13;
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<MeasurementKey, Double> e : pattern.measurements().asMap().entrySet()) {
                if (!sb.isEmpty()) sb.append("  |  ");
                sb.append(e.getKey().label()).append(": ").append(fmt(e.getValue()));
            }
            for (String line : wrap(sb.toString(), 115)) { text(cs, regular, 8, x, y, line); y -= 11; }
            y -= 10;

            // Mapa de montagem
            if (!opt.paper().roll()) {
                text(cs, bold, 11, x, y, "Mapa de montagem (" + layout.usedPageCount() + " folhas)");
                y -= 8;
                // quadrado de teste à direita do mapa (mesmo trabalho de impressão = mesma escala)
                testSquare(cs, page, 140, (h - y) / MM + 2);
                float boxW = mm(118), boxH = Math.max(mm(40), y - mm(20));
                float scale = (float) Math.min(boxW / (layout.widthMm() * MM), boxH / (layout.heightMm() * MM));
                float mapH = (float) (layout.heightMm() * MM * scale);
                float ox = x, oy = y - mapH;
                drawMap(cs, pattern, layout, tileW, tileH, ox, oy, scale);
            } else {
                text(cs, regular, 9, x, y, String.format(Locale.ROOT, "Faixa de plotter: %.0f x %.0f cm.",
                        opt.paper().widthMm() / 10, (layout.heightMm() + 2 * MARGIN_MM) / 10));
            }
        }
    }

    private void drawMap(PDPageContentStream cs, Pattern pattern, SheetLayout.Result layout, double tileW, double tileH,
                         float ox, float oy, float scale) throws IOException {
        float s = MM * scale;
        float top = oy + (float) (layout.heightMm() * s);
        // peças
        cs.setStrokingColor(0.55f, 0.55f, 0.55f);
        cs.setLineWidth(0.5f);
        for (SheetLayout.Placed pl : layout.placed()) {
            List<Vec2> cut = pl.piece().cutLine();
            for (int i = 0; i < cut.size(); i++) {
                Vec2 p = cut.get(i).plus(pl.offset());
                float px = ox + (float) (p.x() * s), py = top - (float) (p.y() * s);
                if (i == 0) cs.moveTo(px, py); else cs.lineTo(px, py);
            }
            cs.closePath();
            cs.stroke();
        }
        // grade de folhas
        for (int r = 0; r < layout.rows(); r++) {
            for (int c = 0; c < layout.columns(); c++) {
                boolean used = layout.usedTiles().get(r)[c];
                float x0 = ox + (float) (c * tileW * s), y0 = top - (float) ((r + 1) * tileH * s);
                float w = (float) (tileW * s), h = (float) (tileH * s);
                if (used) { cs.setStrokingColor(0.1f, 0.3f, 0.8f); cs.setLineDashPattern(new float[]{}, 0); }
                else { cs.setStrokingColor(0.8f, 0.8f, 0.8f); cs.setLineDashPattern(new float[]{2, 2}, 0); }
                cs.setLineWidth(0.6f);
                cs.addRect(x0, y0, w, h);
                cs.stroke();
                cs.setLineDashPattern(new float[]{}, 0);
                if (used) {
                    cs.setNonStrokingColor(0.1f, 0.3f, 0.8f);
                    text(cs, bold, Math.max(5, Math.min(10, h / 5)), x0 + 2, y0 + h - Math.max(6, Math.min(11, h / 4)), tileName(r, c));
                    cs.setNonStrokingColor(0f, 0f, 0f);
                }
            }
        }
        cs.setStrokingColor(0f, 0f, 0f);
    }

    // ---------------------------------------------------------------- páginas do molde

    private void drawTile(PDPageContentStream cs, PDPage page, Pattern pattern, Options opt, SheetLayout.Result layout,
                          double tileX, double tileY, double tileW, double tileH, String name, boolean tiled) throws IOException {
        float pageH = page.getMediaBox().getHeight();
        Transform t = new Transform(tileX, tileY, pageH);

        cs.saveGraphicsState();
        cs.addRect(mm(MARGIN_MM), pageH - mm(MARGIN_MM + tileH), mm(tileW), mm(tileH));
        cs.clip();
        for (SheetLayout.Placed pl : layout.placed()) {
            drawPiece(cs, t, pl, pattern, opt);
        }
        cs.restoreGraphicsState();

        if (tiled) {
            // borda da área útil (linha de recorte/colagem) e losangos de alinhamento
            cs.setStrokingColor(0.6f, 0.6f, 0.6f);
            cs.setLineWidth(0.4f);
            cs.addRect(mm(MARGIN_MM), pageH - mm(MARGIN_MM + tileH), mm(tileW), mm(tileH));
            cs.stroke();
            diamond(cs, mm(MARGIN_MM + tileW / 2), pageH - mm(MARGIN_MM));
            diamond(cs, mm(MARGIN_MM + tileW / 2), pageH - mm(MARGIN_MM + tileH));
            diamond(cs, mm(MARGIN_MM), pageH - mm(MARGIN_MM + tileH / 2));
            diamond(cs, mm(MARGIN_MM + tileW), pageH - mm(MARGIN_MM + tileH / 2));
            cs.setStrokingColor(0f, 0f, 0f);
            cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);
            text(cs, bold, 9, mm(MARGIN_MM), pageH - mm(MARGIN_MM) + 6, "Folha " + name);
            text(cs, regular, 7, mm(MARGIN_MM + 25), pageH - mm(MARGIN_MM) + 6, safe(opt.title()) + " - imprimir em 100%");
            cs.setNonStrokingColor(0f, 0f, 0f);
        }
    }

    private void drawPiece(PDPageContentStream cs, Transform t, SheetLayout.Placed pl, Pattern pattern, Options opt) throws IOException {
        PatternPiece p = pl.piece();
        Vec2 o = pl.offset();

        // linha de costura (tracejada)
        if (opt.showSeamLine()) {
            cs.setStrokingColor(0.45f, 0.45f, 0.45f);
            cs.setLineWidth(0.5f);
            cs.setLineDashPattern(new float[]{4, 3}, 0);
            polyline(cs, t, p.seamLine(), o, true);
            cs.stroke();
        }
        // linhas internas (pences, dobras de cós)
        cs.setStrokingColor(0.2f, 0.2f, 0.2f);
        cs.setLineWidth(0.6f);
        cs.setLineDashPattern(new float[]{6, 2, 1, 2}, 0);
        for (List<Vec2> line : p.internalLines()) {
            polyline(cs, t, line, o, false);
            cs.stroke();
        }
        cs.setLineDashPattern(new float[]{}, 0);

        // linha de corte
        cs.setStrokingColor(0f, 0f, 0f);
        cs.setLineWidth(1.0f);
        polyline(cs, t, p.cutLine(), o, true);
        cs.stroke();

        // dobra do tecido (por cima da linha de corte, que coincide com ela)
        cs.setStrokingColor(0.1f, 0.35f, 0.75f);
        cs.setLineWidth(2.0f);
        cs.setLineDashPattern(new float[]{10, 3, 2, 3}, 0);
        for (List<Vec2> fold : p.foldLines()) {
            polyline(cs, t, fold, o, false);
            cs.stroke();
        }
        cs.setLineDashPattern(new float[]{}, 0);
        cs.setStrokingColor(0f, 0f, 0f);

        // piques
        cs.setLineWidth(1.4f);
        for (PatternPiece.Segment n : p.notches()) {
            Vec2 a = n.from().plus(o), b = n.to().plus(o);
            cs.moveTo(t.x(a), t.y(a));
            cs.lineTo(t.x(b), t.y(b));
            cs.stroke();
        }

        // fio
        grainline(cs, t, p.grainline(), o);

        // textos de dobra
        for (List<Vec2> fold : p.foldLines()) {
            Vec2 a = fold.getFirst().plus(o), b = fold.getLast().plus(o);
            Vec2 dir = b.minus(a).normalized();
            Vec2 inward = inwardNormal(p, fold);
            Vec2 mid = a.lerp(b, 0.5).plus(inward.times(6));
            double ang = Math.atan2(-(dir.y()), dir.x());
            if (Math.cos(ang) < 0) ang += Math.PI; // texto sempre legível
            cs.setNonStrokingColor(0.1f, 0.35f, 0.75f);
            rotatedText(cs, bold, 9, t.x(mid), t.y(mid), ang, "COLOCAR NA DOBRA DO TECIDO", true);
            cs.setNonStrokingColor(0f, 0f, 0f);
        }

        // rótulo
        Vec2 c = p.labelAnchor().plus(o);
        float lx = t.x(c), ly = t.y(c);
        if (p.bounds().height() < 90) {
            // peça estreita (cós, ribana): rótulo numa linha só
            centered(cs, bold, 9, lx + 0, ly - 3, p.code() + " - " + p.name() + " - " + p.cutInstruction());
        } else {
            centered(cs, bold, 28, lx, ly + 18, p.code());
            centered(cs, bold, 11, lx, ly + 2, p.name());
            centered(cs, regular, 9, lx, ly - 10, p.cutInstruction() + " - " + p.material());
            centered(cs, regular, 7, lx, ly - 20, pattern.garmentLabel());
        }
    }

    private Vec2 inwardNormal(PatternPiece p, List<Vec2> fold) {
        Vec2 a = fold.getFirst(), b = fold.getLast();
        Vec2 n = b.minus(a).normalized().perp();
        Vec2 test = a.lerp(b, 0.5).plus(n.times(5));
        return com.moldelab.modules.pattern.geometry.Polygons.contains(p.seamLine(), test) ? n : n.times(-1);
    }

    private void grainline(PDPageContentStream cs, Transform t, PatternPiece.Segment g, Vec2 o) throws IOException {
        Vec2 a = g.from().plus(o), b = g.to().plus(o);
        Vec2 d = b.minus(a).normalized();
        Vec2 n = d.perp();
        cs.setLineWidth(0.8f);
        cs.moveTo(t.x(a), t.y(a));
        cs.lineTo(t.x(b), t.y(b));
        for (Vec2[] head : new Vec2[][]{{b, d}, {a, d.times(-1)}}) {
            Vec2 tip = head[0], dir = head[1];
            Vec2 l = tip.minus(dir.times(6)).plus(n.times(2.5));
            Vec2 r = tip.minus(dir.times(6)).minus(n.times(2.5));
            cs.moveTo(t.x(l), t.y(l));
            cs.lineTo(t.x(tip), t.y(tip));
            cs.lineTo(t.x(r), t.y(r));
        }
        cs.stroke();
        Vec2 mid = a.lerp(b, 0.3).plus(n.times(2));
        double ang = Math.atan2(-d.y(), d.x());
        if (Math.cos(ang) < -1e-6) ang += Math.PI;
        rotatedText(cs, regular, 7, t.x(mid), t.y(mid), ang, "FIO", false);
    }

    private void testSquare(PDPageContentStream cs, PDPage page, double xMm, double yMm) throws IOException {
        float h = page.getMediaBox().getHeight();
        cs.setStrokingColor(0f, 0f, 0f);
        cs.setLineWidth(0.8f);
        cs.addRect(mm(xMm), h - mm(yMm + 50), mm(50), mm(50));
        cs.stroke();
        cs.addRect(mm(xMm), h - mm(yMm + 55 + 25.4), mm(25.4), mm(25.4));
        cs.stroke();
        text(cs, bold, 9, mm(xMm + 3), h - mm(yMm + 8), "TESTE 5 x 5 cm");
        text(cs, regular, 7, mm(xMm + 3), h - mm(yMm + 12), "Meça antes de cortar!");
        text(cs, regular, 6, mm(xMm + 1), h - mm(yMm + 59), "1 polegada");
    }

    private static void diamond(PDPageContentStream cs, float cx, float cy) throws IOException {
        float r = mm(4);
        cs.setNonStrokingColor(0.6f, 0.6f, 0.6f);
        cs.moveTo(cx, cy + r);
        cs.lineTo(cx + r, cy);
        cs.lineTo(cx, cy - r);
        cs.lineTo(cx - r, cy);
        cs.closePath();
        cs.fill();
        cs.setNonStrokingColor(0f, 0f, 0f);
    }

    // ---------------------------------------------------------------- utilitários

    private record Transform(double tileX, double tileY, float pageH) {
        float x(Vec2 p) { return mm(MARGIN_MM + p.x() - tileX); }
        float y(Vec2 p) { return pageH - mm(MARGIN_MM + p.y() - tileY); }
    }

    private static void polyline(PDPageContentStream cs, Transform t, List<Vec2> pts, Vec2 o, boolean close) throws IOException {
        for (int i = 0; i < pts.size(); i++) {
            Vec2 p = pts.get(i).plus(o);
            if (i == 0) cs.moveTo(t.x(p), t.y(p)); else cs.lineTo(t.x(p), t.y(p));
        }
        if (close) cs.closePath();
    }

    private static float mm(double v) { return (float) (v * MM); }

    private void text(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String s) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(safe(s));
        cs.endText();
    }

    private void centered(PDPageContentStream cs, PDType1Font font, float size, float cx, float y, String s) throws IOException {
        String txt = safe(s);
        float w = font.getStringWidth(txt) / 1000f * size;
        text(cs, font, size, cx - w / 2, y, txt);
    }

    private void rotatedText(PDPageContentStream cs, PDType1Font font, float size, float x, float y, double angle,
                             String s, boolean center) throws IOException {
        String txt = safe(s);
        float w = font.getStringWidth(txt) / 1000f * size;
        cs.beginText();
        cs.setFont(font, size);
        Matrix m = Matrix.getRotateInstance(angle, x, y);
        if (center) m.translate(-w / 2, 0);
        cs.setTextMatrix(m);
        cs.showText(txt);
        cs.endText();
    }

    /** Mantém apenas caracteres codificáveis em WinAnsi (fontes padrão do PDF). */
    static String safe(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            if (ch == '\n' || ch == '\r' || ch == '\t') sb.append(' ');
            else if (ch < 0x20) continue;
            else if (ch < 0x7F || (ch >= 0xA0 && ch <= 0xFF)) sb.append(ch);
            else if (ch == '–' || ch == '—') sb.append('-');
            else if (ch == '“' || ch == '”') sb.append('"');
            else if (ch == '‘' || ch == '’') sb.append('\'');
            else if (ch == '•' || ch == '·') sb.append('-');
            else sb.append('?');
        }
        return sb.toString();
    }

    private static List<String> wrap(String s, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : s.split(" ")) {
            if (cur.length() + word.length() + 1 > width && !cur.isEmpty()) {
                lines.add(cur.toString());
                cur.setLength(0);
            }
            if (!cur.isEmpty()) cur.append(' ');
            cur.append(word);
        }
        if (!cur.isEmpty()) lines.add(cur.toString());
        return lines;
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }
}
