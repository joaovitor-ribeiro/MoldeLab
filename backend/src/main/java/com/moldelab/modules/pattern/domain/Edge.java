package com.moldelab.modules.pattern.domain;

import com.moldelab.modules.pattern.geometry.Curves;
import com.moldelab.modules.pattern.geometry.Vec2;

import java.util.List;

/**
 * Um trecho do contorno (linha de costura) de uma peça. As arestas de uma peça são
 * consecutivas: o último ponto de uma é o primeiro da seguinte, e a última fecha na primeira.
 */
public record Edge(String name, EdgeKind kind, List<Vec2> points) {

    public Edge {
        if (points == null || points.size() < 2) throw new IllegalArgumentException("Aresta precisa de >= 2 pontos: " + name);
        points = List.copyOf(points);
    }

    public static Edge seam(String name, List<Vec2> pts) { return new Edge(name, EdgeKind.SEAM, pts); }
    public static Edge hem(String name, List<Vec2> pts) { return new Edge(name, EdgeKind.HEM, pts); }
    public static Edge fold(String name, List<Vec2> pts) { return new Edge(name, EdgeKind.FOLD, pts); }
    public static Edge finish(String name, List<Vec2> pts) { return new Edge(name, EdgeKind.FINISH, pts); }

    public Vec2 start() { return points.getFirst(); }
    public Vec2 end() { return points.getLast(); }
    public double length() { return Curves.length(points); }
}
