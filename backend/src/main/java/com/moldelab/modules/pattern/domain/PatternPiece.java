package com.moldelab.modules.pattern.domain;

import com.moldelab.modules.pattern.geometry.Bounds;
import com.moldelab.modules.pattern.geometry.Vec2;

import java.util.List;

/**
 * Peça de molde pronta para desenhar. Coordenadas em mm, y para baixo, origem livre (ver bounds).
 *
 * @param seamLine      linha de costura (polígono fechado)
 * @param cutLine       linha de corte = costura + margens (polígono fechado)
 * @param foldLines     trechos que vão na dobra do tecido
 * @param grainline     fio do tecido (de→para); deve ficar paralelo à ourela
 * @param notches       piques (marcas de encaixe) já como segmentos na linha de corte
 * @param internalLines pences, marcações de posição (polilinhas abertas)
 * @param labelAnchor   ponto interno bom para escrever o nome da peça
 */
public record PatternPiece(
        String id,
        String code,
        String name,
        String material,
        int cutQuantity,
        boolean cutOnFold,
        boolean mirrored,
        List<Vec2> seamLine,
        List<Vec2> cutLine,
        List<List<Vec2>> foldLines,
        Segment grainline,
        List<Segment> notches,
        List<List<Vec2>> internalLines,
        List<EdgeInfo> edges,
        Vec2 labelAnchor,
        Bounds bounds
) {
    public record Segment(Vec2 from, Vec2 to) {}

    /** Resumo de cada aresta (nome, tipo e comprimento da linha de costura em mm). */
    public record EdgeInfo(String name, EdgeKind kind, double lengthMm) {}

    /** Instrução de corte legível, ex.: "Cortar 2x (par espelhado)" ou "Cortar 1x na dobra". */
    public String cutInstruction() {
        StringBuilder sb = new StringBuilder("Cortar ").append(cutQuantity).append("x");
        if (cutOnFold) sb.append(" na dobra");
        if (mirrored && cutQuantity > 1) sb.append(" (par espelhado)");
        return sb.toString();
    }
}
