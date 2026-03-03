package com.toolbox.shizutools;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.toolbox.shizutools.databinding.ActivityInfoBinding;

public class InfoActivity extends AppCompatActivity {

    private ActivityInfoBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeHelper.applyLanguage(this);
        super.onCreate(savedInstanceState);

        binding = ActivityInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

        try {
            String versionName =
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            String buildType = BuildConfig.BUILD_TYPE.toUpperCase();

            String fullVersionInfo =
                    getString(R.string.version_format, versionName) + " | " + buildType;

            binding.txtVersionInfo.setText(fullVersionInfo);
        } catch (Exception e) {
            binding.txtVersionInfo.setText(getString(R.string.version_format, "1.0.0"));
        }

        binding.btnDevice.setText(
                getString(R.string.device_model) + ": " + Build.MANUFACTURER + " " + Build.MODEL);
        binding.btnAndroidVer.setText(
                getString(R.string.android_version)
                        + ": "
                        + Build.VERSION.RELEASE
                        + " (API "
                        + Build.VERSION.SDK_INT
                        + ")");

        setupClickListeners();

        applyAmoledLogic();
    }

    private void setupClickListeners() {

        binding.btnCheckUpdate.setOnClickListener(
                v -> {
                    androidx.appcompat.app.AlertDialog dialog =
                            new MaterialAlertDialogBuilder(this)
                                    .setTitle(R.string.update_title)
                                    .setMessage(R.string.update_no_new)
                                    .setPositiveButton("OK", null)
                                    .show();
                    DialogHelper.applyDefaultStyle(dialog);
                });

        binding.btnContactDev.setOnClickListener(
                v -> {
                    String url = "https://t.me/your_username";
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {

                    }
                });
    }

    private void applyAmoledLogic() {
        ThemeHelper.applyAmoledMode(this, binding.getRoot(), binding.appBar, binding.toolbar);
    }
}
