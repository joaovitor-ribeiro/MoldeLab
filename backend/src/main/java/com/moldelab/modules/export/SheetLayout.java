package com.moldelab.modules.export;

import com.moldelab.modules.pattern.domain.PatternPiece;
import com.moldelab.modules.pattern.geometry.Bounds;
import com.moldelab.modules.pattern.geometry.Vec2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Distribui as peças numa "folha virtual" (mm) para depois fatiar em páginas.
 * Para papel em folhas, testa várias larguras (em nº de colunas) e escolhe a que usa menos páginas.
 */
public final class SheetLayout {

    public static final double GAP_MM = 15;

    /** Peça posicionada: deslocamento a somar às coordenadas originais da peça. */
    public record Placed(PatternPiece piece, Vec2 offset) {
        public Bounds placedBounds() {
            Bounds b = piece.bounds();
            return new Bounds(b.minX() + offset.x(), b.minY() + offset.y(), b.maxX() + offset.x(), b.maxY() + offset.y());
        }
    }

    public record Result(List<Placed> placed, double widthMm, double heightMm, int columns, int rows, List<boolean[]> usedTiles) {
        public int usedPageCount() {
            int n = 0;
            for (boolean[] row : usedTiles) for (boolean b : row) if (b) n++;
            return n;
        }
    }

    private SheetLayout() {}

    /** Layout para folhas: tileW × tileH é a área útil de cada página. */
    public static Result forTiles(List<PatternPiece> pieces, double tileW, double tileH) {
        double maxPieceW = pieces.stream().mapToDouble(p -> p.bounds().width()).max().orElse(tileW);
        int minCols = Math.max(1, (int) Math.ceil((maxPieceW + 2 * GAP_MM) / tileW));
        Result best = null;
        for (int cols = minCols; cols <= minCols + 4; cols++) {
            Result r = shelf(pieces, cols * tileW, tileW, tileH);
            if (best == null || r.usedPageCount() < best.usedPageCount()
                    || (r.usedPageCount() == best.usedPageCount() && r.columns() < best.columns())) {
                best = r;
            }
        }
        return best;
    }

    /** Layout para rolo: largura fixa, uma única "página" alta. */
    public static Result forRoll(List<PatternPiece> pieces, double usableWidth) {
        return shelf(pieces, usableWidth, usableWidth, Double.NaN);
    }

    private static Result shelf(List<PatternPiece> pieces, double width, double tileW, double tileH) {
        List<PatternPiece> sorted = new ArrayList<>(pieces);
        sorted.sort(Comparator.comparingDouble((PatternPiece p) -> p.bounds().height()).reversed());
        List<Placed> placed = new ArrayList<>();
        double x = GAP_MM, y = GAP_MM, rowH = 0;
        for (PatternPiece p : sorted) {
            Bounds b = p.bounds();
            if (x + b.width() + GAP_MM > width && x > GAP_MM) {
                x = GAP_MM;
                y += rowH + GAP_MM;
                rowH = 0;
            }
            placed.add(new Placed(p, Vec2.of(x - b.minX(), y - b.minY())));
            x += b.width() + GAP_MM;
            rowH = Math.max(rowH, b.height());
        }
        double height = y + rowH + GAP_MM;
        double usedWidth = Math.max(width, placed.stream().mapToDouble(pl -> pl.placedBounds().maxX()).max().orElse(0) + GAP_MM);

        if (Double.isNaN(tileH)) {
            return new Result(placed, usedWidth, height, 1, 1, List.<boolean[]>of(new boolean[]{true}));
        }
        int cols = (int) Math.ceil(usedWidth / tileW - 1e-9);
        int rows = (int) Math.ceil(height / tileH - 1e-9);
        List<boolean[]> used = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            boolean[] row = new boolean[cols];
            for (int c = 0; c < cols; c++) {
                double tx0 = c * tileW, ty0 = r * tileH, tx1 = tx0 + tileW, ty1 = ty0 + tileH;
                for (Placed pl : placed) {
                    if (pieceTouches(pl, tx0, ty0, tx1, ty1)) { row[c] = true; break; }
                }
            }
            used.add(row);
        }
        return new Result(placed, cols * tileW, rows * tileH, cols, rows, used);
    }

    /** Verifica se algum ponto/segmento da linha de corte entra no retângulo (aproximação por amostragem). */
    private static boolean pieceTouches(Placed pl, double x0, double y0, double x1, double y1) {
        Bounds b = pl.placedBounds();
        if (b.maxX() < x0 || b.minX() > x1 || b.maxY() < y0 || b.minY() > y1) return false;
        List<Vec2> cut = pl.piece().cutLine();
        Vec2 o = pl.offset();
        for (int i = 0; i < cut.size(); i++) {
            Vec2 a = cut.get(i).plus(o), c = cut.get((i + 1) % cut.size()).plus(o);
            int steps = Math.max(1, (int) (a.distanceTo(c) / 5));
            for (int s = 0; s <= steps; s++) {
                Vec2 p = a.lerp(c, (double) s / steps);
                if (p.x() >= x0 && p.x() <= x1 && p.y() >= y0 && p.y() <= y1) return true;
            }
        }
        // peça maior que a página cobrindo-a por inteiro (nenhuma borda dentro): testar o centro da página
        Vec2 center = Vec2.of((x0 + x1) / 2, (y0 + y1) / 2).minus(o);
        return com.moldelab.modules.pattern.geometry.Polygons.contains(cut, center);
    }
}
