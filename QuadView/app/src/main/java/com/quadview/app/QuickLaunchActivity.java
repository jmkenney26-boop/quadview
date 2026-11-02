package com.quadview.app;

import android.app.Activity;
import android.os.Bundle;

/** "Quad View ▶ Go": opens the last saved setup in one click, then gets out of the way. */
public class QuickLaunchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Multiview.launchAll(this, new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
        boolean anySlot = false;
        Layouts.Layout layout = Layouts.get(Multiview.getLayout(this));
        for (int i = 0; i < layout.count(); i++) {
            if (Multiview.getSlot(this, i) != null) anySlot = true;
        }
        if (!anySlot) finish();
    }
}
