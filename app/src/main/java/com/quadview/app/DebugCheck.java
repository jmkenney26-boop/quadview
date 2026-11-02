package com.quadview.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Reads the box's debug settings and checks whether ADB is listening anywhere. */
public final class DebugCheck {

    private static final String[] PROPS = {
            "ro.build.type",
            "ro.debuggable",
            "ro.secure",
            "ro.adb.secure",
            "init.svc.adbd",
            "sys.usb.config",
            "persist.sys.usb.config",
            "sys.usb.state",
            "service.adb.tcp.port",
            "persist.adb.tcp.port",
            "persist.adb.tls_server.enable",
    };

    private static final int[] PORTS = {5555, 5556, 5557, 5037, 4444, 23, 8080};

    public static void run(final Activity act) {
        final AlertDialog dialog = new AlertDialog.Builder(act)
                .setTitle("Box debug check")
                .setMessage("Checking… (takes a few seconds)")
                .setPositiveButton("OK", null)
                .show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final String report = buildReport();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        dialog.setMessage(report);
                    }
                });
            }
        }).start();
    }

    private static String buildReport() {
        StringBuilder sb = new StringBuilder();
        String adbd = prop("init.svc.adbd");
        String usb = prop("sys.usb.config") + " " + prop("persist.sys.usb.config");
        String tcp = prop("service.adb.tcp.port");

        StringBuilder ports = new StringBuilder();
        boolean anyOpen = false;
        for (int p : PORTS) {
            boolean open = portOpen(p);
            if (open) anyOpen = true;
            ports.append("  port ").append(p).append(": ").append(open ? "OPEN" : "closed").append('\n');
        }

        sb.append("VERDICT\n");
        if ("running".equals(adbd) && usb.contains("adb")) {
            sb.append("✔ ADB is running on USB. A cable to your laptop has a good chance of working.\n");
        } else if ("running".equals(adbd)) {
            sb.append("~ ADB is running, but USB may not be set to use it. A cable might still work.\n");
        } else if (adbd.isEmpty()) {
            sb.append("? The box hides whether ADB is running. A cable is the only way to find out.\n");
        } else {
            sb.append("✘ ADB is turned off (" + adbd + "). A cable probably won't work.\n");
        }
        if (anyOpen) sb.append("✔ Something is listening on this box's network ports (see below). Send Claude a photo.\n");
        sb.append('\n');

        sb.append("DETAILS\n");
        for (String k : PROPS) {
            String v = prop(k);
            sb.append("  ").append(k).append(" = ").append(v.isEmpty() ? "(hidden/empty)" : v).append('\n');
        }
        sb.append("\nLOCAL PORTS\n").append(ports);
        if (!tcp.isEmpty()) sb.append("\nADB network port setting: ").append(tcp).append('\n');
        return sb.toString();
    }

    private static boolean portOpen(int port) {
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress("127.0.0.1", port), 400);
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            try {
                s.close();
            } catch (Throwable ignored) {
            }
        }
    }

    private static String prop(String key) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            Object v = get.invoke(null, key);
            return v == null ? "" : v.toString().trim();
        } catch (Throwable t) {
            return "";
        }
    }

    private DebugCheck() {}
}
