package com.quadview.app;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * When armed, watches the Settings app. The instant the Developer Options screen draws anything,
 * it clicks the Developer options master switch and the freeform switches before the box can close it.
 */
public class UnlockHelperService extends AccessibilityService {

    static final String PREFS = "unlock_helper";

    /** Labels to switch ON, lower case. The first ones turn on Developer options itself. */
    private static final String[] TARGETS = {
            "use developer options",
            "developer options",
            "enable freeform windows",
            "freeform window",
            "freeform",
            "force activities to be resizable",
            "resizable",
            "force desktop mode",
            "desktop mode",
            "usb debugging",
    };

    private final Set<String> clicked = new HashSet<>();
    private long lastArm;

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static boolean isArmed(Context c) {
        return System.currentTimeMillis() < prefs(c).getLong("armed_until", 0);
    }

    static void log(Context c, String line) {
        SharedPreferences p = prefs(c);
        String old = p.getString("log", "");
        if (old.length() > 6000) old = old.substring(old.length() - 6000);
        p.edit().putString("log", old + line + "\n").apply();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || !isArmed(this)) return;
        CharSequence pkg = event.getPackageName();
        if (pkg == null || !pkg.toString().contains("settings")) return;

        long armedAt = prefs(this).getLong("armed_at", 0);
        if (armedAt != lastArm) {
            lastArm = armedAt;
            clicked.clear();
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            log(this, "screen: " + event.getClassName());
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        scan(root, 0);
    }

    private void scan(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 40) return;
        CharSequence t = node.getText();
        if (t != null && t.length() > 0) {
            String text = t.toString().trim();
            String lower = text.toLowerCase(Locale.US);
            SharedPreferences p = prefs(this);
            String seen = p.getString("seen", "");
            if (!seen.contains("|" + text + "|") && seen.length() < 4000) {
                p.edit().putString("seen", seen + "|" + text + "|").apply();
            }
            for (String target : TARGETS) {
                if (lower.contains(target) && !clicked.contains(target) && lower.length() < 60) {
                    if (switchOn(node)) {
                        clicked.add(target);
                        log(this, "CLICKED: " + text);
                    }
                    break;
                }
            }
        }
        if (node.isScrollable() && depth < 6) {
            node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            scan(node.getChild(i), depth + 1);
        }
    }

    /** Clicks the row for this label, unless its switch is already on. */
    private boolean switchOn(AccessibilityNodeInfo label) {
        AccessibilityNodeInfo row = label;
        for (int i = 0; i < 5 && row != null && !row.isClickable(); i++) row = row.getParent();
        if (row == null) return false;
        AccessibilityNodeInfo toggle = findCheckable(row, 0);
        if (toggle != null && toggle.isChecked()) {
            log(this, "already on: " + label.getText());
            return true;
        }
        if (toggle != null && toggle.isClickable() && toggle.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true;
        }
        return row.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    private AccessibilityNodeInfo findCheckable(AccessibilityNodeInfo n, int depth) {
        if (n == null || depth > 6) return null;
        if (n.isCheckable()) return n;
        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo f = findCheckable(n.getChild(i), depth + 1);
            if (f != null) return f;
        }
        return null;
    }

    @Override
    public void onInterrupt() {
    }
}
