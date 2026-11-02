package com.quadview.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Lists every screen inside the box's settings apps and lets you try opening each one. */
public class ExplorerActivity extends Activity {

    private static final String[] PRIORITY_WORDS = {
            "develop", "freeform", "desktop", "window", "debug", "adb", "multi", "resiz", "hidden", "engineer", "factory"
    };

    private TextView result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(12, 12, 16));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(40);
        root.setPadding(pad, dp(24), pad, dp(24));
        scroll.addView(root);

        TextView title = text("Settings Explorer", 28);
        title.setTextColor(Color.WHITE);
        root.addView(title);
        root.addView(text("Every screen found inside this box's settings apps. Highlighted ones are the most "
                + "likely to lead to Developer Options or window settings. Click one to try opening it. "
                + "Most will be blocked; you only need one to work.", 14));

        int dev = Multiview.globalSetting(this, "development_settings_enabled");
        root.addView(text("Developer options flag: " + (dev == 1 ? "ON" : dev == 0 ? "OFF" : "unknown"), 14));

        result = text("", 15);
        result.setTextColor(Color.rgb(255, 190, 90));
        result.setPadding(0, dp(8), 0, dp(8));
        root.addView(result);

        List<String[]> screens = findScreens();
        root.addView(text(screens.size() + " screens found.", 14));

        for (final String[] s : screens) {
            final String pkg = s[0];
            final String cls = s[1];
            boolean hot = isPriority(cls);
            String shortName = cls.substring(cls.lastIndexOf('.') + 1).replace('$', ' ');
            Button b = new Button(this);
            b.setAllCaps(false);
            b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            b.setText((hot ? "★ " : "") + shortName + "\n" + pkg);
            b.setTextSize(TypedValue.COMPLEX_UNIT_SP, hot ? 15 : 13);
            if (hot) b.setTextColor(Color.rgb(255, 210, 120));
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tryOpen(pkg, cls);
                }
            });
            root.addView(b);
        }

        setContentView(scroll);
    }

    private void tryOpen(String pkg, String cls) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setClassName(pkg, cls);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            result.setText("Opened " + cls + ". If it closed right away, the box blocked it.");
        } catch (Throwable t) {
            result.setText("Blocked: " + cls.substring(cls.lastIndexOf('.') + 1) + " (" + t.getClass().getSimpleName() + ")");
        }
    }

    private List<String[]> findScreens() {
        PackageManager pm = getPackageManager();
        List<String[]> out = new ArrayList<>();
        List<ApplicationInfo> apps;
        try {
            apps = pm.getInstalledApplications(0);
        } catch (Throwable t) {
            apps = new ArrayList<>();
        }
        for (ApplicationInfo app : apps) {
            String p = app.packageName.toLowerCase();
            if (!(p.contains("settings") || p.contains("setting") || p.contains("tvsetting")
                    || p.contains("droidlogic") || p.contains("factory") || p.contains("engineer"))) continue;
            try {
                PackageInfo info = pm.getPackageInfo(app.packageName, PackageManager.GET_ACTIVITIES);
                if (info.activities == null) continue;
                for (ActivityInfo a : info.activities) {
                    out.add(new String[]{app.packageName, a.name});
                }
            } catch (Throwable ignored) {
                // Package hid its screen list.
            }
        }
        Collections.sort(out, new Comparator<String[]>() {
            @Override
            public int compare(String[] a, String[] b) {
                boolean pa = isPriority(a[1]);
                boolean pb = isPriority(b[1]);
                if (pa != pb) return pa ? -1 : 1;
                return a[1].compareToIgnoreCase(b[1]);
            }
        });
        return out;
    }

    private static boolean isPriority(String cls) {
        String c = cls.toLowerCase();
        for (String w : PRIORITY_WORDS) if (c.contains(w)) return true;
        return false;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private TextView text(String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(Color.rgb(200, 200, 210));
        return t;
    }
}
