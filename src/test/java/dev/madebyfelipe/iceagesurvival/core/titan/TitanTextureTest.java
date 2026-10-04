package dev.madebyfelipe.iceagesurvival.core.titan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TitanTextureTest {
    private static int argb(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int red(int p) {
        return p >> 16 & 0xFF;
    }

    private static int green(int p) {
        return p >> 8 & 0xFF;
    }

    private static int blue(int p) {
        return p & 0xFF;
    }

    @Test
    void darkBrownSkinBecomesNearlyNeutralCharcoal() {
        int out = TitanTexture.recolorPixel(argb(255, 60, 40, 36));
        assertTrue(Math.abs(red(out) - green(out)) <= 3 && Math.abs(blue(out) - green(out)) <= 3, "carvão neutro");
        assertTrue(red(out) < 110, "continua escuro: " + red(out));
        assertEquals(255, out >>> 24);
    }

    @Test
    void beigeBellyAndBlueThroatBecomeGoldenOchre() {
        for (int pixel : new int[] {argb(255, 186, 171, 168), argb(255, 117, 159, 178)}) {
            int out = TitanTexture.recolorPixel(pixel);
            assertTrue(red(out) > green(out) && green(out) > blue(out), "ocre: R > G > B");
            assertTrue(red(out) - blue(out) > 80, "bem saturado");
        }
    }

    @Test
    void lighterOriginalsGiveLighterOchre() {
        int dark = TitanTexture.recolorPixel(argb(255, 156, 136, 132));
        int light = TitanTexture.recolorPixel(argb(255, 211, 201, 199));
        assertTrue(red(light) > red(dark));
    }

    @Test
    void eyesAndMouthKeepTheirColourAndTeethTurnIvory() {
        int eye = argb(255, 142, 160, 48);
        int mouth = argb(255, 126, 40, 40);
        assertEquals(eye, TitanTexture.recolorPixel(eye));
        assertEquals(mouth, TitanTexture.recolorPixel(mouth));
        int tooth = TitanTexture.recolorPixel(argb(255, 255, 255, 255));
        assertTrue(red(tooth) > 220 && blue(tooth) < red(tooth), "marfim, não branco puro");
    }

    @Test
    void transparentPixelsAndAlphaAreKept() {
        assertEquals(0, TitanTexture.recolorPixel(0));
        int translucent = TitanTexture.recolorPixel(argb(128, 60, 40, 36));
        assertEquals(128, translucent >>> 24);
    }

    @Test
    void recolorLeavesTheSourceUntouched() {
        int[] source = {argb(255, 60, 40, 36), argb(255, 186, 171, 168)};
        int[] copy = source.clone();
        TitanTexture.recolor(source);
        assertEquals(copy[0], source[0]);
        assertEquals(copy[1], source[1]);
    }

    @Test
    void scarsOnlyTouchTheFreePatchOfTheAtlas() {
        int[] pixels = new int[TitanTexture.WIDTH * TitanTexture.HEIGHT];
        TitanTexture.paintScars(pixels);
        int painted = 0;
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] != 0) {
                painted++;
                int x = i % TitanTexture.WIDTH;
                int y = i / TitanTexture.WIDTH;
                assertTrue(x >= 24 && x <= 53 && y >= 0 && y <= 9, "pixel de cicatriz fora da área livre: " + x + "," + y);
            }
        }
        assertTrue(painted > 40, "cicatrizes pintadas: " + painted);
        double[] empty = TitanTexture.EMPTY_RECT;
        for (int y = (int) empty[1]; y < empty[1] + empty[3]; y++) {
            for (int x = (int) empty[0]; x < empty[0] + empty[2]; x++) {
                assertEquals(0, pixels[y * TitanTexture.WIDTH + x], "o trecho vazio das faces escondidas foi pintado");
            }
        }
    }

    @Test
    void everyScarTileFitsInsideItsPatchWithoutOverlap() {
        boolean[] used = new boolean[TitanTexture.WIDTH * TitanTexture.HEIGHT];
        for (TitanTexture.ScarKind kind : TitanTexture.ScarKind.values()) {
            double[] r = kind.rect();
            assertTrue(r[0] >= 24 && r[0] + r[2] <= 54 && r[1] + r[3] <= 10, kind + " sai da área livre");
            for (int y = (int) r[1]; y < r[1] + r[3]; y++) {
                for (int x = (int) r[0]; x < r[0] + r[2]; x++) {
                    assertTrue(!used[y * TitanTexture.WIDTH + x], kind + " sobrepõe outra cicatriz");
                    used[y * TitanTexture.WIDTH + x] = true;
                }
            }
        }
    }
}
