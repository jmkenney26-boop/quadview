package com.quadview.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Setup screen: choose a layout, pick an app for each window, launch. */
public class MainActivity extends Activity {

    private LinearLayout layoutRow;
    private LinearLayout slotList;
    private LayoutPreview preview;
    private TextView status;
    private Button gapsButton;
    private Button launchButton;

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

        TextView title = text("Quad View", 30);
        title.setTextColor(Color.WHITE);
        root.addView(title);
        root.addView(text("Watch up to 4 apps at once. Pick a layout, pick the apps, press Launch.", 15));

        status = text("", 14);
        status.setPadding(0, dp(10), 0, dp(10));
        root.addView(status);

        // Columns: controls on the left, preview on the right.
        LinearLayout columns = new LinearLayout(this);
        columns.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(columns);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        columns.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setPadding(dp(24), 0, 0, 0);
        columns.addView(right, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        left.addView(heading("Layout"));
        layoutRow = new LinearLayout(this);
        layoutRow.setOrientation(LinearLayout.VERTICAL);
        left.addView(layoutRow);

        left.addView(heading("Apps"));
        slotList = new LinearLayout(this);
        slotList.setOrientation(LinearLayout.VERTICAL);
        left.addView(slotList);

        right.addView(heading("Preview"));
        preview = new LayoutPreview(this);
        right.addView(preview, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        launchButton = button("▶  Launch Multiview", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Multiview.launchAll(MainActivity.this, new Runnable() {
                    @Override
                    public void run() {
                        moveTaskToBack(true);
                    }
                });
            }
        });
        launchButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        right.addView(spacer(16));
        right.addView(launchButton);

        gapsButton = button("", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Multiview.setGaps(MainActivity.this, !Multiview.getGaps(MainActivity.this));
                refresh();
            }
        });
        right.addView(gapsButton);

        right.addView(button("Find hidden Developer Options", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findDeveloperOptions();
            }
        }));

        right.addView(button("★ Settings Explorer (search hidden screens)", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ExplorerActivity.class));
            }
        }));

        right.addView(button("Open full Android Settings (phone style)", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFirst("full Settings",
                        component("com.android.settings", "com.android.settings.Settings"),
                        component("com.android.settings", "com.android.settings.Settings$MyDeviceInfoActivity"),
                        component("com.android.settings", "com.android.settings.Settings$DeviceInfoSettingsActivity"),
                        new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS));
            }
        }));

        right.addView(spacer(12));
        right.addView(text(
                "Tips:\n"
                + "• Use a mouse or air-mouse to click inside the windows.\n"
                + "• Mute every app except the one you want to hear.\n"
                + "• If an app opens full screen, close it from Recents and launch again.\n"
                + "• A black window with sound means that app blocks windowed playback.\n"
                + "• \"Quad View ▶ Go\" on your home screen launches your last setup in one click.",
                13));

        setContentView(scroll);
        refresh();
        launchButton.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        int current = Multiview.getLayout(this);
        Layouts.Layout layout = Layouts.get(current);

        layoutRow.removeAllViews();
        for (int i = 0; i < Layouts.ALL.length; i++) {
            final int index = i;
            String mark = i == current ? "●  " : "○  ";
            layoutRow.addView(button(mark + Layouts.ALL[i].name, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Multiview.setLayout(MainActivity.this, index);
                    refresh();
                }
            }));
        }

        String[] names = new String[Multiview.MAX_SLOTS];
        slotList.removeAllViews();
        for (int i = 0; i < layout.count(); i++) {
            final int slot = i;
            String pkg = Multiview.getSlot(this, i);
            names[i] = pkg == null ? null : Multiview.appLabel(this, pkg);
            slotList.addView(button("Window " + (i + 1) + ":  " + (names[i] == null ? "choose app…" : names[i]),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            pickApp(slot);
                        }
                    }));
        }
        preview.show(layout, names);

        gapsButton.setText(Multiview.getGaps(this) ? "Borders between windows: ON" : "Borders between windows: OFF");

        int resizable = Multiview.globalSetting(this, "force_resizable_activities");
        int freeform = Multiview.globalSetting(this, "enable_freeform_support");
        StringBuilder s = new StringBuilder("Setup check:  ");
        s.append("Force resizable ").append(word(resizable)).append("   ·   ");
        s.append("Freeform windows ").append(word(freeform));
        if (resizable == 0 || freeform == 0) {
            s.append("\nTurn both on in Developer Options, then restart the box, or windows will open full screen.");
            status.setTextColor(Color.rgb(255, 190, 90));
        } else {
            status.setTextColor(Color.rgb(140, 220, 150));
        }
        status.setText(s);
    }

    private static String word(int v) {
        return v == 1 ? "ON" : v == 0 ? "OFF" : "unknown";
    }

    private void pickApp(final int slot) {
        PackageManager pm = getPackageManager();
        Map<String, String> apps = new LinkedHashMap<>();
        String[] categories = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String cat : categories) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || apps.containsKey(pkg)) continue;
                apps.put(pkg, ri.loadLabel(pm).toString());
            }
        }

        final List<Map.Entry<String, String>> list = new ArrayList<>(apps.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, String>>() {
            @Override
            public int compare(Map.Entry<String, String> a, Map.Entry<String, String> b) {
                return a.getValue().compareToIgnoreCase(b.getValue());
            }
        });

        CharSequence[] labels = new CharSequence[list.size() + 1];
        labels[0] = "(leave empty)";
        for (int i = 0; i < list.size(); i++) labels[i + 1] = list.get(i).getValue();

        new AlertDialog.Builder(this)
                .setTitle("App for window " + (slot + 1))
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        Multiview.setSlot(MainActivity.this, slot,
                                which == 0 ? null : list.get(which - 1).getKey());
                        refresh();
                    }
                })
                .show();
    }

    // ---------- hidden settings screens ----------

    /** Tries every known way into Developer Options, including screens the box's menus hide. */
    private void findDeveloperOptions() {
        openFirst("Developer Options",
                component("com.android.tv.settings",
                        "com.android.tv.settings.system.development.DevelopmentActivity"),
                component("com.android.settings",
                        "com.android.settings.Settings$DevelopmentSettingsDashboardActivity"),
                component("com.android.settings",
                        "com.android.settings.Settings$DevelopmentSettingsActivity"),
                component("com.android.settings", "com.android.settings.DevelopmentSettings"),
                new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
    }

    private static Intent component(String pkg, String cls) {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setClassName(pkg, cls);
        return i;
    }

    private void openFirst(String what, Intent... candidates) {
        for (Intent i : candidates) {
            try {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return;
            } catch (Throwable ignored) {
                // Not on this box, or blocked. Try the next one.
            }
        }
        toast("This box blocks every route into " + what + ".");
    }

    // ---------- small view helpers ----------

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

    private TextView heading(String s) {
        TextView t = text(s, 18);
        t.setTextColor(Color.WHITE);
        t.setPadding(0, dp(14), 0, dp(6));
        return t;
    }

    private Button button(String label, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        b.setOnClickListener(click);
        return b;
    }

    private View spacer(int heightDp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(heightDp)));
        return v;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
