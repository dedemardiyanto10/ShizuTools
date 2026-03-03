package com.toolbox.shizutools;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.ContextThemeWrapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import rikka.shizuku.Shizuku;
import com.toolbox.shizutools.databinding.LayoutFloatingLogcatBinding;

public class LogcatService extends Service {
    private WindowManager windowManager;
    private LayoutFloatingLogcatBinding binding;
    private boolean isRunning = true;
    private String targetPackage = "";
    private Handler monitorHandler = new Handler(Looper.getMainLooper());
    private java.lang.Process currentProcess;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("package_filter")) {
            targetPackage = intent.getStringExtra("package_filter");
            if (binding != null) binding.txtFloatingLog.setText("");
            startAppMonitoring();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sendServiceStatus(true);

        ContextThemeWrapper contextWrapper = new ContextThemeWrapper(this, R.style.AppTheme);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        binding = LayoutFloatingLogcatBinding.inflate(LayoutInflater.from(contextWrapper));

        float density = getResources().getDisplayMetrics().density;
        int widthPx = (int) (280 * density);
        int heightPx = (int) (340 * density);

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        widthPx,
                        heightPx,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                                : WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;

        windowManager.addView(binding.getRoot(), params);
        setupDragListener(params);

        binding.btnCloseLog.setOnClickListener(v -> stopSelf());
    }

    private void startAppMonitoring() {
        monitorHandler.removeCallbacksAndMessages(null);
        monitorHandler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (!isRunning || binding == null) return;
                        String currentApp = getForegroundPackage();

                        if (currentApp.equals(targetPackage)) {
                            if (currentProcess == null) {
                                binding.txtFloatingLog.append(
                                        "--- Monitoring: " + targetPackage + " ---\n");
                                startShizukuLogcat();
                            }
                        } else {
                            if (currentProcess != null) {
                                binding.txtFloatingLog.append("--- Paused (App Background) ---\n");
                                stopShizukuLogcat();
                            }
                        }
                        monitorHandler.postDelayed(this, 2000);
                    }
                },
                1000);
    }

    private String getForegroundPackage() {
        try {
            android.app.usage.UsageStatsManager usm =
                    (android.app.usage.UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            java.util.List<android.app.usage.UsageStats> stats =
                    usm.queryUsageStats(
                            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
                            time - 1000 * 60,
                            time);
            if (stats != null && !stats.isEmpty()) {
                android.app.usage.UsageStats lastApp = null;
                for (android.app.usage.UsageStats s : stats) {
                    if (lastApp == null || s.getLastTimeUsed() > lastApp.getLastTimeUsed())
                        lastApp = s;
                }
                return lastApp.getPackageName();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void startShizukuLogcat() {
        new Thread(
                        () -> {
                            try {
                                Method newProcessMethod =
                                        Shizuku.class.getDeclaredMethod(
                                                "newProcess",
                                                String[].class,
                                                String[].class,
                                                String.class);
                                newProcessMethod.setAccessible(true);

                                String[] cmd = {
                                    "sh", "-c", "logcat -v time | grep " + targetPackage
                                };
                                currentProcess =
                                        (java.lang.Process)
                                                newProcessMethod.invoke(null, cmd, null, null);

                                BufferedReader reader =
                                        new BufferedReader(
                                                new InputStreamReader(
                                                        currentProcess.getInputStream()));
                                String line;
                                while (isRunning
                                        && currentProcess != null
                                        && (line = reader.readLine()) != null) {
                                    final String finalLine = line;
                                    new Handler(Looper.getMainLooper())
                                            .post(
                                                    () -> {
                                                        if (binding != null) {
                                                            binding.txtFloatingLog.append(
                                                                    finalLine + "\n");
                                                            binding.logScroll.post(
                                                                    () ->
                                                                            binding.logScroll
                                                                                    .fullScroll(
                                                                                            View
                                                                                                    .FOCUS_DOWN));

                                                            // Auto Clear biar gak berat
                                                            if (binding.txtFloatingLog
                                                                            .getLineCount()
                                                                    > 200) {
                                                                binding.txtFloatingLog.setText(
                                                                        "--- Log Refreshed ---\n");
                                                            }
                                                        }
                                                    });
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        })
                .start();
    }

    private void stopShizukuLogcat() {
        if (currentProcess != null) {
            currentProcess.destroy();
            currentProcess = null;
        }
    }

    private void setupDragListener(WindowManager.LayoutParams params) {
        binding.logHeader.setOnTouchListener(
                new View.OnTouchListener() {
                    private int initialX, initialY;
                    private float initialTouchX, initialTouchY;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                initialX = params.x;
                                initialY = params.y;
                                initialTouchX = event.getRawX();
                                initialTouchY = event.getRawY();
                                return true;
                            case MotionEvent.ACTION_MOVE:
                                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                                windowManager.updateViewLayout(binding.getRoot(), params);
                                return true;
                        }
                        return false;
                    }
                });
    }

    private void sendServiceStatus(boolean active) {
    Intent intent = new Intent("LOGCAT_STATUS_CHANGED");
    intent.putExtra("active", active);
    sendBroadcast(intent);
}

    @Override
    public void onDestroy() {
        isRunning = false;
        monitorHandler.removeCallbacksAndMessages(null);
        sendServiceStatus(false);
        stopShizukuLogcat();
        if (windowManager != null && binding != null) {
            windowManager.removeView(binding.getRoot());
        }
        super.onDestroy();
    }
}
