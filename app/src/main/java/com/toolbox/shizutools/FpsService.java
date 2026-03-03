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
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import rikka.shizuku.Shizuku;

public class FpsService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private TextView fpsText;
    private boolean isRunning = true;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        
        sendFpsStatus(true);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_fps, null);
        fpsText = floatingView.findViewById(R.id.fpsText);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.END;
        params.x = 0;
        params.y = 0;

        windowManager.addView(floatingView, params);
        setupDrag(params);

        startSurfaceFlingerMonitoring();
    }

    private void startSurfaceFlingerMonitoring() {
        new Thread(() -> {
            try {
                Method newProcessMethod = Shizuku.class.getDeclaredMethod(
                        "newProcess", String[].class, String[].class, String.class);
                newProcessMethod.setAccessible(true);

                String[] cmd = {"sh", "-c", "while true; do dumpsys SurfaceFlinger --latency-clear; sleep 1; dumpsys SurfaceFlinger --latency | head -n 1; done"};
                
                java.lang.Process process = (java.lang.Process) newProcessMethod.invoke(null, cmd, null, null);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                while (isRunning) {
                    String line = reader.readLine();
                    if (line != null && !line.isEmpty()) {
                        try {
                            long refreshPeriodNanos = Long.parseLong(line.trim());
                            if (refreshPeriodNanos > 0) {
                                float fps = 1000000000.0f / refreshPeriodNanos;
                                
                                mainHandler.post(() -> {
                                    if (fpsText != null) {
                                        fpsText.setText(String.format("%.0f FPS", fps));
                                        fpsText.setTextColor(fps < 50 ? 0xFFFF5252 : 0xFF00FF00);
                                    }
                                });
                            }
                        } catch (Exception e) {}
                    }
                    Thread.sleep(800); 
                }
                process.destroy();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setupDrag(WindowManager.LayoutParams params) {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
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
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });
    }

    private void sendFpsStatus(boolean active) {
        Intent intent = new Intent("FPS_STATUS_CHANGED");
        intent.putExtra("active", active);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        sendFpsStatus(false);
        if (floatingView != null) windowManager.removeView(floatingView);
        super.onDestroy();
    }
}
