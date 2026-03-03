package com.toolbox.shizutools;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.toolbox.shizutools.databinding.ActivityMainBinding;
import com.toolbox.shizutools.databinding.LayoutDialogBatteryBinding;
import com.toolbox.shizutools.databinding.LayoutDialogDpiBinding;
import com.toolbox.shizutools.databinding.LayoutDialogLogcatBinding;
import com.toolbox.shizutools.databinding.LayoutDialogPowerBinding;
import com.toolbox.shizutools.databinding.LayoutDialogRefreshRateBinding;
import com.toolbox.shizutools.databinding.LayoutDialogResolutionBinding;
import rikka.shizuku.Shizuku;
import java.lang.reflect.Method;
import com.toolbox.shizutools.BuildConfig;
import android.text.Html;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private String currentThemeAtStart;
    private boolean lastBlackStatus;

    private final android.content.BroadcastReceiver logcatReceiver =
            new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context context, Intent intent) {
                    boolean isActive = intent.getBooleanExtra("active", false);
                    updateLogcatUI(isActive);
                }
            };

    private final android.content.BroadcastReceiver fpsReceiver =
            new android.content.BroadcastReceiver() {
                @Override
                public void onReceive(android.content.Context context, Intent intent) {
                    boolean isActive = intent.getBooleanExtra("active", false);
                    updateFpsUI(isActive);
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String savedTheme = ThemeHelper.getSavedTheme(this);
        currentThemeAtStart = savedTheme;

        int mode;
        switch (savedTheme) {
            case "Always on":
                mode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case "Always off":
                mode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            default:
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(mode);

        ThemeHelper.applyLanguage(this);

        super.onCreate(savedInstanceState);

        lastBlackStatus = ThemeHelper.isBlackModeEnabled(this);
        DynamicColors.applyToActivityIfAvailable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        setupToolbarSubtitle();

        updateShizukuStatus();

        setupClickListeners();

        updateLogcatUI(isServiceRunning(LogcatService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_about) {
            Intent intent = new Intent(this, InfoActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        android.app.ActivityManager manager =
                (android.app.ActivityManager)
                        getSystemService(android.content.Context.ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void updateLogcatUI(boolean isActive) {
        if (isActive) {
            binding.logcatIndicator.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            binding.cardLogcat.setClickable(false);
            binding.cardLogcat.setAlpha(0.7f);
        } else {
            binding.logcatIndicator.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#808080")));
            binding.cardLogcat.setClickable(true);
            binding.cardLogcat.setAlpha(1.0f);
        }
    }

    private void setupToolbarSubtitle() {
        try {
            String versionName =
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            String buildType = BuildConfig.BUILD_TYPE.toUpperCase();

            int color;
            if (BuildConfig.DEBUG) {
                color = Color.parseColor("#FF5252");
            } else {
                int resId = getResources().getIdentifier("colorPrimary", "attr", getPackageName());

                if (resId == 0) resId = android.R.attr.colorPrimary;

                android.util.TypedValue typedValue = new android.util.TypedValue();
                getTheme().resolveAttribute(resId, typedValue, true);
                color = typedValue.data;
            }

            String hexColor = String.format("#%06X", (0xFFFFFF & color));
            String subtitleText =
                    "V"
                            + versionName
                            + " | <font color='"
                            + hexColor
                            + "'>"
                            + buildType
                            + "</font>";

            binding.toolbar.setSubtitle(Html.fromHtml(subtitleText, Html.FROM_HTML_MODE_LEGACY));
        } catch (Exception e) {
            binding.toolbar.setSubtitle("V1.0 | " + BuildConfig.BUILD_TYPE);
        }
    }

    private void setupClickListeners() {
        binding.statusCard.setOnClickListener(
                v -> {
                    if (Shizuku.getBinder() == null || !Shizuku.pingBinder()) {
                        openShizukuApp();
                    } else {
                        Toast.makeText(this, "Shizuku Aktif!", Toast.LENGTH_SHORT).show();
                    }
                });

        binding.cardDpi.setOnClickListener(
                v -> {
                    if (checkShizuku()) showDpiDialog();
                });
        binding.cardResolution.setOnClickListener(
                v -> {
                    if (checkShizuku()) showResolutionDialog();
                });
        binding.cardRefresh.setOnClickListener(
                v -> {
                    if (checkShizuku()) showRefreshRateMenu();
                });
        binding.cardFps.setOnClickListener(
                v -> {
                    if (checkShizuku()) {
                        if (isServiceRunning(FpsService.class)) {

                            stopService(new Intent(this, FpsService.class));
                            shizukuExec("setprop debug.hwui.profile false");
                            updateFpsUI(false);
                        } else {

                            if (!Settings.canDrawOverlays(this)) {
                                Intent intent =
                                        new Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            } else {
                                startService(new Intent(this, FpsService.class));
                                updateFpsUI(true);
                            }
                        }
                    }
                });
        binding.cardBattery.setOnClickListener(
                v -> {
                    if (checkShizuku()) showBatteryHealthDialog();
                });
        binding.cardPower.setOnClickListener(
                v -> {
                    if (checkShizuku()) showPowerMenu();
                });

        binding.cardLogcat.setOnClickListener(
                v -> {
                    if (checkShizuku()) startFloatingLogcat();
                });
    }

    private void openShizukuApp() {
        PackageManager pm = getPackageManager();
        String[] shizukuPackages = {"dev.rikka.shizuku", "moe.shizuku.privileged.api"};

        for (String pkg : shizukuPackages) {
            Intent intent = pm.getLaunchIntentForPackage(pkg);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            }
        }

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        for (android.content.pm.ResolveInfo info : pm.queryIntentActivities(mainIntent, 0)) {
            String foundPkg = info.activityInfo.packageName;
            if (foundPkg.toLowerCase().contains("shizuku")) {
                Intent intent = pm.getLaunchIntentForPackage(foundPkg);
                if (intent != null) {
                    startActivity(intent);
                    return;
                }
            }
        }

        Toast.makeText(
                        this,
                        "Aplikasi Shizuku tidak terinstall di perangkat ini",
                        Toast.LENGTH_LONG)
                .show();
    }

    private void updateShizukuStatus() {
        if (Shizuku.getBinder() != null && Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                binding.statusTitle.setText(getString(R.string.status_active));
                binding.statusDescription.setText(getString(R.string.status_desc_full));

                binding.statusIndicator.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                binding.statusIcon.setImageResource(R.drawable.ic_info);

                showTechnicalInfo(true);
            } else {
                binding.statusTitle.setText(getString(R.string.permission_required));
                binding.statusDescription.setText(getString(R.string.permission_desc));

                binding.statusIndicator.setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#FFC107")));
                showTechnicalInfo(false);
            }
        } else {
            binding.statusTitle.setText(getString(R.string.status_disconnected));
            binding.statusDescription.setText(getString(R.string.status_desc_wait));

            binding.statusIndicator.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#F44336")));
            binding.statusIcon.setImageResource(R.drawable.ic_warning);
            showTechnicalInfo(false);
        }
    }

    private void showTechnicalInfo(boolean show) {
        if (show) {
            binding.txtShizukuVersion.setText(String.valueOf(Shizuku.getVersion()));
            binding.txtShizukuUid.setText(String.valueOf(Shizuku.getUid()));

            new Thread(
                            () -> {
                                String selinux = getShizukuOutput("getenforce");
                                runOnUiThread(
                                        () -> {
                                            binding.txtSelinux.setText(
                                                    selinux.isEmpty()
                                                            ? "UNKNOWN"
                                                            : selinux.trim().toUpperCase());
                                            binding.technicalInfoArea.setVisibility(View.VISIBLE);
                                        });
                            })
                    .start();
        } else {
            binding.technicalInfoArea.setVisibility(View.GONE);
        }
    }

    private boolean checkShizuku() {
        try {
            if (Shizuku.getBinder() != null && Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    return true;
                } else {
                    Shizuku.requestPermission(100);
                    return false;
                }
            } else {
                showErrorDialog(
                        getString(R.string.shizuku_not_active_title),
                        getString(R.string.shizuku_not_active_msg));
                return false;
            }
        } catch (Exception e) {
            showErrorDialog(
                    getString(R.string.shizuku_error_title),
                    getString(R.string.shizuku_error_msg_format, e.getMessage()));
            return false;
        }
    }

    private void showErrorDialog(String title, String message) {
        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setIcon(R.drawable.ic_warning)
                        .setPositiveButton(
                                getString(R.string.error_open_shizuku),
                                (dialog, which) -> {
                                    openShizukuApp();
                                })
                        .setNegativeButton(getString(R.string.error_close), null);

        AlertDialog dialog = builder.create();

        dialog.show();
        DialogHelper.applyDefaultStyle(dialog);
    }

    private void shizukuExec(String command) {
        try {

            Method newProcessMethod =
                    Shizuku.class.getDeclaredMethod(
                            "newProcess", String[].class, String[].class, String.class);
            newProcessMethod.setAccessible(true);

            String[] cmd = {"sh", "-c", command};
            java.lang.Process process =
                    (java.lang.Process) newProcessMethod.invoke(null, cmd, null, null);

            new Thread(
                            () -> {
                                try {
                                    int exitCode = process.waitFor();
                                    runOnUiThread(
                                            () -> {
                                                if (exitCode == 0) {
                                                    Toast.makeText(
                                                                    this,
                                                                    "Berhasil: " + command,
                                                                    Toast.LENGTH_SHORT)
                                                            .show();
                                                } else {
                                                    Toast.makeText(
                                                                    this,
                                                                    "Gagal! Code: " + exitCode,
                                                                    Toast.LENGTH_SHORT)
                                                            .show();
                                                }
                                            });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            })
                    .start();

        } catch (Exception e) {
            Toast.makeText(this, "Reflection Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showDpiDialog() {
        LayoutDialogDpiBinding dpiBinding = LayoutDialogDpiBinding.inflate(getLayoutInflater());

        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        int currentDpi = metrics.densityDpi;

        dpiBinding.textInputLayoutDpi.setHelperText(
                getString(R.string.dpi_current_helper, currentDpi));

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.dpi_title))
                        .setMessage(getString(R.string.dpi_msg))
                        .setView(dpiBinding.getRoot())
                        .setPositiveButton(getString(R.string.dpi_apply), null)
                        .setNeutralButton(
                                getString(R.string.dpi_reset),
                                (d, w) -> {
                                    shizukuExec("wm density reset");
                                    Toast.makeText(
                                                    this,
                                                    getString(R.string.dpi_reset_toast),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                })
                        .setNegativeButton(getString(R.string.dpi_cancel), null)
                        .create();

        dialog.show();
        DialogHelper.applyDefaultStyle(dialog);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        v -> {
                            String input = dpiBinding.editDpi.getText().toString().trim();

                            if (input.isEmpty()) {
                                dpiBinding.textInputLayoutDpi.setError(
                                        getString(R.string.dpi_error_empty));
                                dpiBinding.textInputLayoutDpi.setHelperTextEnabled(false);
                            } else {
                                try {
                                    int dpi = Integer.parseInt(input);
                                    if (dpi < 72 || dpi > 1000) {
                                        dpiBinding.textInputLayoutDpi.setError(
                                                getString(R.string.dpi_error_range));
                                        dpiBinding.textInputLayoutDpi.setHelperTextEnabled(false);
                                    } else {
                                        dpiBinding.textInputLayoutDpi.setError(null);
                                        dpiBinding.textInputLayoutDpi.setHelperTextEnabled(true);
                                        shizukuExec("wm density " + dpi);
                                        dialog.dismiss();
                                    }
                                } catch (NumberFormatException e) {
                                    dpiBinding.textInputLayoutDpi.setError(
                                            getString(R.string.dpi_error_nan));
                                }
                            }
                        });
    }

    private void showResolutionDialog() {
        LayoutDialogResolutionBinding resBinding =
                LayoutDialogResolutionBinding.inflate(getLayoutInflater());

        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        int realW = dm.widthPixels;
        int realH = dm.heightPixels;

        resBinding.layoutWidth.setHelperText(getString(R.string.res_original_helper, realW));
        resBinding.layoutHeight.setHelperText(getString(R.string.res_original_helper, realH));

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.res_title))
                        .setMessage(getString(R.string.res_msg))
                        .setView(resBinding.getRoot())
                        .setPositiveButton(getString(R.string.res_apply), null)
                        .setNeutralButton(
                                getString(R.string.res_reset),
                                (d, w) -> {
                                    shizukuExec("wm size reset");
                                    Toast.makeText(
                                                    this,
                                                    getString(R.string.res_reset_toast),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                })
                        .setNegativeButton(getString(R.string.res_cancel), null)
                        .create();

        dialog.show();
        DialogHelper.applyDefaultStyle(dialog);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        view -> {
                            String w = resBinding.editWidth.getText().toString().trim();
                            String h = resBinding.editHeight.getText().toString().trim();

                            if (w.isEmpty()) {
                                resBinding.layoutWidth.setError(
                                        getString(R.string.res_error_w_empty));
                            } else if (h.isEmpty()) {
                                resBinding.layoutHeight.setError(
                                        getString(R.string.res_error_h_empty));
                            } else {
                                resBinding.layoutWidth.setError(null);
                                resBinding.layoutHeight.setError(null);

                                shizukuExec("wm size " + w + "x" + h);
                                dialog.dismiss();
                            }
                        });
    }

    private void showRefreshRateMenu() {
        LayoutDialogRefreshRateBinding refreshBinding =
                LayoutDialogRefreshRateBinding.inflate(getLayoutInflater());

        android.view.Display display = getWindowManager().getDefaultDisplay();
        float refreshRate = display.getRefreshRate();

        String formattedHz = String.format("%.0f", refreshRate);
        refreshBinding.txtCurrentRefreshRate.setText(getString(R.string.hz_current, formattedHz));

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.hz_title))
                        .setView(refreshBinding.getRoot())
                        .setPositiveButton(
                                getString(R.string.hz_apply),
                                (d, w) -> {
                                    int checkedId =
                                            refreshBinding.radioGroupRefresh
                                                    .getCheckedRadioButtonId();

                                    if (checkedId == R.id.rb60) {
                                        applyHz("60.0");
                                    } else if (checkedId == R.id.rb90) {
                                        applyHz("90.0");
                                    } else if (checkedId == R.id.rb120) {
                                        applyHz("120.0");
                                    }
                                })
                        .setNeutralButton(
                                getString(R.string.hz_reset),
                                (d, w) -> {
                                    shizukuExec(
                                            "settings delete secure min_refresh_rate && settings delete secure peak_refresh_rate");
                                    Toast.makeText(
                                                    this,
                                                    getString(R.string.hz_reset_toast),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                })
                        .setNegativeButton(getString(R.string.hz_cancel), null)
                        .create();

        dialog.show();
        DialogHelper.applyDefaultStyle(dialog);
    }

    private void applyHz(String hz) {
        String command =
                String.format(
                        "settings put secure min_refresh_rate %1$s && "
                                + "settings put secure peak_refresh_rate %1$s && "
                                + "settings put secure user_refresh_rate %1$s",
                        hz);

        shizukuExec(command);
    }

    private void updateFpsUI(boolean isActive) {
        if (isActive) {
            binding.fpsIndicator.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        } else {
            binding.fpsIndicator.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#808080")));
        }
    }

    private void showBatteryHealthDialog() {
        LayoutDialogBatteryBinding batteryBinding =
                LayoutDialogBatteryBinding.inflate(getLayoutInflater());

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.battery_title))
                        .setView(batteryBinding.getRoot())
                        .setPositiveButton(getString(R.string.battery_done), null)
                        .create();

        dialog.show();
        DialogHelper.applyDefaultStyle(dialog);

        new Thread(
                        () -> {
                            try {
                                String level =
                                        getShizukuOutput(
                                                "cat /sys/class/power_supply/battery/capacity");
                                String status =
                                        getShizukuOutput(
                                                "cat /sys/class/power_supply/battery/status");
                                String cycle =
                                        getShizukuOutput(
                                                "cat /sys/class/power_supply/battery/cycle_count");
                                String temp =
                                        getShizukuOutput(
                                                "cat /sys/class/power_supply/battery/temp");
                                String volt =
                                        getShizukuOutput(
                                                "cat /sys/class/power_supply/battery/voltage_now");

                                String capFull =
                                        getShizukuOutput(
                                                "cat /sys/class/power_supply/battery/charge_full || cat /sys/class/power_supply/battery/charge_full_design");
                                String capDesign =
                                        getShizukuOutput(
                                                "cat /sys/class/power_supply/battery/charge_full_design || cat /sys/class/power_supply/battery/design_capacity");

                                String healthStatus =
                                        getShizukuOutput(
                                                "cat /sys/class/power_supply/battery/health");

                                runOnUiThread(
                                        () -> {
                                            batteryBinding.txtBatteryLevel.setText(
                                                    level.trim() + "%");
                                            batteryBinding.txtBatteryStatus.setText(
                                                    status.trim().toUpperCase());
                                            batteryBinding.txtBatteryCycle.setText(
                                                    getString(
                                                            R.string.battery_cycles_format,
                                                            cycle.trim()));

                                            String finalHealthText =
                                                    getString(
                                                            R.string.battery_health_label,
                                                            healthStatus.trim());

                                            if (!capFull.isEmpty() && !capDesign.isEmpty()) {
                                                try {
                                                    long current = Long.parseLong(capFull.trim());
                                                    long design = Long.parseLong(capDesign.trim());

                                                    if (current > 10000) current /= 1000;
                                                    if (design > 10000) design /= 1000;

                                                    int healthPct =
                                                            (int) ((current * 100) / design);
                                                    if (healthPct > 100) healthPct = 100;

                                                    finalHealthText =
                                                            getString(
                                                                    R.string
                                                                            .battery_health_pct_label,
                                                                    healthStatus.trim(),
                                                                    healthPct);

                                                    batteryBinding.txtBatteryCapacity.setText(
                                                            getString(
                                                                    R.string
                                                                            .battery_capacity_format,
                                                                    current,
                                                                    design));
                                                } catch (Exception e) {
                                                    finalHealthText =
                                                            getString(
                                                                    R.string.battery_health_label,
                                                                    healthStatus.trim());
                                                }
                                            }

                                            batteryBinding.txtBatteryHealth.setText(
                                                    finalHealthText);

                                            if (!temp.isEmpty()) {
                                                float t = Float.parseFloat(temp.trim()) / 10;
                                                batteryBinding.txtBatteryTemp.setText(t + "°C");
                                            }

                                            if (!volt.isEmpty()) {
                                                float vNow =
                                                        Float.parseFloat(volt.trim()) / 1000000;
                                                batteryBinding.txtBatteryVoltage.setText(
                                                        getString(
                                                                R.string.battery_voltage_format,
                                                                String.format("%.2f", vNow)));
                                            }
                                        });
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        })
                .start();
    }

    private void showPowerMenu() {

        LayoutDialogPowerBinding powerBinding =
                LayoutDialogPowerBinding.inflate(getLayoutInflater());

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.power_title))
                        .setView(powerBinding.getRoot())
                        .setNegativeButton(getString(R.string.power_cancel), null)
                        .create();

        dialog.show();
        DialogHelper.applyDefaultStyle(dialog);

        powerBinding.btnReboot.setOnClickListener(
                v -> {
                    shizukuExec("reboot");
                    dialog.dismiss();
                });

        powerBinding.btnRecovery.setOnClickListener(
                v -> {
                    shizukuExec("reboot recovery");
                    dialog.dismiss();
                });

        powerBinding.btnBootloader.setOnClickListener(
                v -> {
                    shizukuExec("reboot bootloader");
                    dialog.dismiss();
                });

        powerBinding.btnShutdown.setOnClickListener(
                v -> {
                    shizukuExec("reboot -p");
                    dialog.dismiss();
                });
    }

    private void startFloatingLogcat() {
        if (!hasUsageStatsPermission()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.logcat_perm_title))
                    .setMessage(getString(R.string.logcat_perm_msg))
                    .setPositiveButton(
                            getString(R.string.logcat_open_settings),
                            (d, w) -> {
                                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                            })
                    .show();
            return;
        }

        showLogcatFilterDialog();
    }

    private void showLogcatFilterDialog() {
        LayoutDialogLogcatBinding logcatBinding =
                LayoutDialogLogcatBinding.inflate(getLayoutInflater());

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.logcat_filter_title))
                        .setMessage(getString(R.string.logcat_filter_msg))
                        .setView(logcatBinding.getRoot())
                        .setPositiveButton(getString(R.string.logcat_start), null)
                        .setNegativeButton(getString(R.string.logcat_cancel), null)
                        .create();

        dialog.show();
        DialogHelper.applyDefaultStyle(dialog);

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(
                        v -> {
                            String pkgName =
                                    logcatBinding.editPackageName.getText().toString().trim();

                            if (pkgName.isEmpty()) {

                                logcatBinding.textInputLayoutLogcat.setError(
                                        getString(R.string.logcat_error_empty));
                            } else {
                                logcatBinding.textInputLayoutLogcat.setError(null);

                                if (Settings.canDrawOverlays(this)) {
                                    Intent intent = new Intent(this, LogcatService.class);
                                    intent.putExtra("package_filter", pkgName);
                                    startService(intent);
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(
                                                    this,
                                                    getString(R.string.logcat_error_overlay),
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                }
                            }
                        });
    }

    private boolean hasUsageStatsPermission() {
        android.app.AppOpsManager appOps =
                (android.app.AppOpsManager) getSystemService(APP_OPS_SERVICE);
        int mode =
                appOps.checkOpNoThrow(
                        android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                        android.os.Process.myUid(),
                        getPackageName());
        return mode == android.app.AppOpsManager.MODE_ALLOWED;
    }

    private String getShizukuOutput(String command) {
        try {
            Method newProcessMethod =
                    Shizuku.class.getDeclaredMethod(
                            "newProcess", String[].class, String[].class, String.class);
            newProcessMethod.setAccessible(true);
            String[] cmd = {"sh", "-c", command};
            java.lang.Process process =
                    (java.lang.Process) newProcessMethod.invoke(null, cmd, null, null);

            java.io.BufferedReader reader =
                    new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor();
            return line != null ? line : "";
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            unregisterReceiver(logcatReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            unregisterReceiver(fpsReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        lastBlackStatus = ThemeHelper.isBlackModeEnabled(this);
        ThemeHelper.applyAmoledMode(this, binding.getRoot(), binding.appBar, binding.toolbar);
        
        updateShizukuStatus();

        try {
            IntentFilter logcatFilter = new IntentFilter("LOGCAT_STATUS_CHANGED");
            IntentFilter fpsFilter = new IntentFilter("FPS_STATUS_CHANGED");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(logcatReceiver, logcatFilter, Context.RECEIVER_EXPORTED);
                registerReceiver(fpsReceiver, fpsFilter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(logcatReceiver, logcatFilter);
                registerReceiver(fpsReceiver, fpsFilter);
            }
        } catch (Exception e) {
        }

        updateFpsUI(isServiceRunning(FpsService.class));
        updateLogcatUI(isServiceRunning(LogcatService.class));
    }
}
