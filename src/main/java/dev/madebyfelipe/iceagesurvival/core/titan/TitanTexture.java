package dev.madebyfelipe.iceagesurvival.core.titan;

/**
 * A pele do Titanovenator, derivada em runtime da textura do Tyrannosaurus do Revival (128 × 64): o jar não leva nada
 * dela. Lógica pura sobre pixels ARGB, sem Minecraft.
 *
 * <p>A recoloração segue o desenho aprovado pelo Felipe (dorso carvão, ventre ocre dourado, olhos e boca originais):
 * <ul>
 *   <li>pele escura marrom → carvão, quase neutro;</li>
 *   <li>ventre bege/cinza e a garganta azul → ocre dourado, mais claro conforme o tom original;</li>
 *   <li>olhos verdes e boca vinho ficam como estão; o branco puro dos dentes vira marfim.</li>
 * </ul>
 * É uma aproximação programática da pintura do estudo local: sem as manchas irregulares que o gerador de imagens
 * fez, só a transição que a própria textura do Revival já traz.
 *
 * <p>As cicatrizes são pintadas numa área livre do atlas original (x 24–53, y 0–9), onde nenhuma face do modelo olha.
 */
public final class TitanTexture {
    public static final int WIDTH = 128;
    public static final int HEIGHT = 64;

    private static final int IVORY = rgb(238, 230, 206);
    private static final int[] OCHRE_DARK = {176, 118, 38};
    private static final int[] OCHRE_LIGHT = {230, 178, 84};

    private static final int PALE = rgb(212, 176, 150);
    private static final int EDGE = rgb(86, 58, 54);
    private static final int RAW = rgb(132, 80, 70);

    /** Trecho do atlas sem pixels: o que as faces escondidas dos decalques usam. */
    public static final double[] EMPTY_RECT = {35, 8, 2, 2};

    /** Cada cicatriz: onde está no atlas ({@code x, y, largura, altura}) e o desenho (p = clara, d = sombra, r = ferida). */
    public enum ScarKind {
        /** Três garras paralelas, em diagonal. */
        SLASH(24, 0, new String[] {
                "p.........",
                "dp.p......",
                ".dp.p.....",
                "..dp.p....",
                "...dp.p...",
                "....dp.p..",
                ".....dp.p.",
                "......dp.p",
                ".......dp.",
                ".........."}),
        /** Duas fileiras de perfurações de dentes, com a borda inflamada. */
        BITE(35, 0, new String[] {
                ".dd.dd",
                "dprdpr",
                ".dd.dd",
                "......",
                ".dd.dd",
                "dprdpr",
                ".dd.dd"}),
        /** Corte longo e irregular atravessando o focinho. */
        JAW(42, 0, new String[] {
                "....pp......",
                ".pppdp.pp.p.",
                "pdd..dpddpd.",
                "d....d..d..."});

        private final int x;
        private final int y;
        private final String[] rows;

        ScarKind(int x, int y, String[] rows) {
            this.x = x;
            this.y = y;
            this.rows = rows;
        }

        /** {@code x, y, largura, altura} no atlas, em texels. */
        public double[] rect() {
            return new double[] {x, y, rows[0].length(), rows.length};
        }
    }

    private TitanTexture() {
    }

    /** A pele do Titanovenator a partir dos pixels ARGB da textura do Rex (128 × 64, linha a linha). */
    public static int[] derive(int[] revivalPixels) {
        int[] pixels = recolor(revivalPixels);
        paintScars(pixels);
        return pixels;
    }

    /** Recolore: carvão no lugar do marrom, ocre no lugar do bege e do azul. Devolve uma cópia. */
    public static int[] recolor(int[] source) {
        int[] result = source.clone();
        for (int i = 0; i < result.length; i++) {
            result[i] = recolorPixel(result[i]);
        }
        return result;
    }

    static int recolorPixel(int argb) {
        int alpha = argb >>> 24;
        if (alpha < 10) {
            return argb;
        }
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        float max = Math.max(red, Math.max(green, blue)) / 255.0F;
        float min = Math.min(red, Math.min(green, blue)) / 255.0F;
        float value = max;
        float saturation = max <= 0.0F ? 0.0F : (max - min) / max;
        float hue = hue(red, green, blue, max - min);

        boolean wine = saturation > 0.5F && (hue < 20.0F || hue > 340.0F);
        boolean eye = saturation > 0.4F && hue >= 60.0F && hue <= 170.0F;
        if (wine || eye) {
            return argb;
        }
        if (value > 0.86F && saturation < 0.25F) {
            return withAlpha(IVORY, alpha);
        }
        boolean blueish = saturation > 0.25F && hue >= 170.0F && hue <= 260.0F;
        if (blueish || value >= 0.45F) {
            float t = Math.min(1.0F, Math.max(0.0F, (value - 0.5F) / 0.45F));
            return withAlpha(rgb(lerp(OCHRE_DARK[0], OCHRE_LIGHT[0], t), lerp(OCHRE_DARK[1], OCHRE_LIGHT[1], t),
                    lerp(OCHRE_DARK[2], OCHRE_LIGHT[2], t)), alpha);
        }
        // Carvão quase neutro, só um fio mais frio; o contraste do original encolhe para não virar ruído.
        int gray = Math.round((0.12F + 0.70F * value) * 255.0F);
        return withAlpha(rgb(gray, gray, Math.min(255, gray + 3)), alpha);
    }

    /** Pinta as cicatrizes na área livre do atlas (altera {@code pixels}). */
    public static void paintScars(int[] pixels) {
        for (ScarKind kind : ScarKind.values()) {
            double[] rect = kind.rect();
            for (int row = 0; row < kind.rows.length; row++) {
                for (int column = 0; column < kind.rows[row].length(); column++) {
                    int color = switch (kind.rows[row].charAt(column)) {
                        case 'p' -> PALE;
                        case 'd' -> EDGE;
                        case 'r' -> RAW;
                        default -> 0;
                    };
                    if (color != 0) {
                        pixels[((int) rect[1] + row) * WIDTH + (int) rect[0] + column] = color;
                    }
                }
            }
        }
    }

    private static float hue(int red, int green, int blue, float delta) {
        if (delta <= 0.0F) {
            return 0.0F;
        }
        float r = red / 255.0F;
        float g = green / 255.0F;
        float b = blue / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float h;
        if (max == r) {
            h = ((g - b) / delta) % 6.0F;
        } else if (max == g) {
            h = (b - r) / delta + 2.0F;
        } else {
            h = (r - g) / delta + 4.0F;
        }
        h *= 60.0F;
        return h < 0.0F ? h + 360.0F : h;
    }

    private static int lerp(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }

    private static int rgb(int red, int green, int blue) {
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static int withAlpha(int rgb, int alpha) {
        return alpha << 24 | (rgb & 0xFFFFFF);
    }
}
