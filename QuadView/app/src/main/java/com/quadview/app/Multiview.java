package com.quadview.app;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.widget.Toast;

import java.lang.reflect.Method;

/** Saved settings plus the code that opens apps in windows. */
public final class Multiview {

    public static final int MAX_SLOTS = 4;
    private static final int WINDOWING_MODE_FREEFORM = 5;
    private static final long DELAY_BETWEEN_APPS_MS = 1200;

    // ---------- saved settings ----------

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("quadview", Context.MODE_PRIVATE);
    }

    public static int getLayout(Context c) {
        return prefs(c).getInt("layout", 0);
    }

    public static void setLayout(Context c, int layout) {
        prefs(c).edit().putInt("layout", layout).apply();
    }

    /** Package name in a slot, or null if empty. */
    public static String getSlot(Context c, int slot) {
        return prefs(c).getString("slot" + slot, null);
    }

    public static void setSlot(Context c, int slot, String pkg) {
        prefs(c).edit().putString("slot" + slot, pkg).apply();
    }

    public static boolean getGaps(Context c) {
        return prefs(c).getBoolean("gaps", true);
    }

    public static void setGaps(Context c, boolean on) {
        prefs(c).edit().putBoolean("gaps", on).apply();
    }

    // ---------- freeform status ----------

    /** 1 = on, 0 = off, -1 = can't tell on this device. */
    public static int globalSetting(Context c, String key) {
        try {
            return Settings.Global.getInt(c.getContentResolver(), key, 0) != 0 ? 1 : 0;
        } catch (Throwable t) {
            return -1;
        }
    }

    public static String appLabel(Context c, String pkg) {
        if (pkg == null) return "(empty)";
        try {
            PackageManager pm = c.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Throwable t) {
            return pkg;
        }
    }

    // ---------- launching ----------

    /** Opens every filled slot of the saved layout, one after another. */
    public static void launchAll(final Activity act, final Runnable whenDone) {
        Layouts.Layout layout = Layouts.get(getLayout(act));

        DisplayMetrics dm = new DisplayMetrics();
        act.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        int w = dm.widthPixels;
        int h = dm.heightPixels;
        int gap = getGaps(act) ? Math.max(2, Math.round(w / 480f)) : 0;

        Handler handler = new Handler(Looper.getMainLooper());
        long delay = 0;
        int launched = 0;

        for (int i = 0; i < layout.count(); i++) {
            final String pkg = getSlot(act, i);
            if (pkg == null) continue;
            float[] f = layout.windows[i];
            final Rect r = new Rect(
                    Math.round(f[0] * w) + gap,
                    Math.round(f[1] * h) + gap,
                    Math.round(f[2] * w) - gap,
                    Math.round(f[3] * h) - gap);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    openInWindow(act, pkg, r);
                }
            }, delay);
            delay += DELAY_BETWEEN_APPS_MS;
            launched++;
        }

        if (launched == 0) {
            Toast.makeText(act, "Pick at least one app first.", Toast.LENGTH_LONG).show();
            return;
        }
        if (whenDone != null) handler.postDelayed(whenDone, delay);
    }

    private static void openInWindow(Context c, String pkg, Rect bounds) {
        PackageManager pm = c.getPackageManager();
        Intent intent = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (intent == null) intent = pm.getLaunchIntentForPackage(pkg);
        if (intent == null) {
            Toast.makeText(c, "Can't open " + appLabel(c, pkg), Toast.LENGTH_LONG).show();
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        ActivityOptions options = ActivityOptions.makeBasic();
        try {
            // Hidden Android call that asks for a floating window (same trick Taskbar uses).
            Method m = ActivityOptions.class.getMethod("setLaunchWindowingMode", int.class);
            m.invoke(options, WINDOWING_MODE_FREEFORM);
        } catch (Throwable ignored) {
            // Older or locked-down builds: launch bounds alone still work when freeform is on.
        }
        options.setLaunchBounds(bounds);

        try {
            c.startActivity(intent, options.toBundle());
        } catch (Throwable t) {
            Toast.makeText(c, "Couldn't open " + appLabel(c, pkg) + ": " + t.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private Multiview() {}
}
