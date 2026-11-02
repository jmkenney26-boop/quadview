package com.quadview.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/** Small drawing of the chosen layout with the app name in each window. */
public class LayoutPreview extends View {

    private Layouts.Layout layout = Layouts.ALL[0];
    private String[] names = new String[Multiview.MAX_SLOTS];

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint screen = new Paint(Paint.ANTI_ALIAS_FLAG);

    public LayoutPreview(Context c) {
        super(c);
        screen.setColor(Color.rgb(20, 20, 24));
        fill.setColor(Color.rgb(38, 70, 120));
        border.setColor(Color.rgb(120, 170, 240));
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(3f);
        text.setColor(Color.WHITE);
        text.setTextAlign(Paint.Align.CENTER);
    }

    public void show(Layouts.Layout layout, String[] names) {
        this.layout = layout;
        this.names = names;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = Math.round(w * 9f / 16f);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        canvas.drawRect(0, 0, w, h, screen);
        float pad = 4f;
        text.setTextSize(h / 16f);
        for (int i = 0; i < layout.count(); i++) {
            float[] f = layout.windows[i];
            RectF r = new RectF(f[0] * w + pad, f[1] * h + pad, f[2] * w - pad, f[3] * h - pad);
            canvas.drawRect(r, fill);
            canvas.drawRect(r, border);
            String label = (i + 1) + "  " + (names[i] == null ? "(empty)" : names[i]);
            float max = r.width() - 12f;
            while (label.length() > 4 && text.measureText(label) > max) {
                label = label.substring(0, label.length() - 2) + "…";
            }
            canvas.drawText(label, r.centerX(), r.centerY() + text.getTextSize() / 3f, text);
        }
    }
}
