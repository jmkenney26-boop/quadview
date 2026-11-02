package com.quadview.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

/** Arms the accessibility helper, opens the hidden Developer Options screen, then reports what happened. */
public final class AutoUnlock {

    private static final long RUN_MS = 9000;

    static boolean helperEnabled(Activity act) {
        try {
            String on = Settings.Secure.getString(act.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return on != null && on.contains(act.getPackageName());
        } catch (Throwable t) {
            return false;
        }
    }

    public static void start(final Activity act) {
        if (!helperEnabled(act)) {
            new AlertDialog.Builder(act)
                    .setTitle("Turn on the Quad View helper first")
                    .setMessage("1. Press OK to open Accessibility settings.\n"
                            + "2. Find \"Quad View Unlock Helper\" (it may be under Services).\n"
                            + "3. Turn it ON and accept the warning.\n"
                            + "4. Come back to Quad View and press Auto-unlock again.\n\n"
                            + "If Accessibility isn't on the menu, try Settings → Device Preferences → Accessibility.")
                    .setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(android.content.DialogInterface d, int w) {
                            openAccessibility(act);
                        }
                    })
                    .show();
            return;
        }

        long now = System.currentTimeMillis();
        UnlockHelperService.prefs(act).edit()
                .putLong("armed_at", now)
                .putLong("armed_until", now + RUN_MS)
                .putString("log", "")
                .putString("seen", "")
                .apply();

        Handler h = new Handler(Looper.getMainLooper());
        // Open the hidden screen several times in case the first attempt is too fast to catch.
        for (int i = 0; i < 3; i++) {
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    openDev(act);
                }
            }, 300 + i * 2500L);
        }
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                report(act);
            }
        }, RUN_MS + 500);
    }

    private static void openDev(Activity act) {
        String[][] screens = {
                {"com.android.tv.settings", "com.android.tv.settings.system.development.DevelopmentActivity"},
                {"com.android.settings", "com.android.settings.Settings$DevelopmentSettingsDashboardActivity"},
        };
        for (String[] s : screens) {
            try {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.setClassName(s[0], s[1]);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                act.startActivity(i);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private static void report(Activity act) {
        Intent back = new Intent(act, MainActivity.class);
        back.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        try {
            act.startActivity(back);
        } catch (Throwable ignored) {
        }

        String log = UnlockHelperService.prefs(act).getString("log", "");
        String seen = UnlockHelperService.prefs(act).getString("seen", "").replace("||", "\n").replace("|", "");
        int dev = Multiview.globalSetting(act, "development_settings_enabled");
        int free = Multiview.globalSetting(act, "enable_freeform_support");
        int res = Multiview.globalSetting(act, "force_resizable_activities");

        String verdict;
        if (free == 1 && res == 1) {
            verdict = "✔ SUCCESS! Freeform is ON. Restart the box, then press Launch Multiview.";
        } else if (dev == 1) {
            verdict = "✔ Developer options got switched ON. The Developer Options screen should now stay open: "
                    + "press Find hidden Developer Options and turn on the freeform switches by hand.";
        } else if (log.contains("CLICKED")) {
            verdict = "~ The helper clicked something, but the settings didn't change. Send Claude a photo of this.";
        } else if (seen.trim().isEmpty()) {
            verdict = "✘ The screen closed before it showed anything, so there was nothing to click.";
        } else {
            verdict = "~ The helper saw the screen but didn't find the switches. Send Claude a photo of this.";
        }

        new AlertDialog.Builder(act)
                .setTitle("Auto-unlock result")
                .setMessage(verdict
                        + "\n\nDeveloper options: " + word(dev)
                        + "\nFreeform windows: " + word(free)
                        + "\nForce resizable: " + word(res)
                        + "\n\nWHAT THE HELPER DID\n" + (log.isEmpty() ? "(nothing)\n" : log)
                        + "\nTEXT IT SAW ON SCREEN\n" + (seen.trim().isEmpty() ? "(nothing)" : seen))
                .setPositiveButton("OK", null)
                .show();
    }

    private static String word(int v) {
        return v == 1 ? "ON" : v == 0 ? "OFF" : "unknown";
    }

    static void openAccessibility(Activity act) {
        try {
            act.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Throwable t) {
            try {
                act.startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (Throwable ignored) {
            }
        }
    }

    private AutoUnlock() {}
}
