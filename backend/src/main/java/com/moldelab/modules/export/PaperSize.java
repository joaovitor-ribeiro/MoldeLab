package com.moldelab.modules.export;

/** Tamanhos de saída. Rolos de plotter têm largura fixa e comprimento livre. */
public enum PaperSize {
    A4(210, 297, false),
    LETTER(215.9, 279.4, false),
    A3(297, 420, false),
    A0(841, 1189, false),
    PLOTTER_914(914, 0, true),
    PLOTTER_1067(1067, 0, true);

    private final double widthMm;
    private final double heightMm;
    private final boolean roll;

    PaperSize(double widthMm, double heightMm, boolean roll) {
        this.widthMm = widthMm;
        this.heightMm = heightMm;
        this.roll = roll;
    }

    public double widthMm() { return widthMm; }
    public double heightMm() { return heightMm; }
    public boolean roll() { return roll; }
}
