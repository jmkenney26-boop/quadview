package com.quadview.app;

/** Screen layouts. Each window is {left, top, right, bottom} as fractions of the screen. */
public final class Layouts {

    public static final class Layout {
        public final String name;
        public final float[][] windows;

        Layout(String name, float[][] windows) {
            this.name = name;
            this.windows = windows;
        }

        public int count() {
            return windows.length;
        }
    }

    private static final float T = 1f / 3f;      // one third
    private static final float TT = 2f / 3f;     // two thirds

    public static final Layout[] ALL = {
        new Layout("2 × 2 Grid", new float[][]{
            {0f, 0f, .5f, .5f},
            {.5f, 0f, 1f, .5f},
            {0f, .5f, .5f, 1f},
            {.5f, .5f, 1f, 1f},
        }),
        new Layout("1 Big + 3 Side", new float[][]{
            {0f, 0f, TT, 1f},
            {TT, 0f, 1f, T},
            {TT, T, 1f, TT},
            {TT, TT, 1f, 1f},
        }),
        new Layout("1 Big + 3 Bottom", new float[][]{
            {T / 2f, 0f, 1f - T / 2f, TT},
            {0f, TT, T, 1f},
            {T, TT, TT, 1f},
            {TT, TT, 1f, 1f},
        }),
        new Layout("1 Big + 2 Side", new float[][]{
            {0f, 0f, TT, 1f},
            {TT, 0f, 1f, .5f},
            {TT, .5f, 1f, 1f},
        }),
        new Layout("Side by Side (2)", new float[][]{
            {0f, .25f, .5f, .75f},
            {.5f, .25f, 1f, .75f},
        }),
    };

    public static Layout get(int index) {
        if (index < 0 || index >= ALL.length) return ALL[0];
        return ALL[index];
    }

    private Layouts() {}
}
